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

import java.awt.Rectangle;
import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.game.*;
import com.fasterlight.exo.newgui.roam.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.SettingsGroup;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.spif.*;
import com.fasterlight.util.Plane4f;
import com.fasterlight.vecmath.*;

// todo: put colors in a file, or make Shaders
// todo: make things work with cockpit mode (shadows, particles, etc)

// todo: get rid of "w1", "h1", etc. (minviewsize/sin_fov) is our const
// todo: tracked & ref are same thing right???

public class VisualView
	extends ViewBase
	implements ThingSelectable, Constants, NotifyingEventObserver
{
	UniverseThing tracked;
	Vector3d sunpos = new Vector3d(1, 0, 0);

	boolean nightVision = false;
	boolean showLabels = false;
	CameraCapability extcamera, curcamera;

	ViewVolume viewvol = new ViewVolume();

	Planet shadow_planet;
	Plane4f shadow_planeeq;

	int minviewsize;
	boolean newcamera;

	ArrayList disprecs;

	//

	static final int DEBUG_SHOWAXES = 1;
	static final int DEBUG_SHOWFORCES = 2;
	static final int DEBUG_WIREFRAME = 4;
	static final int DEBUG_ROAM_PRINT_STATS = 8;
	static final int DEBUG_ROAM_COLORFLAGS = 16;
	static final int DEBUG_PLANETLIGHTING = 256;
	static final int DEBUG_FRUSTUM = 512;

	static int debugflags = 0;

	//

	static final int PTC_NOCLOUDS = 1;
	static final int PTC_ONLYCLOUDS = 2;
	static final int PTC_NIGHTVISION = 8;

	//

	public static final float NEARDIST = 1.0f / 1000; // km
	public static final float INT_NEARDIST = 0.1f / 1000; // km
	static final float FARDIST = 1e15f; // pretty far?
	static final int MAX_FRUSTUM_SECTIONS = 4;
	static final float VISRAD_SLOP = 1.01f; // a factor
	static final float FRUSTUM_SLOP = 50.0f / 1000; // km

	Matrix3f last_view_matrix = new Matrix3f();

	//

	public VisualView(SpaceGame game)
	{
		super(game);

		setFOV(60);
		MAX_FOV = 120f;
		MIN_FOV = 0.01f;
		neardist = NEARDIST;
		fardist = FARDIST;
		setViewDistance(0.05f);
		ball.setTarget(new Quat4d(0, 1, 0.17, 0));
	}

	public void setSize(int w, int h)
	{
		super.setSize(w, h);
	}

	public void init(GUIContext guictx)
	{
		super.init(guictx);

		extcamera = new CameraCapability(null);
		extcamera.setName("External Camera");
		extcamera.setAttitudeLocked(false);
		curcamera = extcamera;
		newcamera = false;
	}

	public boolean notifyObservedEvent(NotifyingEvent event)
	{
		return false;
	}

	public CameraCapability getCurrentCamera()
	{
		return curcamera;
	}

	public void setCurrentCamera(CameraCapability curcamera)
	{
		if (curcamera != null)
		{
			this.curcamera = curcamera;
			newcamera = true;
			if (curcamera.getModule() != null)
				game.hintMessage(
					curcamera.getModule().getName()
						+ " -- "
						+ curcamera.getName());
			else
				game.hintMessage(curcamera.getName());
		}
	}

	// pretty tricky .. if we're inside the module, render
	// the "cockpit" version
	void renderModule(Module m)
	{
		if (curcamera.isInterior() && curcamera.getModule() == m)
		{
			String name = m.getName();
			ModelRenderer mrend =
				rendcache.getModelRenderer(name + " Interior");
			if (mrend == null)
				super.renderModule(m);
			else
				mrend.render();
		}
		else
		{
			super.renderModule(m);
		}
	}

	float calcLighting(UniverseThing ut, PlanetRenderer prend)
	{
		// iterate thru our parents until we find the light source
		UniverseThing star = null;
		UniverseThing par = ut.getParent();
		while (par != null)
		{
			if (par instanceof Star)
			{
				star = par;
				break;
			}
			par = par.getParent();
		}
		// no sun?  forget it...
		if (star == null)
			return 0;

		// check for eclipses by parent bodies
		float level = 1.0f;
		par = ut.getParent();
		while (par != star)
		{
			double d = UniverseUtil.getLOSArea(ut, star, par, game.time());
			level *= (float) d;
			if (level == 0)
				break;
			par = par.getParent();
		}

		// check for eclipses by *small* child bodies
		// (todo!)

		float[] lightDiffuse;
		if (prend == null)
			lightDiffuse = new float[] { level, level, level, 0 };
		else
		{
			Vector3f col = prend.diffuseColor;
			lightDiffuse =
				new float[] { level * col.x, level * col.y, level * col.z };
		}
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse, 0);

		// make some ambient light if there is an atmosphere
		float temp;
		float l = AMBIENT_LIGHT;
		par = ut.getParent();
		if (level > 0 && par instanceof Planet)
		{
			Planet plan = (Planet) par;
			if (plan.getAtmosphere() != null)
			{
				float ceil = plan.getAtmosphere().getCeiling();
				float h =
					Math.min(1, (float) ut.getTelemetry().getALT() / ceil);
				l += (1 - h) * 0.2f * level;
			}
		}

		// nightVision has high ambient component
		if (nightVision)
			l = 0.5f;

		// todo: use prend.ambiendColor
		// set ambient - if spacecraft heating, make it red :)
		if (ut instanceof SpaceShip
			&& (temp = ((SpaceShip) ut).getStructure().getTemperature()) > 1200)
		{
			float red = (temp - 1200) / 2000;
			float[] lightRed = { l + red, l, l, 1.0f };
			gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lightRed, 0);
		}
		else
		{
			// TODO: ambient lighting for atmosphere
			// TODO: reflected lighting
			float[] lightNone = { l, l, l, 1.0f };
			gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lightNone, 0);
		}

		return level;
	}

	void renderStarburst(GLOContext ctx, Star ut, DisplayRec drec)
	{
		gl.glPushMatrix();

		texcache.setTexture("lens2.png");

		translateThing(ut);

		Matrix3f mat = new Matrix3f(last_view_matrix);
		mat.invert();
		GLOUtil.glMultMatrixf(gl, mat);

		float r = (drec.rad * STARBURST_SCALE); // *sin_fov);
		gl.glScalef(r, r, r);
		gl.glColor3f(0.95f, 0.95f, 0.95f); // todo: const

		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glEnable(GL.GL_BLEND);
		gl.glDisable(GL.GL_DEPTH_TEST);

		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2f(-1, -1);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2f(1, -1);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2f(1, 1);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2f(-1, 1);
		gl.glEnd();

		gl.glPopAttrib();
		gl.glPopMatrix();
	}

	void renderPlanet(GLOContext ctx, Planet ut, DisplayRec drec)
	{
		// translate by center of planet
		gl.glPushMatrix(); // #1

		Vector3d utpos = translateThing(ut);

		// find out how the radius of the planet in pixels on our screen
		double height = utpos.length() - drec.rad;
		if (drec.rad == 0)
		{
			gl.glPopMatrix();
			return; //todo?
		}

		// get planet rotate matrix
		Matrix3d mat = getPlanetRotateMatrix(ut);

		// get sun position
		sunpos.set(ut.getPosition(null, game.time()));

		double utdist = drec.rad * minviewsize / (height * fov / 90);

		PlanetRenderer prend = getPlanetRenderer(ut);
		prend.doClouds = DO_CLOUDS;

		// use simple display-list models if sufficiently close or far
		// todo: take screen size, fov into account
		int spherelevel = -1;

		// find out if we should use ROAM or a prerendered sphere
		if (!prend.isIrregular && height > 10)
		{
			if (Double.isNaN(utdist))
				utdist = 0;

			if (utdist < 16)
			{
				spherelevel = 7;
				// todo: what if don't have levels above 7?
			}
			else if (utdist < 128)
			{
				spherelevel = 8;
			}
			else if (utdist < 256)
			{
				spherelevel = 9;
			}
			else if (utdist < 512)
			{
				spherelevel = 10;
			}
			else if (utdist < 1024)
			{
				spherelevel = 11;
			} /* else
							spherelevel = 12; */
		}

		if (!USE_ROAM && spherelevel < 0)
			spherelevel = 12;

		// todo: transform sunpos by inv planet rotation
		Vector3d sunpos_rot = new Vector3d(sunpos);
		sunpos_rot.scale(-1);
		mat.transform(sunpos_rot);
		//		ut.ijk2llr(sunpos_rot, AstroUtil.tick2dbl(game.time()));
		prend.setSunPos(sunpos_rot);

		// calculate adjusted viewpoint
		Vector3d vp = new Vector3d(drec.pos);
		Vector3d cenvp = new Vector3d(drec.pos);
		// move computed viewpoint away from the ref object
		// by the camera view distance amt.
		if (curcamera == extcamera) // seems better when we DON'T do this??
		{
			Vector3f viewvec = new Vector3f(0, 0, -getViewDistance());
			last_view_matrix.transform(viewvec);
			//			curcamera.getOrientationMatrixd().transform(viewvec);
			vp.x += viewvec.x;
			vp.y += viewvec.y;
			vp.z += viewvec.z;
		}
		vp.scale(-1);
		mat.transform(vp); // translate into planet's coord system
		cenvp.scale(-1);
		mat.transform(cenvp); // translate into planet's coord system

		Vector3f viewvel = new Vector3f(); //todo

		//

		gl.glEnable(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_BLEND);

		// multiply by planet's rotation
		GLOUtil.glMultMatrixd(gl, mat);

		// show axes, if enabled
		if ((debugflags & DEBUG_SHOWAXES) != 0)
		{
			renderAxes(drec.rad * 1.25);
		}

		// render ROAM sphere

		if ((debugflags & DEBUG_ROAM_PRINT_STATS) != 0)
		{
			prend.roam.clearStatistics();
		}

		ROAMPlanet roam = prend.roam;
		if ((debugflags & DEBUG_WIREFRAME) != 0)
			roam.renderMode = roam.MODE_WIREFRAME;
		else if ((debugflags & DEBUG_ROAM_COLORFLAGS) != 0)
			roam.renderMode = roam.MODE_COLORFLAGS;
		else
			roam.renderMode = roam.MODE_NORMAL;

		Vector3f viewp = new Vector3f(vp);

		// if this planet is our viewpoint's parent,
		// save some information for doing shadows later
		if (ut == tracked.getParent())
		{
			shadow_planet = ut;
			Telemetry tele = tracked.getTelemetry();
			float agl = (float) tele.getALTAGL();
			Vector3f nml = new Vector3f(tele.getCenDistVec());
			nml.scale(-1.0f / nml.length());
			shadow_planeeq = new Plane4f(nml, -agl);
		}

		if (prend.doNormals)
		{
			float intensity = calcLighting(ut, prend);
			if (intensity > 0.5f / 255)
				gl.glEnable(GL.GL_LIGHTING);
			else
			{
				gl.glDisable(GL.GL_LIGHTING);
				gl.glColor3f(0, 0, 0);
			}
		}
		else
			gl.glDisable(GL.GL_LIGHTING);

		if (spherelevel >= 0)
		{
			prend.renderWithSectors(spherelevel, viewp, drec.r);
		}
		else
		{
			// render planet
			prend.render(viewp, viewvel, this, fov, minviewsize);
		}

		prend.renderRings();

		gl.glPopMatrix(); // #1

		if ((debugflags & DEBUG_ROAM_PRINT_STATS) != 0)
		{
			prend.roam.printStatistics();
			if (prend.roam.viewvol != null)
				prend.roam.viewvol.printStats(System.out);
			debugflags &= ~DEBUG_ROAM_PRINT_STATS;
		}
	}

	PlanetRenderer getPlanetRenderer(Planet planet)
	{
		PlanetRenderer prend = guictx.getPlanetRenderer(planet);
		prend.nightVision = nightVision;
		return prend;
	}


	//
	//
	//

	void renderThing(GLOContext ctx, UniverseThing ut, DisplayRec drec)
	{

		if (ut instanceof SpaceBase && drec.ang < 1)
		{
			return;
		}

		GL gl = ctx.getGL();

		// first decide whether to show labels!
		if (showLabels && drec.ang < MIN_SHOWLABEL_PIXSIZE)
		{
			boolean showLabel = true;
			// don't show the label if the object and its parent are
			// sufficiently close in angle
			if (drec.parentrec != null)
			{
				Vector3d dvec = new Vector3d(drec.parentrec.pos);
				dvec.sub(drec.pos);
				double dot = dvec.dot(drec.parentrec.pos) * getWidth()
						/ (drec.parentrec.pos.lengthSquared() * getSinFOV());
				showLabel = (Math.abs(dot) > MIN_SHOWLABEL_PIXSIZE*2);
			}
			if (showLabel)
			{
				addPickPoint(PICK_RAD, ut, drec.pos);
				showHintFor(ut);
			}
		}

		// if it is too small to draw, draw as a point
		if (drec.ang < 1)
		{
			// check for LOS
			if (ut.getParent() != tracked.getParent()
				|| UniverseUtil.hasLOS(
					tracked,
					ut,
					tracked.getParent(),
					game.time()))
			{
				gl.glDisable(GL.GL_TEXTURE_2D);
				gl.glBegin(GL.GL_POINTS);
				if (nightVision)
					gl.glColor3f(1, 1, 1);
				else
				{
					// todo: proper magnitude
					float mag = drec.ang;
					gl.glColor3f(mag, mag, mag);
				}
				gl.glVertex3d(drec.pos.x, drec.pos.y, drec.pos.z);
				gl.glEnd();
			}
		}
		// test for frustum inclusion and LOS
		else if (
			viewvol.intersectsSphere(drec.pos, drec.visrad)
				&& UniverseUtil.getLOSArea(
					tracked,
					ut,
					tracked.getParent(),
					game.time())
					> 0)
		{
			// push some attribs, to be safe...
			gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_LIGHTING_BIT | GL.GL_POLYGON_BIT);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glColor3f(1, 1, 1);

			// check for LOS
			if (ut instanceof Planet)
			{
				if (ut instanceof Star)
				{
					if (drec.ang > MIN_STAR_ANGLE)
						renderPlanet(ctx, (Star) ut, drec);
					renderStarburst(ctx, (Star) ut, drec);
				}
				else
				{
					renderPlanet(ctx, (Planet) ut, drec);
				}
			}
			else if (ut instanceof SpaceShip)
			{
				// calculate lighting

				float lightlevel = calcLighting(ut, null);
				gl.glEnable(GL.GL_LIGHTING);

				// draw shadow
				Telemetry telem = ut.getTelemetry();
				if (shadow_planet != null
					&& lightlevel > 0.01f
					&& !nightVision
					&& ut.getParent() == shadow_planet
					&& minviewsize
						* ut.getRadius()
						/ (sin_fov * telem.getALTAGL())
						> 1)
				{
					gl.glPushAttrib(GL.GL_ENABLE_BIT);
					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
					gl.glDisable(GL.GL_DEPTH_TEST);
					gl.glDisable(GL.GL_LIGHTING);
					gl.glDisable(GL.GL_CULL_FACE);
					gl.glEnable(GL.GL_BLEND);
					gl.glPushMatrix();
					gl.glLoadIdentity();

					Vector3d lightpos = ut.getPosition(null, game.time());
					lightpos.normalize();
					float[] arr = getShadowMatrix(shadow_planeeq, lightpos);
					//					System.out.println(shadow_planeeq + "\t" + shadow_planeeq.distFromPt(new Vector3f()));

					transformForView(gl);

					gl.glMultMatrixf(arr, 0);

					translateThing(ut);
					rotateThing(ut);

					gl.glColor4f(0, 0, 0, lightlevel);
					renderSpaceShipShadow((SpaceShip) ut);
					// todo: render smoke shadow
					gl.glPopMatrix();
					gl.glPopAttrib();
				}

				gl.glPushMatrix(); // #1

				translateThing(ut);
				gl.glPushMatrix(); // #2

				rotateThing(ut);
				renderSpaceShip((SpaceShip) ut);

				// show forces on ship
				if ((debugflags & DEBUG_SHOWFORCES) != 0)
				{
					gl.glDisable(GL.GL_BLEND);
					gl.glDisable(GL.GL_LIGHTING);
					gl.glDisable(GL.GL_TEXTURE_2D);
					AeroForces af =
						((SpaceShip) ut).getStructure().getLastForces();
					if (af != null)
					{
						gl.glBegin(GL.GL_LINES);

						gl.glColor3f(1, 0, 0);
						Vector3d p = af.pf.f;
						gl.glVertex3d(0, 0, 0);
						double s = 0.0002;
						gl.glVertex3d(p.x * s, p.y * s, p.z * s);

						gl.glColor3f(0, 1, 0);
						p = new Vector3d(af.pf.T);
						gl.glVertex3d(0, 0, 0);
						gl.glVertex3d(p.x * s, p.y * s, p.z * s);

						gl.glColor3f(0, 0, 1);
						s *= 100;
						p = ut.getTrajectory().getAngularVelocity();
						gl.glVertex3d(0, 0, 0);
						gl.glVertex3d(p.x * s, p.y * s, p.z * s);

						gl.glEnd();
					}
					if (ut instanceof StructureThing)
						renderContactPoints((StructureThing) ut);
				}

				gl.glPopMatrix(); // #2

				// particles are drawn w.r.s. the spaceship
				// but with fixed orientation
				renderParticles((SpaceShip) ut);

				gl.glPopMatrix(); // #1
			}
			else if (ut instanceof SpaceBase)
			{
				SpaceBase base = (SpaceBase) ut;

				// calculate lighting
				calcLighting(ut, null);
				gl.glEnable(GL.GL_LIGHTING);
				gl.glPushMatrix();
				translateThing(ut);
				rotateThing(ut);
				gl.glScalef(0.001f, 0.001f, 0.001f);
				gl.glDisable(GL.GL_BLEND);
				ModelRenderer mrend =
					rendcache.getModelRenderer(base.getModelName());
				if (mrend != null)
					mrend.render();
				gl.glPopMatrix();
			}

			gl.glPopAttrib();
		}
	}

	//
	// RENDER LIST
	// We render things in reverse Z-order here...
	//

	void addDispRec(DisplayRec drec)
	{
		disprecs.add(drec);
	}

	public void renderObject(GLOContext ctx)
	{
		tracked = getTracked();
		if (tracked == null)
			return;

		// if the object has disappered, return
		if (tracked.getTrajectory() == null)
		{
			setCurrentCamera(extcamera);
			tracked = null;
			return; //todo?
		}

		if (curcamera == extcamera)
		{
			if (curcamera.getReference() != tracked)
			{
				curcamera.setReference(tracked);
				curcamera.setViewDistance(
					(float) tracked.getRadius() * DEFAULT_VIEW_DIST_SCALE);
				//todo:const
			}
		}
		else
		{
			setTracked(curcamera.getThing());
		}

		minviewsize = Math.min(w1, h1);

		picklist.clear();
		shadow_planet = null;

		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDisable(GL.GL_DEPTH_TEST);

		// render stars
		gl.glPushMatrix();
		float sr = fardist * 0.1f;
		gl.glScalef(sr, sr, sr);
		float star_alpha = minviewsize * STAR_ALPHA_FACTOR / fov;
		if (nightVision)
			star_alpha *= 4;
		guictx.renderStars(star_alpha);
		gl.glPopMatrix();

		// setup lighting
		sunpos.set(tracked.getPosition(null, game.time()));
		float[] lightPosition =
			{(float) - sunpos.x, (float) - sunpos.y, (float) - sunpos.z, 0.0f };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition, 0);

		float ambient_mag = 0.1f;
		float[] lightAmbient =
			{ ambient_mag / 3, ambient_mag / 3, ambient_mag, 1.0f };
		float[] lightDiffuse =
			{ 1 - ambient_mag / 3, 1 - ambient_mag / 3, 1 - ambient_mag, 1.0f };
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse, 0);
		float[] lightNone = { 0, 0, 0, 1.0f };
		gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lightNone, 0);
		gl.glEnable(GL.GL_LIGHT0);
		float[] materialcol = { 1, 1, 1, 1 };
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, materialcol, 0);
		gl.glEnable(GL.GL_NORMALIZE); // just in case...

		// setup view volume
		viewvol.setup(gl);
		// can we disable the FAR plane?  it does us no good...
		viewvol.setNumPlanes(5);

		// build DisplayRec list
		disprecs = new ArrayList();

		// add children recursively, starting from the Universe object

		addChildrenRecursive(tracked.getUniverse(), null);

		// sort reverse by distance

		Collections.sort(disprecs);

		// now render in order

		computeFrustumSections();
		renderDisplayRecs();

		// draw the foreground stuff, like the pitch wheel

		drawForegroundStuff();

		disprecs = null;
	}

	//

	void addChildrenRecursive(UniverseThing p, DisplayRec parentrec)
	{
		Iterator it = p.getChildren();
		while (it.hasNext())
		{
			UniverseThing ut = (UniverseThing) it.next();
			addDispRecRecursive(ut, parentrec);
		}
	}

	// render the object and its children,
	// if the price is right
	void addDispRecRecursive(UniverseThing ut, DisplayRec parentrec)
	{
		DisplayRec drec = new DisplayRec(ut);
		drec.parentrec = parentrec;
		addDispRec(drec);

		// if no children, just return
		if (!ut.getChildren().hasNext())
			return;

		boolean drawchildren = false;
		if (ut instanceof Star)
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
				double adjrad = infrad;
				Vector3d pos = drec.pos;
				// only recurse if influence radius is above
				// certain amount, and if it intersects the view volume
				// (todo: too much stuff recomputed)
				if (adjrad * minviewsize / (drec.r * sin_fov)
					> MIN_VIEW_PIXSIZE)
				{
					//					System.out.print(ut + " " + adjrad + " " + pos + " " + drawchildren);
					if (viewvol.intersectsSphere(pos, adjrad))
						drawchildren = true;
					//					System.out.println(drawchildren);
				}
			}
		}

		// if drawchildren == true, recurse
		if (drawchildren)
		{
			addChildrenRecursive(ut, drec);
		}
	}

	//

	void renderParticles(SpaceShip ship)
	{
		ShipParticleSystemManager spsm =
			(ShipParticleSystemManager) guictx.partsyss.get(ship);
		if (spsm == null)
		{
			spsm = new ShipParticleSystemManager(this, ship);
			guictx.partsyss.put(ship, spsm);
		}
		spsm.renderParticles();
	}

	public double getSinFOV()
	{
		return sin_fov;
	}

	public double getCosFOV()
	{
		return cos_fov;
	}

	// used to sort objects for display

	class DisplayRec implements Comparable
	{
		UniverseThing thing;
		Vector3d pos; // distance from 'tracked'
		double r; // radius of 'pos'
		float rad; // radius
		float visrad; // visible radius
		float frustrad; // radius in frustum (not visible radius)
		float ang; // pixel size
		FrustumSection fsect;
		DisplayRec parentrec;

		DisplayRec(UniverseThing thing)
		{
			this.thing = thing;
			this.pos = thing.getPosition(tracked, game.time());
			this.rad = (float) thing.getRadius();
			this.visrad = thing.getVisibleRadius() * VISRAD_SLOP;
			this.frustrad = this.rad * VISRAD_SLOP;

			if (thing == tracked)
			{
				this.r = 0;
				this.ang = 10000; // big number! (because it's US)
				this.visrad += getViewDistance();
				this.frustrad += getViewDistance();
			}
			else
			{
				this.r = pos.length();
				this.ang =
					(float) (visrad * minviewsize / (r * getSinFOV() + 0.001));
			}
		}
		public int hashCode()
		{
			return thing.hashCode();
		}
		public boolean equals(Object o)
		{
			if (!(o instanceof DisplayRec))
				return false;
			return ((DisplayRec) o).thing.equals(thing);
		}
		public int compareTo(Object o)
		{
			return AstroUtil.sign(((DisplayRec) o).r - r);
		}
	}

	/**
	  * Represents the near & far planes for the frustum,
	  * and has a merge() operation to merge with another object.
	  */
	class FrustumSection extends Tuple2f
	{
		FrustumSection()
		{
			super(FARDIST, 0);
		}
		FrustumSection(float near, float far)
		{
			super(near, far);
		}
		float getNear()
		{
			return x;
		}
		float getFar()
		{
			return y;
		}
		void setNear(float x)
		{
			this.x = x;
		}
		void setFar(float y)
		{
			this.y = y;
		}
		void merge(DisplayRec drec)
		{
			float near, far;
			near = (float) (drec.r - drec.frustrad) - FRUSTUM_SLOP;
			far = (float) (drec.r + drec.frustrad) + FRUSTUM_SLOP;
			this.x = Math.max(NEARDIST, Math.min(this.x, near));
			this.y = Math.max(this.y, far);
		}
		boolean intersects(DisplayRec drec)
		{
			float rad = drec.frustrad;
			float drec_near = (float) (drec.r - rad - FRUSTUM_SLOP);
			float drec_far = (float) (drec.r + rad + FRUSTUM_SLOP);
			if (((debugflags & DEBUG_FRUSTUM) != 0))
				System.out.println(
					drec.thing
						+ ": "
						+ this
						+ ", "
						+ drec_near
						+ ", "
						+ drec_far);
			return !(drec_far < getNear() || drec_near > getFar());
		}
	}

	//

	// We start with the farthest visible object,
	// create a FrustumSection object and set it's near & far planes.
	// Then we crawl up & down the tree, adding objects to that frustum
	// that meet certain criteria of closeness.
	void computeFrustumSections()
	{
		FrustumSection fsect = null;
		int mode = 0;
		UniverseThing refparent = tracked.getParent();

		int i;
		int l = disprecs.size();
		for (i = 0; i < l; i++)
		{
			DisplayRec drec = (DisplayRec) disprecs.get(i);
			// is this an internal camera?
			if (drec.thing == tracked
				&& curcamera != extcamera
				&& curcamera.getShip() == tracked)
			{
				// make this a separate frustum section
				fsect = new FrustumSection(INT_NEARDIST, drec.frustrad);
				// merge it with all nearby SpaceShip objects
				for (int j = i - 1; j >= 0; j--)
				{
					// if this object is near the 'tracked' object,
					// assign its view frustum to be the same
					// (this fixes the "stage separation" problem)
					// TODO: does this really work?
					DisplayRec mergedrec = (DisplayRec) disprecs.get(j);
					if (mergedrec.thing instanceof SpaceShip
						&& mergedrec.r < mergedrec.frustrad + drec.frustrad)
					{
						mergedrec.fsect = fsect;
						if (((debugflags & DEBUG_FRUSTUM) != 0))
							System.out.println("   Merged " + mergedrec.thing);
					}
					else
						break;
				}
				if (((debugflags & DEBUG_FRUSTUM) != 0))
					System.out.println("   Internal view: " + fsect);
			}
			// is this the thing we are orbiting?
			else if (drec.thing == refparent && drec.ang > MIN_VIEW_PIXSIZE)
			{
				// are we near the planet surface?
				if (drec.thing instanceof Planet && drec.r < drec.frustrad)
				{
					Planet planet = (Planet) (drec.thing);
					float chicken_sacrifice = drec.rad / 200;
					double height = drec.r - planet.getMinRadius();
					if (height < NEARDIST)
						height = NEARDIST;
					float horiz_dist =
						(float) planet.getHorizonDist(
							height + chicken_sacrifice);
					/*
					fardist = horiz_dist;
					neardist =
						(float) Math.min(
							getViewDistance() / 2,
							Math.max(NEARDIST, height));
					neardist = NEARDIST;
					fsect = new FrustumSection(neardist, fardist);
					*/
					fsect = new FrustumSection(NEARDIST, horiz_dist);
					if (((debugflags & DEBUG_FRUSTUM) != 0))
						System.out.println("   Near surface: " + drec.thing
								+ " " + horiz_dist + " " + height + " -> "
								+ fsect);
					mode = 3;
				}
				else
				{
					// we are "far" from the surface...
					fsect = new FrustumSection();
					fsect.merge(drec);
					if (((debugflags & DEBUG_FRUSTUM) != 0))
					{
						System.out.println("   Far surface: " + drec.thing
								+ " " + neardist + " " + fardist);
						System.out.println("      " + drec.r + " "
								+ drec.frustrad);
					}
					mode = 1;
				}
			}
			else if (mode == 1 && drec.ang > MIN_VIEW_PIXSIZE)
			{
				// far mode, so make the "near" frustum for the ship & other
				// stuff in front of the planet
				if (!fsect.intersects(drec))
					fsect = new FrustumSection();
				fsect.merge(drec);
				if (((debugflags & DEBUG_FRUSTUM) != 0))
				{
					System.out.println("  Far mode: " + drec.thing + " -> " + fsect);
				}
			}
			else if (mode == 2)
			{
				fsect.merge(drec);
			}
			// todo: interior cameras
			drec.fsect = fsect;
		}
	}

	void renderDisplayRecs()
	{
		// going in we should have a big ol' frustum
		// and depth test should be disabled

		FrustumSection fsect = null;
		boolean first = true;
		int section_count = 0;

		Iterator it = disprecs.iterator();
		while (it.hasNext())
		{
			DisplayRec drec = (DisplayRec) it.next();
			if (drec.fsect != fsect)
			{
				// if null, disable depth testing and make a really big frustum
				if (drec.fsect == null)
				{
					if (fsect != null)
					{
						gl.glDisable(GL.GL_DEPTH_TEST);
					}
					this.neardist = NEARDIST;
					this.fardist = FARDIST;
					super.setPerspective(ctx);
					fsect = drec.fsect;
				}
				else
				{
					// if fsect was null, we gotta enable depth testing
					if (fsect == null)
					{
						gl.glEnable(GL.GL_DEPTH_TEST);
					}
					// clear depth buffer OR do glDepthRange()
					if (MAX_FRUSTUM_SECTIONS > 0)
					{
						if (((debugflags & DEBUG_FRUSTUM) != 0))
							System.out.println(
								"SECTION " + section_count + ": " + drec.fsect);
						float inc = (1f / MAX_FRUSTUM_SECTIONS);
						gl.glDepthRange(
							1 - (section_count + 1) * inc,
							1 - section_count * inc);
						section_count++;
					}
					else
					{
						if (!first)
						{
							gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
						}
						else
						{
							first = false;
						}
					}
					fsect = drec.fsect;
					// set our planes and change the GL frustum
					this.neardist = fsect.getNear() / 2;
					this.fardist = fsect.getFar() * 2;
					super.setPerspective(ctx);
				}

				if (((debugflags & DEBUG_FRUSTUM) != 0))
					System.out.println(
						drec.thing
							+ ": "
							+ fsect
							+ " "
							+ drec.r
							+ " "
							+ drec.frustrad
							+ " "
							+ drec.ang);
			}

			renderThing(ctx, drec.thing, drec);
		}
	}

	//

	protected void transformForView(GL gl)
	{
		transformForCamera(gl, curcamera);
	}

	protected void transformForCamera(GL gl, CameraCapability cam)
	{
		UniverseThing ref = cam.getReference();
		Vector3f vofs = cam.getViewOffset();

		// if vofs is nonnull, we have an internal (fixed) camera
		if (vofs == null && ref != null)
		{
			gl.glTranslatef(0, 0, - getViewDistance());
		}

		// modify the cached matrix for future methods
		// to use (like particle mgr)
		Matrix3f mat = last_view_matrix;
		// if we have a fixed camera, get the rot matrix from
		// the orientation matrix
		if (vofs != null)
		{
			mat.set(cam.getOrientationMatrix());
		}
		else
		{
			// else use the trackball
			// use the camera's matrix as the reference coord system
			mat.set(ball.getQuat());
			mat.mul(extcamera.getOrientationMatrix(), mat);
		}
		// convert to gl
		GLOUtil.glMultMatrixf(gl, mat);

		// if internal camera, move the viewpoint
		if (vofs != null)
		{
			Orientation refort =
				cam.getReference().getTelemetry().getOrientationFixed();
			refort.transform(vofs);
			gl.glTranslatef(vofs.x, vofs.y, vofs.z);
			Vector3f jitofs = curcamera.getJitterOfs();
			if (jitofs != null)
				gl.glTranslatef(jitofs.x, jitofs.y, jitofs.z);
		}
	}

	public Matrix3f getLastViewMatrix()
	{
		return last_view_matrix;
	}

	/*
		protected void rotateTrackball(float x1, float y1, float x2, float y2)
		{
			extcamera.getViewOrientation().rotateTrackball(0.5f, x1, y1, x2, y2);
		}
	*/

	protected void update()
	{
		if (curcamera == extcamera)
		{
			ball.update();
		}
	}

	public void renderForeground(GLOContext ctx)
	{
		super.renderForeground(ctx);

		if (nightVision)
		{
			getContext().set2DProjection();
			setShader("nightvision");
			Rectangle rect = getBounds();
			drawBox(ctx, rect.x, rect.y, rect.width, rect.height);
		}
	}

	void drawForegroundStuff()
	{
		/*
		{
			gl.glLoadIdentity();
			rotateByCameraPOV();
			ModelRenderer mrend = rendcache.getModelRenderer("pitchwheel",
				ModelRenderer.WIREFRAME |  ModelRenderer.NO_NORMALS | ModelRenderer.NO_MATERIALS);
			if (mrend != null)
			{

				gl.glPushAttrib(GL.GL_ENABLE_BIT);
				gl.glDisable(GL.GL_CULL_FACE);
				gl.glDisable(GL.GL_DEPTH_TEST);
				gl.glColor3f(0,1,0);
				mrend.render();
				gl.glPopAttrib();
			}
		}
		*/
	}

	protected void setPerspective(GLOContext ctx)
	{
		float newfov = (float) Math.toDegrees(curcamera.getFOV());
		float newdist = curcamera.getViewDistance();
		// if new camera, "jump" immediately to FOV
		if (newcamera)
		{
			setFOV(newfov);
			setViewDistance(newdist);
			newcamera = false;
		}
		else
		{
			setTargetFOV(newfov);
			setTargetViewDistance(newdist);
		}

		UniverseThing ref = curcamera.getReference();

		// we have to make the depth buffer happy, so we'll start
		// with a very large view frustum
		// this is also what we'll use to cull DisplayRecs

		neardist = NEARDIST;
		fardist = FARDIST;
		if (ref != null)
			MIN_DIST = (float) ref.getRadius() + NEARDIST;

		// call super to perform glPerspective() call
		super.setPerspective(ctx);
	}

	public void render(GLOContext ctx)
	{
		super.render(ctx);
	}

	public void setNextCamera() //todo
	{
		if (!(getTracked() instanceof SpaceShip))
			return;
		SpaceShip ship = (SpaceShip) getTracked();
		List cameras =
			ship.getStructure().getCapabilitiesOfClass(CameraCapability.class);
		if (cameras.size() == 0)
			return;
		for (int i = 0; i < cameras.size(); i++)
		{
			if (curcamera == cameras.get(i))
			{
				if (i == cameras.size() - 1)
				{
					setCurrentCamera(extcamera);
					return;
				}
				else
				{
					setCurrentCamera((CameraCapability) cameras.get(i + 1));
					return;
				}
			}
		}
		setCurrentCamera((CameraCapability) cameras.get(0));
	}

	public void setNextCamera(int i)
	{
		setNextCamera(); //todo
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOKeyEvent)
		{
			GLOKeyEvent keyev = (GLOKeyEvent) event;
			if (keyev.isPressed() && keyev.getFlags() == GLOKeyEvent.MOD_CTRL)
			{
				switch (keyev.getKeyCode())
				{
					case GLOKeyEvent.VK_P :
						debugflags ^= DEBUG_ROAM_PRINT_STATS;
						return true;
					case GLOKeyEvent.VK_C :
						debugflags ^= DEBUG_ROAM_COLORFLAGS;
						return true;
					case GLOKeyEvent.VK_W :
						debugflags ^= DEBUG_WIREFRAME;
						return true;
					case GLOKeyEvent.VK_F :
						debugflags ^= DEBUG_SHOWFORCES;
						return true;
					case GLOKeyEvent.VK_A :
						debugflags ^= DEBUG_SHOWAXES;
						return true;
					case GLOKeyEvent.VK_R :
						debugflags ^= DEBUG_FRUSTUM;
						return true;
					case GLOKeyEvent.VK_B :
						System.out.println("trackball = " + ball.getDest());
						return true;
					default :
						//   	         	System.out.println("keycode: " + keyev.getKeyCode());
						break;
				}
			}
		}

		return super.handleEvent(event);
	}

	//

	public boolean getNightVision()
	{
		return nightVision;
	}

	public void setNightVision(boolean nightVision)
	{
		this.nightVision = nightVision;
	}

	public boolean getShowLabels()
	{
		return showLabels;
	}

	public void setShowLabels(boolean showLabels)
	{
		this.showLabels = showLabels;
	}

	public int getDebugFlags()
	{
		return debugflags;
	}

	public void setDebugFlags(int debugflags)
	{
		this.debugflags = debugflags;
	}

	//

	boolean debug = !true;

	// PROPERTIES

	private static PropertyHelper prophelp =
		new PropertyHelper(VisualView.class);

	static {
		prophelp.registerGetSet(
			"camera",
			"CurrentCamera",
			CameraCapability.class);
		prophelp.registerSet("nextcamera", "setNextCamera", int.class);
		prophelp.registerGetSet("nightvision", "NightVision", boolean.class);
		prophelp.registerGetSet("showlabels", "ShowLabels", boolean.class);
		prophelp.registerGetSet("debugflags", "DebugFlags", int.class);
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

	// SETTINGS

	static float DEFAULT_VIEW_DIST_SCALE; // times * obj radius
	static float STAR_ALPHA_FACTOR;
	static float STARBURST_SCALE;
	static float MIN_STAR_ANGLE;
	static int MIN_SHOWLABEL_PIXSIZE;
	static boolean USE_ROAM;
	static boolean DO_CLOUDS;
	static float planetLightingScale;
	static int MIN_VIEW_PIXSIZE;
	static float AMBIENT_LIGHT;
	static boolean DO_CAMERA_JITTER;

	static SettingsGroup settings =
		new SettingsGroup(VisualView.class, "Visual")
	{
		public void updateSettings()
		{
			DEFAULT_VIEW_DIST_SCALE = getFloat("InitialViewDistRadii", 6);
			STAR_ALPHA_FACTOR = getFloat("StarAlphaFactor", 0.25f);
			STARBURST_SCALE = getFloat("StarburstScale", 12);
			MIN_STAR_ANGLE = getFloat("MinStarAngle", 8);
			MIN_SHOWLABEL_PIXSIZE = getInt("MinShowLabelSize", 16);
			MIN_VIEW_PIXSIZE = getInt("MinViewSize", 4);
			USE_ROAM = getBoolean("UseROAM", true);
			DO_CLOUDS = getBoolean("DoClouds", true);
			planetLightingScale = getFloat("PlanetLightingContrast", 3);
			AMBIENT_LIGHT = getFloat("AmbientLight", 0.15f);
			DO_CAMERA_JITTER = getBoolean("DoCameraJitter", true);
		}
	};

}
