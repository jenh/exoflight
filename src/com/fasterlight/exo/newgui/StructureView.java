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
import java.util.Iterator;

import javax.media.opengl.GL;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.*;

// todo: put colors in a file, or make Shaders

/**
  * Displays a schematic view of a SpaceShip,
  * allows rotation.
  */
public class StructureView
extends GLOComponent
implements Constants
{
	SpaceShip ship;
	String struct_prop, helper_prop;

	Orientation ort;
	Vector3f rotvel, rotvel2;
	long rot_msec;
	long rot_dur = 1000;

	static boolean DRAW_MODULE_BOXES = false;

	//

	public StructureView()
	{
		ort = new Orientation(new Vector3d(0,1,0),new Vector3d(0,0,1));
		rotvel = new Vector3f();
		rotvel2 = new Vector3f();
	}

	public GUIContext getGUIContext()
	{
		return (GUIContext)getContext();
	}

	public boolean is3d()
	{
		return true;
	}

	public boolean needsClipping()
	{
		return true;
	}

	public String getPropertyForStructure()
	{
		return struct_prop;
	}

	public void setPropertyForStructure(String struct_prop)
	{
		this.struct_prop = struct_prop;
	}

	public String getPropertyForHelper()
	{
		return helper_prop;
	}

	public void setPropertyForHelper(String helper_prop)
	{
		this.helper_prop = helper_prop;
	}

	// todo: make work with screen resizing
   public void setProjection(GLOContext ctx)
  	{
  		Structure structure = getStructure();
  		if (structure == null)
  			return;

  		float rad = structure.getRadius()*1000;
  		if (rad <= 0)
  			return;

  		GL gl = ctx.getGL();

  		if (w1 <= 0 || h1 <= 0)
  			return;

  		float aspratio = (float)w1/(float)h1;

  		Point o = origin;
		gl.glViewport(o.x, ctx.getHeight()-o.y-h1, w1, h1);
      gl.glMatrixMode( GL.GL_PROJECTION );
      gl.glLoadIdentity();
      if (w1 > h1)
	      gl.glOrtho( -rad*aspratio, rad*aspratio, -rad, rad, -rad, rad );
	   else
	      gl.glOrtho( -rad, rad, -rad/aspratio, rad/aspratio, -rad, rad );
      gl.glMatrixMode( GL.GL_MODELVIEW );
      gl.glLoadIdentity();
	}

	public Structure getStructure()
	{
		return (Structure)getForPropertyKey(struct_prop);
	}

	public StructureViewHelper getHelper()
	{
		return (helper_prop != null) ? (StructureViewHelper)getForPropertyKey(helper_prop) : null;
	}

	Orientation getRotation()
	{
		if (rot_msec == 0)
			return ort;
		Orientation o = new Orientation(ort);
		long t = ctx.getFrameStartMillis();
		if (t > rot_msec)
		{
			long dur = (t-rot_msec);
			if (dur > rot_dur)
				dur = rot_dur;
			o.mul(new Vector3d(rotvel), dur*1d/rot_dur);
			if (dur == rot_dur)
			{
				ort.set(o);
				if (rotvel2.lengthSquared() > 0)
				{
					rot_msec = t;
					rotvel.set(rotvel2);
					rotvel2.set(0,0,0);
				} else {
					rot_msec = 0;
				}
			}
		}
		return o;
	}

	public void render(GLOContext ctx)
	{
  		Structure structure = getStructure();
		if (structure == null)
			return;

		GL gl = ctx.getGL();

		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_LIGHTING_BIT | GL.GL_POLYGON_BIT);
		/*
		float[] lightPosition = { -1, 0, 1, 0.0f };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition);
		// todo: rest of light params
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT0);
		*/
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc( GL.GL_LEQUAL );

		renderStructure(ctx);

		gl.glPopAttrib();
	}

   void renderStructure(GLOContext ctx)
   {
   	GL gl = ctx.getGL();

   	gl.glPushMatrix();
   	ViewBase.rotateByOrientation(gl, getRotation());

  		Structure structure = getStructure();
   	Iterator it = structure.getModules().iterator();
   	while (it.hasNext())
   	{
   		Module m = (Module)it.next();
   		gl.glPushMatrix();

   		Vector3f ofs = m.getOffset();
  			gl.glTranslatef( ofs.x, ofs.y, ofs.z );
  			ViewBase.rotateByOrientation(gl, m.getOrientation());
  			renderModule(gl, m);

  			Vector3f dim = m.getDimensions();
  			dim.scale(0.5f);
  			if (DRAW_MODULE_BOXES)
	  			renderCube(gl, dim);

   		gl.glPopMatrix();
   	}

   	gl.glPopMatrix();
   }

   void renderCube(GL gl, Vector3f p)
   {
   	gl.glBegin(GL.GL_LINE_LOOP);
   	gl.glVertex3f(-p.x, p.y, p.z);
   	gl.glVertex3f(p.x, p.y, p.z);
   	gl.glVertex3f(p.x, -p.y, p.z);
   	gl.glVertex3f(-p.x, -p.y, p.z);
   	gl.glEnd();
   	gl.glBegin(GL.GL_LINE_LOOP);
   	gl.glVertex3f(-p.x, p.y, -p.z);
   	gl.glVertex3f(p.x, p.y, -p.z);
   	gl.glVertex3f(p.x, -p.y, -p.z);
   	gl.glVertex3f(-p.x, -p.y, -p.z);
   	gl.glEnd();
   	gl.glBegin(GL.GL_LINES);
   	gl.glVertex3f(-p.x, p.y, p.z);
   	gl.glVertex3f(-p.x, p.y, -p.z);
   	gl.glVertex3f(p.x, p.y, p.z);
   	gl.glVertex3f(p.x, p.y, -p.z);
   	gl.glVertex3f(p.x, -p.y, p.z);
   	gl.glVertex3f(p.x, -p.y, -p.z);
   	gl.glVertex3f(-p.x, -p.y, p.z);
   	gl.glVertex3f(-p.x, -p.y, -p.z);
   	gl.glEnd();
   }

   Vector3f getStructureDims(Structure struct)
   {
   	Iterator it = getStructure().getModules().iterator();
   	while (it.hasNext())
   	{
   		Module m = (Module)it.next();
   		Vector3f ofs = m.getOffset();
   		// todo
   	}
   	return null;
   }

   void renderModule(GL gl, Module m)
   {
   	String name = m.getType();
   	ModelRenderer mrend;
   	Module selmodule = null;

   	StructureViewHelper svh = getHelper();
   	if (svh != null)
   	{
   		Object o = svh.getSelectedModule();
   		if (o instanceof Module)
   			selmodule = (Module)o;
   	}

		gl.glEnable (GL.GL_POLYGON_OFFSET_FILL);
	   gl.glPolygonOffset(1, 1);
	   if (selmodule == m)
			gl.glColor3f(1f/2, 0.8f/2, 0.2f/2);
		else
			gl.glColor3f(0, 0.5f, 0);
		int mrflags = ModelRenderer.NO_MATERIALS | ModelRenderer.NO_NORMALS | ModelRenderer.NO_TEXTURES;
		ModelRenderCache rendcache = getGUIContext().rendcache;
   	mrend = rendcache.getModelRenderer(name + "-d2", mrflags);
   	if (mrend == null)
   		mrend = rendcache.getModelRenderer(name, mrflags);
   	if (mrend != null)
	   	mrend.render();
	   else
	   	System.out.println("model " + name + " not found");
	   gl.glPolygonOffset(0, 0);
		gl.glDisable (GL.GL_POLYGON_OFFSET_FILL);

   	if (selmodule == m)
	   	gl.glColor3f(1f, 0.8f, 0.2f);
	   else
	   	gl.glColor3f(0, 1.0f, 0);

	   mrflags = ModelRenderer.WIREFRAME | ModelRenderer.NO_NORMALS | ModelRenderer.NO_MATERIALS;
   	mrend = rendcache.getModelRenderer(name + "-d2", mrflags);
   	if (mrend == null)
   		mrend = rendcache.getModelRenderer(name, mrflags);
   	if (mrend != null)
	   	mrend.render();
	   else
	   	System.out.println("model " + name + " not found");

   }

	///

	public void rotate(float x, float y, float z)
	{
		System.out.println("rotate " + x + " " + y + " " + z);
		Vector3f v;
		if (rot_msec == 0)
		{
			v = rotvel;
			rot_msec = ctx.getFrameStartMillis();
		} else {
			v = rotvel2;
		}
		v.set(x,y,z);
		v.scale(0.25f);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(StructureView.class);

	static {
		prophelp.registerGetSet("struct_prop", "PropertyForStructure", String.class);
		prophelp.registerGetSet("helper_prop", "PropertyForHelper", String.class);
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
