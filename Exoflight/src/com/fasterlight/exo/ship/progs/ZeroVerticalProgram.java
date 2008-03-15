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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * This guidance program tries to keep vertical velocity constant.
  */
public class ZeroVerticalProgram
extends PYRProgram
implements GuidanceProgram, Constants
{
	double targvvel = 0;

	//

	public ZeroVerticalProgram()
	{
		super();
	}

	public ZeroVerticalProgram(double yaw, double roll)
	{
		super(new Vector3d(0, yaw, roll));
	}

	public double getTargetVertVel()
	{
		return targvvel;
	}

	public void setTargetVertVel(double targvvel)
	{
		this.targvvel = targvvel;
	}

	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		DefaultTelemetry telem = (DefaultTelemetry)ship.getTelemetry();

		double dv = ship.getTotalAccel();
		if (dv <= 0)
		{
			ship.getShipWarningSystem().setWarning("GUID-ZEROVERT", "Overflow in zero vertical program");
			return;
		}

		double r0l = telem.getCENDIST();
		double vvel = telem.getVERTVEL();
		double tvel = telem.getTANGVEL();
		double EU = ship.getParent().getMass()*GRAV_CONST_KM;
		double Ediff = (EU/r0l-tvel*tvel)/r0l;
		double targdv = Ediff/dv - (vvel-targvvel);
		double minaa = Math.PI/2 - Math.acos(targdv);

		if (Double.isNaN(minaa))
			return;

//		System.out.println(dv + "\t" + r0l + "\t" + tvel + "\t" + Ediff);

		// set pitch
		pyr.x = minaa;

		// call super to set orientation
		super.compute(attctrl);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ZeroVerticalProgram.class);

	static {
		prophelp.registerGetSet("targvvel", "TargetVertVel", double.class);
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
