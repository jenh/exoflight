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

import java.awt.Point;

import javax.media.opengl.GL;

import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * Displays two inputs similar to the LM crosspointer display,
  * using two arcs deflected from the center of the graph.
  */
public class ArrowIndicator
extends GLOComponent
{
	protected PropertyEvaluator prop_xvalue, prop_yvalue;
	protected float xscale=1, yscale=1;

	//

	public boolean needsClipping()
	{
		return true;
	}

	public String getPropertyForXValue()
	{
		return getKey(prop_xvalue);
	}

	public void setPropertyForXValue(String prop_xvalue)
	{
		this.prop_xvalue = new PropertyEvaluator(prop_xvalue);
	}

	public String getPropertyForYValue()
	{
		return getKey(prop_yvalue);
	}

	public void setPropertyForYValue(String prop_yvalue)
	{
		this.prop_yvalue = new PropertyEvaluator(prop_yvalue);
	}

	public float getXScale()
	{
		return xscale;
	}

	public void setXScale(float xscale)
	{
		this.xscale = xscale;
	}

	public float getYScale()
	{
		return yscale;
	}

	public void setYScale(float yscale)
	{
		this.yscale = yscale;
	}

	public float getScale()
	{
		return Math.max(xscale,yscale);
	}

	public void setScale(float scale)
	{
		this.xscale = this.yscale = scale;
	}

	public float getXValue()
	{
		float x = PropertyUtil.toFloat(getForPropertyKey(prop_xvalue)) * xscale;
		return x;
	}

	public float getYValue()
	{
		float y = PropertyUtil.toFloat(getForPropertyKey(prop_yvalue)) * yscale;
		return y;
	}

	///

	public void render(GLOContext ctx)
	{
		GUIContext guictx = (GUIContext)ctx;
		GL gl = ctx.getGL();

		setShader("arrowind");

		Point org = origin;
		float w2 = getWidth()*0.5f;
		float h2 = getHeight()*0.5f;
		float cenx = org.x+w2;
		float ceny = org.y+h2;

		float xval = getXValue();
		float yval = getYValue();

		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(cenx, ceny, 0);
		gl.glVertex3f(cenx+xval*xscale*w2, ceny+yval*yscale*h2, 0);
		gl.glEnd();
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ArrowIndicator.class);

	static {
		prophelp.registerGetSet("xvalue_prop", "PropertyForXValue", String.class);
		prophelp.registerGetSet("yvalue_prop", "PropertyForYValue", String.class);
		prophelp.registerGetSet("xscale", "XScale", float.class);
		prophelp.registerGetSet("yscale", "YScale", float.class);
		prophelp.registerGetSet("scale", "Scale", float.class);
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
