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
  * This guidance program performs a gravity turn,
  * keeping air velocity and thrust velocity vectors
  * the same.  It can also add an angle of attack
  * (pitch, yaw, and roll, if you like) to the resulting
  * vector.
  */
public class AngleOfAttackProgram
extends PYRProgram
{
	private boolean useAirVector = true;

	//

	public Vector3d getVelocityVector(Telemetry telem)
	{
		return telem.getVelocityVec();
	}

	public Vector3d getAirVector(Telemetry telem)
	{
		return telem.getAirVelocityVector();
	}

	// todo: doesn't call super.compute, so
	// doincl does nothing
	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		Telemetry telem = ship.getTelemetry();
		Vector3d airvel = new Vector3d( getVelocityVector(telem) );
		if (useAirVector)
			airvel.sub( getAirVector(telem) );
		Vector3d up = new Vector3d( telem.getCenDistVec() );
		Orientation ort = new Orientation(airvel, up);

		// multiply by PYR
		Orientation pyrort = new Orientation();
		pyrort.setEulerPYR(pyr);
		pyrort.concat(ort);

		// if doincl, we follow the plane of the orbit
		if (doincl)
		{
			// convert to planet-centric frame
			Orientation planetort = telem.getPlanetRefOrientation();
			pyrort.concatInverse(planetort);

			// get normal of target plane
			Vector3d targnml = ship.getShipLaunchSystem().getTargetPlaneNormal();
			// todo: what if ascending??
			// cross with position, get direction
			targnml.cross(telem.getCenDistVec(), targnml);
			// transfer to SEZ frame
			Orientation targort = new Orientation(targnml, telem.getCenDistVec());

			targort.concatInverse(planetort);
			double azimuth = targort.getYaw();

			pyrort.setYaw(azimuth);
			pyrort.concat(planetort);
		}

		attctrl.setTargetOrientation(pyrort);
	}

	public boolean getUseAirVector()
	{
		return useAirVector;
	}

	public void setUseAirVector(boolean useAirVector)
	{
		this.useAirVector = useAirVector;
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(AngleOfAttackProgram.class);

	static {
		prophelp.registerGetSet("useairvector", "UseAirVector", boolean.class);
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
