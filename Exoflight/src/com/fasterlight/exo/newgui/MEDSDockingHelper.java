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

import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * Helps the MEDS docking panel set the various ranges.
  */
public class MEDSDockingHelper
extends GLOComponent
{
	private float[] ranges = { 0.020f, 0.100f, 0.500f, 2.000f };

	private int rangeidx = 0;
	private float scale = ranges[0];


	//

	public void setRangeIndex(int r)
	{
		this.rangeidx = r;
		scale = ranges[r];

		GLOComponent cmpt;
		cmpt = getContext().getDescendantNamed(getParent(), "xy-posarrow");
		if (cmpt instanceof ArrowIndicator)
			((ArrowIndicator)cmpt).setScale(1f/scale);
		cmpt = getContext().getDescendantNamed(getParent(), "xz-posarrow");
		if (cmpt instanceof ArrowIndicator)
			((ArrowIndicator)cmpt).setScale(1f/scale);

		cmpt = getContext().getDescendantNamed(getParent(), "xy-velarrow");
		if (cmpt instanceof ArrowIndicator)
			((ArrowIndicator)cmpt).setScale(4f/scale);
		cmpt = getContext().getDescendantNamed(getParent(), "xz-velarrow");
		if (cmpt instanceof ArrowIndicator)
			((ArrowIndicator)cmpt).setScale(4f/scale);
	}

	public int getRangeIndex()
	{
		return rangeidx;
	}

	public void render(GLOContext ctx)
	{
		//
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(MEDSDockingHelper.class);

	static {
		prophelp.registerGetSet("rangeindex", "RangeIndex", int.class);
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
