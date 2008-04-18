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

import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.proctex.TexKey;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Implements the ROAM algorithm as described in the ROAM paper,
  * except it's spherical and much more hairy.
  */
public class ROAMPlanet
{
	protected GUIContext guictx;

	// minimum, maximum radius of the sphere
	protected float refrad, minrad, maxrad;
	// eccentricity (maxrad-minrad)/maxrad
	protected float radecc, refcircum;

	// the current viewpoint
	protected Vector3f vp = new Vector3f();
	// the viewpoint to being rendering from
	protected float vpl; // vp.length()
	// the point which to divide terrain around
	protected Vector3f cenvp;

	protected Vector3f velvec = new Vector3f();
	protected boolean useVelocity = false;

	public ViewVolume viewvol;

	public int renderMode;

	public float ELEV_SCALE = 1.0f;

	public int SUBTEX_POWER = 9;
	public int SUBTEX_SCALE = (1 << SUBTEX_POWER);

	boolean doNormals = false;
	boolean doubleSided = false;

	protected ElevationModel elevmodel;
	protected PlanetTextureCache ptcache;
	protected PlanetTextureCache overlcache;

	protected float skyHeight = 0.0f;

	protected Map nmlpts = new HashMap();

	protected float fovratio = 1;

	private static final double sin60 = 0.86602540378;
	private static final float PI_F = (float) Math.PI;

	// these are used to query results of last rendering
	// they might should not be there...
	public float horizon_dist; // distance to horizon
	public float visible_dist; // visible distance (+ sky + irregularity)
	public float visible_angle, visible_angle_cos;
	public float height;

	float minelevpix2; // size of a quad edge at maximum elevation precision (km)

	int npolys = 0;
	TriNode lasttri;

	static int MAX_LEVEL = 29;

	int DEFAULT_TRIS = 20000;
	TriNode[] trinodes;
	TriNode freelist;

	int MAX_TRIS = 20000;

	int nodecount, numseeds;
	int numtris;
	float closest_dist;
	TriNode closest_tri;

	int stats_texchanges, stats_nstrips, stats_maxlevel, stats_nrecurse, stats_maxtexlevel;
	int stats_frustchecks;
	Vector3d stats_closest = new Vector3d();

	static final int INVISIBLE = 1;
	static final int IN_FRUSTUM = 2;
	static final int OUTOF_FRUSTUM = 4;
	static final int CONTAINS_VP = 8;
	static final int MERGABLE = 16;
	static final int REMOVED = 32;
	static final int FRUST_FLAGS = (IN_FRUSTUM | OUTOF_FRUSTUM);

	static final int MIN_TEXINT = -0x20000000;
	static final int MAX_TEXINT = 0x20000000;
	static final int RANGE_TEXINT = (MAX_TEXINT - MIN_TEXINT);

	public static final int MODE_NORMAL = 0;
	public static final int MODE_WIREFRAME = 1;
	public static final int MODE_COLORFLAGS = 2;
	public static final int MODE_COLORTEXLEV = 3;
	public static final int MODE_MAX = 3;

	static final boolean VFLIP = true;

	static final int TEX_LEVEL_ADJUST = 1;
	static final int ELEV_LEVEL_ADJUST = -3;
	static final int PRIO_DEFER_MASK = 0; // 1 every 8 frames, compute prio

	float wedgeradius[] = new float[MAX_LEVEL + 1];

	VarianceTree varitree;

	static final float VARIANCE_SCRN_FACTOR = 0.02f;

	float MAX_SPLIT_PRIO = 3.0e-5f;
	float MIN_MERGE_PRIO = 1.0e-5f;
	int SPLIT_LIMIT = 64;
	int MERGE_LIMIT = 64;

	int unique_id;
	int frame_num;

	//** TEXTURE STAGE STUFF

	class TexStage
	{
		PlanetTextureCache ptc;
		TexKey tex;
		int last_texint;
		int scale;
	}

	TexStage texstage[] = new TexStage[8];
	int ntexstages = 0;
	int active_texstage = 0;

	/****************/

	public ROAMPlanet(GUIContext ctx, float refrad)
	{
		this.guictx = ctx;
		this.refrad = refrad;
		setElevationModel(null);
		refcircum = (float) (refrad * Math.PI * 2);
	}

	private void makeTriNodes()
	{
		trinodes = new TriNode[DEFAULT_TRIS];
		for (int i = 0; i < trinodes.length; i++)
		{
			trinodes[i] = new TriNode();
		}
	}

	public void setMaxTriangles(int maxtris)
	{
		this.MAX_TRIS = maxtris;
	}

	public ElevationModel getElevationModel()
	{
		return elevmodel;
	}

	public void setElevationModel(ElevationModel elevmodel)
	{
		this.elevmodel = elevmodel;
		if (elevmodel != null)
		{
			minrad = refrad + elevmodel.getMinDisplacement();
			maxrad = refrad + elevmodel.getMaxDisplacement();
			radecc = (maxrad - minrad) / maxrad;
			minelevpix2 = refrad * 3.141592f / (1 << elevmodel.getMaxPrecision());
			minelevpix2 *= minelevpix2;
		}
		else
		{
			minrad = maxrad = refrad;
			minelevpix2 = 0;
			radecc = 0;
		}

		// build wedgie ratios
		// todo: this isn't entirely accurate
		double SQRT_2 = 1.4142135623;
		double rad = maxrad * SQRT_2 / 2;
		for (int level = 0; level <= MAX_LEVEL; level++)
		{
			wedgeradius[level] = (float) rad;
			if ((level & 1) == 1)
				rad /= 2; //SQRT_2;
		}
	}
	
	public void clearTextureStages()
	{
		setNumberOfTexStages(0);
	}

	private void setTextureStage(int ts)
	{
		if (texstage[ts] == null)
		{
			texstage[ts] = new TexStage();
			texstage[ts].tex = new TexKey(0, 0, 0);
		}
		if (ts + 1 > ntexstages)
			setNumberOfTexStages(ts + 1);
	}

	public void setTextureStage(int ts, PlanetTextureCache ptc)
	{
		setTextureStage(ts);
		texstage[ts].ptc = ptc;
	}

	public void setTextureStage(int ts, int detailScale)
	{
		setTextureStage(ts);
		texstage[ts].ptc = null;
		texstage[ts].scale = detailScale;
	}

	public void setNumberOfTexStages(int nts)
	{
		ntexstages = nts;
	}

	public void setVarianceTree(VarianceTree vtree)
	{
		this.varitree = vtree;
	}

	protected void addNormal(Vector3f pt, TriNode tn)
	{
		Vector3f n = (Vector3f) nmlpts.get(pt);
		if (n == null)
		{
			n = new Vector3f(tn.nmlx, tn.nmly, tn.nmlz);
			nmlpts.put(pt, n);
		}
		else
		{
			n.x += tn.nmlx;
			n.y += tn.nmly;
			n.z += tn.nmlz;
		}
	}

	protected void doNormal(GL gl, Vector3f pt)
	{
		Vector3f nml = (Vector3f) nmlpts.get(pt);
		if (nml != null)
		{
			gl.glNormal3f(nml.x, nml.y, nml.z);
		}
		else
		{
			gl.glNormal3f(pt.x, pt.y, pt.z);
		}
	}

	protected int getFrustumFlags(Vector3f pt)
	{
		return viewvol.getFrustumFlags(pt);
	}

	private Vector3f tmp_frustpt = new Vector3f();

