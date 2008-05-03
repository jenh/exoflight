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
package com.fasterlight.exo.newgui.roam;

import java.io.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.Settings;
import com.fasterlight.glout.*;
import com.fasterlight.math.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.proctex.TexKey;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
 * Takes care of rendering a planet sphere, with clouds, bumpmap, etc. todo: use
 * config .ini files
 */
public class PlanetRenderer implements Constants
{
	GUIContext guictx;
	Planet planet;
	GL gl;
	GLU glu;
	ViewVolume viewvol = new ViewVolume(); // for mesh renderer

	public ROAMPlanet roam, cloudroam;

	PlanetTextureCache ptc, cloudptc;
	PlanetTextureCache nightptc, normalptc;

	SectorMeshProvider smp;

	public float skyHeight = 0;
	public Func1d atmoStartFunc;
	public Func1d atmoEndFunc;

	public boolean nightVision = false;
	public boolean isIrregular = false;
	public boolean showAxes = false;
	public boolean doFog = false;
	public boolean doNormals = true;
	public boolean doNormalMap = false;
	public boolean doClouds = true;
	public boolean doDetail = true;
	public boolean doSkyBox = true;

	public boolean useMeshes = !true;
	public boolean showBoundingBoxes = true;

	Vector3d sunpos = new Vector3d(); // sun position vector in planet-space

	float neardist = 0.005f;

	static float FOG_BIAS = 20.0f;
	static float MIN_FOG_DIST = 20.0f;

	int cloudDetailPower = 23; // todo: const
	int colorDetailPower1 = 18;
	int colorDetailPower2 = 21;

	public Vector3f ambientColor = new Vector3f();
	public Vector3f diffuseColor = new Vector3f();

	String ringTexture;
	float ringInnerRadius, ringOuterRadius;

	private int displayList = 0;
	private int displayListMode = 0;
	private boolean frozen = false;

	static boolean atmoDithering = Settings.getBoolean("Visual", "AtmoDithering", false);

	float[] fogcolor = { 0.75f, 0.8f, 1f, 1f }; // todo: const

	//

