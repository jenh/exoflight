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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.roam.PlanetRenderer;
import com.fasterlight.exo.orbit.Constants;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.exo.strategy.Mission;
import com.fasterlight.game.SettingsGroup;
import com.fasterlight.glout.*;
import com.fasterlight.io.IOUtil;
import com.fasterlight.spif.PropertyRejectedException;
import com.sun.opengl.util.BufferUtil;

/**
  * a GLOContext that contains lots of rendering stuff
  * for the various widgets, and also holds NV pairs for
  * ship, selected, tracked etc.
  */
public class GUIContext extends GLOContext
{
	public StarRenderer2 starr;
	public TextureCache texcache;
	String TEX_URL_BASE = "file:./texs/";
	public ModelRenderCache rendcache;
	public int list_base;
	public int tex_ints[];

	Map vars = new HashMap();
	SpaceGame game;

	// gl caps
	public boolean glMultiTex;
	public int glTexUnits;
	public boolean glPaletting;

	static final float MIN_STAR_ALPHA = 1 / 64f;
	static final int SPHERE_DIVISIONS = 48;

	GLODemonstrator democmpt;

	// caches for VisualView

	Map plantexcaches = new HashMap();
	WeakHashMap partsyss = new WeakHashMap();

	// for inertial modification
	//	Vector3d lastInertAccel = new Vector3d();
	//	float inertx,inerty,inertz;

	public static final int BORDER=2;
	public static final int TEX_LEVEL=8;
	public static final int TEX_SIZE=(1<<TEX_LEVEL);
	public static final int USABLE_TEX_SIZE=TEX_SIZE-BORDER*2;

	//

	public GUIContext()
	{
		this.nearz = -2;
		this.farz = 2;
		// create GLODemonstrator component
		this.democmpt = new GLODemonstrator();
		this.add(democmpt);
	}

	public GUIContext(SpaceGame game)
	{
		this();
		setGame(game);
	}

	public SpaceGame createNewGame(Mission m)
	{
		if (m == null)
			throw new GLOUserException("Could not find mission");

		SpaceGame game = new SpaceGame();
		m.prepare(game);
		if (m.getSequencer() != null)
		{
			m.getSequencer().setVar("ui", ctx);
			m.getSequencer().start();
			// doesn't work here
			//   	   setProp("ship", game.getAgency().getShips()[0]);
		}
		return game;
	}

	public void reset()
	{
		super.reset();

		// each ShipParticleSystem holds a reference to a VisualView,
		// so we must clear them here
		plantexcaches.clear();
		vars.clear();
	}

	// clear all plantexcaches, etc
	public void clearCaches()
	{
		Iterator it = plantexcaches.values().iterator();
		while (it.hasNext())
		{
			((PlanetRenderer) it.next()).close();
		}
		plantexcaches.clear();
		partsyss.clear();
	}

	public void renderScene()
	{
		/**
				// compute the super inertial transform
				if (doInertialTransform)
				{
			  		SpaceShip ship = getCurrentShip();
		  			if (ship != null)
		  			{
		  				Vector3d accelvec = ship.getTelemetry().getAccelVec();
		// we don't do this transform anymore
		//				ship.getTelemetry().getOrientationFixed().invTransform(accelvec);
						lastInertAccel.sub(accelvec);
						lastInertAccel.scale(inertialDecayRate);
						lastInertAccel.add(accelvec);
						inertx = (float)lastInertAccel.x*inertialLateralScale;
						inerty = (float)lastInertAccel.y*inertialLateralScale;
						inertz = (float)lastInertAccel.z*inertialAxialScale;
		  			} else {
		  				inertx = inerty = inertz = 0;
		  			}
		  		}
		**/
		// render
		super.renderScene();
		// draw the demonstrator boxes
		democmpt.renderHighlights(this);
	}

	public void setSize(int w, int h)
	{
		super.setSize(w, h);
		democmpt.setSize(w, h);
	}

	public void setGame(SpaceGame game)
	{
		if (game != this.game)
		{
			this.game = game;
			clearCaches();
		}
	}