	protected int getFrustumFlags(Vector3f pt, float ratio)
	{
		tmp_frustpt.set(pt.x * ratio, pt.y * ratio, pt.z * ratio);
		return viewvol.getFrustumFlags(tmp_frustpt);
	}

	public float getRadiusAt(Point2i tex, int prec)
	{
		if (elevmodel != null)
		{
			double lat = int2lat(tex.y);
			double lon = int2lon(tex.x);
			float alt = refrad + elevmodel.getDisplacement(lat, lon, prec) * ELEV_SCALE;
			if (debug2)
				System.out.println(
					"lat="
						+ Math.toDegrees(lat)
						+ " lon="
						+ Math.toDegrees(lon)
						+ " alt="
						+ alt
						+ " prec="
						+ prec);
			return alt;
		}
		else
			return refrad;
	}

	public float fixRadius(Vector3f p, Point2i tex, int prec)
	{
		float rad = getRadiusAt(tex, prec);
		p.scale(rad / p.length());
		return rad;
	}

	public float fixRadius(Vector3f p, Point2i tex)
	{
		float eyepl = vecdistsqr(p, vp);
		//todo??
		int elevlevel = AstroUtil.log2(1 + (int) (maxrad * maxrad / eyepl));
		if (debug2)
			System.out.println("fixRadius(): " + elevlevel + " " + eyepl + " " + p + " " + vp);
		return fixRadius(p, tex, elevlevel);
	}

	/**
	  * Get the visible distance at a given height above the surface (sea level)
	  */
	public float getHorizonDist(double h)
	{
		return (float) (Math.sqrt(h * (h + 2 * minrad)));
	}

	/**
	  * Get the angle subtending the surface that is visible
	  * at a given height above the surface (sea level)
	  */
	public double getVisibleAngle(double h)
	{
		if (h > 0)
			return Math.PI / 2 - Math.asin(minrad / (h + minrad));
		else
			return 0;
	}

	public boolean isVisible(Vector3f p)
	{
		float visible_dist_sqr = visible_dist * visible_dist;
		return (getEyeDistSqr(p) < visible_dist_sqr);
	}

	public float getEyeDistSqr(Vector3f p)
	{
		return vecdistsqr(p, vp);
	}

	private static float vecdistsqr(Vector3f a, Vector3f b)
	{
		float xx = a.x - b.x;
		float yy = a.y - b.y;
		float zz = a.z - b.z;
		return xx * xx + yy * yy + zz * zz;
	}

	private static double vecdist(Vector3f a, Vector3f b)
	{
		return Math.sqrt(vecdistsqr(a, b));
	}

	private static float vecdistsqr(Vector2f a, Vector2f b)
	{
		float xx = a.x - b.x;
		float yy = a.y - b.y;
		return xx * xx + yy * yy;
	}

	private static float vecdistsqr(Point2i a, Point2i b)
	{
		float xx = int2flt(a.x - b.x);
		float yy = int2flt(a.y - b.y);
		return xx * xx + yy * yy;
	}

	/**
	  * Get the cosine of angle subtended by a & b
	  * actually it's cos(angle)*constant
	  */
	private float getEyeAngle(Vector3f a, Vector3f b)
	{
		float dot =
			(a.x - vp.x) * (b.x - vp.x) + (a.y - vp.y) * (b.y - vp.y) + (a.z - vp.z) * (b.z - vp.z);
		return dot;
	}

	// nml*(v-p)
	private float getNormalAngle(Vector3f nml, Vector3f v, Vector3f p)
	{
		return nml.x * (v.x - p.x) + nml.y * (v.y - p.y) + nml.z * (v.z - p.z);
	}

	// nml*(v-p)
	private float getNormalAngle(TriNode tn, Vector3f v, Vector3f p)
	{
		return tn.nmlx * (v.x - p.x) + tn.nmly * (v.y - p.y) + tn.nmlz * (v.z - p.z);
	}

	private boolean needToClear()
	{
		// todo
		return (numseeds == 0);
	}

	public void reset()
	{
		numseeds = 0;
	}

	private void setupSeedPolys()
	{
		nodecount = numtris = 0;

		float l = 1.0f;
		int i000f = flt2int(0.00f);
		int i025f = flt2int(0.25f);
		int i050f = flt2int(0.50f);
		int i075f = flt2int(0.75f);
		int i100f = flt2int(1.00f);
		Point2i t0 = new Point2i(i075f, i100f);
		Point2i t1 = new Point2i(i025f, i100f);
		Point2i t2 = new Point2i(i100f, i050f);
		Point2i t3 = new Point2i(i075f, i050f);
		Point2i t4 = new Point2i(i050f, i050f);
		Point2i t5 = new Point2i(i025f, i050f);
		Point2i t6 = new Point2i(i000f, i050f);
		Point2i t7 = new Point2i(i075f, i000f);
		Point2i t8 = new Point2i(i025f, i000f);

		float r = minrad;
		Vector3f p1 = new Vector3f(0, 0, -r);
		Vector3f p2 = new Vector3f(-r, 0, 0);
		Vector3f p3 = new Vector3f(0, r, 0);
		Vector3f p4 = new Vector3f(r, 0, 0);
		Vector3f p5 = new Vector3f(0, -r, 0);
		Vector3f p6 = new Vector3f(0, 0, r);
		// todo: use variance tree if available
		fixRadius(p1, t0);
		fixRadius(p2, t2);
		fixRadius(p3, t3);
		fixRadius(p4, t4);
		fixRadius(p5, t5);
		fixRadius(p6, t8);
		TriNode n1 = newTriNode(p1, p3, p2, t0, t3, t2, 8);
		TriNode n2 = newTriNode(p1, p4, p3, t0, t4, t3, 9);
		TriNode n3 = newTriNode(p1, p5, p4, t1, t5, t4, 10);
		TriNode n4 = newTriNode(p1, p2, p5, t1, t6, t5, 11);
		TriNode n5 = newTriNode(p6, p2, p3, t7, t2, t3, 12);
		TriNode n6 = newTriNode(p6, p3, p4, t7, t3, t4, 13);
		TriNode n7 = newTriNode(p6, p4, p5, t8, t4, t5, 14);
		TriNode n8 = newTriNode(p6, p5, p2, t8, t5, t6, 15);

		n1.set(n5, n4, n2);
		n2.set(n6, n1, n3);
		n3.set(n7, n2, n4);
		n4.set(n8, n3, n1);
		n5.set(n1, n6, n8);
		n6.set(n2, n7, n5);
		n7.set(n3, n8, n6);
		n8.set(n4, n5, n7);

		numseeds = nodecount;
	}

	public void setFrustum(GL gl)
	{
		if (gl != null)
		{
			if (viewvol == null)
				viewvol = new ViewVolume();
			viewvol.setup(gl);
		}
	}

	public void setViewpoint(Vector3f viewpoint)
	{
		this.vp.set(viewpoint);
		this.vpl = vp.length();

		height = (float) Math.abs((vpl - minrad) / sin60);
		float h = height + (maxrad - minrad) / 2 + skyHeight;
		horizon_dist = getHorizonDist(height);
		visible_dist = getHorizonDist(h);
		visible_angle = (float) getVisibleAngle(h);
		visible_angle_cos = (float) Math.cos(visible_angle);
	}

	public void setVelocity(Vector3f vv)
	{
		if (vv != null)
		{
			velvec.set(vv);
			useVelocity = true;
		}
		else
			useVelocity = false;
	}

