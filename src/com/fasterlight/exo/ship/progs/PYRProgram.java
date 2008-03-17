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
  * A guidance program that maintains a specific pitch, yaw, & roll
  * with respect to the planetary frame.
  */
public class PYRProgram
implements GuidanceProgram, Constants, PropertyAware
{
	protected Vector3d pyr = new Vector3d();
	protected boolean doincl;

	//

	public PYRProgram()
	{
	}

	public PYRProgram(Vector3d pyr)
	{
		setPYR(pyr);
	}

	public void setPYR(Vector3d pyr)
	{
		this.pyr.set(pyr);
	}

	public void setDoIncl(boolean b)
	{
		this.doincl = b;
	}

	public boolean getDoIncl()
	{
		return doincl;
	}

	public double getPitch()
	{
		return pyr.x;
	}

	public void setPitch(double x)
	{
		this.pyr.x = x;
	}

	public double getYaw()
	{
		return pyr.y;
	}

	public void setYaw(double y)
	{
		this.pyr.y = y;
	}

	public double getRoll()
	{
		return pyr.z;
	}

	public void setRoll(double z)
	{
		this.pyr.z = z;
	}

	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		Telemetry telem = ship.getTelemetry();

		// if using inclination, set yaw
		// todo: make azimuth depend on position, like before
		if (doincl)
		{
			double azimuth = ship.getShipLaunchSystem().getAzimuth();
			if (Double.isNaN(azimuth))
			{
				azimuth = Math.PI/2;
				System.out.println("***NaN computing azimuth");
			}
			pyr.y = azimuth;
		}

		// convert pyr coords to planet-reference frame
		Orientation planetort = telem.getPlanetRefOrientation();
		Orientation pyrort = new Orientation();
		pyrort.setEulerPYR(pyr);
		pyrort.concat(planetort);
		attctrl.setTargetOrientation(pyrort);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(PYRProgram.class);

	static {
		prophelp.registerGetSet("pitch", "Pitch", double.class);
		prophelp.registerGetSet("yaw", "Yaw", double.class);
		prophelp.registerGetSet("roll", "Roll", double.class);
		prophelp.registerGetSet("doincl", "DoIncl", boolean.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
