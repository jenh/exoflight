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

import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Settings;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

// todo: put colors in a file, or make Shaders

/**
  * Base class for VisualView and TacticalView.
  */
public class ViewBase
	extends GLODefault3DCanvas
	implements UniverseProjection, Constants
{
	protected SpaceGame game;

	protected String selthingkey = "selected";
	protected String trackthingkey = "tracked";

	protected PickList picklist;

	protected TextureCache texcache;
	protected ModelRenderCache rendcache;

	protected GUIContext guictx;
	protected GL gl;
	protected GLU glu;

	protected float PICK_RAD = Settings.getFloat("UI", "PickRadius", 10);

	// how many frames do we have before isPicking() returns false?
	protected static final int MOUSE_START_TIMER = 60;
	protected int mouseTimer;

	protected HintRenderer hintrend = new HintRenderer(this);

	///

	public ViewBase(SpaceGame game)
	{
		this.game = game;
		setFOV(45);
		MAX_FOV = 80f;
		neardist = 0.1f;
		fardist = 100.0f;
		picklist = new PickList();
		init((GUIContext) getContext());
	}

	void init(GUIContext guictx)
	{
		this.guictx = guictx;

		gl = guictx.getGL();
		glu = guictx.getGLU();

		texcache = guictx.texcache;
		rendcache = guictx.rendcache;
	}

	void renderAxes(double radius)
	{
		gl.glPushMatrix();
		float ar = (float) (radius);
		gl.glScalef(ar, ar, ar);
		ModelRenderer mrend =
			rendcache.getModelRenderer(
				"xyz",
				ModelRenderer.NO_NORMALS | ModelRenderer.COLORS);
		mrend.render();
		gl.glColor3f(1, 1, 1);
		gl.glPopMatrix();
	}

	void transformModuleDimensions(Module m)
	{
		float x = m.getChuteOpenAmount();
		if (x < 1)
		{
			gl.glScalef(x * x * x, x * x * x, x);
		}
	}

	void renderModule(Module m)
	{
		String name = m.getType();
		ModelRenderer mrend = rendcache.getModelRenderer(name + "-d2");
		if (mrend == null)
			mrend = rendcache.getModelRenderer(name);
		if (mrend != null)
			mrend.render();
		else
			System.out.println("model " + name + " not found");

		// now draw connecting bits
		for (int i = 0; i < Module.NUM_DIRS; i++)
		{
			// todo: walk down the direction until we reach a solid
			if (m.canConnect(i)
				&& (m.getLink(i) == null || !m.getLink(i).isHollow()))
			{
				mrend =
					rendcache.getModelRenderer(
						name + "-" + Module.connectDirToChar(i));
				if (mrend != null)
					mrend.render();
			}
		}
	}

	void renderModuleShadow(Module m)
	{
		String name = m.getType();
		ModelRenderer mrend =
			rendcache.getModelRenderer(
				name,
				ModelRenderer.NO_MATERIALS
					| ModelRenderer.NO_NORMALS
					| ModelRenderer.NO_TEXTURES);
		if (mrend != null)
			mrend.render();
		else
			System.out.println("model " + name + " not found");
	}

	void renderSpaceShip(SpaceShip ship)
	{
		gl.glPushMatrix();

		gl.glScalef(0.001f, 0.001f, 0.001f); // m to km
		//   	gl.glEnable(GL.GL_NORMALIZE);
		gl.glDisable(GL.GL_BLEND);

		Structure struct = ship.getStructure();
		Iterator it = struct.getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module) it.next();
			gl.glPushMatrix();

			// todo: can get position, translate by CM to make faster
			Vector3f ofs = m.getOffset();
			gl.glTranslatef(ofs.x, ofs.y, ofs.z);
			rotateByOrientation(m.getOrientation());
			transformModuleDimensions(m);
			renderModule(m);

			gl.glPopMatrix();
		}

		gl.glPopMatrix();
	}

	void renderContactPoints(StructureThing ship)
	{
		ContactPoint[] cpts = ship.getContactPoints();
		if (cpts != null && cpts.length > 0)
		{
			gl.glBegin(GL.GL_POINTS);
			for (int i = 0; i < cpts.length; i++)
			{
				ContactPoint cpt = cpts[i];
				gl.glColor3f(1, 1, 1);
				gl.glVertex3f(cpt.extpos.x, cpt.extpos.y, cpt.extpos.z);
			}
			gl.glEnd();
		}
	}

	// warning: does not preserve matrix
	void renderSpaceShipShadow(SpaceShip ship)
	{
		gl.glScalef(0.001f, 0.001f, 0.001f); // m to km

		Structure struct = ship.getStructure();
		Iterator it = struct.getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module) it.next();
			gl.glPushMatrix();

			rotateByOrientation(m.getOrientation());
			Vector3f ofs = m.getOffset();
			gl.glTranslatef(ofs.x, ofs.y, ofs.z);
			transformModuleDimensions(m);
			renderModuleShadow(m);

			gl.glPopMatrix();
		}
	}

	Vector3f addPickPoint(float radius, UniverseThing ut)
	{
		setManualProject();
		Vector3f v = new Vector3f();
		projectCoords(v);
		if (v.z <= 1)
		{
			float x = ctx.xscrn2world(v.x);
			// todo: why is this 768???
			float y = 768 /*getHeight()*/
			-ctx.yscrn2world(v.y);
			picklist.addPickRec(x, y, radius, ut);
			//System.out.println(x + " " + y + " " + getHeight() + " " + ut);
		}
		return v;
	}

	Vector3f addPickPoint(float radius, UniverseThing ut, Vector3d pos)
	{
		setManualProject();
		Vector3d v = new Vector3d(pos);
		projectCoords(v);
		if (v.z <= 1)
		{
			float x = ctx.xscrn2world((float) v.x);
			float y = getHeight() - ctx.yscrn2world((float) v.y);
			picklist.addPickRec(x, y, radius, ut);
		}
		return new Vector3f(v);
	}

	Vector3d translateThing(UniverseThing thing)
	{
		Vector3d pos = thing.getPosition(getTracked(), game.time());
		gl.glTranslated(pos.x, pos.y, pos.z);
		return pos;
	}

	Vector3d invTranslateThing(UniverseThing thing)
	{
		Vector3d pos = thing.getPosition(getTracked(), game.time());
		gl.glTranslated(-pos.x, -pos.y, -pos.z);
		return pos;
	}

	Orientation rotateThing(UniverseThing thing)
	{
		Orientation ort = thing.getOrientation(game.time());
		rotateByOrientation(ort);
		return ort;
	}

	void rotateByOrientation(Orientation ort)
	{
		rotateByOrientation(gl, ort);
	}

	static void rotateByOrientation(GL gl, Orientation ort)
	{
		Matrix3d mat = ort.getInvertedMatrix();
		GLOUtil.glMultMatrixd(gl, mat);
	}

	static void rotateByInvOrientation(GL gl, Orientation ort)
	{
		Matrix3d mat = ort.getMatrix();
		GLOUtil.glMultMatrixd(gl, mat);
	}

	Orientation rotateThingByVelocity(UniverseThing thing)
	{
		Vector3d vel = thing.getTrajectory().getVel(game.time());
		vel.normalize();
		Orientation ort = new Orientation(vel);
		Matrix3d mat = ort.getInvertedMatrix();
		GLOUtil.glMultMatrixd(gl, mat);
		return ort;
	}

	public Matrix3d rotatePlanet(Planet p)
	{
		Matrix3d mat = getPlanetRotateMatrix(p);
		GLOUtil.glMultMatrixd(gl, mat);
		return mat;
	}

	public Matrix3d rotateInvPlanet(Planet p)
	{
		Matrix3d mat = getPlanetRotateMatrix(p);
		mat.invert();
		GLOUtil.glMultMatrixd(gl, mat);
		return mat;
	}

	public Matrix3d getPlanetRotateMatrix(Planet p)
	{
		return p.getRotateMatrix(game.time());
	}

	//

	public static Plane4f getPlaneEquation(
		Vector3f v0,
		Vector3f v1,
		Vector3f v2)
	{
		return new Plane4f(v0, v1, v2);
	}

	float[] getShadowMatrix(Plane4f plane, Vector3d lightpos)
	{
		double px = plane.x;
		double py = plane.y;
		double pz = plane.z;
		double pw = plane.w;
		double dot = lightpos.x * px + lightpos.y * py + lightpos.z * pz;

		float[] mat = new float[16];

		mat[0] = (float) (dot - lightpos.x * px);
		mat[4] = (float) (-lightpos.x * py);
		mat[8] = (float) (-lightpos.x * pz);
		mat[12] = (float) (-lightpos.x * pw);

		mat[1] = (float) (-lightpos.y * px);
		mat[5] = (float) (dot - lightpos.y * py);
		mat[9] = (float) (-lightpos.y * pz);
		mat[13] = (float) (-lightpos.y * pw);

		mat[2] = (float) (-lightpos.z * px);
		mat[6] = (float) (-lightpos.z * py);
		mat[10] = (float) (dot - lightpos.z * pz);
		mat[14] = (float) (-lightpos.z * pw);

		mat[15] = (float) (dot);

		return mat;
	}

	protected double[] manproj_model = new double[16];
	protected double[] manproj_proj = new double[16];
	protected int[] manproj_view = new int[4];
	protected double[] manproj = new double[3];

	protected void setManualProject()
	{
		gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, manproj_model, 0);
		gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, manproj_proj, 0);
		gl.glGetIntegerv(GL.GL_VIEWPORT, manproj_view, 0);
	}

	protected boolean projectCoords(Vector3f v)
	{
		if (!glu
			.gluProject(
				v.x,
				v.y,
				v.z,
				manproj_model, 0,
				manproj_proj, 0,
				manproj_view, 0,
				manproj, 0))
			return false;
		v.set((float) manproj[0], (float) manproj[1], (float) manproj[2]);
		return true;
	}

	protected boolean projectCoords(Vector3d v)
	{
		if (!glu
			.gluProject(
				v.x,
				v.y,
				v.z,
				manproj_model, 0,
				manproj_proj, 0,
				manproj_view, 0,
				manproj, 0))
			return false;
		v.set(manproj[0], manproj[1], manproj[2]);
		return true;
	}

	///

	public Vector3f world2scrn(Tuple3d wpos)
	{
		Vector3d wpos2 = new Vector3d(wpos);
		if (projectCoords(wpos2))
			return new Vector3f(
				(float) wpos2.x,
				(float) wpos2.y,
				(float) wpos2.z);
		else
			return new Vector3f(0, 0, -1);
	}
	public Vector3d scrn2world(Tuple3f spos)
	{
		throw new RuntimeException("TODO: scrn2world");
	}
	public Rect4f getViewport()
	{
		int[] a = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, a, 0);
		return new Rect4f(a[0], a[1], a[0] + a[2], a[1] + a[3]);
	}

	///

	public void showHintFor(UniverseThing ut)
	{
		hintrend.showHintFor(ut, ut.getName());
	}

	public void renderForeground(GLOContext ctx)
	{
		super.renderForeground(ctx);

		if (hintrend.size() > 0)
		{
			getContext().set2DProjection();
			hintrend.renderHints(ctx, picklist);
		}
		if (mouseTimer > 0)
			mouseTimer--;
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOMouseButtonEvent)
		{
			GLOMouseButtonEvent mbe = (GLOMouseButtonEvent) event;
			if (mbe.isPressed(1))
			{
				mouseTimer = MOUSE_START_TIMER;
			}
			else if (mbe.isReleased(1))
			{
				event.getContext().requestFocus(this);
				mouseTimer = MOUSE_START_TIMER;
				// check pick list
				int x = mbe.x;
				int y = mbe.y;

				Object o = picklist.pickObject(x, y);
				if (o != null)
				{
					setSelected((UniverseThing) o);
				}
			}
		}
		else if (event instanceof GLOMouseMovedEvent)
		{
			if (!super.handleEvent(event))
			{
				GLOMouseMovedEvent mbe = (GLOMouseMovedEvent) event;
				mouseTimer = MOUSE_START_TIMER;
				// check pick list
				int x = mbe.x;
				int y = mbe.y;

				PickList.PickRec pickrec = picklist.pickObjectRec(x, y);
				if (pickrec != null)
				{
					showHintFor((UniverseThing) pickrec.obj);
				}
				return true;
			}
		}

		return super.handleEvent(event);
	}

	protected boolean isPicking()
	{
		return (mouseTimer > 0);
	}

	///

	public void setSelected(UniverseThing ut)
	{
		guictx.setProp(selthingkey, ut);
	}

	public UniverseThing getSelected()
	{
		return (UniverseThing) guictx.getProp(selthingkey);
	}

	public void setTracked(UniverseThing ut)
	{
		guictx.setProp(trackthingkey, ut);
	}

	public UniverseThing getTracked()
	{
		return (UniverseThing) guictx.getProp(trackthingkey);
	}

	/*
		// returns the parent of the current tracked, selected, or ship
		public UniverseThing getRefThing()
		{
			UniverseThing ut;
			ut = getSelected();
			if (ut != null)
				return ut.getParent();
			ut = getTracked();
			if (ut != null)
				return ut.getParent();
			ut = getCurrentShip();
			if (ut != null)
				return ut.getParent();
			return null;
		}
	*/

	public SpaceShip getCurrentShip()
	{
		return (SpaceShip) guictx.getProp("ship");
	}

	public void setCurrentShip(SpaceShip selected)
	{
		guictx.setProp("ship", selected);
	}

	public UniverseThing getTarget()
	{
		SpaceShip ship = getCurrentShip();
		if (ship != null)
			return ship.getShipTargetingSystem().getTarget();
		else
			return null;
	}

}