	public void setScreenDims(int scrnheight, float vfov)
	{
		this.fovratio = (float) (scrnheight / Math.toRadians(vfov));
		this.MAX_SPLIT_PRIO = VARIANCE_SCRN_FACTOR / fovratio;
		this.MIN_MERGE_PRIO = MAX_SPLIT_PRIO * 0.33f;
	}

	public void setCenterPoint(Vector3f viewpoint)
	{
		cenvp = new Vector3f(viewpoint);
	}

	public void renderSetup(GL gl, Vector3f viewpoint)
	{
		setFrustum(gl);
		setViewpoint(viewpoint);
		renderSetup();
	}

	private void clear()
	{
		if (doNormals)
			nmlpts.clear();

		freelist = null;
		unique_id = 0;

		setupSeedPolys();
	}

	public void renderSetup()
	{
		if (trinodes == null)
			makeTriNodes();

		frame_num++;
		closest_dist = 1e30f;
		closest_tri = null;
		{
			boolean clear = needToClear();
			if (clear)
			{
				clear();
			}

			for (int i = 0; i < numseeds; i++)
			{
				checkForInvalids(null, "pre-recurse " + i);
				recurse(trinodes[i], 0, null);
				checkForInvalids(null, "post-recurse " + i);
			}
		}
	}

	//**** RENDERING

	/**
	  * Renders the sphere patch starting directly under
	  * the viewpoint and extending to the visible horizon.
	  */
	public void renderGo(GLOContext ctx)
	{
		GL gl = ctx.getGL();

		{
			GLOContext.checkGL();
			renderStart(gl);
			// iterate through all 8 seeds
			for (int i = 0; i < numseeds; i++)
			{
				switch (renderMode)
				{
					case MODE_NORMAL :
						renderNode(gl, trinodes[i], trinodes[i], 0);
						break;
					default :
						renderEffect(gl, trinodes[i], trinodes[i], 0);
						break;
				}
			}
			renderEnd(gl);
			GLOContext.checkGL();
			// clean up texture stages
			for (int i = ntexstages - 1; i >= 0; i--)
			{
				activateTextureStage(gl, i);
				gl.glDisable(GL.GL_TEXTURE_2D);
			}
		}
	}

	private void activateTextureStage(GL gl, int ts)
	{
		if (ts != active_texstage)
		{
			gl.glActiveTexture(GL.GL_TEXTURE0 + ts);
			active_texstage = ts;
		}
	}

	private void renderStart(GL gl)
	{
		lasttri = null;
		active_texstage = 0;
		for (int i = 0; i < ntexstages; i++)
		{
			if (texstage[i].ptc != null)
			{
				texstage[i].tex.level = -1;
				texstage[i].last_texint = -1;
			}
			activateTextureStage(gl, i);
			if (renderMode != MODE_NORMAL)
			{
				gl.glDisable(GL.GL_TEXTURE_2D);
			}
			else
			{
				gl.glEnable(GL.GL_TEXTURE_2D);
			}
		}
		if (doNormals)
			gl.glEnable(GL.GL_NORMALIZE);
	}

	private void renderEnd(GL gl)
	{
		if (lasttri != null)
		{
			gl.glEnd();
			lasttri = null;
		}
	}

	private void renderWireframe(GL gl, TriNode tn, int level)
	{
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glColor3f(1, 1, 1);
		gl.glVertex3f(tn.p1.x, tn.p1.y, tn.p1.z);
		gl.glVertex3f(tn.p2.x, tn.p2.y, tn.p2.z);
		gl.glVertex3f(tn.p3.x, tn.p3.y, tn.p3.z);
		gl.glEnd();

		Vector3f nml = new Vector3f(tn.nmlx, tn.nmly, tn.nmlz);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glColor3f(0, 1, 0);
		gl.glVertex3f(tn.p1.x, tn.p1.y, tn.p1.z);
		nml.scale(maxrad / (nml.length() * (1l << level)));
		nml.add(tn.p1);
		gl.glVertex3f(nml.x, nml.y, nml.z);
		gl.glEnd();
	}

	private void colorByInt(GL gl, int i)
	{
		int x = (i << 13) ^ i;
		x = (x * (x * x * 15731 + 789221) + 1376312589) & 0x7fffffff;
		gl.glColor3f(((x >> 0) & 3) / 4.0f, ((x >> 2) & 3) / 4.0f, ((x >> 4) & 3) / 4.0f);
	}

	private void colorSpectrum(GL gl, float x)
	{
		gl.glColor3f(x - 0.33f, x - 0.66f, x);
	}

	private void renderColorFlagsTri(GL gl, TriNode tn, TriNode parent, int level)
	{
		renderEnd(gl);

		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glBegin(GL.GL_TRIANGLES);

		switch (renderMode)
		{
			case MODE_COLORFLAGS :
				float g = ((tn.flags & IN_FRUSTUM) != 0) ? 0.5f : 0;
				float r = ((tn.flags & OUTOF_FRUSTUM) != 0) ? 0.5f : 0.0f;
				float b = ((tn.flags & INVISIBLE) != 0) ? 0.5f : 0.0f;
				b += (parent != null && (parent.flags & MERGABLE) != 0) ? 0.3f : 0.0f;
				r += (parent != null && parent.isDiamond()) ? 0.3f : 0.0f;
				gl.glColor3f(r, g, b);
				break;
			case MODE_COLORTEXLEV :
				colorSpectrum(gl, tn.texlevel / 20.0f);
				break;
		}

		//		colorByInt(gl, stats_nstrips);

		gl.glVertex3f(tn.p3.x, tn.p3.y, tn.p3.z);
		gl.glVertex3f(tn.p2.x, tn.p2.y, tn.p2.z);
		gl.glVertex3f(tn.p1.x, tn.p1.y, tn.p1.z);
		gl.glEnd();
		gl.glPopAttrib();
	}

	private void doVertex(GL gl, TriNode tn, int level, ROAMVertex vtx)
	{
		doTex(gl, tn, level, vtx);
		if (doNormals)
			doNormal(gl, vtx);
		gl.glVertex3f(vtx.x, vtx.y, vtx.z);
	}

	private boolean pickTexture(
		TexKey dest,
		PlanetTextureCache ptc,
		TriNode tn,
		TriNode parent,
		int level)
	{
		// get tex coords of centroid
		float txcen = int2flt((tn.p1.tx + tn.p2.tx + tn.p3.tx) / 3);
		float tycen = int2flt((tn.p1.ty + tn.p2.ty + tn.p3.ty) / 3);
		// todo: this right?
		int texlev = Math.min(level, tn.texlevel);

		// penalty for not being totally in view volume
		//		if ((tn.flags & IN_FRUSTUM) == 0)
		//			texlev--;

		// query the ptc to find out what tex level we should use
		int l = ptc.getBestLevel(txcen, tycen, texlev);
		int xx, yy;
		if (l > 7)
		{
			int s = 1 << (l - 7);
			xx = (int) (txcen * s);
			yy = (int) (tycen * (s >> 1));
		}
		else
		{
			l = 7;
			xx = 0;
			yy = 0;
		}

		// if the texture has changed, return true
		if (l != dest.level || xx != dest.x || yy != dest.y)
		{
			;
			dest.level = l;
			dest.x = xx;
			dest.y = yy;
			return true;
		}
		else
			return false;
	}

	private void setTexture(PlanetTextureCache ptc, TexKey dest)
	{
		int l = dest.level;
		// todo: if not available, don't re-setTexture()
		//		int levelgot = ptc.setTexture((1<<(l-7))-1-dest.x, dest.y, l);
		int levelgot = ptc.setTexture(dest.x, dest.y, l);
		if (levelgot < l)
		{
			int shift = l - levelgot;
			dest.x >>= shift;
			dest.y >>= shift;
			dest.level = levelgot;
		}
		stats_texchanges++;
	}

