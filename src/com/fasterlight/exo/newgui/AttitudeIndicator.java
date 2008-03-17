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

import com.fasterlight.exo.orbit.Orientation;
import com.fasterlight.exo.ship.GuidanceCapability;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.*;

/**
  * Displays the dreaded "8-ball" :)
  * todo: make ship-specific version (for efficiency & features)
  */
public class AttitudeIndicator extends GLOComponent
{
	protected PropertyEvaluator prop_refort, prop_ort, prop_targort, prop_pathvec;
	protected PropertyEvaluator prop_guidancecap;
	protected String pointermodel = "8ballptr2";
	protected String pathindmodel = "8ball-pathind";
	protected static String ballshader = "8ball";
	protected static String ptrshader = "8ballptr";
	protected static String pathshader = "8ballpath";
	protected float zoom = 1;

	//

	public boolean needsClipping()
	{
		return true;
	}

	public String getPropertyForReferenceOrt()
	{
		return getKey(prop_refort);
	}

	public void setPropertyForReferenceOrt(String prop_refort)
	{
		this.prop_refort = new PropertyEvaluator(prop_refort);
	}

	public String getPropertyForVehicleOrt()
	{
		return getKey(prop_ort);
	}

	public void setPropertyForVehicleOrt(String prop_ort)
	{
		this.prop_ort = new PropertyEvaluator(prop_ort);
	}

	public String getPropertyForTargetOrt()
	{
		return getKey(prop_targort);
	}

	public void setPropertyForTargetOrt(String prop_targort)
	{
		this.prop_targort = new PropertyEvaluator(prop_targort);
	}

	public String getPropertyForPathVec()
	{
		return getKey(prop_pathvec);
	}

	public void setPropertyForPathVec(String prop_pathvec)
	{
		this.prop_pathvec = new PropertyEvaluator(prop_pathvec);
	}

	public String getPropertyForGuidanceCap()
	{
		return getKey(prop_guidancecap);
	}

	public void setPropertyForGuidanceCap(String prop_guidancecap)
	{
		this.prop_guidancecap = new PropertyEvaluator(prop_guidancecap);
	}

	//

	protected Orientation getOrientation()
	{
		return prop_ort != null ? (Orientation) getForPropertyKey(prop_ort) : null;
	}

	protected Orientation getRefOrientation()
	{
		return prop_refort != null ? (Orientation) getForPropertyKey(prop_refort) : null;
	}

	protected Orientation getTargetOrientation()
	{
		return prop_targort != null ? (Orientation) getForPropertyKey(prop_targort) : null;
	}

	protected Vector3d getPathVector()
	{
		return prop_pathvec != null ? (Vector3d) getForPropertyKey(prop_pathvec) : null;
	}

	protected GuidanceCapability getGuidanceCap()
	{
		return prop_guidancecap != null
			? (GuidanceCapability) getForPropertyKey(prop_guidancecap)
			: null;
	}

	void reorient(GL gl)
	{
		// we have +Z axis pointing up, and +X axis pointing right,
		// and +Y pointing into the screen
		gl.glRotatef(180, 0, 0, 1);
		gl.glRotatef(90, 0, 1, 0);
		gl.glRotatef(-90, 1, 0, 0);
		// now we should have +Z going into the screen,
		// +X pointing right, and +Y pointing up?
	}

	public void render(GLOContext ctx)
	{
		GUIContext guictx = (GUIContext) ctx;
		GL gl = ctx.getGL();

		gl.glPushMatrix(); // #1
		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
		gl.glEnable(GL.GL_CULL_FACE);
		//		gl.glFrontFace(GL.GL_CW);

		setShader(ballshader);
		Point org = origin;
		float w2 = getWidth() * 0.5f;
		float h2 = getHeight() * 0.5f;
		float cenx = org.x + w2;
		float ceny = org.y + h2;

		gl.glTranslatef(cenx, ceny, 0);
		gl.glScalef(w2 * zoom, h2 * zoom, 1); // must convert right-hand to left-hand coords

		Orientation ortref = getRefOrientation(); // get reference orientation
		Orientation mainort = getOrientation(); // get target orientation
		Orientation ort = (mainort == null) ? new Orientation() : new Orientation(mainort);
		// translate target orientation to reference orientation
		if (ortref != null)
			ort.concatInverse(ortref);

		gl.glPushMatrix(); // #2

		Matrix3d mat = ort.getMatrix();
		GLOUtil.glMultMatrixd(gl, mat);

		reorient(gl);
		gl.glCallList(guictx.getSphereIndex(2));

		gl.glPopMatrix(); // #2

		// draw the path vector, if applicable
		Vector3d pathvec = getPathVector();
		if (pathvec != null)
		{
			gl.glDisable(GL.GL_DEPTH_TEST);
			setShader(pathshader);

			Orientation ort2 = (mainort != null) ? new Orientation(mainort) : new Orientation();
			ort2.concatInverse(new Orientation(pathvec));

			gl.glPushMatrix();
			Matrix3d mat2 = ort2.getMatrix();
			GLOUtil.glMultMatrixd(gl, mat2);
			ModelRenderer mrend =
				((GUIContext) ctx).rendcache.getModelRenderer(
					pathindmodel,
					ModelRenderer.NO_NORMALS);
			mrend.render();
			gl.glPopMatrix();
		}

		// get the guidance capability
		GuidanceCapability guidcap = getGuidanceCap();
		if (guidcap != null && guidcap.isActive())
		{

			// draw the pointer, if applicable
			Orientation targort = getTargetOrientation();
			if (targort != null)
			{
				//gl.glDisable(GL.GL_CULL_FACE);
				gl.glDisable(GL.GL_DEPTH_TEST);
				setShader(ptrshader);

				Orientation ort2 = (mainort != null) ? new Orientation(mainort) : new Orientation();
				ort2.concatInverse(targort);

				gl.glPushMatrix();
				Matrix3d mat2 = ort2.getMatrix();
				float[] marr = GLOUtil.toArray(mat2);
				gl.glMultMatrixf(marr, 0);
				ModelRenderer mrend =
					((GUIContext) ctx).rendcache.getModelRenderer(
						pointermodel,
						ModelRenderer.NO_NORMALS);
				mrend.render();
				gl.glPopMatrix();
			}

		}

		gl.glPopAttrib();
		gl.glPopMatrix(); // #1
	}

	public float getZoom()
	{
		return zoom;
	}

	public void setZoom(float zoom)
	{
		this.zoom = zoom;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(AttitudeIndicator.class);

	static {
		prophelp.registerGetSet("refort_prop", "PropertyForReferenceOrt", String.class);
		prophelp.registerGetSet("ort_prop", "PropertyForVehicleOrt", String.class);
		prophelp.registerGetSet("targort_prop", "PropertyForTargetOrt", String.class);
		prophelp.registerGetSet("pathvec_prop", "PropertyForPathVec", String.class);
		prophelp.registerGetSet("guidancecap_prop", "PropertyForGuidanceCap", String.class);
		prophelp.registerGetSet("zoom", "Zoom", float.class);
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
		try
		{
			prophelp.setProp(this, key, value);
		}
		catch (PropertyRejectedException e)
		{
			super.setProp(key, value);
		}
	}

}