	public PlanetRenderer(GUIContext guictx, Planet planet)
	{
		this.guictx = guictx;
		this.planet = planet;
		this.gl = guictx.getGL();
		this.glu = guictx.getGLU();
		String planetname = planet.getName();

		// get other params

		try
		{
			InputStream inistream;
			try
			{
				inistream = GLOUtil.getInputStream("texs/" + planetname + "/options.txt");
			} catch (IOException ioe)
			{
				System.out.println(ioe);
				inistream = new StringBufferInputStream("");
			}
			INIFile ini = new CachedINIFile(inistream);

			doNormals = ini.getBoolean("Settings", "normals", true);
			isIrregular = ini.getBoolean("Settings", "irregular", false);
			doNormalMap = guictx.glMultiTex && guictx.hasGLExtension("ARB_texture_env_combine")
					&& guictx.hasGLExtension("ARB_texture_env_dot3") && (guictx.glTexUnits >= 3);
			if (doNormalMap)
				doNormals = false; // TODO: turn off?
			doFog = ini.getBoolean("Settings", "fog", false);

			ambientColor.set(AstroUtil.parseVector(ini.getString("Settings", "ambientcolor",
				"0.05,0.05,0.05")));
			diffuseColor.set(AstroUtil.parseVector(ini.getString("Settings", "diffusecolor",
				"2.5,2.5,2.5")));

			// ring stuff
			ringTexture = ini.getString("Rings", "texture", null);
			ringInnerRadius = ini.getFloat("Rings", "inner_radius", 0);
			ringOuterRadius = ini.getFloat("Rings", "outer_radius", 0);

			roam = new ROAMPlanet(guictx, (float) planet.getRadius());

			if (guictx.glPaletting)
			{
				ptc = getPTC(ini, "Color", ptc.SRC_COLOR | ptc.DO_PALETTE, GL.GL_COLOR_INDEX);
			} else
				ptc = getPTC(ini, "Color", ptc.SRC_COLOR, GL.GL_RGB16);
			// ptc = getPTC(ini, "Color", ptc.SRC_NIGHT, GL.GL_RGB16);
			// ptc.nightptp.setPixelConfabulator(new
			// PointLightPixelConfabulator());
			// allptc = getPTC(ini, "All", ptc.SRC_COLOR | ptc.DO_BUMPMAP |
			// ptc.DO_CLOUDS, GL.GL_RGB16);

			ElevationModel elevmodel = planet.getElevationModel();

			if (elevmodel != null)
			{
				roam.setElevationModel(elevmodel);
				try
				{
					VarianceTree vt = new VarianceTree("texs/" + planetname + "/" + planetname
							+ ".vtr");
					roam.setVarianceTree(vt);
				} catch (IOException ioe)
				{
					System.out.println(ioe);
				}

				normalptc = getPTC(ini, "Normal", ptc.SRC_ELEV | ptc.DO_NORMALMAP, GL.GL_RGB);
			}

			// do we have clouds?
			skyHeight = ini.getFloat("Clouds", "height", 0);
			if (skyHeight > 0)
			{
				cloudroam = new ROAMPlanet(guictx, (float) planet.getMinRadius() + skyHeight);
				cloudroam.skyHeight = skyHeight;
				cloudptc = getPTC(ini, "Clouds", ptc.SRC_CLOUDS, GL.GL_INTENSITY);
				cloudptc.solidColor = 0;
				cloudroam.setTextureStage(0, cloudptc);
			}

			// how about an atmosphere?
			String tmp;
			tmp = ini.getString("Atmosphere", "texy1", "");
			if (tmp.length() > 0)
			{
				atmoStartFunc = CurveParser.parseCurve1d(tmp);
				tmp = ini.getString("Atmosphere", "texy2", "");
				atmoEndFunc = CurveParser.parseCurve1d(tmp);
			}

		} catch (IOException ioe)
		{
			ioe.printStackTrace();
			throw new RuntimeException(ioe.toString());
		}
	}

	public PlanetTextureCache getTextureCacheColor()
	{
		return ptc;
	}

	public boolean getFrozen()
	{
		return frozen;
	}

	public void setFrozen(boolean b)
	{
		this.frozen = b;
	}

	public boolean getCached()
	{
		return (displayListMode == 2);
	}

	public void setCached(boolean b)
	{
		if (getCached())
		{
			if (!b)
				displayListMode = 0;
		} else
		{
			if (b)
			{
				if (displayList == 0)
					displayList = gl.glGenLists(1);
				displayListMode = 1;
			}
		}
	}

	private PlanetTextureCache getPTC(INIFile ini, String section, int source, int gltype)
			throws IOException
	{
		source = Integer.parseInt(ini.getString(section, "source", "" + source));
		gltype = Integer.parseInt(ini.getString(section, "gltype", "" + gltype));
		PlanetTextureCache ptc = new PlanetTextureCache(planet, gl, source, gltype);
		ptc.NUM_GEN_LEVELS = Integer.parseInt(ini.getString(section, "genlevels", "0"));
		ptc.BUMPMAP_FACTOR = Float.parseFloat(ini.getString("Settings", "bumpmapfactor", "0.31"));
		ptc.MAX_ELEV_SCALE = Integer.parseInt(ini.getString("Settings", "maxelevscale", "20"));
		return ptc;
	}

	Vector3d sect_tmp1 = new Vector3d();
	Vector3d sect_tmp2 = new Vector3d();
	Vector3f sect_tmp3 = new Vector3f();