	// query for extensions, etc
	void queryGLParams()
	{
		String exts = gl.glGetString(GL.GL_EXTENSIONS);
		System.out.println("GL Extensions: " + exts);
		// check for multitexture
		glMultiTex = (exts.indexOf("ARB_multitexture") >= 0);
		if (glMultiTex)
		{
			// get the max tex units
			int[] arr = new int[1];
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, arr, 0);
			glTexUnits = arr[0];
		}
		// are we palette boy?
		glPaletting = (exts.indexOf("EXT_paletted_texture") >= 0);
		System.out.println("Paletted textures: " + glPaletting);
		// fluffing (neccessary?)
		fluffGLDriver();
	}

	void fluffGLDriver()
	{
		/*
		try
		{
			BufferedReader in =
				com.fasterlight.io.IOUtil.getTextResource(
					"com/fasterlight/exo/newgui/glfuncs.txt");
			if (in != null)
			{
				String line;
				while ((line = in.readLine()) != null)
				{
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0)
					{
						if (!getGLContext().gljTestGLProc(line, false))
						{
							System.out.println(
								"GL function \"" + line + "\" not found!");
						}
					}
				}
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		*/
		// fluff these 'specially
		gl.glVertex3f(0, 0, 0);
		gl.glNormal3f(0, 0, 1);
		gl.glTexCoord2f(0, 0);
		gl.glMultiTexCoord2f(GL.GL_TEXTURE0, 0, 0);
		gl.glActiveTexture(GL.GL_TEXTURE0);
	}

	public SpaceGame getGame()
	{
		return game;
	}

	public SpaceShip getShip()
	{
		return (SpaceShip) vars.get("ship");
	}

	public void renderStars(float alpha)
	{
		starr.render(this, alpha);
	}

	/**
	  * Returns a sphere with no normals and y tex coord upside down.
	  */
	public int getSphereIndex(int level)
	{
		return list_base + level * 4;
	}

	/**
	  * Returns a sphere with normals and y tex coord rightisde up.
	  */
	public int getGridSphereIndex(int level)
	{
		return list_base + level * 4 + 1;
	}

	/**
	  * Returns a sphere with normals and y tex coord rightisde up.
	  */
	public int getNmlSphereIndex(int level)
	{
		return list_base + level * 4 + 2;
	}

	/**
	  * Returns a half-sphere, texcoord is 0 at horizon, 1 at pole. (?)
	  */
	public int getHemiSphereIndex(int level)
	{
		return list_base + level * 4 + 3;
	}

	public int getSphereSectorIndex(int level, int index)
	{
		switch (level)
		{
			case 0 :
				return list_base + 20;
			case 1 :
				return (list_base + 21) + index;
			case 2 :
				return (list_base + 23) + index;
			case 3 :
				return (list_base + 27) + index;
			default :
				throw new RuntimeException("getSphereSectorIndex " + level);
		}
	}

	protected void init()
	{
		super.init();

		// todo: should only use 1 tex cache?
		// also: the Frame sux
		String urlbase = IOUtil.findBaseURLForResource("texs/grid.png");
		texcache = new TextureCache(urlbase, gl, glu, new java.awt.Frame());
		texcache.init();

		rendcache = new ModelRenderCache(gl, texcache);

		list_base = gl.glGenLists(5 * 4 + 100);
		int li = list_base;
		int n = 36;

		// make the sphere shape
		for (int i = 0; i < 5; i++)
		{
			// without normals
			gl.glNewList(li++, GL.GL_COMPILE);
			sphere(gl, n * 2, n, 1, -1, false, false); // should be neg?
			gl.glEndList();

			// grid (36x18)
			gl.glNewList(li++, GL.GL_COMPILE);
			sphere(gl, n * 2, n, 36, 18, true, false);
			gl.glEndList();

			// with normals
			gl.glNewList(li++, GL.GL_COMPILE);
			sphere(gl, n * 2, n, 1, 1, true, false);
			gl.glEndList();

			// hemisphere
			gl.glNewList(li++, GL.GL_COMPILE);
			sphere(gl, n * 2, n, 0, 2, false, true);
			// sphereSector(gl, n*2, n, 0, n*2, 0, n, false, true, 0, 2, 0, 0);
			// top of sphere should be y=0, equator y=0.5
			gl.glEndList();

			n /= 2;
		}

		tex_ints = new int[1];
		gl.glGenTextures(1, tex_ints, 0);

		// make gradient texture
		byte[] barr = new byte[4];
		barr[0] = 0;
		barr[1] = 0;
		barr[2] = (byte) 0xff;
		barr[3] = (byte) 0xff;
		ByteBuffer buf = BufferUtil.newByteBuffer(4);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tex_ints[0]);
		gl.glTexImage2D(
			GL.GL_TEXTURE_2D,
			0,
			GL.GL_ALPHA,
			4,
			1,
			0,
			GL.GL_ALPHA,
			GL.GL_UNSIGNED_BYTE,
			buf);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

		// make sphere slices
		int nslices = SPHERE_DIVISIONS;
		int nstacks = nslices / 2;
		float to = BORDER * 1f / TEX_SIZE;
		for (int i = 0; i < 4; i++)
		{
			for (int j = 0; j < (1 << i); j++)
			{
				gl.glNewList(li++, GL.GL_COMPILE);
				int k = nstacks;
				sphereSector(
					gl,
					nslices << i,
					nstacks << i,
					0,
					nslices / 2,
					j * k,
					(j + 1) * k,
					true,
					false,
					1 - to * 2,
					1 - to * 2,
					to,
					to);
				gl.glEndList();
			}
		}

		// load & render stars to display list
		starr = new StarRenderer2();
		starr.loadStars();
		starr.setup(this);

		// get extensions, gl params, etc
		queryGLParams();

		// load x-tra shaders
		try
		{
			loadShaders("panels/shaders.txt");
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	public static void sphere(
		GL gl,
		int slices,
		int stacks,
		float tx,
		float ty,
		boolean nmls,
		boolean hemi)
	{
		sphereSector(
			gl,
			slices,
			stacks,
			0,
			slices,
			0,
			stacks,
			nmls,
			hemi,
			tx,
			ty,
			0,
			0);
	}

	public static void sphereSector(
		GL gl,
		int slices,
		int stacks,
		int loslice,
		int hislice,
		int lostack,
		int histack,
		boolean nmls,
		boolean hemi)
	{
		sphereSector(
			gl,
			slices,
			stacks,
			loslice,
			hislice,
			lostack,
			histack,
			nmls,
			hemi,
			1,
			1,
			0,
			0);
	}

	public static void sphereSector(
		GL gl,
		int slices,
		int stacks,
		int loslice,
		int hislice,
		int lostack,
		int histack,
		boolean nmls,
		boolean hemi,
		float txs,
		float tys,
		float tx0,
		float ty0)
	{
		// make sin, cos table
		double sintab[] = new double[slices + 1];
		double costab[] = new double[slices + 1];
		for (int i = 0; i <= slices; i++)
		{
			double ii = i * 1d / slices;
			double ti = (hislice - i) * 1d / (hislice - loslice);
			double theta = Math.PI * 2 * ii;
			sintab[i] = Math.sin(theta);
			costab[i] = Math.cos(theta);
		}

		double cx, sx, cy, sy, cy2, sy2, jj, jj2, theta;
		double tj, tj2;
		jj = lostack * 1d / stacks;
		tj = 0;
		cy = Math.cos(Math.PI * jj);
		sy = Math.sin(Math.PI * jj);

		// iterate over stacks
		int endstack = hemi ? histack / 2 + 1 : histack;
		for (int j = lostack + 1; j <= endstack; j++)
		{
			jj2 = jj;
			jj = j * 1d / stacks;
			tj2 = tj;
			tj = (j - lostack) * 1d / (histack - lostack);
			cy2 = cy;
			sy2 = sy;
			cy = Math.cos(Math.PI * jj);
			sy = Math.sin(Math.PI * jj);

			// don't make degenerate quads -- use triangles
			boolean atTop = (j == 1);
			boolean atBottom = (j == stacks);
			int p = 0;
			gl.glBegin(/* (atTop||atBottom) ? GL.GL_TRIANGLES : */
			GL.GL_QUAD_STRIP);
			for (int i = loslice; i <= hislice; i++)
			{
				double texx, texy;
				double ti = (hislice - i) * 1d / (hislice - loslice);
				sx = sintab[i];
				cx = costab[i];

				texx = ti * txs + tx0;
				texy = tj * tys + ty0;
				//				if (hemi)
				//					texy = expcorrect(texy);
				gl.glTexCoord2d(texx, texy);
				if (nmls)
					gl.glNormal3d(-sy * cx, sy * sx, cy);
				gl.glVertex3d(-sy * cx, sy * sx, cy);

				texy = tj2 * tys + ty0;
				//				if (hemi)
				//					texy = expcorrect(texy);
				gl.glTexCoord2d(texx, texy);
				if (nmls)
					gl.glNormal3d(-sy2 * cx, sy2 * sx, cy2);
				gl.glVertex3d(-sy2 * cx, sy2 * sx, cy2);
			}
			gl.glEnd();
		}
	}

	private static double expcorrect(double ty)
	{
		return (Math.pow(10, ty) - 1) / (10 - 1);
	}

	//

	public Object getProp(String key)
	{
		switch (key.charAt(0))
		{
			case 'g' :
				if ("game".equals(key))
					return game;
				break;
			case 'u' :
				if ("ui".equals(key))
					return this;
				break;
			case 'd' :
				if ("demo".equals(key))
					return democmpt;
				break;
			case 'v' :
				if ("version".equals(key))
					return Constants.EXOFLIGHT_VERSION;
			default :
				Object o = vars.get(key);
				if (o != null)
					return o;
		}
		return super.getProp(key);
	}

	public void gameMessage(Object value)
	{
		game.message(value.toString());
		if (showDialogForMessages)
		{
			GLOMessageBox.showOk(this, value.toString());
		}
		else if (showMessageWndForMessages)
		{
			GLOWindow wnd =
				(GLOWindow) getDescendantNamed(this, "Messages Window");
			if (wnd != null)
			{
				wnd.setVisible(true);
				wnd.raise();
			}
		}
	}

	public void setProp(String key, Object value)
	{
		// todo: paused?
		switch (key.charAt(0))
		{
			case 'm' :
				if ("message".equals(key) && value != null)
				{
					gameMessage(value);
					return;
				}
			case 's' :
				if ("ship".equals(key))
				{
					if (!(value instanceof SpaceShip))
						throw new PropertyRejectedException("'ship' must be instanceof SpaceShip");
					vars.put(key, value);
					vars.put("tracked", value);
					vars.put("selected", value);
					return;
				}
			default :
				try
				{
					super.setProp(key, value);
				}
				catch (PropertyRejectedException pre)
				{
					// todo? bad to catch & put?
					vars.put(key, value);
					return;
				}
		}
	}

	public SpaceShip getCurrentShip()
	{
		return (SpaceShip) getProp("ship");
	}

	public void setCurrentShip(SpaceShip selected)
	{
		setProp("ship", selected);
	}

	//

	static boolean showDialogForMessages;
	static boolean showMessageWndForMessages;
	static final boolean doInertialTransform = false;
	//	static float inertialLateralScale, inertialAxialScale, inertialDecayRate;

	static SettingsGroup settings = new SettingsGroup(GUIContext.class, "UI")
	{
		public void updateSettings()
		{
			showDialogForMessages = getBoolean("ShowDialogForMessages", false);
			showMessageWndForMessages = getBoolean("PopupMessageWindow", true);
			/*			doInertialTransform = getBoolean("DoInertialTransform", false);
						inertialLateralScale = getFloat("InertialLateralScale", 0.01f);
						inertialAxialScale = getFloat("InertialAxialScale", 0.01f);
						inertialDecayRate = getFloat("InertialDecayRate", 0.2f); */
		}
	};
}
