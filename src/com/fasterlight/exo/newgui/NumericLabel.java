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
package com.fasterlight.exo.newgui;

import java.text.*;

import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.glout.GLOLabel;
import com.fasterlight.spif.*;
import com.fasterlight.util.UnitConverter;

/**
  * A label extended with units capability.
  */
public class NumericLabel
extends GLOLabel
{
	protected String units, format;
	protected NumberFormat nfmt;
	protected boolean israd;
	protected boolean negangles = true;

	public static String NUM_IS_NAN = "-";

	//

	public String getUnits()
	{
		return units;
	}

	public void setUnits(String units)
	{
		this.units = units;
		israd = (units != null) && units.startsWith("rad");
	}

	private static final String NUMCHARS = "0123456789-.eE";

	protected void setTextProperty(String s)
	{
		if (s == null)
		{
			super.setTextProperty(s);
			return;
		}

		s = s.trim();

		try {
			double n;
			// time must be handled specially
			if (units!=null && units.startsWith("s"))
			{
				n = AstroUtil.parseDuration(s);
			} else {
				n = UnitConverter.parse(s);
				if (israd)
					n = Math.toRadians(n);
			}
			super.setTextProperty(n+"");
		} catch (NumberFormatException nfe) {
			// if we get an exception... don't set the text, vic, the text!
			System.out.println(nfe.getMessage());
		}
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
		if (format != null)
			this.nfmt = new DecimalFormat(format);
	}

	public boolean isRadians()
	{
		return israd;
	}

	public String convertToString(Object o)
	{
		double x = PropertyUtil.toDouble(o);
		if (Double.isNaN(x))
			return NUM_IS_NAN;

		if (format != null)
		{
			String u = units;
			if (israd) {
				if (!negangles)
					x = AstroUtil.fixAngle(x);
				x = Math.toDegrees(x);
				u = null;
			}
			String s = nfmt.format(x);
			if (u != null)
				s += u;
			return s;
		} else {
			if (units == null)
				return AstroUtil.format(x);
			else if (units.startsWith("km"))
				return AstroUtil.toDistance(x) + units.substring(2);
			else if (units.startsWith("rad"))
				return AstroUtil.toDegrees(x) + units.substring(3);
			else if (units.startsWith("deg"))
				return AstroUtil.formatDegrees(x) + units.substring(3);
			else if (units.startsWith("lat"))
				return AstroUtil.formatDMS(Math.toDegrees(x), true) + units.substring(3);
			else if (units.startsWith("long"))
				return AstroUtil.formatDMS(Math.toDegrees(x), false) + units.substring(4);
			else if (units.startsWith("s"))
				return AstroUtil.toDuration(x) + units.substring(1);
			else if (units.startsWith("kg"))
				return AstroUtil.toMass(x) + units.substring(2);
			else
				return AstroUtil.format(x);
		}
	}

	public boolean getNegativeAngles()
	{
		return negangles;
	}

	public void setNegativeAngles(boolean negangles)
	{
		this.negangles = negangles;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(NumericLabel.class);

	static {
		prophelp.registerGetSet("format", "Format", String.class);
		prophelp.registerGetSet("units", "Units", String.class);
		prophelp.registerGetSet("negangles", "NegativeAngles", boolean.class);
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
