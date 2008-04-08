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

import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.SettingsGroup;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.*;

// todo: put colors in a file, or make Shaders

/**
  * Displays a 3D rotatable & zoomable schematic of
  * the local universe.
  */
public class TacticalView extends ViewBase implements Constants
{
	Planet refthing;
	double refrad, maxrad;
	UniverseThing tracked, selected;

	Conic navorbit;
	long navt1, navt2;

	protected PropertyEvaluator prop_disporbit;

	ViewVolume viewvol = new ViewVolume();

	GLOSmoother vscaleSmoother = new GLOSmoother(1.0f / 6000);
	double vscale;

	boolean zoomToOrbit, zoomToPlanet;
	boolean showEcliptic, showEquatorialPlane;

	///

	public TacticalView(SpaceGame game)
	{
		super(game);
		setFOV(45);
		//		MIN_FOV = 0.03f;
		//		MAX_FOV = 80f;
		neardist = 1.0f;
		fardist = 150.0f;
	}

	float ambient_mag = 0.5f;

	public void init(GUIContext guictx)
	{
		super.init(guictx);
	}

	public void setRefPlanet(Planet planet)
	{
		this.refthing = planet;
	}

	public Planet getRefPlanet()
	{
		return refthing;
	}

	//

	Vector3d translateThing(UniverseThing thing)
	{
		Vector3d pos = thing.getPosition(refthing, game.time());
		adjustRadius(pos);
		gl.glTranslated(pos.x, pos.y, pos.z);
		return pos;
	}

	double adjustRadius(double r)
	{
		return r * vscale;
	}

	double getPlanetRadius(double r)
	{
		return Math.max(adjustRadius(r), sin_fov * MIN_PLANET_RADIUS);
	}

	void adjustRadius(Vector3d v)
	{
		double vl = v.length();
		double newr = adjustRadius(vl);
		if (newr > 0)
			v.scale(newr / vl);
	}

	void adjustRadius(Vector3f v)
	{
		double vl = v.length();
		double newr = adjustRadius(vl);
		if (newr > 0)
			v.scale((float) (newr / vl));
	}

	protected boolean projectCoords(Vector3f v)
	{
		adjustRadius(v);
		return super.projectCoords(v);
	}

	protected boolean projectCoords(Vector3d v)
	{
		adjustRadius(v);
		return super.projectCoords(v);
	}

	void renderConic(Vector3d pos, Conic o, boolean track)
	{
		renderConic(pos, o, 0, 0, track);
	}

	void renderConic(Vector3d pos, Conic o, long t1, long t2, boolean track)
	{
		// if orbit is not in current frustum, go away
		double adjperi = adjustRadius(o.getPeriapsis());
		double adjapo = adjustRadius(o.getApoapsis());
		Vector3d adjpos = new Vector3d();
		if (pos != null)
		{
			adjpos.set(pos);
			adjustRadius(adjpos);
		}

		// if distance to periapsis > getViewScale(), return
		// todo: use pos?
		//		if (Math.abs(adjperi-pos.length()) > fardist)
		if (adjperi > fardist * MAX_ORBIT_RENDER_SCALE)
			return;

		// if view frustum does not intersect sphere to apoapsis, return
		if (adjapo > 0 && !Double.isNaN(adjapo) && !viewvol.intersectsSphere(adjpos, adjapo))
			return;

		OrbitPolyline op = new OrbitPolyline(o, this, ORBIT_MIN_DEV);
		if (pos != null)
			op.setOrigin(pos);

		/*
		if (t1 > 0 || t2 > 0)
			op.setTimes(t1,t2);
		*/

		// todo: whassup??

		Vector3d nml = o.getPlaneNormal();
		nml.scale(ORBIT_WIDTH * sin_fov / nml.length());

		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);

		gl.glDisable(GL.GL_CULL_FACE);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		gl.glDisable(GL.GL_TEXTURE_2D);

		setManualProject();

