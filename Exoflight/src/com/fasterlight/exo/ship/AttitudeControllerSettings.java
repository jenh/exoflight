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

import java.util.StringTokenizer;

import com.fasterlight.spif.*;
import com.fasterlight.util.Util;

/**
  * Settings for AttitudeController
  * Defines rate & deviation deadbands, and maximum rate
  * @see AttitudeController
  */
public class AttitudeControllerSettings
implements PropertyAware
{
	float maxrate; // max angular velocity allowed by our RCS (2x actual value)
	float rateband; // rate deadband (2x actual value)
	float devband; // dead zone angle (2x actual value)
	float deadrate = 3e-5f; // when close to deadband, go to this rate

	public AttitudeControllerSettings()
	{
		this((float)Util.toRadians(4), (float)Util.toRadians(4), (float)Util.toRadians(4));
	}

	public AttitudeControllerSettings(float maxrate, float rateband, float devband)
	{
		this.maxrate = maxrate;
		this.rateband = rateband;
		this.devband = devband;
	}

	public AttitudeControllerSettings(String s)
	{
		try {
			StringTokenizer st = new StringTokenizer(s, ",");
			setMaxRate( (float)Util.toRadians(Util.parseDouble(st.nextToken())) );
			setRateDeadband( (float)Util.toRadians(Util.parseDouble(st.nextToken())) );
			setAngleDeadband( (float)Util.toRadians(Util.parseDouble(st.nextToken())) );
		} catch (Exception ex) {
			throw new RuntimeException("Invalid guidance settings format: " + s);
		}
	}

	public float getMaxRate()
	{
		return maxrate;
	}

	public void setMaxRate(float maxrate)
	{
		this.maxrate = maxrate;
	}

	public float getRateDeadband()
	{
		return rateband;
	}

	public void setRateDeadband(float rateband)
	{
		this.rateband = rateband;
	}

	public float getAngleDeadband()
	{
		return devband;
	}

	public void setAngleDeadband(float devband)
	{
		this.devband = devband;
	}

	public float getDeadrate()
	{
		return devband;
	}

	public void setDeadrate(float devband)
	{
		this.devband = devband;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(AttitudeControllerSettings.class);

	static {
		prophelp.registerGetSet("maxrate", "MaxRate", float.class);
		prophelp.registerGetSet("rate_deadband", "RateDeadband", float.class);
		prophelp.registerGetSet("ang_deadband", "AngleDeadband", float.class);
		prophelp.registerGetSet("deadrate", "Deadrate", float.class);
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

