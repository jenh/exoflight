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

import java.util.List;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.spif.*;

/**
  * An abstract capability that represents anything that
  * can perturb a spaceship -- any sort of engine or propulsion device.
  */
public abstract class PerturbationCapability
extends PeriodicCapability
{
	private boolean armed;
	protected Perturbation thrustperturb;

	//

	public PerturbationCapability(Module module)
	{
		super(module);
	}

	//

	protected abstract Perturbation getRocketPerturbation();

	public abstract boolean isBlocked();

	//

	public boolean getArmed()
	{
		return armed;
	}

	public boolean isArmed()
	{
		return armed;
	}

	public void setArmed(boolean armed)
	{
		if (this.armed != armed)
		{
			this.armed = armed;
			if (!armed)
				deactivate();
		}
	}

	public boolean activate()
	{
		if (!isArmed())
		{
			getShip().getShipWarningSystem().setWarning("PROP-NOTARMED", getName() + " not armed");
			return false;
		}
		if (isBlocked())
		{
			getShip().getShipWarningSystem().setWarning("PROP-BLOCKED", getName() + " is blocked");
			return false;
		}
		return super.activate();
	}

	protected void addPerturbation()
	{
		if (thrustperturb != null)
			return;
		thrustperturb = getRocketPerturbation();
		if (thrustperturb != null)
		{
			((MutableTrajectory)getThing().getTrajectory()).addPerturbation(thrustperturb);
			if (debug)
				System.out.println("added perturbation " + thrustperturb);
		}
	}

	protected void removePerturbation()
	{
		if (thrustperturb != null)
		{
			((MutableTrajectory)getThing().getTrajectory()).removePerturbation(thrustperturb);
			if (debug)
				System.out.println("removed perturbation " + thrustperturb);
			thrustperturb = null;
		}
	}

	public boolean notifyDeactivated()
	{
		removePerturbation();
		return true;
	}

	protected void addStringAttrs(List l)
	{
		super.addStringAttrs(l);
		if (isArmed())
			l.add("armed");
	}

	static boolean debug = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(PerturbationCapability.class);

	static {
		prophelp.registerGetSet("armed", "Armed", boolean.class);
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
