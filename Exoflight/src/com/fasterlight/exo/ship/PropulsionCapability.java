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

import com.fasterlight.spif.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.Vector3d;

/**
  * Abstract class for a capability that consumes and exhausts
  * mass to provide propulsion.
  */
public abstract class PropulsionCapability
extends PerturbationCapability
{
	protected transient float exhaustv;
	// exhmass is per-interval
	// exhmass1 is per-second
	protected transient float maxexhrate, exhmass, exhmass1;
	/**
	  * True if thrust vector affects attitude
	  */
	protected transient boolean vectoredThrustAffectsAttitude;

	//

	public PropulsionCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		exhaustv = Util.parseFloat(props.getProperty("exhaustv", "0"));
		maxexhrate = getMaximumReactants().mass()/getIntervalFloat();
		vectoredThrustAffectsAttitude = "true".equals(props.getProperty("vectoratt", "false"));
	}

	//

	public float getConsumeMassPerSec()
	{
		return exhmass1;
	}

	public float getExhaustVel()
	{
		return exhaustv;
	}

	// returns true if there is a net thrust along one of the axes
	// only valid when isRunning()==true
	public boolean isTranslating()
	{
		return true;
	}

	public boolean notifyDeactivated()
	{
		if (super.notifyDeactivated())
		{
			getStructure().setMassFlowRate(this, 0, 0, null);
			return true;
		} else
			return false;
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		float m = react.mass();
		exhmass1 = m/getIntervalFloat();
		exhmass = m;
		updateMassFlowRate();
	}

	protected void updateMassFlowRate()
	{
		Vector3d tvec = vectoredThrustAffectsAttitude ? getThrustVector() : new Vector3d(0,0,1);
		getStructure().setMassFlowRate(this, exhmass1, exhaustv, tvec);
	}

	/**
	  * Returns the current normalized thrust vector, in structure coordinates.
	  * Resulting object should be passable to another routine (that is,
	  * don't store it anywhere, it should be new)
	  */
	public Vector3d getThrustVector()
	{
		return new Vector3d(0,0,1); // +Z
	}

	/**
	  * Return the total acceleration possible at this weight
	  * and at this throttle setting
	  */
	public double getCurrentForce()
	{
		if (!isRunning())
			return 0;
		return getExhaustVel()*exhmass1;
	}

	/**
	  * Return the nominal (100% throttle) force
	  * possible at this weight
	  */
	public double getNominalForce()
	{
		if (!isArmed())
			return 0;
		return getExhaustVel()*maxexhrate;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(PropulsionCapability.class);

	static {
		prophelp.registerGet("exhaustvel", "getExhaustVel");
		prophelp.registerGet("consumerate", "getConsumeMassPerSec");
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