	// todo: GROSSLY overconservative
	// returns distance to center, or -1 if not in frustum
	public double sectorInFrustum(int x, int y, int level, Vector3f vp)
	{
		if (level <= 8)
			return 0.001; // true

		float xmin, ymin, zmin;
		float xmax, ymax, zmax;
		xmin = ymin = zmin = Float.MAX_VALUE;
		xmax = ymax = zmax = Float.MIN_VALUE;

		// compute bounding box for sector
		// todo: add factor for min-max elevation
		double prad = planet.getRadius();
		// System.out.println(x + " " + y + " " + level);
		for (int i = 0; i < 4; i++)
		{
			int xp = ((i & 1) == 0) ? x : (x + 1);
			int yp = ((i & 2) == 0) ? y : (y + 1);
			double lat = Math.PI / 2 - yp * Math.PI / (1 << (level - 8));
			double lon = -Math.PI + xp * Math.PI / (1 << (level - 8));
			Vector3d corner = sect_tmp1;
			corner.set(lon, lat, prad);
			// System.out.println(" " + corner);
			planet.llr2ijk(corner);
			xmin = Math.min(xmin, (float) corner.x);
			xmax = Math.max(xmax, (float) corner.x);
			ymin = Math.min(ymin, (float) corner.y);
			ymax = Math.max(ymax, (float) corner.y);
			zmin = Math.min(zmin, (float) corner.z);
			zmax = Math.max(zmax, (float) corner.z);
		}

		// check bounding box against frustum
		boolean inter = false;
		int flags = 0x3f;
		Vector3f p = sect_tmp3;
		for (int j = 0; j < 8; j++) // box corner
		{
			p.x = ((j & 1) == 0) ? xmax : xmin;
			p.y = ((j & 2) == 0) ? ymax : ymin;
			p.z = ((j & 4) == 0) ? zmax : zmin;
			flags &= viewvol.getFrustumFlags(p);
			if (flags == 0)
			{
				inter = true;
				break;
			}
		}
		/*
		 * if (!inter) { System.out.println(level+"-"+x+"-"+y+" f=" + flags); }
		 */
		if (showBoundingBoxes && inter)
		{
			gl.glPushMatrix();
			gl.glTranslated((xmin + xmax) / 2, (ymin + ymax) / 2, (zmin + zmax) / 2);
			gl.glScaled((xmax - xmin), (ymax - ymin), (zmax - zmin));
			gl.glColor3f(1, 1, 1);
			guictx.rendcache.getModelRenderer("box", ModelRenderer.WIREFRAME).render();
			gl.glPopMatrix();
		}

		if (inter)
		{
			Vector3f cen = sect_tmp3;
			// compute closest dist to 1 of the corner pts
			double mindist = Double.MAX_VALUE;
			for (int i = 0; i < 8; i++)
			{
				cen.x = ((i & 1) == 0) ? xmin : xmax;
				cen.y = ((i & 2) == 0) ? ymin : ymax;
				cen.z = ((i & 4) == 0) ? zmin : zmax;
				double dist = AstroUtil.vecdistsqr(cen, vp);
				if (dist < mindist)
					mindist = dist;
			}
			return mindist;
		} else
			return -1;
	}

	private void renderRecursiveMesh(int level, int x, int y, Vector3f vp)
	{
		double dist = sectorInFrustum(x, y, level, vp);
		if (dist > 0)
		{
			int desiredlevel;
			if (level < 12)
				desiredlevel = 12;
			else
			{
				desiredlevel = AstroUtil.log2((long) (planet.getRadius() * 1024 / Math.sqrt(dist)));
				// System.out.println(level+"-"+x+"-"+y+", desired=" +
				// desiredlevel + ", dist=" + dist);
			}
			if (level >= desiredlevel)
			{
				TexKey key = new TexKey(x, y, level);
				SectorMeshProvider.Mesh mesh = smp.getMesh(key);

				int texint = ptc.getTextureInt(key);
				gl.glBindTexture(GL.GL_TEXTURE_2D, texint);

				mesh.setTexCoords(key);
				mesh.render();
			} else
			{
				renderRecursiveMesh(level + 1, x * 2, y * 2, vp);
				renderRecursiveMesh(level + 1, x * 2 + 1, y * 2, vp);
				renderRecursiveMesh(level + 1, x * 2, y * 2 + 1, vp);
				renderRecursiveMesh(level + 1, x * 2 + 1, y * 2 + 1, vp);
			}
		}
	}