	private void renderTriangle(GL gl, TriNode tn, TriNode parent, int level)
	{
		// select a texture
		int needtex = 0;

		// figure out what texture for this tri
		for (int i = 0; i < ntexstages; i++)
		{
			TexStage ts = texstage[i];
			if (ts.ptc != null)
			{
				if (pickTexture(ts.tex, ts.ptc, tn, parent, level))
					needtex |= (1 << i);
			}
		}

		if (needtex != 0 || lasttri == null || !(tn.p1 == lasttri.p1 && tn.p3 == lasttri.p2))
		{
			renderEnd(gl);

			stats_nstrips++;

			// see if we need to set the maintexture
			for (int i = 0; i < ntexstages; i++)
			{
				if ((needtex & (1 << i)) != 0)
				{
					TexStage ts = texstage[i];
					if (ts.ptc != null)
					{
						int texint = ts.ptc.getTextureInt(ts.tex);
						if (texint != ts.last_texint)
						{
							ts.last_texint = texint;
							activateTextureStage(gl, i);
							gl.glBindTexture(GL.GL_TEXTURE_2D, texint);
							stats_texchanges++;
						}
					}
				}
			}

			gl.glBegin(GL.GL_TRIANGLE_FAN);
			doVertex(gl, tn, level, tn.p1);
			doVertex(gl, tn, level, tn.p3);
			doVertex(gl, tn, level, tn.p2);
		}
		else
		{
			doVertex(gl, tn, level, tn.p2);
		}

		npolys++;
		lasttri = tn;
	}

	private void renderNode(GL gl, TriNode tn, TriNode parn, int level)
	{
		if (!tn.isSplit())
		{
			if (tn.isVisible())
			{
				// check for visibility (again!)
				/**
				float nmldotvp = doubleSided ? 1 : getNormalAngle(tn, vp, tn.p1);
				if (nmldotvp > 0)
				**/
				{
					renderTriangle(gl, tn, parn, level);
				}
			}
		}
		else
		{
			if ((tn.flags & OUTOF_FRUSTUM) == 0)
			{
				if (lasttri != null
					&& ((tn.lc.texlevel == lasttri.texlevel)
						|| (tn.lc.p1 == lasttri.p1 && tn.lc.p3 == lasttri.p2)))
				{
					renderNode(gl, tn.lc, tn, level + 1);
					renderNode(gl, tn.rc, tn, level + 1);
				}
				else
				{
					renderNode(gl, tn.rc, tn, level + 1);
					renderNode(gl, tn.lc, tn, level + 1);
				}
			}
		}
		/*
		if (tn.isVisible())
		{
			renderSceneryObjects(gl, tn, level);
		}
		*/
	}

	private void renderEffect(GL gl, TriNode tn, TriNode parn, int level)
	{
		if (!tn.isSplit())
		{
			switch (renderMode)
			{
				case MODE_WIREFRAME :
					renderWireframe(gl, tn, level);
					break;
				default :
					renderColorFlagsTri(gl, tn, parn, level);
					break;
			}
		}
		else
		{
			renderEffect(gl, tn.lc, tn, level + 1);
			renderEffect(gl, tn.rc, tn, level + 1);
		}
	}

	private void renderNodeScenery(GL gl, TriNode tn, int level)
	{
		if (tn.isSplit())
		{
			renderNodeScenery(gl, tn.lc, level + 1);
			renderNodeScenery(gl, tn.rc, level + 1);
		}
		// draw scenery objects
		if (tn.isVisible())
		{
			renderSceneryObjects(gl, tn, level);
		}
	}

	private void renderSceneryObjects(GL gl, TriNode tn, int level)
	{
		if (tn.texlevel != 20)
			return;
		float rad = wedgeradius[level];

		if (guictx == null)
			return;
		ModelRenderer mrend = guictx.rendcache.getModelRenderer("rock1");
		if (mrend == null)
			return;

		Random rnd = new Random(tn.hashCode());
		for (int i = 0; i < 2; i++)
		{
			float u = rnd.nextFloat();
			float v = rnd.nextFloat();
			if (u + v > 1) // this is so the coords don't bunch up at the ends
			{
				// todo: fold tri on itself
				float tmp = v;
				v = 1 - u;
				u = 1 - tmp;
			}
			Vector3f pt = new Vector3f(tn.p2);
			pt.interpolate(tn.p3, u);
			pt.interpolate(tn.p1, v);
			if (viewvol == null || viewvol.containsSphere(pt, rad))
			{
				renderEnd(gl);
				gl.glPushMatrix();
				gl.glPushAttrib(GL.GL_ENABLE_BIT);
				gl.glEnable(GL.GL_LIGHTING);
				gl.glTranslatef(pt.x, pt.y, pt.z);
				gl.glScalef(rad, rad, rad);
				// todo: rotate
				mrend.render();
				gl.glPopAttrib();
				gl.glPopMatrix();
			}
		}
	}

	//*** SPLITTING

	/***
		private static final int TAB_SIZE = RANGE_TEXINT>>16;
		private static final int TEXINT_SHIFT = 28;
		private static float[] t2w_tab = new float[TAB_SIZE];

		static {
			for (int i=0; i<TAB_SIZE; i++)
			{
				double ang = (Math.PI*2*i)/TAB_SIZE;
				t2w_tab[i] = (float)Math.sin(ang);
			}
		}

		private static float i_sin(int x)
		{
			int mask = RANGE_TEXINT-1;
			x = (x+RANGE_TEXINT) & mask;
			int xx = x & 0xffff;
			if (xx == 0)
				return t2w_tab[x>>16];
			else {
				// interpolate
				int ii = x>>16;
				float y1 = t2w_tab[ii];
				float y2 = t2w_tab[(ii+1) & (TAB_SIZE-1)];
				return y1 + ((y2-y1)*xx)/0x10000;
			}
		}

		private static float i_cos(int x)
		{
			return i_sin(x + RANGE_TEXINT/4);
		}

		private Vector3f texToWorld(Point2i p)
		{
			float cx = i_cos(p.x);
			float sx = i_sin(p.x);
			float cy = i_cos(-p.y>>1);
			float sy = i_sin(-p.y>>1);
			if (debug2)
				System.out.println(p + " -> " + sx + "," + sy + " - " + cx + "," + cy);
			return new Vector3f(cy*cx, cy*sx, sy);
		}
	***/

	private Vector3f texToWorld(Point2i p)
	{
		double lat = int2lat(p.y);
		double lon = int2lon(p.x);
		// todo: use table?
		double cx = Math.cos(lon);
		double cy = Math.cos(lat);
		double sx = Math.sin(lon);
		double sy = Math.sin(lat);
		if (debug2)
			System.out.println(p + " -> " + sx + "," + sy + " - " + cx + "," + cy);
		return new Vector3f((float) (cy * cx), (float) (cy * sx), (float) sy);
	}

	static final float LO = GUIContext.BORDER * 1.0f / GUIContext.TEX_SIZE;
	static final float HI = 1 - LO;
	static final float RNG = 1 - LO * 2;

