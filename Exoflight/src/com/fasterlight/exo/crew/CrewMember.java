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
package com.fasterlight.exo.crew;

import java.util.*;

import com.fasterlight.exo.orbit.Constants;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;

/**
  * Simulates a bag of water & salt that we call a "person".
  * Highly trained in the skills of astronautics.
  *
  * A person consumes per day:
  * oxygen: 0.84 kg
  * food: 0.62 kg
  * water: 3.32 kg
  * (sanitation water): 24 kg
  *
  * emits per day:
  * CO2: 1 kg
  * water vapor: 2.28 kg
  * urine: 1.50 kg
  */
public class CrewMember
implements java.io.Serializable, Constants, PropertyAware, Comparable
{
	protected Game game;
	protected SpaceBase base;
	protected Module module;
	protected String name;
	protected float basemass = DEFAULT_MASS;
	protected float age = DEFAULT_AGE;

	public static final int STATE_OK = 0;
	public static final int STATE_PASSEDOUT = 1;
	public static final int STATE_INCAPACITATED = 2;
	public static final int STATE_EXPIRED = 3;

	public static final String[] STATE_DESC = {
		"ok", "passed out", "incapacitated"
	};

	protected byte state = STATE_OK;

	static final float DEFAULT_MASS = 72;
	static final float DEFAULT_AGE = 30;

	protected ResourceSet contents = new ResourceSet("food:10,water:10");

	protected static final int update_interval = 15; // nextver: dynamic

	protected static final float AVG_BASE_MASS = 52.0f;

	protected static final float O2_PER_DAY = 0.84f;
	protected static final float FOOD_PER_DAY = 0.62f;
	protected static final float WATER_PER_DAY = 3.32f;

	protected static final float CO2_PER_DAY = 1.00f;
	protected static final float VAPOR_PER_DAY = 2.28f;
	protected static final float URINE_PER_DAY = 1.50f;

	protected static final float CO2_RATIO_A = 0.005f;
	protected static final float CO2_RATIO_B = 0.08f;

	protected List events = new ArrayList();

	static final Resource RES_O2 = Resource.getResourceByName("O2");
	static final Resource RES_CO2 = Resource.getResourceByName("CO2");
	static final Resource RES_water = Resource.getResourceByName("water");
	static final Resource RES_food = Resource.getResourceByName("food");
	static final Resource RES_urine = Resource.getResourceByName("urine");

	static final float MIN_G_FORCE = (float)(1*EARTH_G);
	static final float PASSOUT_G_FORCE = (float)(12*EARTH_G);
	static final float INCAP_G_FORCE = (float)(25*EARTH_G);

	//

	public CrewMember()
	{
		name = hashCode()+"";
	}

	public void setBase(SpaceBase base)
	{
		// crew is deactivated (resting) while at base
		deactivate();
		this.base = base;
		if (module != null)
		{
			module.removeCrewMember(this);
			module = null;
		}
		this.game = null;
	}

	public SpaceBase getBase()
	{
		return base;
	}

	public void setModule(Module module)
	{
		if (module == this.module)
			return;
		Module oldmodule = this.module;
		deactivate();
		if (module != null)
		{
			// time to start breathing, pal
			module.addCrewMember(this);
			activate(module.getGame());
		}
		if (oldmodule != null)
		{
			oldmodule.removeCrewMember(this);
		}
		this.module = module;
		this.base = null;
	}

	public Module getModule()
	{
		return module;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public float getMass()
	{
		return basemass + contents.mass();
	}

	public void setMass(float mass)
	{
		this.basemass = mass - contents.mass();
	}

	public float getAge()
	{
		return age;
	}

	public void setAge(float age)
	{
		this.age = age;
	}

	public float getMetabolicFactor()
	{
		return basemass*(update_interval/(86400*AVG_BASE_MASS));
	}

	//

	protected float consume(Resource res, float amt)
	{
		return contents.consume(res, amt, true);
	}

	public ResourceSet getContents()
	{
		return contents;
	}

	//

	protected void addEvent(GameEvent event)
	{
		events.add(event);
		game.postEvent(event);
	}

	protected void removeEvent(GameEvent event)
	{
		events.remove(event);
	}

	void activate(Game game)
	{
		if (!isAlive())
			return;
		this.game = game;
		addEvent(new CrewUpdate(
			Game.quantize(game.time(), update_interval*TICKS_PER_SEC)
		));
	}

	void deactivate()
	{
		if (game == null)
			return;
		Iterator it = events.iterator();
		while (it.hasNext())
		{
			GameEvent event = (GameEvent)it.next();
			game.cancelEvent(event);
		}
		game = null;
	}

	private void breathe()
	{
		ResourceSet atmo = module.getAtmosphere();
		float pressure = module.getPressure();
		float factor = getMetabolicFactor();
		float atmomag = atmo.mag();

		// get percentage of O2
		float o2ratio = atmo.getAmountOf(RES_O2)/atmomag + 0.001f;
		// get minimum pressure at ratio
		float desired_pres = 12.200f + 15.456f/o2ratio;

		// if co2 level is higher than desired, reduce factor
		float co2ratio = atmo.getAmountOf(RES_CO2)/atmomag;
		if (co2ratio > CO2_RATIO_B)
		{
			warning("LIFE-CO2", getName() + " has CO2 poisoning!");
			setState(STATE_INCAPACITATED);
			return;
		}
		else if (co2ratio > CO2_RATIO_A)
		{
			warning("LIFE-CO2WARNING", getName() + " has a headache!");
		}

		// now if pressure < minimum, reduce factor accordingly
		if (pressure < desired_pres)
		{
			factor *= (pressure/desired_pres);
			warning("LIFE-O2", getName() + " has hypoxia!");
			setState(state+1);
		}

		// take O2 out of atmosphere (inhale)
		float o2wanted = O2_PER_DAY*factor;
		float o2got = module.consumeAtmosphere(RES_O2, o2wanted, true);

		if (o2got == 0)
		{
			warning("LIFE-O2", getName() + " can't breathe!!");
			setState(state+1);
			if (state == STATE_INCAPACITATED)
				return;
		}

		// now exhale! (add CO2)
		// gotta consume food to make up C component of CO2
		if (consume(RES_food, FOOD_PER_DAY*factor) == 0)
		{
			warning("LIFE-FOOD", getName() + " is starving!!");
			setState(STATE_INCAPACITATED);
			return;
		}
		// and consume water
		if (consume(RES_water, WATER_PER_DAY*factor) == 0)
		{
			warning("LIFE-H2O", getName() + " is thirsty!!");
			setState(STATE_INCAPACITATED);
			return;
		}

		// produce urine
		contents.add(new ResourceSet(RES_urine,
			(URINE_PER_DAY+VAPOR_PER_DAY)*factor));

		// exhale CO2
		ResourceSet co2out = new ResourceSet();
		float co2wanted = CO2_PER_DAY*factor;
		co2out.setAmount(RES_CO2, co2wanted);
		module.addAtmosphere(co2out);
	}

	private void warning(String id, String msg)
	{
		if (module != null && module.getShip() != null)
		{
			module.getShip().getShipWarningSystem().setWarning(id, msg);
		}
	}

	private void updateMeters()
	{
	}

	protected void crewUpdate()
	{
		breathe();
		updateMeters();
	}

	public void notifyAccel(float alen)
	{
		if (!DO_CREW)
			return;

		if (alen < MIN_G_FORCE)
			return;

		if (alen > INCAP_G_FORCE && state < STATE_INCAPACITATED)
		{
			warning("LIFE-G-INCAP", getName() + " pulled " + (int)(alen/EARTH_G) +
				" G's and has been incapacitated!");
			setState(STATE_INCAPACITATED);
		}
		else if (alen > PASSOUT_G_FORCE && state < STATE_PASSEDOUT)
		{
			warning("LIFE-G-PASSOUT", getName() + " has passed out!");
			setState(STATE_PASSEDOUT);
		}
	}

	public void setState(int newstate)
	{
		if (!isAlive())
			return;
		if (state != newstate)
		{
			switch (newstate) {
				case STATE_INCAPACITATED:
					state = (byte)newstate;
					deactivate();
					break;
				case STATE_OK:
				case STATE_PASSEDOUT:
					state = (byte)newstate;
					break;
			}
		}
	}

	public boolean isAlive()
	{
		return state != STATE_EXPIRED;
	}

	//

	class CrewUpdate
	extends GameEvent
	{
		public CrewUpdate(long time)
		{
			super(time);
		}
		public void handleEvent(Game game)
		{
			if (!DO_CREW)
				return;
			if (!isAlive())
				return;
			crewUpdate();
			if (module != null) {
				eventtime += update_interval*TICKS_PER_SEC;
				game.postEvent(this);
			}
		}
	}

	//

	public String toString()
	{
		return name + " (" + STATE_DESC[state] + ")";
	}

	public int compareTo(Object o)
	{
		return getName().compareTo( ((CrewMember)o).getName() );
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(CrewMember.class);

	static {
		prophelp.registerGetSet("base", "Base", SpaceBase.class);
		prophelp.registerGetSet("module", "Module", Module.class);
		prophelp.registerGetSet("name", "Name", String.class);
		prophelp.registerGetSet("mass", "Mass", float.class);
		prophelp.registerGetSet("age", "Age", float.class);
		prophelp.registerGet("contents", "getContents");
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	// SETTINGS

	static boolean DO_CREW;

	static SettingsGroup settings = new SettingsGroup(CrewMember.class, "Crew")
	{
		public void updateSettings()
		{
			DO_CREW = getBoolean("DoCrew", true);
		}
	};

}