	public void renderWithMeshes(int spherelevel, Vector3f vp, double vpdist)
	{
		// setup the mesh provider
		if (smp == null)
		{
			smp = new SectorMeshProvider(planet, gl);
		}

		// use a quadtree approach -- some will get rendered with sphere
		// sectors from guictx, some will need meshes
		// todo
		// get dimension stuff

		viewvol.setup(gl);
		// viewvol.printStats(System.out);

		renderRecursiveMesh(8, 0, 0, vp);
		renderRecursiveMesh(8, 1, 0, vp);
	}

	public void renderWithSectors(int spherelevel, Vector3f vp, double vpdist)
	{
		ptc.update();

		if (spherelevel > 8)
		{
			roam.setViewpoint(vp);
			setupAtmosphereAndFog(vp);
		}

		if (useMeshes)
		{
			renderWithMeshes(spherelevel, vp, vpdist);
			spherelevel = 11;
			return; // todo
		} else
		{
			gl.glPushMatrix();
			float r = (float) planet.getRadius();
			gl.glScalef(r, r, r);

			renderPlanetSphere(planet, ptc, spherelevel);
		}

		boolean haveClouds = doClouds && cloudroam != null;
		if (haveClouds)
		{
			gl.glPushAttrib(GL.GL_ENABLE_BIT);
			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glPushMatrix();
			float sc = 1 + skyHeight / (float) planet.getRadius();
			gl.glScalef(sc, sc, sc);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL.GL_BLEND);
			renderPlanetSphere(planet, cloudptc, spherelevel);
			gl.glPopMatrix();
			gl.glPopAttrib();
		}

