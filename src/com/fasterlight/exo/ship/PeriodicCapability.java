/********************************************************************
    Copyright (c) 2000-2008 Steven E. Hugg.

    This file is part of Exoflight.

    Exoflight is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Exoflight is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Exoflight.  If not, see <http://www.gnu.org/licenses/>.
*********************************************************************/
package com.fasterlight.exo.ship;

import java.util.*;

import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Util;

/**
  * A PeriodicCapability consumes resources at given intervals,
  * performing some action during each consume event.
  * It also generates products, which need to be stored in the
  * capability itself (coming soon: resource sinks)
  */
public class PeriodicCapability
extends Capability
{
	private transient int default_interval;

	private int interval;
	private ResourceSet reactants = new ResourceSet();
	private ResourceSet reactrate = new ResourceSet();
	private ResourceSet products = new ResourceSet();

	private PeriodEvent periodevent;

	private long activate_time;
	private boolean activated;
	private boolean running;

	//

	public PeriodicCapability(Module module)
	{
		super(module);
	}

	protected ResourceSet getMaximumReactants()
	{
		return reactants;
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		default_interval = interval =
			(int)(Util.parseFloat(props.getProperty("interval", "1"))*TICKS_PER_SEC);

		reactants.set(props.getProperty("reactants", "-"));
		reactants.scale(interval*1f/TICKS_PER_HOUR); // convert from per-hr to per-interval
		reactants = new ImmutableResourceSet(reactants);
		products.set(props.getProperty("products", "-"));
		products.scale(interval*1f/TICKS_PER_HOUR);
		products = new ImmutableResourceSet(products);
		reactrate.set(reactants);
		reactrate.scale(TICKS_PER_SEC/interval);
		reactrate = new ImmutableResourceSet(reactrate);
	}

	public int getInterval()
	{
		return interval;
	}

	public float getIntervalFloat()
	{
		return (interval*1f/TICKS_PER_SEC);
	}

	public ResourceSet getReactants()
	{
		return reactants;
	}

	public ResourceSet getAverageReactRate()
	{
		return reactrate;
	}

	public ResourceSet getProducts()
	{
		return products;
	}

	public long getActivateTime()
	{
		return activate_time;
	}

	public boolean activate()
	{
		if (isActive())
			return false;
		if (periodevent == null)
		{
			Game game = getGame();
			long t = Game.quantize(game.time(), getInterval());
			periodevent = new PeriodEvent(t);
			game.postEvent(periodevent);
		}
		activated = true;
		return true;
	}

	/**
	  * Called before the first doPeriodic() is called
	  * returns true if OK to continue
	  * you might wanna override this
	  */
	protected boolean notifyActivated()
	{
		return true;
	}

	/**
	  * Called when deactivate() is called, and only if
	  * at least one event has fired (that is, notifyActivated()
	  * has been called and has returned true)
	  * you might wanna override this
	  */
	protected boolean notifyDeactivated()
	{
		return true;
	}

	public final boolean deactivate()
	{
		if (!isActive())
			return false;
		activated = false;
		return true;
	}

	public boolean isRunning()
	{
		return running;
	}

	public void shutdown()
	{
		deactivate();
		if (periodevent != null)
		{
			getGame().cancelEvent(periodevent);
			periodevent = null;
		}

		running = false;
		activate_time = Game.INVALID_TICK;
		notifyDeactivated();
	}

	public boolean isActive()
	{
		return activated;
	}

	public void setActive(boolean b)
	{
		if (b)
			activate();
		else
			deactivate();
	}

	public boolean doPeriodic()
	{
		// if first time called, call notifyActivated()
		// if it returns false, don't continue, otherwise
		// set activate_time var
		if (!running)
		{
			if (!notifyActivated())
				return false;
			running = true;
			activate_time = getGame().time();
		}
		// make sure we have enuff room for the products
		ResourceSet prod = getProducts();
		if (prod.mag() != 0)
		{
			ResourceSet sup = new ResourceSet(getCapacity());
			sup.sub(getSupply());
			sup.sub(prod);
			if (sup.getMinAmount() < 0)
			{
				notifyCapacityFull();
				return true; // no room, but don't deactivate yet
			}
		}
		// we're ok, do the reaction
		// nextver: reaction types besides all or none?
		ResourceSet reac = request(getReactants(), ALL_OR_NONE);
		if (reac.mag() > 0)
		{
			addSupply(prod); // nextver: sinks?
			doReact(reac, prod);
			return true;
		} else {
			notifyNoSupply();
			running = false;
			activate_time = Game.INVALID_TICK;
			notifyDeactivated();
			return false;
		}
	}

	protected void notifyCapacityFull()
	{
		// override me
	}

	protected void notifyNoSupply()
	{
		// override me
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		// override me
	}

	//

	protected void addStringAttrs(List l)
	{
		super.addStringAttrs(l);
		if (isRunning())
			l.add("active");
	}

	//

	public class PeriodEvent
	extends GameEvent
	{
		PeriodEvent(long time)
		{
			super(time);
		}
		public void handleEvent(Game game)
		{
			if (activated && doPeriodic())
			{
				eventtime += interval;
				game.postEvent(this);
			} else {
				periodevent = null;
				activated = false;
				running = false;
				notifyDeactivated();
			}
		}
		public String toString()
		{
			return "PeriodEvent: " + getModule() + " " + PeriodicCapability.this;
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(PeriodicCapability.class);

	static {
		prophelp.registerGet("interval", "getInterval");
		prophelp.registerGet("reactants", "getReactants");
		prophelp.registerGet("products", "getProducts");
		prophelp.registerGet("active", "isActive");
		prophelp.registerSet("active", "setActive", boolean.class);
		prophelp.registerGet("running", "isRunning");
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try {
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}

}
