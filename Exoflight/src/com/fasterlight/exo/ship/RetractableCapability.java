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

import com.fasterlight.spif.PropertyHelper;
import com.fasterlight.util.Util;

/**
  * A capability that can be extended/retracted,
  * like flaps, gear, doors, etc.
  * It has an option to modify one of the aerosurfaces
  * depending on the value of the thingie.
  */
public class RetractableCapability
extends Capability
{
	protected float period; // time to extend full range (ticks) 0 if instant
	protected int detents; // # of indentions (>= 0)

	protected float v0; // value at t0
	protected float v1; // value at t0+tdur
	protected long t0;
	protected int tdur;

	protected AeroSurface aerosurf;

	//

	public RetractableCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		period = Util.parseFloat(props.getProperty("period", "0"));
		detents = Integer.parseInt(props.getProperty("detents", "0"))-1;
		if (detents < 1)
			detents = 0;

		// now modify an aerosurface
		AeroSurface baseas = null;
		Module mod = getModule();
		String surfname = props.getProperty("surface", "");
		if (surfname.equals("fore"))
			baseas = mod.getForeSurface();
		else if (surfname.equals("obliq"))
			baseas = mod.getObliqueSurface();
		else if (surfname.equals("aft"))
			baseas = mod.getAftSurface();

		if (baseas != null)
		{
			baseas.linked_cap = this;
			baseas.time_drag_curve = mod.loadCurve("ModCurves", props.getProperty("drag_scale", ""));
			baseas.time_lift_curve = mod.loadCurve("ModCurves", props.getProperty("lift_scale", ""));
			baseas.time_aa_curve = mod.loadCurve("ModCurves", props.getProperty("aa_scale", ""));
		} else
			System.out.println("Error: surface " + surfname + " not found");
	}

	public float getValue()
	{
		return getValue( getGame().time() );
	}

	public float getValue(long t)
	{
		float x;
		if (t <= t0 || tdur == 0)
			x = v0;
		else if (t >= t0+tdur)
			x = v1;
		else
			x = v0 + (t-t0)*(v1-v0)/tdur;
		return x;
	}

	/**
	  * Sets the target value of this capability to be the closest
	  * detent to 'val'.
	  */
	public void setValue(float val)
	{
		float curvalue = getValue();
		float newvalue = (detents > 0) ? (float)(Math.round(val*detents)/detents) : val;
		v0 = curvalue;
		v1 = newvalue;
		t0 = getGame().time();
		tdur = (int)(Math.abs(v1-v0)*period*TICKS_PER_SEC)+1;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(RetractableCapability.class);

	static {
		prophelp.registerGetSet("value", "Value", float.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	public static boolean debug = false;

}