		gl.glPopMatrix();
	}

	public void renderRings()
	{
		if (ringTexture == null)
			return;

		SpaceGame game = planet.getUniverse().getGame();
		long time = game.time();
		double dtime = AstroUtil.tick2dbl(time);
		gl.glRotated((float) Math.toDegrees(-planet.getRotation(dtime)), 0, 0, 1);

		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_CULL_FACE);
		guictx.texcache.setTexture(ringTexture);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
		gl.glBegin(GL.GL_QUAD_STRIP);
		// gl.glNormal3f(0,0,1);
		int ndivs = 128;
		double rad = 0;
		double radinc = (Math.PI * 2) / ndivs;
		float tc = 0;

		UniverseThing sun = game.getBody("Sun");
		// get planet center
		float illum_i, illum_o;
		Vector3d pos = new Vector3d();

		for (int i = 0; i <= ndivs; i++)
		{
			float x = (float) Math.sin(rad);
			float y = (float) Math.cos(rad);

			// get illumination
			pos.set(-x * ringInnerRadius, -y * ringInnerRadius, 0);
			planet.ijk2xyz(pos);
			illum_i = UniverseUtil.hasLOS(pos, sun, planet, time) ? 1 : 0;

			pos.set(-x * ringOuterRadius, -y * ringOuterRadius, 0);
			planet.ijk2xyz(pos);
			illum_o = UniverseUtil.hasLOS(pos, sun, planet, time) ? 1 : 0;

			// output 2 vertices
			gl.glColor3f(illum_i, illum_i, illum_i);
			gl.glTexCoord2f(0, tc);
			gl.glVertex3f(x * ringInnerRadius, y * ringInnerRadius, 0);
			gl.glColor3f(illum_o, illum_o, illum_o);
			gl.glTexCoord2f(1, tc);
			gl.glVertex3f(x * ringOuterRadius, y * ringOuterRadius, 0);
			rad += radinc;
			tc += 1f / ndivs;
		}
		gl.glEnd();
		gl.glPopAttrib();
	}

	void renderPlanetSphere(Planet ut, PlanetTextureCache ptc, int spherelevel)
	{
		gl.glEnable(GL.GL_TEXTURE_2D);
		ptc.update();
		if (spherelevel <= 7)
		{
			ptc.setTexture(0, 0, 7);
			gl.glCallList(guictx.getNmlSphereIndex(3));
			return;
		}

		// get dimension stuff
		int x, y, xs, ys;
		xs = (1 << spherelevel) / 128;
		ys = (1 << spherelevel) / 256;

		// "fluff" the cache
		for (x = 0; x < xs; x++)
		{
			for (y = 0; y < ys; y++)
			{
				if (!ptc.hasTextureCached(x, y, spherelevel))
				{
					renderPlanetSphere(ut, ptc, spherelevel - 1);
					return;
				}
			}
		}

		// draw each sector of the sphere w/ a different texture

		gl.glPushMatrix();
		for (x = 0; x < xs; x++)
		{
			gl.glRotatef(360 / xs, 0, 0, 1);
			for (y = 0; y < ys; y++)
			{
				ptc.setTexture(x, y, spherelevel);
				gl.glCallList(guictx.getSphereSectorIndex(spherelevel - 8, y));
			}
		}
		gl.glPopMatrix();
	}

	double renderAtmosphere(Atmosphere atmo, Vector3f vpos)
	{
		// find angle to sun from viewpoint
		Vector3d vp = new Vector3d(vpos);
		double sundot = sunpos.dot(vp) / (vp.length() * sunpos.length());

		if (atmoStartFunc == null || atmoEndFunc == null || !doSkyBox)
			return sundot;

		float r = (float) planet.getMinRadius(); // height above ground
		double h = vp.length() - r;

		// get tex coordinates
		double texy1 = (float) atmoStartFunc.f(h);
		double texy2 = (float) atmoEndFunc.f(h);
		if (texy2 > 1000)
			return sundot;

		double ang = Math.PI / 2 - Math.asin(r / (h + r));
		// find ellipse dimensions
		double eb = Math.sin(ang) * r;
		double eh = Math.cos(ang) * r;

		// center the sky sphere on ground,
		// scale it to horizon
		gl.glPushMatrix();
		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
		if (atmoDithering)
			gl.glEnable(GL.GL_DITHER);

		Vector3d pos = new Vector3d(vp);
		pos.scale(eh / pos.length());
		gl.glTranslated(pos.x, pos.y, pos.z);
		gl.glScaled(eb, eb, eb);
		// find proper orientation of dome
		// todo: proper 'up' vector
		Orientation ort = new Orientation(vp);
		Matrix3d matrix = ort.getInvertedMatrix();
		GLOUtil.glMultMatrixd(gl, matrix);

		// manipulate texture matrix
		double texx = Math.acos(sundot) / Math.PI;
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		// System.out.println(texy1 + " " + texy2);
		gl.glTranslated(texx, texy2 * 2, 0);
		gl.glScaled(1, texy1 - texy2 * 2, 1);

		// load sky texture (todo)
		guictx.texcache.setTexture("sky1.png");
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glFrontFace(GL.GL_CW);
		gl.glColor4f(1f, 1f, 1f, 1f);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE); // _MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
		gl.glCallList(guictx.getHemiSphereIndex(1));

		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopAttrib();
		gl.glPopMatrix();

		// System.out.println("sundot=" + sundot);
		return sundot;
	}

	private void setDetailTex(ROAMPlanet rs, int unit, int power)
	{
		if (unit > 0)
			gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
		guictx.texcache.setTexture("detail1.png");
		rs.setTextureStage(unit, power);
		if (unit > 0)
			gl.glActiveTexture(GL.GL_TEXTURE0);
	}

	//

	private boolean belowClouds, haveClouds, closeClouds;

	private void renderROAMSpheres()
	{
		int glTexUnits = guictx.glTexUnits;
		roam.clearTextureStages();

		// if we are below the clouds, draw them first
		if (haveClouds && belowClouds)
		{
			if (doDetail && glTexUnits > 1)
			{
				setDetailTex(cloudroam, 1, cloudDetailPower);
			}
			gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_POLYGON_BIT);
			try
			{
				gl.glDisable(GL.GL_DEPTH_TEST);
				gl.glDisable(GL.GL_CULL_FACE);
				gl.glColor3f(1, 1, 1);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				gl.glEnable(GL.GL_BLEND);
				cloudroam.renderGo(guictx);
			} finally
			{
				gl.glPopAttrib();
			}
		}

		// set up roam texture caches
		roam.setTextureStage(0, ptc);
		if (doNormalMap && !nightVision)
		{
			// TODO : we don't need to compute normals when using a map
			// scale and bias light vector
			// normalize to 1/2 unit vector
			double sk = 0.5 / sunpos.length();
			gl.glColor4d(sk * sunpos.x + 0.5, sk * sunpos.y + 0.5, sk * sunpos.z + 0.5, 1);
			gl.glDisable(GL.GL_LIGHTING); // TODO: necc?

			// combine texture w/ light vector dot3
			roam.setTextureStage(0, normalptc);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_DOT3_RGB);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, GL.GL_TEXTURE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, GL.GL_PRIMARY_COLOR);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);

			// combine light map with texture color
			roam.setTextureStage(1, ptc);
			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_MODULATE);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, GL.GL_PREVIOUS);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, GL.GL_TEXTURE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_SRC_COLOR);

			// detail texture
			setDetailTex(roam, 2, colorDetailPower1);
			gl.glActiveTexture(GL.GL_TEXTURE2);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_MODULATE);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, GL.GL_PREVIOUS);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);

			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, GL.GL_TEXTURE);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_ADD_SIGNED);

			// detail texture 2
			if (glTexUnits > 3)
			{
				setDetailTex(roam, 3, colorDetailPower2);
				gl.glActiveTexture(GL.GL_TEXTURE3);
				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_MODULATE);

				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE0_RGB, GL.GL_PREVIOUS);
				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND0_RGB, GL.GL_SRC_COLOR);

				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SOURCE1_RGB, GL.GL_TEXTURE);
				gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_OPERAND1_RGB, GL.GL_ADD_SIGNED);
			}

			gl.glActiveTexture(GL.GL_TEXTURE0);
		} else
		{
			if (glTexUnits > 1)
			{
				setDetailTex(roam, 1, colorDetailPower1);
				if (glTexUnits > 2)
					setDetailTex(roam, 2, colorDetailPower2);
			} else
				roam.setNumberOfTexStages(1);
		}

		roam.renderGo(guictx);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
		gl.glEnable(GL.GL_LIGHTING);

		if (haveClouds && !belowClouds)
		{
			gl.glEnable(GL.GL_BLEND);
			gl.glColor3f(1, 1, 1);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDisable(GL.GL_DEPTH_TEST);
			ROAMPlanet thisroam;
			if (closeClouds)
			{
				roam.setTextureStage(0, cloudptc);
				float pr = (float) planet.getRadius();
				float rat = (pr + skyHeight) / pr;
				gl.glScalef(rat, rat, rat);
				thisroam = roam;
			} else
				thisroam = cloudroam;
			if (doDetail && glTexUnits > 1)
			{
				setDetailTex(thisroam, 1, cloudDetailPower);
			}
			thisroam.doubleSided = false;
			thisroam.renderGo(guictx);
		}

	}

	protected void setupAtmosphereAndFog(Vector3f vp)
	{
		Atmosphere atmo = planet.getAtmosphere();
		float sundot = 0;
		if (atmo != null)
		{
			sundot = (float) renderAtmosphere(atmo, vp);

			if (doFog && sundot > 0.05f)
			{
				gl.glEnable(GL.GL_FOG);
				gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
				float fogstart = Math.max(MIN_FOG_DIST, roam.height + FOG_BIAS);
				gl.glFogf(GL.GL_FOG_START, fogstart);
				gl.glFogf(GL.GL_FOG_END, Math.max(fogstart, Math.abs(roam.horizon_dist) / sundot)
						+ FOG_BIAS);
				// System.out.println("height=" + roam.height + " horizon=" +
				// roam.horizon_dist);
				gl.glFogfv(GL.GL_FOG_COLOR, fogcolor, 0);
			}
			gl.glDisable(GL.GL_BLEND); // todo: why needed?
		}
	}

	public void render(Vector3f vp, Vector3f vel, GLOComponent cmpt, float fov, int scrnheight)
	{
		roam.setViewpoint(vp);
		// TODO: should use reference point (ship) and not viewpoint?
		roam.setCenterPoint(vp);
		// todo: what if we change from normals to no normals?
		roam.doNormals = doNormals;

		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		try
		{
			gl.glEnable(GL.GL_CULL_FACE);

			// if atmosphere, draw a sky first
			setupAtmosphereAndFog(vp);

			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glPushMatrix();
			try
			{
				gl.glLoadIdentity();
				double neard = Math.max(neardist,
					(roam.vpl - planet.getMaxRadius() - skyHeight) / 2);
				glu.gluPerspective(fov, (float) cmpt.getWidth() / (float) cmpt.getHeight(), neard,
					roam.visible_dist);

				roam.setFrustum(gl);

				// find vel-per-frame
				/**
				 * Vector3f fvel = new Vector3f(vel);
				 * fvel.scale(1f/guictx.getLastFPS()); roam.setVelocity(fvel);
				 */

				roam.setScreenDims(scrnheight, fov);

				if (!frozen)
					roam.renderSetup();

				belowClouds = false;
				closeClouds = false;
				haveClouds = doClouds && !nightVision && cloudroam != null;
				if (haveClouds)
				{
					belowClouds = roam.height < skyHeight;
					closeClouds = (skyHeight * scrnheight * 90 / (roam.height * fov) < 1.0f);
					// todo!
					if (!closeClouds)
					{
						cloudroam.doNormals = roam.doNormals;
						cloudroam.doubleSided = true;
						cloudroam.setViewpoint(vp);
						gl.glLoadIdentity();
						glu.gluPerspective(fov, (float) cmpt.getWidth() / (float) cmpt.getHeight(),
							neard, cloudroam.visible_dist);
						cloudroam.setFrustum(gl);
						/**
						 * cloudroam.setVelocity(fvel);
						 */
						cloudroam.setScreenDims(scrnheight, fov);
						if (!frozen)
							cloudroam.renderSetup();
					}
				}
			} finally
			{
				gl.glPopMatrix();
				gl.glMatrixMode(GL.GL_MODELVIEW);
			}

			// render the ROAM clouds, and main surface
			switch (displayListMode)
			{
				default:
					renderROAMSpheres();
					break;
				case 1:
					System.out.println("Rendering to list " + displayList);
					gl.glNewList(displayList, GL.GL_COMPILE);
					renderROAMSpheres();
					gl.glEndList();
					GLOContext.checkGL();
					displayListMode = 2;
					break;
				case 2:
					gl.glCallList(displayList);
					GLOContext.checkGL();
					break;
			}

			ptc.update();
			if (cloudptc != null)
				cloudptc.update();
			if (normalptc != null)
				normalptc.update();

		} finally
		{
			gl.glPopAttrib();
		}
	}

	public void setSunPos(Vector3d sunpos)
	{
		this.sunpos.set(sunpos);
		ptc.setSunPos(sunpos);
		if (cloudptc != null)
			cloudptc.setSunPos(sunpos);
	}

	public Vector3d getSunPos()
	{
		return new Vector3d(sunpos);
	}

	// closes all ptc's and frees resources
	public void close()
	{
		if (guictx != null)
		{
			ptc.close();
			if (cloudptc != null)
				cloudptc.close();
			if (normalptc != null)
				normalptc.update();
			guictx = null;
		}
	}

}