	private void setTexCoord(GL gl, TriNode tri, ROAMVertex p, TexStage ts, int unit)
	{
		// we start texture at (b,b) and end at (256-b*2,256-b*2)
		// to leave a 'b'-pixel border
		float tx, ty;

		if (ts.ptc != null)
		{
			TexKey tk = ts.tex;
			if (tk.level <= 7)
			{
				tx = int2flt(p.tx);
				ty = int2flt(p.ty);
			}
			else
			{
				int xs = 1 << (tk.level - 7);
				int ys = (xs >> 1);
				tx = LO + (int2flt(p.tx) * xs - tk.x) * RNG;
				ty = LO + (int2flt(p.ty) * ys - tk.y) * RNG;
			}
		}
		else
		{
			// detail texture
			int sc = ts.scale;
			int txmin = tri.p1.tx & (-1 << sc);
			int tymin = tri.p1.ty & (-1 << sc);
			tx = (p.tx - txmin) * (1f / (1 << sc));
			ty = (p.ty - tymin) * (1f / (1 << sc));
		}

		if (unit < 0)
		{
			gl.glTexCoord2f(tx, ty);
		}
		else
		{
			gl.glMultiTexCoord2f(GL.GL_TEXTURE0 + unit, tx, ty);
		}
	}

	// todo: use ints natively
	private void doTex(GL gl, TriNode tri, int level, ROAMVertex p)
	{
		for (int i = 0; i < ntexstages; i++)
		{
			setTexCoord(gl, tri, p, texstage[i], (ntexstages > 1) ? i : -1);
		}
	}

	private int getUniqueID()
	{
		// todo: wank
		return (unique_id -= 2);
	}

	private void split(TriNode tn, int level)
	{
		// if node is split, return
		if (tn.isSplit())
			return;

		//checkForInvalids(tn, "pre-split");

		// first we linearly interpolate the texture coordinates
		// and use those to get the new midpoint
		Point2i newtex = new Point2i((tn.p2.tx + tn.p3.tx) / 2, (tn.p2.ty + tn.p3.ty) / 2);
		if (tn.p2.ty == MIN_TEXINT || tn.p2.ty == MAX_TEXINT)
			newtex.set(tn.p3.tx, newtex.y);
		else if (tn.p3.ty == MIN_TEXINT || tn.p3.ty == MAX_TEXINT)
			newtex.set(tn.p2.tx, newtex.y);

		// get midpoint of base
		Vector3f midp = new Vector3f(tn.p2);
		midp.add(tn.p3);
		midp.scale(0.5f);

		// todo: find a real elevlevel, not affected by viewpoint
		//int elevlevel = tn.texlevel + ELEV_LEVEL_ADJUST;
		int elevlevel = 99;

		Vector3f newp = texToWorld(newtex);
		float newrad;
		if (varitree != null && tn.vtindex > 0 && tn.vtindex < varitree.length())
		{
			// todo: combine both calls into 1 fetch
			float ratio = varitree.getElevationRatio(tn.vtindex);
			newrad = minrad + ratio * (maxrad - minrad);
			//			System.out.println("newrad=" + newrad + ", ratio=" + ratio);
			newp.scale(newrad / newp.length());
			tn.midpl = varitree.getVarianceForByte(tn.vtindex);
		}
		else
		{
			newrad = fixRadius(newp, newtex, elevlevel); //todo??
			// compute distance to midpoint
			tn.midpl = vecdistsqr(newp, midp);
		}
		ROAMVertex newvtx = new ROAMVertex(newp, newtex);

		if (level > stats_maxlevel)
		{
			stats_maxlevel = level;
			stats_closest.y = int2lat(newtex.y);
			stats_closest.x = int2lon(newtex.x);
			stats_closest.z = newrad;
		}

		if (tn.texlevel > stats_maxtexlevel)
			stats_maxtexlevel = tn.texlevel;

		if (debug2)
			System.out.println(newtex + " -> " + midp + " " + newp);

		// split our base if it is doesn't point to us
		if (tn.b != null && tn.b.b != tn)
		{
			tn.b.texlevel = (tn.texlevel);
			split(tn.b, level);
		}

		// split base
		// compute vt index -- strange way of doing it because
		// we have 8 bases and they need to be together (i guess?)
		int newvtindex;
		if (varitree != null && tn.vtindex > 0 && tn.vtindex < varitree.length())
			newvtindex = ((tn.vtindex >> 3) << 4) + ((tn.vtindex & 7) << 1);
		else
			newvtindex = getUniqueID();
		TriNode lc = newTriNode(newvtx, tn.p3, tn.p1, newvtindex, tn);
		TriNode rc = newTriNode(newvtx, tn.p1, tn.p2, newvtindex + 1, tn);
		if (debug2)
		{
			System.out.println("lc = " + lc.vtindex);
			System.out.println("rc = " + rc.vtindex);
		}
		lc.b = tn.l;
		lc.l = rc;
		rc.b = tn.r;
		rc.r = lc;
		tn.split(lc, rc);
		lc.texlevel = rc.texlevel = (tn.texlevel);
		lc.flags = (byte) ((tn.flags & FRUST_FLAGS) | (lc.flags & ~FRUST_FLAGS));
		rc.flags = (byte) ((tn.flags & FRUST_FLAGS) | (rc.flags & ~FRUST_FLAGS));

		// Link our Left Neighbor to the new children
		if (tn.l != null)
		{
			if (tn.l.b == tn)
				tn.l.b = lc;
			else if (tn.l.l == tn)
				tn.l.l = lc;
			else if (tn.l.r == tn)
				tn.l.r = lc;
			else
				System.out.println("Warning: Illegal Left Neighbor!");
		}

		// Link our Right Neighbor to the new children
		if (tn.r != null)
		{
			if (tn.r.b == tn)
				tn.r.b = rc;
			else if (tn.r.r == tn)
				tn.r.r = rc;
			else if (tn.r.l == tn)
				tn.r.l = rc;
			else
				System.out.println("Warning: Illegal Right Neighbor!");
		}

		// Link our Base Neighbor to the new children
		if (tn.b != null)
		{
			if (tn.b.lc != null)
			{
				tn.b.lc.r = rc;
				tn.b.rc.l = lc;
				tn.lc.r = tn.b.rc;
				tn.rc.l = tn.b.lc;
			}
			else
			{
				tn.b.texlevel = (tn.texlevel);
				split(tn.b, level);
				// Base Neighbor (in a diamond with us) was not split yet, so do that now.
			}
		}
		else
		{
			// An edge triangle, trivial case.
			tn.lc.r = null;
			tn.rc.l = null;
		}

		//checkForInvalids(tn, "post-split");
	}

