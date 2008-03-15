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
package com.fasterlight.exo.ship.progs;

import com.fasterlight.exo.ship.*;
import com.fasterlight.spif.*;

/**
  * Implements roll reentry, similar to the Apollo and Gemini
  * reentry programs.
  */
public class RollReentryProgram
extends AngleOfAttackProgram
{
	double dthresh = 6;
	double xthresh = 2;

	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();

		// make sure there is a target
		if (ship.getShipTargetingSystem().getTarget() == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for reentry program");
			return;
		}

		// get range & cross error
		double derr = ship.getShipReentrySystem().getDownrangeError();
		double xerr = ship.getShipReentrySystem().getCrossrangeError();

		if (Math.abs(derr) > Math.abs(xerr))
		{
			// if we are coming in short (derr < -dthresh),
			// roll upside down for positive lift
			if (derr < 0)
			{
				setRoll(Math.PI);
			}
			// if we are coming in long (derr > dthresh),
			// roll rightside up for negative lift
			else
			{
				setRoll(0);
			}
		} else {
			// if we are too far left (xerr < -xthresh),
			// roll left
			if (xerr < 0)
			{
				setRoll(-Math.PI/2);
			}
			// if we are too far right (xerr > xthresh),
			// roll right
			else
			{
				setRoll(Math.PI/2);
			}
		}

		super.compute(attctrl);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(RollReentryProgram.class);

	static {
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
