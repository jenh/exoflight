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

import java.awt.*;
import java.util.*;

import javax.media.opengl.GL;

import Acme.LruHashtable;

import com.fasterlight.exo.newgui.roam.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.glout.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Rect4f;
import com.fasterlight.vecmath.*;

// todo: put colors in a file, or make Shaders

// todo: when outside influence radius, don't show
// todo: when zooming, draw duplicates on l and r
// todo: don't scroll too far n or s

/**
 * Displays the groundtrack for a space object.
 */
public class GroundtrackView extends GLOComponent implements ThingSelectable,
		Constants
{
	protected Game game;
	protected GUIContext guictx;
	protected GL gl;
	protected UniverseThing selected;
	protected Planet refthing;
	protected ThingSelectable thingselectable;

	protected String selthingkey = "selected";
	protected String trackthingkey = "tracked";

	protected float TRACKS_PER_ORBIT = 2;
	protected long TICK_INC = TICKS_PER_SEC * 60;
	protected int MAX_TICKS = 800;
	protected float MAX_ZOOM = 64;

	protected PickList picklist = new PickList();

	protected GLOSmoother zoomSmoother = new GLOSmoother(1);
	protected float cenlon, cenlat;
	protected float x1, y1, xw, yh;
	protected int drawMode = 0;
	protected boolean drawGrid = true;

	public static final int MODE_MAP = 0;
	public static final int MODE_COLOR = 1;

	// ORBIT CACHE to speed up groundtrack

	WeakHashMap ohashmap = new WeakHashMap();
	int ocache_age = 0; // age of cache in frames
	boolean use_cache = false;

	//

	public GroundtrackView(Game game)
	{
		this.game = game;
		init((GUIContext) getContext());
	}

	public float getZoomFactor()
	{
		float z = zoomSmoother.getValue();
		return (z < 1.01f) ? 1.0f : z;
	}

	public void zoom(float x)
	{
		zoomSmoother.setTarget(Math.max(1, Math.min(MAX_ZOOM, zoomSmoother.getTarget() * x)));
		// TODO: must clear cache more often
		if (use_cache)
			ohashmap.clear();
	}

	public boolean needsClipping()
	{
		return true;
	}

	public void init(GUIContext guictx)
	{
		this.guictx = guictx;
		this.gl = guictx.getGL();
	}

	Vector2f ll2xy(double lon, double lat)
	{
		lon = AstroUtil.fixAngle(lon + Math.PI) - Math.PI;
		double xx = xw / 2 + lon * xw / (Math.PI * 2);
		double yy = yh / 2 - lat * yh / Math.PI;
		return new Vector2f((float) xx, (float) yy);
	}

	Vector3d getThingLLR(UniverseThing ut, Planet p, long t)
	{
		double dt = t * (1d / TICKS_PER_SEC);
		Vector3d pos = ut.getPosition(p, t);
		p.xyz2ijk(pos);
		p.ijk2llr(pos, dt);
		return pos;
	}

	void renderThing(GLOContext ctx, UniverseThing ut, Planet p, long t)
	{
		if (ut.getTrajectory() == null)
			return;

		Vector3d pos = getThingLLR(ut, p, t);
		if (ut instanceof SpaceBase)
		{
			guictx.texcache.setTexture("circle-ALPHA.png");
			gl.glColor3f(0.5f, 0.5f, 0.5f);
		} else
		{
			guictx.texcache.setTexture("triangle-ALPHA.png");
			gl.glColor3f(0.25f, 0.25f, 1f);
		}
		if (ut == getSelected())
		{
			gl.glColor3f(1f, 1f, 1f);
		} else if (ut == getTarget())
		{
			gl.glColor3f(0.2f, 1f, 1f);
		}

		Vector2f scrnpos = ll2xy(pos.x, pos.y);
		picklist.addPickRec(x1 + scrnpos.x, y1 + scrnpos.y, 8, ut);

		gl.glPushMatrix();
		gl.glTranslatef(x1 + scrnpos.x, y1 + scrnpos.y, 0);
		drawTexturedBox(ctx, -8, -8, 16, 16);
		gl.glPopMatrix();
	}

	static int NUM_LMAPS = 32;

	public void render(GLOContext ctx)
	{
		// todo
		if (getSelected() != null
				&& getSelected().getParent() instanceof Planet)
			refthing = (Planet) getSelected().getParent();
		if (refthing == null)
			return;

		cenlon = cenlat = 0;
		drawMode = MODE_MAP;
		if (getTracked() != null)// && getZoomFactor() > 1)
		{
			/*
			double ecc = getTracked().getTelemetry().getECCENT();
			double peri = getTracked().getTelemetry().getPERIAPSIS();
			*/
			double parentRadius = getTracked().getParent().getRadius();
			double peri = getTracked().getTelemetry().getPERIAPSIS() + parentRadius;
			float ratio = (float)(peri * 2 / parentRadius);
			if (ratio < 1)
			{
				Vector3d llr = getThingLLR(getTracked(), refthing, game.time());
				cenlon = (float) llr.x;
				cenlat = (float) llr.y;
				zoomSmoother.setTarget(1.0f / (ratio+0.01f));
				drawMode = MODE_COLOR;
			}
		}

		Planet p = refthing;
		picklist.clear();

		switch (drawMode)
		{
			case MODE_MAP:
				drawMap(ctx, p);
				break;
			case MODE_COLOR:
				drawTexturedSurface(p);
				break;
		}

		drawGrid();

		// draw sun umbra mask
		if (!(p instanceof Star))
		{
			drawUmbra(p);
		}

		drawThings(p);

		if (hintrend.size() > 0)
		{
			gl.glPushMatrix();
			// gl.glTranslatef(o.x, o.y, 0);
			hintrend.renderHints(ctx, picklist);
			gl.glPopMatrix();
		}
	}

	// TODO
	private void drawTexturedSurface(Planet p)
	{
		float zoom = getZoomFactor();
		Point o = origin;
		PlanetRenderer prend = guictx.getPlanetRenderer(p);
		PlanetTextureCache ptc = prend.getTextureCacheColor();
		float t = GUIContext.BORDER * 1.0f / GUIContext.TEX_SIZE;

		int wsize = (int)(w1*zoom);
		int level = Math.max(8, AstroUtil.log2(wsize));
		int lo = level - GUIContext.TEX_LEVEL;
		// compute view boundaries in "world" coordinates
		Rectangle quadBounds = new Rectangle(0, 0, 2<<lo, 1<<lo);
		float cx = (float)(cenlon / (Math.PI*2) + 0.5f);
		float cy = (float)(-cenlat / Math.PI + 0.5f);
		float sx = 0.5f/zoom;
		float sy = 0.5f/zoom;
		Rect4f worldRect = new Rect4f(cx - sx, cy - sy, cx + sx, cy + sy);
		//System.out.println(worldRect);
		setMapColor();
		gl.glPushMatrix();
		gl.glTranslatef(o.x - w1*worldRect.x1*zoom, o.y - h1*worldRect.y1*zoom, 0);
		gl.glScalef(w1*zoom/quadBounds.width, h1*zoom/quadBounds.height, 1);
		// compute boundary for quad rectangles
		Rect4f quadRect = new Rect4f(worldRect);
		quadRect.scale(quadBounds.width, quadBounds.height);
		Rectangle quadView = new Rectangle(
				(int) Math.floor(quadRect.x1), 
				(int) Math.floor(quadRect.y1), 
				(int) Math.ceil(quadRect.width() + 1),
				(int) Math.ceil(quadRect.height() + 1)); // .intersection(quadBounds);
		for (int yy = quadView.y; yy < quadView.y+quadView.height; yy++)
		{
			for (int xx = quadView.x; xx < quadView.x+quadView.width; xx++)
			{
				ptc.setTexture(xx & ((2<<lo)-1), yy & ((1<<lo)-1), level);
				int x = xx;
				int y = yy;
				int w = 1;
				int h = 1;
				gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(t, 1 - t);
				gl.glVertex2f(x, y + h);
				gl.glTexCoord2f(1 - t, 1 - t);
				gl.glVertex2f(x + w, y + h);
				gl.glTexCoord2f(1 - t, t);
				gl.glVertex2f(x + w, y);
				gl.glTexCoord2f(t, t);
				gl.glVertex2f(x, y);
				gl.glEnd();
			}
		}
		gl.glPopMatrix();
		
		transformComponentForMap();
	}

	private void drawThings(Planet p)
	{
		// draw satellites
		long t = game.time();
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		UniverseThing sel = getSelected();

		// renderThing(ctx, p.getParent(), p, t); // render sun?

		Iterator it = p.getChildren();
		while (it.hasNext())
		{
			UniverseThing ut = (UniverseThing) it.next();
			if (ut != sel)
				renderThing(ctx, ut, p, t);
		}

		// draw selected thing & its orbit
		if (sel != null)
		{
			gl.glColor3f(1f, 0.25f, 0.25f);
			// gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
			Conic conic = UniverseUtil.getConicFor(sel);
			// System.out.println(conic);
			if (conic != null)
			{
				renderConic(p, conic);
			}
			renderThing(ctx, sel, p, t);
		}

		gl.glPopAttrib();
	}

	private void drawUmbra(Planet p)
	{
		// todo: make work for other Stars (besides Sun)
		Vector3d sunpos = p.getPosition(null, game.time());
		sunpos.scale(-1);
		p.xyz2ijk(sunpos);
		p.ijk2llr(sunpos, game.time() * (1d / TICKS_PER_SEC));

		double theta = sunpos.x;
		double phi = sunpos.y;

		float tranx = (float) (theta / (Math.PI * 2));
		int lmap = (int) Math.round(Math.abs(phi) * NUM_LMAPS / Math.PI);

		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glLoadIdentity();
		gl.glTranslatef(-tranx, 0, 0);
		if (phi < 0)
			gl.glScalef(1, -1, 1);
		guictx.texcache.setTexture("gtlmap/lmap-" + lmap + ".tga");
		gl.glColor3f(0.2f, 0.1f, 0.1f);
		drawTexturedBox(ctx, x1, y1, xw, yh);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	private void drawGrid()
	{
		// draw grid
		gl.glPushAttrib(GL.GL_ENABLE_BIT);

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glLoadIdentity();
		gl.glScalef(36, 18, 1);
		guictx.texcache.setTexture("grid.png");
		drawTexturedBox(ctx, x1, y1, xw, yh);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	private void drawMap(GLOContext ctx, Planet p)
	{
		// draw map
		setPlanetTexture(p);
		transformComponentForMap();

		setMapColor();
		drawTexturedBox(ctx, x1, y1, xw, yh);
	}

	/**
	 * For some reason, we find it neccessary to modify the window coordinates
	 * here to render the rest of the map ... ewwww...
	 */
	private void transformComponentForMap()
	{
		float zoom = getZoomFactor();
		Point o = origin;
		int w = getWidth();
		int h = getHeight();
		x1 = o.x + w * 0.5f * (1 - zoom);
		y1 = o.y + h * 0.5f * (1 - zoom);
		xw = w * zoom;
		yh = h * zoom;
		// offset the view
		x1 -= cenlon * xw / (Math.PI * 2);
		y1 += cenlat * yh / Math.PI;
	}

	private void setMapColor()
	{
		gl.glColor3f(0.1f, 0.333f, 0.1f);
	}

	void renderConic(Planet p, Conic conic)
	{
		long dur, tinc;
		long t = game.time();
		double period = conic.getPeriod();
		if (!Double.isNaN(period))
		{
			tinc = TICK_INC / 2;
			do
			{
				tinc *= 2;
				dur = (long) (TRACKS_PER_ORBIT * period * Constants.TICKS_PER_SEC);
			} while (tinc > 0 && dur / tinc > MAX_TICKS);
		} else
		{
			dur = TICK_INC * MAX_TICKS;
			tinc = TICK_INC;
		}
		renderConic(p, conic, t, t + dur, tinc);
	}

	void renderConic(Planet p, Conic conic, long t1, long t2, long tinc)
	{
		guictx.texcache.setTexture("circle-ALPHA.png");

		long t = (long) (Math.floor((t1 + tinc - 1) * 1d / tinc) * tinc);
		while (t < t2)
		{
			float alpha = (t2 - t) * 0.8f / (t2 - t1) + 0.2f;
			gl.glColor4f(1f, 0.25f, 0.25f, alpha);

			Vector2f pt = getCachedConicPoint(p, conic, t);
			if (Float.isNaN(pt.x))
				break;

			gl.glPushMatrix();
			gl.glTranslatef(x1 + pt.x, y1 + pt.y, 0);
			drawTexturedBox(guictx, -8, -8, 16, 16);
			gl.glPopMatrix();
			t += tinc;
		}
	}

	Vector2f computeConicPoint(Planet p, Conic conic, long t)
	{
		double dt = t * (1d / TICKS_PER_SEC);
		StateVector res;
		try
		{
			res = conic.getStateVectorAtTime(dt);
		} catch (ConvergenceException cve)
		{
			return new Vector2f(Float.NaN, Float.NaN);
		}
		if (res.r.length() < p.getRadius())
			return new Vector2f(Float.NaN, Float.NaN);
		Vector3d r = new Vector3d(res.r);
		p.xyz2ijk(r);
		p.ijk2llr(r, dt);
		return ll2xy(r.x, r.y);
	}

	Vector2f getCachedConicPoint(Planet p, Conic conic, long t)
	{
		if (!use_cache)
		{
			return computeConicPoint(p, conic, t);
		}

		Map ptmap = (Map) ohashmap.get(conic);
		if (ptmap == null)
		{
			ptmap = new LruHashtable(MAX_TICKS);
			ohashmap.put(conic, ptmap);
			if (debug)
				System.out.println("Conic cache size=" + ohashmap.size());
		}

		Long key = new Long(t);
		Vector2f pt = (Vector2f) ptmap.get(key);
		if (pt == null)
		{
			pt = computeConicPoint(p, conic, t);
			ptmap.put(key, pt);
			if (debug)
				System.out.println("Point cache size=" + ptmap.size());
		}
		return pt;
	}

	//

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

	//

	HintRenderer hintrend = new HintRenderer(this);

	public void showHintFor(UniverseThing ut)
	{
		hintrend.showHintFor(ut, ut.getName());
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOMouseButtonEvent)
		{
			GLOMouseButtonEvent mbe = (GLOMouseButtonEvent) event;
			if (mbe.isPressed(1))
			{
				event.getContext().requestFocus(this);
				// check pick list
				int x = mbe.x;
				int y = mbe.y;

				Object o = picklist.pickObject(x, y);
				if (o != null)
				{
					System.out.println("SELECTED " + o);
					setSelected((UniverseThing) o);
					return true;
				}
			}
		} else if (event instanceof GLOMouseMovedEvent)
		{
			if (!super.handleEvent(event))
			{
				GLOMouseMovedEvent mbe = (GLOMouseMovedEvent) event;
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

	//

	public void setSelected(UniverseThing ut)
	{
		guictx.setProp(selthingkey, ut);
	}

	public UniverseThing getSelected()
	{
		return (UniverseThing) guictx.getProp(selthingkey);
	}

	public UniverseThing getTracked()
	{
		return (UniverseThing) guictx.getProp(trackthingkey);
	}

	public void setThingSelectable(ThingSelectable thingsel)
	{
		this.thingselectable = thingsel;
	}

	public void setRefPlanet(Planet planet)
	{
		this.refthing = planet;
	}

	public Planet getRefPlanet()
	{
		return refthing;
	}

	public SpaceShip getCurrentShip()
	{
		return (SpaceShip) guictx.getProp("ship");
	}

	public UniverseThing getTarget()
	{
		SpaceShip ship = getCurrentShip();
		if (ship != null)
			return ship.getShipTargetingSystem().getTarget();
		else
			return null;
	}

	//

	boolean debug = !true;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(
			GroundtrackView.class);

	static
	{
		prophelp.registerSet("zoom", "zoom", float.class);
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
		} catch (PropertyRejectedException e)
		{
			super.setProp(key, value);
		}
	}

}