	private void mergeDiamond(TriNode tn)
	{
		tn.flags &= ~MERGABLE;

		TriNode newLeft, newRight;
		TriNode tnB = tn.b;

		// Update neighbour pointers to reflect the fact that the child tns have been deleted.

		newLeft = tn.lc.b;
		tn.l = newLeft; // The diamond's left neighbour pointer may be out of date, so update it.

		if (newLeft != null)
		{
			if (newLeft.b == tn.lc)
			{
				newLeft.b = tn;
			}
			else
			{
				if (newLeft.l == tn.lc)
				{
					newLeft.l = tn;
				}
				else
				{
					newLeft.r = tn;
				}
			}
		}

		newRight = tn.rc.b;

		tn.r = newRight;

		if (newRight != null)
		{
			if (newRight.b == tn.rc)
			{
				newRight.b = tn;
			}
			else
			{
				if (newRight.r == tn.rc)
				{
					newRight.r = tn;
				}
				else
				{
					newRight.l = tn;
				}
			}
		}

		//	  	if (tnB != null)
		{
			tnB.flags &= ~MERGABLE;

			newLeft = tnB.lc.b;

			tnB.l = newLeft;

			if (newLeft != null)
			{

				if (newLeft.b == tnB.lc)
				{
					newLeft.b = tnB;
				}
				else
				{
					if (newLeft.l == tnB.lc)
					{
						newLeft.l = tnB;
					}
					else
					{
						newLeft.r = tnB;
					}
				}
			}

			newRight = tnB.rc.b;

			tnB.r = newRight;

			if (newRight != null)
			{
				if (newRight.b == tnB.rc)
				{
					newRight.b = tnB;
				}
				else
				{
					if (newRight.r == tnB.rc)
					{
						newRight.r = tnB;
					}
					else
					{
						newRight.l = tnB;
					}
				}
			}

		}

		checkForInvalids(tn, "mergeDiamond-1");

		tn.lc = removeNode(tn.lc);
		tn.rc = removeNode(tn.rc);
		if (tnB != null)
		{
			tnB.lc = removeNode(tnB.lc);
			tnB.rc = removeNode(tnB.rc);
		}

		checkForInvalids(tn, "mergeDiamond-2");

		// Add all newly mergeable diamonds to Qm.

		// Check to see if merge parents T, Tb are part of newly mergeable diamonds.

	}

	private void checkForInvalids(TriNode srctn, String fname)
	{
		if (!debug2)
			return;

		int total = 0;
		for (int i = 0; i < numseeds; i++)
			total += recurseInvalids(trinodes[i]);
		if (total != numtris)
			printBoth("***" + fname + " numtris mismatch: " + total + ", " + numtris);
	}

	private int recurseInvalids(TriNode tn)
	{
		int total = 1;
		if ((tn.flags & REMOVED) != 0)
			printBoth("***" + tn + " has invalid flags");
		if (tn.isSplit())
		{
			total += recurseInvalids(tn.lc);
			total += recurseInvalids(tn.rc);
		}
		return total;
	}

	private void printBoth(String fname, TriNode srctn, TriNode tn, String s)
	{
		printBoth("***" + fname + "(" + srctn + ") -- " + tn + s);
		if (debug2)
			System.out.println(srctn.hashCode() + " " + tn.hashCode());
	}

	private void printBoth(String s)
	{
		System.out.println(s);
		System.err.println(s);
		/*
		try {
			throw new Exception();
		} catch (Exception ee) {
			ee.printStackTrace(System.err);
		}
		*/
	}

	private Vector3f cenpt_tmp = new Vector3f();

	private static Vector3f wedgie[] = new Vector3f[6];
	//	private static Vector3f boundbox[] = new Vector3f[8];

	static {
		for (int i = 0; i < wedgie.length; i++)
			wedgie[i] = new Vector3f();
		//		for (int i=0; i<boundbox.length; i++)
		//			boundbox[i] = new Vector3f();
	}

	private static void setRatio(Vector3f dest, Vector3f src, float ratio, Vector3f cenpt)
	{
		// we move the pts away from the center point of the wedgie
		// but this is a hack to keep large wedgies from disappearing
		// when viewed from certain angles (sigh)
		dest.x = src.x * ratio - (cenpt.x - src.x) * 0.5f;
		dest.y = src.y * ratio - (cenpt.y - src.y) * 0.5f;
		dest.z = src.z * ratio - (cenpt.z - src.z) * 0.5f;
	}

	private void fillWedgies(TriNode tn, int level)
	{
		float ratio;
		//			ratio = (float)Math.sqrt(tn.midpl)/minrad;
		if (level < wedgeradius.length)
			ratio = wedgeradius[level];
		else
			ratio = wedgeradius[wedgeradius.length - 1];
		ratio /= minrad;

		// fill array of 6 wedgie points
		Vector3f cenpt = tn.getCenterPt();
		setRatio(wedgie[0], tn.p1, 1 - ratio, cenpt);
		setRatio(wedgie[1], tn.p2, 1 - ratio, cenpt);
		setRatio(wedgie[2], tn.p3, 1 - ratio, cenpt);
		setRatio(wedgie[3], tn.p1, 1 + ratio, cenpt);
		setRatio(wedgie[4], tn.p2, 1 + ratio, cenpt);
		setRatio(wedgie[5], tn.p3, 1 + ratio, cenpt);
		/**
				// set bounding box coords
				for (int i=0; i<8; i++)
				{
					boundbox[i].x = ((i&1)!=0) ? Math.min(Math.min(tn.p1.x,tn.p2.x),tn.p3.x) : Math.max(Math.max(tn.p1.x,tn.p2.x),tn.p3.x);
					boundbox[i].y = ((i&2)!=0) ? Math.min(Math.min(tn.p1.y,tn.p2.y),tn.p3.y) : Math.max(Math.max(tn.p1.y,tn.p2.y),tn.p3.y);
					boundbox[i].z = ((i&4)!=0) ? Math.min(Math.min(tn.p1.z,tn.p2.z),tn.p3.z) : Math.max(Math.max(tn.p1.z,tn.p2.z),tn.p3.z);
				}
		**/
	}

	private void checkTriFrustum(TriNode tn, int level)
	{
		// first see if the poly is wholly inside the view volume
		// todo: frustum flags move into ROAMVertex
		// todo: massive over-estimation of wedgie size!
		// todo: weird blanking-out effect!
		if (viewvol != null)
		{
			fillWedgies(tn, level);
			int ff = tn.frustflags;

			// iterate thru planes
			int pi = tn.firstplane; // start with this one, might be early-out
			for (int i = 0; i < 6; i++)
			{
				// if IN flag for plane is not set
				if ((ff & (1 << pi)) == 0)
				{
					stats_frustchecks++;
					// iterate thru all wedgie pts
					int z = 0;
					for (int wi = 0; wi < 6; wi++)
					{
						if (viewvol.isPtInPlane(wedgie[wi], pi))
							z |= (1 << wi);
					}
					// if all pts in wedgie outside the plane, early-out
					if (z == 0x0)
					{
						tn.flags |= OUTOF_FRUSTUM;
						tn.firstplane = (byte) pi;
						return;
					}
					// all pts in plane, success, set flag
					if (z == 0x3f)
						ff |= (1 << pi);
				}
				pi++;
				if (pi == 6)
					pi = 0;
			}

			if (ff == 0x3f)
				tn.flags |= IN_FRUSTUM;
			tn.frustflags = (byte) ff;
			if (debug2)
				System.out.println(tn + " ff=" + Integer.toString(ff, 2));
		}
	}