		for (int i = 0; i < 2; i++)
		{
			// compute points, use callback
			// todo: don't need to do twice, can use plane to compute
			// orbit shadow
			if (i == 1)
			{
				op.setFixedRadius(refthing.getRadius());
				gl.glColor4f(0.25f, 0.25f, 0.25f, 1.0f);
				gl.glDepthFunc(GL.GL_LESS);
			}
			Collection pts = op.getPoints();

			gl.glPolygonOffset(-1, -1);
			gl.glBegin(GL.GL_QUAD_STRIP);
			Iterator it = pts.iterator();
			Vector3d w1 = new Vector3d();
			Vector3d w2 = new Vector3d();
			while (it.hasNext())
			{
				Vector3d wpos = (Vector3d) it.next();
				adjustRadius(wpos);
				w1.set(wpos);
				w1.add(nml);
				gl.glTexCoord2f(0f, 0.5f);
				gl.glVertex3d(w1.x, w1.y, w1.z);
				w2.set(wpos);
				w2.sub(nml);
				gl.glTexCoord2f(1f, 0.5f);
				gl.glVertex3d(w2.x, w2.y, w2.z);
			}
			gl.glEnd();

			// if no groundtrack, exit out
			if (!track)
				break;
		}

		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glPolygonOffset(0, 0);
		gl.glPopAttrib();
	}

	void renderConicFor(UniverseThing ut, boolean track)
	{
		Conic o = UniverseUtil.getConicFor(ut);
		if (o != null)
		{
			Vector3d pos = ut.getParent().getPosition(refthing, game.time());
			if (ut == selected)
				gl.glColor4f(0.5f, 0.0f, 0.0f, 1.0f);
			else
				gl.glColor4f(0.0f, 0.25f, 0.25f, 1.0f);
			renderConic(pos, o, track);
		}
	}

	void renderOrientation(SpaceShip ship, double r)
	{
		// show direction of travel, if applicable?
		AttitudeController att = ship.getAttitudeController();
		if (ship == selected && att != null)
		{
			Vector3d dir = att.getTargetOrientation().getDirection();
			Vector3d up = att.getTargetOrientation().getUpVector();
			gl.glBegin(GL.GL_LINES);
			gl.glColor3f(1, 1, 0);
			gl.glVertex3d(0, 0, 0);
			gl.glVertex3d(dir.x * r, dir.y * r, dir.z * r);
			gl.glColor3f(0, 1, 1);
			gl.glVertex3d(0, 0, 0);
			gl.glVertex3d(up.x * r, up.y * r, up.z * r);
			gl.glEnd();

			/*
			if (att instanceof LambertManeuver)
			{
				Conic o = ((LambertManeuver)att).lastorbit;
				if (o != null)
				{
					navorbit = o;
				}
			}
			*/
		}
	}

	void renderThing(UniverseThing ut)
	{
		if (ut instanceof Planet)
		{
			renderPlanet((Planet) ut);
			return;
		}

		gl.glPushMatrix();
		if (THING_LIGHTING)
		{
			gl.glEnable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_CULL_FACE);
		}

		Vector3d pos = translateThing(ut);
		double dist = getViewDistance();

		// add point to pick list
		// todo: only do this when selecting
		if (isPicking())
			addPickPoint(PICK_RAD, ut);

		// scale by fov
		float sc = (float) (THING_PIXSIZE * sin_fov * dist / Math.min(w1, h1));
		gl.glScalef(sc, sc, sc);
		Trajectory traj = ut.getTrajectory();
		if (ut instanceof SpaceBase)
			gl.glColor3f(0, 0, 1);
		else if (ut instanceof SpaceShip)
		{
			//			renderOrientation( (SpaceShip)ut, 2 );
			if (ut.getTrajectory() instanceof CowellTrajectory)
				gl.glColor3f(1f, 1f, 1f);
			else
				gl.glColor3f(1f, 1f, 0.5f);
		}
		else
			gl.glColor3f(0.75f, 0.25f, 0.25f);
		if (traj instanceof LandedTrajectory)
		{
			rotateThing(ut);
			// render cone
			ModelRenderer mr = rendcache.getModelRenderer("base", ModelRenderer.NO_MATERIALS);
			mr.render();
		}
		else
		{
			rotateThingByVelocity(ut);
			// render cone
			ModelRenderer mr = rendcache.getModelRenderer("cone", ModelRenderer.NO_MATERIALS);
			mr.render();
		}
		/*
		Vector3d pos = ut.getPosition(refthing, game.time());
		adjustRadius(pos);
		gl.glBegin(GL.GL_POINTS);
		gl.glColor3f(1,1,1);
		gl.glVertex3d(pos.x, pos.y, pos.z);
		gl.glEnd();
		*/

		if (THING_LIGHTING)
		{
			gl.glDisable(GL.GL_LIGHTING);
			gl.glDisable(GL.GL_CULL_FACE);
		}
		gl.glPopMatrix();
	}

	void renderChildren(UniverseThing p, int level)
	{
		Iterator it = p.getChildren();
		while (it.hasNext())
		{
			UniverseThing ut = (UniverseThing) it.next();
			renderThing(ut);
			if (level > 0)
				renderChildren(ut, level - 1);
		}
	}

	void setPlanetTexture(Planet p)
	{
		String n = p.getName();
		if (guictx.texcache.setTexture(n + "/map.png") >= 0)
			return;
		else if (guictx.texcache.setTexture(n + "/bw.png") >= 0)
			return;
		else if (guictx.texcache.setTexture(n + "/" + n + ".png") >= 0)
			return;
	}

	void renderPlanet(Planet p)
	{
		gl.glPushMatrix();

		float r = 1, d;
		boolean culled = false;

		Vector3d pos = translateThing(p);
		r = (float) getPlanetRadius(p.getRadius());
		d = (float) (r * getWidth() / (sin_fov * (pos.length() + getViewDistance())));

		if (!viewvol.intersectsSphere(pos, r))
			culled = true;

		if (!culled)
		{
			gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
			gl.glEnable(GL.GL_CULL_FACE);

			if (isPicking())
				addPickPoint(PICK_RAD, p);

			rotatePlanet(p);
			gl.glScalef(r, r, r);
			setPlanetTexture(p);
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glColor3f(0.1f, 0.5f, 0.1f);

			// find out what resolution to draw sphere at (todo)
			int level = 1;
			if (d > 350)
				level--;
			else if (d < 60)
				level++;
			int sphereindex = guictx.getNmlSphereIndex(level);

			gl.glMatrixMode(GL.GL_TEXTURE);
			gl.glLoadIdentity();
			gl.glScalef(1, -1, 1);

			gl.glCallList(sphereindex);

			// draw the grid

			// multiply texture coords to get grid lines

			texcache.setTexture("grid.png");
			sphereindex = guictx.getGridSphereIndex(level);

			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			gl.glEnable(GL.GL_BLEND);
			gl.glColor3f(0.1f, 0.5f, 0.1f);
			gl.glCallList(sphereindex);
			gl.glDisable(GL.GL_BLEND);

			gl.glLoadIdentity();
			gl.glMatrixMode(GL.GL_MODELVIEW);

			gl.glPopAttrib();
		}

		//		renderAxes(1.25);

		gl.glPopMatrix();

		// draw orbit for this object too
		renderConicFor(p, false);
	}

	void renderChildrenRecursive(UniverseThing p)
	{
		Iterator it = p.getChildren();
		while (it.hasNext())
		{
			UniverseThing ut = (UniverseThing) it.next();
			renderRecursive(ut);
		}
	}

	// render the object and its children,
	// if the price is right
	void renderRecursive(UniverseThing ut)
	{
		renderThing(ut);

		// if no children, just return
		if (!ut.getChildren().hasNext())
			return;

		boolean drawchildren = false;
		if (ut instanceof Universe || ut instanceof Star)
		{
			drawchildren = true;
		}
		else
		{
			double infrad = ut.getInfluenceRadius(game.time());
			if (infrad == 0)
				drawchildren = true;
			else
			{
				double adjrad = adjustRadius(infrad);
				Vector3d pos = ut.getPosition(refthing, game.time());
				adjustRadius(pos);
				// only recurse if influence radius is above
				// certain amount, and if it intersects the view volume
				if (adjrad > MIN_RECURSE_RADIUS)
				{
					if (viewvol.intersectsSphere(pos, adjrad))
						drawchildren = true;
				}
			}
		}

		if (drawchildren)
		{
			renderChildrenRecursive(ut);
		}
	}

	public void renderObject(GLOContext ctx)
	{
		vscale = vscaleSmoother.getValue();
		tracked = getTracked();
		selected = getSelected();

		// move tracked to center
		if (tracked != null && (zoomToOrbit || zoomToPlanet))
		{
			double rad = tracked.getTelemetry().getCENDIST();
			{
				double apo = tracked.getTelemetry().getAPOAPSIS() + tracked.getParent().getRadius();
				if (apo > rad)
					rad = apo;
			}
			setViewScale((float) (getViewDistance() * 0.25 / (rad * sin_fov)));
			// todo
			if (zoomToPlanet)
				tracked = tracked.getParent();
		}

		// todo
		if (selected != null && selected.getParent() instanceof Planet)
			refthing = (Planet) selected.getParent();
		if (refthing == null)
			return;

		refrad = refthing.getRadius();
		if (refthing instanceof Star)
			maxrad = refthing.getMass() * (4e10 / 2e20);
		else
			maxrad = refthing.getInfluenceRadius(game.time()) * 4;

		picklist.clear();

		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		// render stars
		gl.glPushMatrix();
		float sr = fardist - (getViewDistance() + neardist);
		gl.glScalef(sr, sr, sr);
		guictx.renderStars(3);
		gl.glPopMatrix();

		//		ModelRenderer mr = rendcache.getModelRenderer("xyz");
		//		mr.render();

		// setup fog
		gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
		gl.glFogf(GL.GL_FOG_START, fardist / 5);
		gl.glFogf(GL.GL_FOG_END, fardist * 6 / 5);
		float[] fogcolor = { 0.0f, 0.0f, 0.0f, 1f };
		gl.glFogfv(GL.GL_FOG_COLOR, fogcolor, 0);
		gl.glEnable(GL.GL_FOG);

		// setup lighting
		Vector3d sun = refthing.getPosition(null, game.time());
		float[] lightPosition = {(float) sun.x, (float) sun.y, (float) sun.z, 0.0f };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition, 0);
		float ambient_mag = 0.33f;
		float[] lightAmbient = { ambient_mag, ambient_mag, ambient_mag, 1.0f };
		float[] lightDiffuse = { 1 - ambient_mag, 1 - ambient_mag, 1 - ambient_mag, 1.0f };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse, 0);
		gl.glDisable(GL.GL_LIGHTING);

		// if an object is tracked, translate
		if (tracked != null)
		{
			Vector3d pos = tracked.getPosition(refthing, game.time());
			adjustRadius(pos);
			gl.glTranslated(-pos.x, -pos.y, -pos.z);
			/*
			Vector3d vel = tracked.getVelocity(refthing, game.time());
			ball.setModifier(new Orientation(vel, pos).getQuat());
			*/
		}

		// set up view volume
		viewvol.setup(gl);

		// render universe
		renderChildrenRecursive(refthing.getUniverse());

		// render navigation orbit
		if (prop_disporbit != null)
		{
			navorbit = (Conic) getForPropertyKey(prop_disporbit);
			if (navorbit != null)
			{
				// todo: origin?
				gl.glColor3f(0, 0, 1);
				renderConic(null, navorbit, 0, navt2 - navt1, true);
			}
		}

		// render selected obj's orbit
		if (selected != null)
		{
			renderConicFor(selected, true);
		}

		if (showEcliptic)
		{
			renderEclipticPlane();
		}

		gl.glPopAttrib();
	}

	void renderEclipticPlane()
	{
		gl.glPushAttrib(GL.GL_ENABLE_BIT);

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glColor4f(1.0f, 1.0f, 0, 0.25f);

		texcache.setTexture("grid.png");
		gl.glBegin(GL.GL_QUADS);
		double r = getViewDistance() * 2;
		gl.glVertex3d(-r, -r, 0);
		gl.glVertex3d(r, -r, 0);
		gl.glVertex3d(-r, r, 0);
		gl.glVertex3d(r, r, 0);
		gl.glEnd();

		gl.glPopAttrib();
	}

	///

	public void setNavOrbit(Conic o, long t1, long t2)
	{
		this.navorbit = o;
		this.navt1 = t1;
		this.navt2 = t2;
	}

	///

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOKeyEvent)
		{
			GLOKeyEvent keyev = (GLOKeyEvent) event;
			if (keyev.isPressed())
			{
				/*
				switch ( keyev.getKeyCode() )
				{
					case GLOKeyEvent.VK_C:
						cameraMode = (cameraMode+1) % NUM_CAMERA_MODES;
				 	return true;
				}
				*/
			}
		}

		return super.handleEvent(event);
	}

	// ZOOM IN/OUT

	// these override the ones in GLODefault3DCanvas
	public void zoom(float x)
	{
		zoomToOrbit = zoomToPlanet = false;
		setViewScale(getViewScale() * x);
	}

	public void closer(float x)
	{
		zoom(x);
	}

	public float getViewScale()
	{
		return vscaleSmoother.getTarget();
	}

	public void setViewScale(float x)
	{
		vscaleSmoother.setTarget(x);
	}

	public String getPropertyForOrbitDisplay()
	{
		return getKey(prop_disporbit);
	}

	public void setPropertyForOrbitDisplay(String s)
	{
		this.prop_disporbit = new PropertyEvaluator(s);
	}

	public boolean getZoomToOrbit()
	{
		return zoomToOrbit;
	}

	public void setZoomToOrbit(boolean b)
	{
		this.zoomToOrbit = b;
		if (b)
			this.zoomToPlanet = false;
	}

	public boolean getZoomToPlanet()
	{
		return zoomToPlanet;
	}

	public void setZoomToPlanet(boolean b)
	{
		this.zoomToPlanet = b;
		if (b)
			this.zoomToOrbit = false;
	}

	public boolean getShowEcliptic()
	{
		return showEcliptic;
	}

	public void setShowEcliptic(boolean showEcliptic)
	{
		this.showEcliptic = showEcliptic;
	}

	public boolean getShowEquatorialPlane()
	{
		return showEquatorialPlane;
	}

	public void setShowEquatorialPlane(boolean showEquatorialPlane)
	{
		this.showEquatorialPlane = showEquatorialPlane;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(TacticalView.class);

	static {
		prophelp.registerGetSet("disporbit_prop", "PropertyForOrbitDisplay", String.class);
		prophelp.registerGetSet("zoom_to_orbit", "ZoomToOrbit", boolean.class);
		prophelp.registerGetSet("zoom_to_planet", "ZoomToPlanet", boolean.class);
		prophelp.registerGetSet("showecliptic", "ShowEcliptic", boolean.class);
		prophelp.registerGetSet("showequatorial", "ShowEquatorialPlane", boolean.class);
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

	//

	static float ORBIT_WIDTH;
	static float MIN_PLANET_RADIUS;
	static float MIN_RECURSE_RADIUS;
	static float MAX_ORBIT_RENDER_SCALE;

	static float THING_PIXSIZE;
	static boolean THING_LIGHTING;

	static float ORBIT_MIN_DEV;

	static SettingsGroup settings = new SettingsGroup(TacticalView.class, "Tactical")
	{
		public void updateSettings()
		{
			MIN_PLANET_RADIUS = getFloat("MinBodyRadius", 0.25f);
			MIN_RECURSE_RADIUS = getFloat("MinRecurseRadius", 0.5f);
			THING_PIXSIZE = getFloat("ObjectSize", 12.0f);
			THING_LIGHTING = false; // getBoolean("ObjectLighting", false);
			MAX_ORBIT_RENDER_SCALE = getFloat("MaxOrbitRenderScale", 16.0f);
			ORBIT_WIDTH = getFloat("OrbitWidth", 0.1f);
			ORBIT_MIN_DEV = getFloat("OrbitMinDev", 0.5f);
		}
	};
}
