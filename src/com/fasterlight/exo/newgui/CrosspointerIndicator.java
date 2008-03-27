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
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.spif.*;

/**
  * Displays two inputs similar to the LM crosspointer display,
  * using two arcs deflected from the center of the graph.
  */
public class CrosspointerIndicator
extends GLOComponent
{
	protected PropertyEvaluator prop_xvalue, prop_yvalue;
	protected float xscale=1, yscale=1;
	protected float maxrot=60;
	protected String xpointermodel = "xpointer";

	//

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

		ModelRenderer mrend = guictx.rendcache.getModelRenderer(
			xpointermodel, ModelRenderer.NO_NORMALS);

		gl.glPushMatrix(); // #1
		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
		gl.glEnable(GL.GL_CULL_FACE);

		setShader("xpointer");
		Point org = origin;
		float w2 = getWidth()*0.5f;
		float h2 = getHeight()*0.5f;
		float cenx = org.x+w2;
		float ceny = org.y+h2;

		gl.glTranslatef(cenx, ceny, 0);
		gl.glScalef(w2, h2, 1); // must convert right-hand to left-hand coords

		// draw the x value
		gl.glPushMatrix();
		float xval = getXValue();
		xval = Math.max(-maxrot, Math.min(maxrot, xval*maxrot));
		gl.glRotatef(90, 0, 0, 1);
		gl.glRotatef(xval, -1, 0, 0);
		mrend.render();
		gl.glPopMatrix();

		// draw the y value
		float yval = getYValue();
		yval = Math.max(-maxrot, Math.min(maxrot, yval*maxrot));
		gl.glRotatef(yval, -1, 0, 0);
		mrend.render();

		gl.glPopAttrib();
		gl.glPopMatrix(); // #1
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(CrosspointerIndicator.class);

	static {
		prophelp.registerGetSet("xvalue_prop", "PropertyForXValue", String.class);
		prophelp.registerGetSet("yvalue_prop", "PropertyForYValue", String.class);
		prophelp.registerGetSet("xscale", "XScale", float.class);
		prophelp.registerGetSet("yscale", "YScale", float.class);
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