	private void recurse(TriNode tn, int level, TriNode parent)
	{
		stats_nrecurse++;

		if ((tn.flags & REMOVED) != 0)
			printBoth("***" + tn + " was REMOVED but is in tree");

		// see if the projected triangle volume contains the viewpoint
		// we only do this test of the parent also contains the viewpoint
		/*
		boolean contvp;	// = (tn.flags & CONTAINS_VP) != 0;
		if ( (parent==null || (parent.flags&CONTAINS_VP)!=0) && containsPoint(tn, cenvp) )
		{
			tn.flags |= CONTAINS_VP;
			contvp = true;
		} else
			contvp = false;
		*/
		
		boolean is_split = tn.isSplit();

		// inherit frustum flags from parent
		tn.flags &= ~(FRUST_FLAGS | INVISIBLE);

		// if no parent, check frustum
		if (parent == null)
		{
			tn.frustflags = 0;
			checkTriFrustum(tn, level);
		}
		// if parent's flags are DONT_KNOW, check frustum
		else if ((parent.flags & FRUST_FLAGS) == 0)
		{
			tn.frustflags = parent.frustflags;
			// if this tri isn't split (is a leaf) don't bother checking
			// todo: why does this make vertices wiggle??
			if (true || is_split)
			{
				checkTriFrustum(tn, level);
			}
		}
		// if parent is IN or OUT, inherit flags
		else
		{
			tn.frustflags = parent.frustflags;
			tn.firstplane = parent.firstplane;
			tn.flags |= (parent.flags & FRUST_FLAGS);
		}

		int flags = tn.flags;

		// only compute priority if we are a diamond or leaf
		if (!is_split && level < MAX_LEVEL && numtris < MAX_TRIS)
		{
			/**
						// cheezy deferral -- only update prio every 4 frames
						if (true ||  (flags & OUTOF_FRUSTUM) != 0 ||
							((tn.vtindex+frame_num) & PRIO_DEFER_MASK) == 0)
			**/

			float prio = computeVariance(tn, parent);
			// give precedence to points which contain the center point
			// TODO
			if (prio < MAX_SPLIT_PRIO)
			{
				if (containsPoint(tn, cenvp))
					prio *= tn.texlevel-6;
			}
			if (prio > MAX_SPLIT_PRIO)
			{
				split(tn, level);
				if (level > 7) //todo:const
					return;
			}

			// if in frustum, don't bother recursing
			else if ((flags & IN_FRUSTUM) != 0)
				return;
		}
		else if (tn.isDiamond() && parent != null)
		{
			float prio = computeVariance(tn, parent);
			if (prio < MIN_MERGE_PRIO)
			{
				if ((tn.b.flags & MERGABLE) != 0)
				{
					mergeDiamond(tn);
					return;
				}
				else
				{
					tn.flags |= MERGABLE;
				}
			}
			else
				tn.flags &= ~MERGABLE;
		}

		// todo: don't go past MAX_LEVEL other places
		if (tn.isSplit())
		{
			// recurse if:
			// 1. node has changed from out of frustum
			// 2. node is visible and texture level is greater than level+2
			// 3. variance is greater than threshold
			recurse(tn.lc, level + 1, tn);
			recurse(tn.rc, level + 1, tn);
		}
	}

	private void flagAllDescendants(TriNode tn, int orflags, int andflags)
	{
		tn.flags = (byte) ((tn.flags & andflags) | orflags);
		if (tn.isSplit())
		{
			flagAllDescendants(tn.lc, orflags, andflags);
			flagAllDescendants(tn.rc, orflags, andflags);
		}
	}

	//

	public void forceSplit(int maxlevel)
	{
		for (int i = 0; i < numseeds; i++)
		{
			forceSplit(trinodes[i], maxlevel, 0);
		}
		System.out.println("forceSplit(): " + numtris);
	}

	public void forceSplit(TriNode tn, int maxlevel)
	{
		forceSplit(tn, maxlevel, 0);
	}

	private void forceSplit(TriNode tn, int maxlevel, int level)
	{
		if (level < maxlevel)
		{
			split(tn, level);
			forceSplit(tn.lc, maxlevel, level + 1);
			forceSplit(tn.rc, maxlevel, level + 1);
		}
	}

	/*** texint conversion utils ***/

	static final float int2flt(int i)
	{
		return (i - MIN_TEXINT) * 1f / RANGE_TEXINT;
	}

	static final double int2dbl(int i)
	{
		return (i - MIN_TEXINT) * 1d / RANGE_TEXINT;
	}

	static final double int2lat(int i)
	{
		return -i * (Math.PI / (2 * MAX_TEXINT));
	}

	static final double int2lon(int i)
	{
		return i * (Math.PI / MAX_TEXINT);
	}

	static final int flt2int(float x)
	{
		return (int) (x * RANGE_TEXINT) + MIN_TEXINT;
	}

	static final int int2dbl(double x)
	{
		return (int) (x * RANGE_TEXINT) + MIN_TEXINT;
	}

	/********** TRINODE CLASS **********/

	// allocate a node, take from freelist	if possible
	private TriNode allocNode()
	{
		// todo: watch for overflow!
		if (freelist == null)
		{
			if (nodecount < DEFAULT_TRIS)
				return trinodes[nodecount++];
			else
				return new TriNode();
		}
		else
		{
			TriNode tn = freelist;
			freelist = tn.b;
			return tn;
		}
	}

	// deallocate a node, put on freelist
	private void deallocNode(TriNode tn)
	{
		// we only deallocate if the thing is not deferred,
		// and the flag is set for removed
		if ((tn.flags & REMOVED) != REMOVED)
			return;
		tn.b = freelist;
		freelist = tn;
	}

	private TriNode newTriNode(
		Vector3f p1,
		Vector3f p2,
		Vector3f p3,
		Point2i t1,
		Point2i t2,
		Point2i t3,
		int vtindex)
	{
		TriNode tn =
			newTriNode(
				new ROAMVertex(p1, t1),
				new ROAMVertex(p2, t2),
				new ROAMVertex(p3, t3),
				vtindex,
				null);
		return tn;
	}

	private TriNode newTriNode(
		ROAMVertex v1,
		ROAMVertex v2,
		ROAMVertex v3,
		int vtindex,
		TriNode parent)
	{
		TriNode tn = allocNode();
		tn.p1 = v1;
		tn.p2 = v2;
		tn.p3 = v3;
		tn.init();
		if (doNormals)
		{
			// don't need to normalize
			/*
			float mag = 1/Math.sqrt(tn.nmlx*tn.nmlx + tn.nmly*tn.nmly + tn.nmlz*tn.nmlz);
			tn.nmlx *= mag;
			tn.nmly *= mag;
			tn.nmlz *= mag;
			*/
			addNormal(tn.p1, tn);
			addNormal(tn.p2, tn);
			addNormal(tn.p3, tn);
		}
		tn.vtindex = vtindex;
		numtris++;
		if (debug)
			System.out.println("ADDED " + tn);
		return tn;
	}

	private TriNode removeNode(TriNode tn)
	{
		tn.flags |= REMOVED;
		numtris--;
		deallocNode(tn);
		if (debug)
			System.out.println("REMOVED " + tn);
		return null;
	}

	public boolean containsPoint(TriNode tn, Vector3f p)
	{
		// if eyepoint is further than 2*(length of 1 side of tri),
		// return false
		//			System.out.println(ROAMPlanet.vecdistsqr(vp, p1) +"\t"+ ROAMPlanet.vecdistsqr(p1,tn.p2));
		if (vecdistsqr(vp, tn.p1) > vecdistsqr(tn.p1, tn.p2) * 8)
			return false;
		// check project planes of tri for inclusion of pt
		Vector3f origin = new Vector3f();
		Plane4f plane1 = new Plane4f(origin, tn.p2, tn.p1);
		if (!plane1.contains(vp))
			return false;
		Plane4f plane2 = new Plane4f(origin, tn.p3, tn.p2);
		if (!plane2.contains(vp))
			return false;
		Plane4f plane3 = new Plane4f(origin, tn.p1, tn.p3);
		if (!plane3.contains(vp))
			return false;
		return true;
	}

	private float getEyepointDist(TriNode tn)
	{
		return Math.min(
			Math.min(vecdistsqr(tn.p1, vp), vecdistsqr(tn.p2, vp)),
			vecdistsqr(tn.p3, vp));
	}

