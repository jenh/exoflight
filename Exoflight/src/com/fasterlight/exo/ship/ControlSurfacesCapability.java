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

import java.util.Properties;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.*;

/**
  * An AttitudeControlComponent that uses the atmosphere to
  * control the attitude of the spacecraft.
  * todo: specific AA of operation for each
  * todo: dont cut & paste with GimbalCapability!
  */
public class ControlSurfacesCapability
extends PerturbationCapability
implements AttitudeControlComponent
{
	/**
	  * The amount of torque per BC (area*modifier*cos(windangle))
	  * units are N*km
	  */
	Vector3f axes_max_force = new Vector3f();

	Vector3f cmd_strength = new Vector3f();
	Vector3f cur_strength = new Vector3f();

	float scale_factor = 1.5f; // to avoid divergence

	//

	public ControlSurfacesCapability(Module module)
	{
		super(module);
	}

	public void initialize2(Properties props)
	{
		super.initialize2(props);

		axes_max_force.set(AstroUtil.parseVector(props.getProperty("axesforce", "0,0,0")));
		scale_factor = Util.parseFloat(props.getProperty("hysterisis", ""+scale_factor));
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		super.doReact(react, product);

		getShip().refreshTrajectory();
		cur_strength.set(cmd_strength);

		// scale, and set fore AeroSurface too
		AeroSurface aeros = getModule().getForeSurface();
		Vector3f torque = new Vector3f();
		if (getTorque(torque, true))
		{
			torque.x *= cur_strength.x;
			torque.y *= cur_strength.y;
			torque.z *= cur_strength.z;
			aeros.control_surfaces_torque = torque;
if (debug)
	System.out.println("set torque: " + torque);
		} else
			aeros.control_surfaces_torque = null;
	}

	public boolean notifyDeactivated()
	{
		// turn off the aero surfaces
		AeroSurface aeros = getModule().getForeSurface();
		aeros.control_surfaces_torque = null;
		return true;
	}


	//

	public void getMaxMoment(Vector3f moment)
	{
		getMoment(moment, true);
	}

	public void getMinMoment(Vector3f moment)
	{
		getMoment(moment, false);
	}

	public void setStrength(Vector3f strength)
	{
		strength.clamp(-1,1);
		if (strength.lengthSquared() > 1e-5) {
			cmd_strength.set(strength);
			setActive(true);
		} else {
			strength.set(0,0,0);
			cmd_strength.set(strength);
			setActive(false);
		}
	}

	private boolean getTorque(Vector3f torque, boolean max)
	{
		torque.set(0,0,0);
		if (!isArmed())
			return false;

		torque.set(axes_max_force);

		return true;
	}

	private void getMoment(Vector3f moment, boolean max)
	{
		if (!getTorque(moment, max))
			return; // moment is set to (0,0,0) in getTorque()

		// by getting the last drag coeff, we can compute torque

		float lastBC = getModule().getForeSurface().last_control_bc2;
		double Q = ((SpaceShipTelemetry)getShip().getTelemetry()).getDYNPRES();
		moment.scale((float)(lastBC*Q*scale_factor));
if (debug)
	System.out.println("lastBC=" + lastBC + " Q=" + Q);
		if (lastBC == 0)
			return;

		double m = getThing().getMass();
	 	Vector3d inertv = getThing().getStructure().getInertiaVector();
	 	moment.x = (float)(moment.x/(inertv.x*m));
	 	moment.y = (float)(moment.y/(inertv.y*m));
	 	moment.z = (float)(moment.z/(inertv.z*m));
	}

	protected Perturbation getRocketPerturbation()
	{
		// we don't have one!
		// so really we shouldn't subclass this...
		// but it's convenient...
		return null;
	}

	public boolean isBlocked()
	{
		// we're never blocked
		return false;
	}

	//

	static boolean debug = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ControlSurfacesCapability.class);

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