	/**
	  * This method computes both texture level and priorty and
	  * inserts the thing into the priorty queue
	  */
	private float computeVariance(TriNode tn, TriNode parent)
	{
		float prio;

		// check frustum flags
		if ((tn.flags & OUTOF_FRUSTUM) != 0)
		{
			prio = -1;
		}
		else
		{

			// compute tri level & viewpoint dist
			float eyepl = getEyepointDist(tn);

			// if it's visible, compute the pixel radius, and therefore
			// the desired tex level
			// todo: compute only for parent

			float dmin2 = eyepl;
			float dmin = (float) Math.sqrt(dmin2);

			float dot = 1 - dmin / horizon_dist;

			if (dot > 0)
			{
				float coslat = 1 - Math.abs(tn.p1.ty + tn.p2.ty + tn.p3.ty) / (3.0f * MAX_TEXINT);
				float polyrad = refcircum * coslat;
				float scrnrad = (fovratio * dot * polyrad / dmin);
				long lscrnrad = (long) scrnrad;

				int texlev = (lscrnrad > 0) ? (AstroUtil.log2(lscrnrad) + TEX_LEVEL_ADJUST) : 0;
				if (texlev > MAX_LEVEL)
					texlev = MAX_LEVEL;
				tn.texlevel = (byte) texlev;

				if (debug2)
					System.out.println(
						"dot="
							+ dot
							+ ", polyrad="
							+ (polyrad)
							+ ", scrnrad="
							+ scrnrad
							+ ", texlevel="
							+ tn.texlevel);
			}
			else
			{
				tn.texlevel = 0;
			}

			// compute priority!
			// (todo: use texlevel to potentially make higher prio)

			if (parent != null)
				prio = parent.midpl / eyepl;
			else
				prio = maxrad * maxrad;
		}

		return prio;
	}

	// only works if split
	float getMidpointDisplacement2(TriNode tn)
	{
		Vector3f midp = new Vector3f(tn.p2);
		midp.add(tn.p3);
		midp.scale(0.5f);

		midp.sub(tn.lc.p1);
		return midp.lengthSquared();
	}

	//

	public Vector3f[] getClosestPlanePts()
	{
		if (closest_tri != null)
		{
			Vector3f arr[] = new Vector3f[2];
			arr[0] = new Vector3f(closest_tri.nmlx, closest_tri.nmly, closest_tri.nmlz);
			arr[0].normalize();
			arr[1] = new Vector3f(closest_tri.p1);
			return arr;
		}
		else
		{
			return null;
		}
	}

	//

	public void printStatistics()
	{
		System.out.println();
		System.out.println("Height AGL:         " + (height));
		System.out.println("Visible distance:   " + (visible_dist));
		System.out.println("Horizon distance:   " + (horizon_dist));
		System.out.println("# of triangles:     " + numtris);
		System.out.println("# of tristrips:     " + stats_nstrips);
		System.out.println("# of recursions:    " + stats_nrecurse);
		System.out.println("# frustum checks:   " + stats_frustchecks);
		System.out.println();
		System.out.println("Maximum level:      " + stats_maxlevel);
		System.out.println("# of tex changes:   " + stats_texchanges);
		System.out.println("Maximum tex level:  " + stats_maxtexlevel);
		System.out.println(
			"Closest LLR:        "
				+ AstroUtil.formatDMS(Math.toDegrees(stats_closest.y), true)
				+ " "
				+ AstroUtil.formatDMS(Math.toDegrees(stats_closest.x), false)
				+ " "
				+ (stats_closest.z));
		System.out.println();
		System.out.println();
	}

	private void dumpTriangle(TriNode tn, int level)
	{
		for (int i = 0; i < level; i++)
			System.out.print(' ');
		System.out.println(level + " " + tn);
		if (tn.isSplit())
		{
			dumpTriangle(tn.lc, level + 1);
			dumpTriangle(tn.rc, level + 1);
		}
	}

	public void dumpTriangles()
	{
		for (int i = 0; i < numseeds; i++)
		{
			dumpTriangle(trinodes[i], 0);
		}
	}

	public void clearStatistics()
	{
		stats_texchanges = 0;
		stats_nstrips = 0;
		stats_maxlevel = 0;
		stats_maxtexlevel = 0;
		stats_nrecurse = 0;
		stats_frustchecks = 0;
	}

	public boolean debug = false;
	public boolean debug2 = false;

	//

	private static void test_sincos(double ang)
	{
		/*
		System.out.println(Math.toDegrees(ang) + ":\t" + Math.sin(ang) + "\t" + Math.cos(ang));
		int x = (int)(ang*RANGE_TEXINT/(Math.PI*2));
		System.out.println("0x" + Integer.toString(x,16) + ":\t" + i_sin(x) + "\t" + i_cos(x));
		*/
	}

	public static void main(String[] args) throws Exception
	{
		float r = 6378.14f;
		float h = 20f;
		Vector3f vp = new Vector3f(r + h, 0, 0);
		ROAMPlanet sp = new ROAMPlanet(null, r);
		sp.debug = args.length > 0 && args[0].equals("-d");
		sp.debug2 = args.length > 0 && args[0].equals("-dd");
		if (sp.debug2)
			sp.debug = true;
		boolean bench = args.length > 0 && args[0].equals("-bench");

		if (!bench)
		{
			test_sincos(0);
			test_sincos(Math.PI / 2);
			test_sincos(Math.PI / 4);
			test_sincos(Math.PI / 13);
			test_sincos(Math.PI);
			test_sincos(Math.PI * 2);
			test_sincos(-Math.PI * 2);
			test_sincos(-Math.PI * 1.55);
			System.out.println(int2lat(0));
			System.out.println(int2lat(0x10000000));
			System.out.println(int2lon(0x10000000));
			System.out.println(int2flt(flt2int(0)));
			System.out.println(int2flt(flt2int(1)));
			System.out.println(int2flt(flt2int(-0.5f)));
			System.out.println(sp.texToWorld(new Point2i(0, 0)));
			System.out.println(sp.texToWorld(new Point2i(flt2int(0.5f), flt2int(0.75f))));
		}

		sp.setVarianceTree(new VarianceTree("texs/Earth/Earth.vtr"));

		vp.set(r + 1, 0, 0);
		//		vp.set(1559.821f, -6141.0605f, 786.4508f);
		for (int i = 0; i < 100; i++)
		{
			System.out.println("ITERATION " + i);

			Plane4f[] planes = new Plane4f[6];
			Vector3f k1 = new Vector3f(vp.x - r, -r, -r);
			Vector3f k2 = new Vector3f(vp.x - r, r, -r);
			Vector3f k3 = new Vector3f(vp.x - r, r, r);
			Vector3f k4 = new Vector3f(vp.x - r, -r, r);
			planes[0] = new Plane4f(vp, vp);
			planes[1] = new Plane4f(vp, k1, k2);
			planes[2] = new Plane4f(vp, k2, k3);
			planes[3] = new Plane4f(vp, k3, k4);
			planes[4] = new Plane4f(vp, k4, k1);
			planes[5] = new Plane4f(k1, vp);
			ViewVolume viewvol = new ViewVolume();
			viewvol.setup(planes);

			//   		sp.viewvol = viewvol;
			sp.clearStatistics();
			sp.setScreenDims(768, 60);
			vp.scale(1.01f);
			sp.renderSetup(null, vp);
			sp.printStatistics();
		}

		if (!bench)
		{
			System.out.println("height = " + sp.height);
			System.out.println("visible_dist = " + sp.visible_dist);
			System.out.println("visible_ang = " + Math.toDegrees(sp.visible_angle));
			System.out.println("cos(visible_ang) = " + sp.visible_angle_cos);
			sp.dumpTriangles();
		}
	}

}
