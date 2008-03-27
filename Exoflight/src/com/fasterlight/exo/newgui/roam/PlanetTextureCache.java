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

import java.nio.*;
import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.glout.*;
import com.fasterlight.proctex.*;
import com.fasterlight.vecmath.*;
import com.sun.opengl.util.BufferUtil;

/**
  * Performs bump-mapping on textures at various levels
  * of detail and caches the results.
  *
  * todo: recover gracefully from errors
  */
public class PlanetTextureCache implements Runnable
{
	Planet planet;
	ElevationModel elevmodel;
	GL gl;

	ProcTexProvider ptp, cloudptp, nightptp;

	Map entrymap = new HashMap();
	Set freeints = new TreeSet();
	TreeSet workqueue = new TreeSet(); // queue of TexKey

	BitSet texalloc = new BitSet();

	BumpMapper bmapper;

	int minBumpMapLevel = 10;

	int destgltype;
	int flags;

	static final int NUM_TEXS = 512;
	int[] texints = new int[NUM_TEXS];

	Vector3d sunpos = new Vector3d(1, 0, 0);
	double sunlon; // longitude of sun
	double sunlat; // latitude of sun
	double sinsunlat, cossunlat;

	double THRESHOLD = Math.toRadians(0.5f);
	// sun has to move this much before refresh

	double BLANK = Math.sin(Math.toRadians(-10));

	int NUM_GEN_LEVELS = 0;
	int MAX_ELEV_SCALE = 20;
	float BUMPMAP_FACTOR = 1.0f;

	int thisref = 0; // incremented every frame
	int min_ty, max_ty;

	//

	public int solidColor = -1; // white

	public static final int SRC_SOLID = 0;
	public static final int SRC_COLOR = 1;
	public static final int SRC_CLOUDS = 2;
	public static final int SRC_ELEV = 4;
	public static final int SRC_NIGHT = 8;

	public static final int DO_BUMPMAP = 0x100;
	public static final int DO_CLOUDS = 0x200;
	public static final int DO_LIGHTMAP = 0x400;
	public static final int DO_NIGHTLIGHTS = 0x800;
	public static final int DO_PALETTE = 0x1000;

	//

	// todo: this is a test
	static int[] skypalette;
	static {
		skypalette = new int[512];
		Vector3f col = new Vector3f();
		for (int i = 0; i < 512; i++)
		{
			float x = (i - 256) / 255.45f;
			float x2 = x + 0.10f;
			float amb = (x2 > 0) ? curve(x2, 0.5) * 0.5f : 0;
			amb = 0;
			col.set(amb, amb, amb);
			if (x > 0)
			{
				col.x += curve(x, 0.35);
				col.y += curve(x, 0.5);
				col.z += curve(x, 0.6);
			}
			skypalette[i] = GLOUtil.rgb2int(col.x, col.y, col.z);
		}
	}

	private static float curve(double x, double pwr)
	{
		return (float) (Math.pow(x, pwr));
	}

	//

	class TexEntry
	{
		int idx;
		float sunang, threshold;
		int lastref;
	}

	//

	class TexQueueKey extends TexKey implements Comparable
	{
		int prio;

		TexQueueKey(int x, int y, int l)
		{
			super(x, y, l);
			prio = l;
		}
		TexQueueKey(TexKey k)
		{
			this(k.x, k.y, k.level);
		}
		public int compareTo(java.lang.Object o)
		{
			if (!(o instanceof TexQueueKey))
				return 0;
			return prio - ((TexQueueKey) o).prio;
		}
	}

	//

	public PlanetTextureCache(Planet planet, GL gl, int flags, int destgltype)
	{
		this.planet = planet;
		this.elevmodel = planet.getElevationModel();
		this.gl = gl;
		this.flags = flags;
		this.destgltype = destgltype;

		this.ptp = new ProcTexProvider();
		ptp.setPathPrefix("texs/" + planet.getName() + "/" + planet.getName());

		//		BasicPixelConfabulator bpc = new BasicPixelConfabulator("com/fasterlight/proctex/pixtab.out");
		//		BetterPixelConfabulator bpc = new BetterPixelConfabulator("com/fasterlight/proctex/pixtab2.out");
		//		ptp.setPixelConfabulator(bpc);

		if ((flags & (DO_CLOUDS | SRC_CLOUDS)) != 0)
		{
			this.cloudptp = new ProcTexProvider();
			cloudptp.setPathPrefix("texs/" + planet.getName() + "/" + planet.getName() + "-clouds");
		}

		if ((flags & (DO_NIGHTLIGHTS | SRC_NIGHT)) != 0)
		{
			this.nightptp = new ProcTexProvider();
			nightptp.setPathPrefix("texs/" + planet.getName() + "/" + planet.getName() + "-night");
		}

		// alloate NUM_TEXS textures
		gl.glGenTextures(NUM_TEXS, texints, 0);

		createBlankTex();
		for (int i = 2; i < NUM_TEXS; i++)
			freeints.add(new Integer(i)); //start from 2, because 1 is reserved

		int l = ROAMPlanet.RANGE_TEXINT >> getMaxLevel();
		min_ty = ROAMPlanet.MIN_TEXINT + l;
		max_ty = ROAMPlanet.MAX_TEXINT - l;
	}

	private void makeBumpMapper()
	{
		if (bmapper == null)
		{
			this.bmapper = new BumpMapper();
			//			bmapper.POWER = (planet.getAtmosphere() != null) ? 0.75 : 1.0;
			bmapper.BIAS = (planet.getAtmosphere() != null) ? 0.15f : 0.0f;
			bmapper.EXPOSURE = 255.45f * 1.0f;
		}
	}

	private void createBlankTex()
	{
		ByteBuffer buf = BufferUtil.newByteBuffer(1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texints[0]);
		gl.glTexImage2D(
			GL.GL_TEXTURE_2D,
			0,
			GL.GL_LUMINANCE,
			1,
			1,
			0,
			GL.GL_LUMINANCE,
			GL.GL_UNSIGNED_BYTE,
			buf);
		int param = GL.GL_REPEAT;
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, param);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, param);
	}

	public int getMaxLevel()
	{
		return (elevmodel != null) ? elevmodel.getMaxPrecision() : 7;
	}

	public int getBestLevel(float tx, float ty, int level)
	{
		int ml = getMaxLevel();
		if (ty > min_ty && ty < max_ty)
			ml = Math.min(ml + NUM_GEN_LEVELS, level);
		return ml;
	}

	//

	private synchronized void freeSomeTexs()
	{
		// todo: smart!
		TexKey bestkey = null;
		TexEntry bestentry = null;

		Iterator it = entrymap.keySet().iterator();
		while (it.hasNext())
		{
			TexKey key = (TexKey) it.next();
			if (key.level > 7)
			{
				TexEntry te = (TexEntry) entrymap.get(key);
				if (bestentry == null || te.lastref < bestentry.lastref)
				{
					bestkey = key;
					bestentry = te;
				}
			}
		}

		if (bestentry == null)
			throw new RuntimeException("Shouldn't be here!");

		returnSlot(bestentry.idx);
		entrymap.remove(bestkey);
		if (debug)
			System.out.println("returned " + bestkey + ", ref=" + bestentry.lastref);
	}

	private int getEmptySlot(int level)
	{
		if (level <= 7)
			return 1; // tex 1 is always level 7

		if (freeints.size() == 0)
		{
			freeSomeTexs();
		}

		Iterator it = freeints.iterator();
		Integer i = (Integer) (it.next());
		it.remove();
		return i.intValue();
	}

	private void returnSlot(int i)
	{
		freeints.add(new Integer(i));
	}

	private static void fixInts(int[] arr)
	{
		for (int i = 0; i < arr.length; i++)
		{
			arr[i] = ((arr[i] & 0xff) << 16) + (arr[i] & 0xff00) + ((arr[i] & 0xff0000) >> 16);
		}
	}

	private IntBuffer getColorImage(int x, int y, int level)
	{
		TexQuad tq = ptp.getTexQuad(x, y, level);
		return ptp.getRGBData(tq);
	}

	private byte[] getCloudsImage(int x, int y, int level)
	{
		TexQuad tq = cloudptp.getTexQuad(x, y, level);
		return tq.getByteData();
	}

	// newsunpos is in geocentric (not ijk)
	public void setSunPos(Vector3d newsunpos)
	{
		sunpos.set(newsunpos);
		planet.ijk2llr(sunpos);
		sunlon = sunpos.x;
		sunlat = sunpos.y;
		//		sunlon = AstroUtil.fixAngle(bias+Math.atan2(sunpos.y, sunpos.x));
		//		System.out.println("sunlon for " + planet + " : " + sunlon);
		sinsunlat = Math.sin(sunlat);
		cossunlat = Math.cos(sunlat);
	}

	//

	private boolean isBlank(double lat, double lon)
	{
		double sinlat = Math.sin(lat);
		double coslat = Math.cos(lat);
		double coslon = Math.cos(lon);
		double ang = coslat * cossunlat * coslon + sinlat * sinsunlat;
		return (ang < BLANK);
	}

	private String getTexName(int x, int y, int level)
	{
		return (level > 7) ? (level + "-" + x + "-" + y) : "7";
	}

	private TexQuad getSourceQuad(int x, int y, int level)
	{
		switch (flags & 0xff)
		{
			case SRC_SOLID :
				{
					TexQuad tq = new TexQuad(x, y, level);
					byte[] barr = new byte[GUIContext.TEX_SIZE * GUIContext.TEX_SIZE];
					tq.setByteData(barr);
					return tq;
				}
			case SRC_COLOR :
				return ptp.getTexQuad(x, y, level);
			case SRC_CLOUDS :
				return cloudptp.getTexQuad(x, y, level);
			case SRC_ELEV :
				return elevmodel.getTexQuad(x, y, level);
			case SRC_NIGHT :
				return nightptp.getTexQuad(x, y, level);
			default :
				throw new RuntimeException(flags + "");
		}
	}

	// TEXTURE UPDATE

	private IntBuffer destbuf = BufferUtil.newIntBuffer(GUIContext.TEX_SIZE * GUIContext.TEX_SIZE);
	private ByteBuffer destbufbytes = BufferUtil.newByteBuffer(GUIContext.TEX_SIZE * GUIContext.TEX_SIZE);
	private IntBuffer destbufpalette;

	Thread updthread;
	TexEntry pendingentry;
	TexQueueKey pendingkey;

	public void run()
	{
		while (updthread != null)
		{
			try
			{
				updateWorkQueue();
				synchronized (this)
				{
					wait();
				}
			} catch (Exception exc)
			{
				exc.printStackTrace();
			}
		}
	}

	private IntBuffer getPaletteInts()
	{
		int[] palints;

		switch (flags & 0xff)
		{
			case SRC_SOLID :
				palints = new int[1];
				palints[0] = solidColor;
				break;
			case SRC_ELEV :
				palints = skypalette;
				break;
			case SRC_CLOUDS :
				palints = cloudptp.getPaletteInts();
				break;
			case SRC_NIGHT :
				palints = nightptp.getPaletteInts();
				break;
			default :
				palints = ptp.getPaletteInts();
				break;
		}

		// TODO: inefficient
		IntBuffer buf = BufferUtil.newIntBuffer(palints.length);
		buf.put(palints);
		buf.rewind();
		return buf;
	}

	private TexEntry prepareTexture(int x, int y, int level, int existidx)
	{
		if ((flags & DO_PALETTE) != 0)
			return prepareTexturePaletted(x, y, level, existidx);
		else
			return prepareTextureRGB(x, y, level, existidx);
	}

	private TexEntry prepareTexturePaletted(int x, int y, int level, int existidx)
	{
		TexQuad srcquad = getSourceQuad(x, y, level);
		destbufbytes = BufferUtil.newByteBuffer(srcquad.data.length);
		destbufbytes.put(srcquad.data);
		// TODO: put the array data back into the quad to save memory
		/*
		try {
			byte[] arr = destbufbytes.array();
			if (arr != null)
				srcquad.setByteData(arr);
		} catch (Exception e) {
			// 
		}
		*/
		destbufpalette = getPaletteInts();

		int ti;
		if (existidx < 0)
		{
			ti = getEmptySlot(level);
		} else
		{
			ti = existidx;
		}

		TexEntry te = new TexEntry();
		te.idx = ti;
		te.lastref = thisref;

		return te;
	}

	private TexEntry prepareTextureRGB(int x, int y, int level, int existidx)
	{
		// todo: support null elevmodel
		{
			// calculate parameters
			double lolat, lolon, hilat, hilon;
			int ti = 0;
			int imgw = 256;
			int imgh = (level <= 7 ? 128 : 256);
			int xs = (2 << level) / imgw;
			int ys = (1 << level) / imgh;
			if (xs == 0)
				xs = 1;
			if (ys == 0)
				ys = 1;
			lolon = x * Math.PI * 2 / xs - Math.PI - sunlon;
			hilon = (x + 1) * Math.PI * 2 / xs - Math.PI - sunlon;
			lolat = Math.PI / 2 - y * Math.PI / ys;
			hilat = Math.PI / 2 - (y + 1) * Math.PI / ys;

			// check to see if this area is definitely BLACK
			// (how much more black could this be? none more black...)
			if ((flags & DO_BUMPMAP) != 0
				&& level > 8
				&& isBlank(lolat, lolon)
				&& isBlank(hilat, lolon)
				&& isBlank(lolat, hilon)
				&& isBlank(hilat, hilon))
			{
				ti = 0;
				if (debug)
					System.out.println(
						"blank for " + planet.getName() + "-" + getTexName(x, y, level));

			} else
			{

				// load source map
				TexQuad srcquad = getSourceQuad(x, y, level);

				if ((flags & (DO_BUMPMAP | DO_LIGHTMAP)) != 0 && elevmodel != null)
				{
					// load elevation map
					byte[] emap;
					double sc;

					TexQuad mq;
					mq = elevmodel.getTexQuad(x, y, level);
					emap = mq.getByteData();
					sc = (mq.maxvalue - mq.minvalue) * BUMPMAP_FACTOR / planet.getRadius();

					// bump-map
					float scale = (float) ((1 << Math.min(MAX_ELEV_SCALE, level)) * sc);
					//todo

					if (debug)
						System.out.println(
							"bumpmapping "
								+ planet.getName()
								+ "-"
								+ getTexName(x, y, level)
								+ ", scale "
								+ scale);

					byte[] cldmap = null;
					if ((flags & DO_CLOUDS) != 0)
					{
						cldmap = getCloudsImage(x, y, level);
					}

					IntBuffer palints = getPaletteInts();

					makeBumpMapper(); // if it isn't there already
					if ((flags & DO_LIGHTMAP) == 0)
					{
						// TODO: support lightmap
						/*
						bmapper.lightSphericalMapIndexed(
							srcquad.getByteData(),
							palints,
							emap,
							cldmap,
							destbuf,
							imgw,
							imgh,
							scale,
							scale,
							(float) lolat,
							(float) lolon,
							(float) hilat,
							(float) hilon,
							(float) sunlat);
					} else
					{
						bmapper.lightSphericalMapBumpOnly(
							srcquad.getByteData(),
							palints,
							destbuf,
							imgw,
							imgh,
							scale,
							scale,
							(float) lolat,
							(float) lolon,
							(float) hilat,
							(float) hilon,
							(float) sunlat);
					*/
					}
				} else
				{
					// no bump or light mapping
					switch (flags & 0xff)
					{
						case SRC_COLOR :
							ptp.getRGBData(srcquad, destbuf);
							break;
						case SRC_CLOUDS :
							cloudptp.getRGBData(srcquad, destbuf);
							break;
						case SRC_NIGHT :
							nightptp.getRGBData(srcquad, destbuf);
							break;
						default :
							if (elevmodel != null)
							{
								((RasterLODElevationModel) elevmodel)
									.getProcTexProvider()
									.getRGBData(
									srcquad,
									destbuf);
								break;
							} else
								throw new RuntimeException("todo: prepareTexture flags = " + flags);
					}
				}

				// if no existing slot, get empty one
				if (existidx < 0)
				{
					ti = getEmptySlot(level);
				} else
				{
					ti = existidx;
				}
			}

			TexEntry te = new TexEntry();
			te.idx = ti;
			te.sunang = (float) sunlon;
			te.lastref = thisref;
			double f1 = 1;
			double f2 = Math.abs(Math.cos((lolon + hilon) / 2)) + 1;
			// todo: threshold multiples of something
			te.threshold = (float) (THRESHOLD * f1 * f2);

			return te;
		}
	}

	/**
	  * Commits the texture to OpenGL.
	  * So it must be executed in the main thread.
	  */
	private void commitTexture()
	{
		if (pendingentry != null)
		{
			boolean doPalette = ((flags & DO_PALETTE) != 0) && (destbufbytes != null);
			int ti = pendingentry.idx;
			int imgw = ptp.getWidth(pendingkey);
			int imgh = ptp.getHeight(pendingkey);
			int level = pendingkey.level;

			gl.glBindTexture(GL.GL_TEXTURE_2D, texints[ti]);

			if (texalloc.get(ti))
			{
				if (doPalette)
					gl.glTexSubImage2D(
						GL.GL_TEXTURE_2D,
						0,
						0,
						0,
						imgw,
						imgh,
						GL.GL_COLOR_INDEX,
						GL.GL_UNSIGNED_BYTE,
						destbufbytes);
				else
					gl.glTexSubImage2D(
						GL.GL_TEXTURE_2D,
						0,
						0,
						0,
						imgw,
						imgh,
						GL.GL_RGBA,
						GL.GL_UNSIGNED_BYTE,
						destbuf);
			} else
			{
				if (doPalette)
				{
					gl.glTexImage2D(
						GL.GL_TEXTURE_2D,
						0,
						GL.GL_COLOR_INDEX8_EXT,
						imgw,
						imgh,
						0,
						GL.GL_COLOR_INDEX,
						GL.GL_UNSIGNED_BYTE,
						destbufbytes);
					gl.glColorTable(
						GL.GL_TEXTURE_2D,
						GL.GL_RGBA8,
						256,
						GL.GL_RGBA,
						GL.GL_UNSIGNED_BYTE,
						destbufpalette);
				} else
					gl.glTexImage2D(
						GL.GL_TEXTURE_2D,
						0,
						destgltype,
						imgw,
						imgh,
						0,
						GL.GL_RGBA,
						GL.GL_UNSIGNED_BYTE,
						destbuf);
				int param = (level > 7) ? GL.GL_CLAMP : GL.GL_REPEAT;
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, param);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, param);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
				texalloc.set(ti);
			}
			GLOContext.checkGL();
			if (debug)
				System.out.println("committed " + pendingkey);
			synchronized (this)
			{
				workqueue.remove(pendingkey);
				entrymap.put(pendingkey, pendingentry);
				pendingkey = null; // always set first
				pendingentry = null;
				destbufbytes = null; // reset so next time won't have issues
			}
		}
	}

	private void updateWorkQueue()
	{
		if (pendingentry == null && !workqueue.isEmpty())
		{
			TexQueueKey key;
			TexEntry te;

			synchronized (this)
			{
				if (debug)
				{
					System.out.println(
						"pending=" + pendingkey + " " + pendingentry + " queue=" + workqueue);
				}

				key = (TexQueueKey) workqueue.first();
				// if there is already a texture there, re-do it
				te = (TexEntry) entrymap.get(key);
			}
			if (debug)
				System.out.println(updthread + ": got " + key);

			// prepare in 3d
			te = prepareTexture(key.x, key.y, key.level, (te != null) ? te.idx : -1);
			if (te != null)
			{
				pendingkey = key;
				pendingentry = te;
			}
		}
	}

	//

	public void update()
	{
		euthanizeMercifully(); // clean up any finalized texture records
		commitTexture(); // commit any new textures
		thisref++;
	}

	public void queueKey(TexKey key)
	{
		synchronized (this)
		{
			workqueue.add(new TexQueueKey(key));
			// there will be no more than 2 entries in the queue..
			// since they're sorted by priority, we are only really
			// interested in the frontmost ones
			if (workqueue.size() > 2)
			{
				workqueue.remove(workqueue.last());
			}
			if (pendingentry == null)
			{
				if (updthread != null)
					notify();
				else
					startUpdateThread();
			}
		}
	}

	public String toString()
	{
		return "PlanetTextureCache:" + planet + "/" + destgltype + "/" + flags;
	}

	private static final int UPD_PRIORITY = Thread.NORM_PRIORITY - 0;

	private void startUpdateThread()
	{
		updthread = new Thread(this, this.toString());
		updthread.setDaemon(true);
		updthread.setPriority(UPD_PRIORITY);
		updthread.start();
	}

	//

	private TexKey priv_key = new TexKey();

	public boolean hasTextureCached(int x, int y, int level)
	{
		priv_key.set(x, y, level);
		if (entrymap.containsKey(priv_key))
		{
			return true;
		} else
		{
			queueKey(priv_key);
			return false;
		}
	}

	/**
	  * Retrieves a texture int from the cache.
	  * If desired texture is not present, will find one of a
	  * smaller level and put its params in 'key'.
	  * Returns texture int, or the blank texture if could not
	  * get any texture for some reason.
	  */
	public int getTextureInt(TexKey key)
	{
		// see if texture is already cached
		TexEntry te = (TexEntry) entrymap.get(key);

		// if it is expired, disregard
		if ((flags & (DO_BUMPMAP | DO_LIGHTMAP)) != 0 && te != null)
		{
			boolean expired = (Math.abs(sunlon - te.sunang) > te.threshold);
			if (expired)
			{
				queueKey(key);
			}
		}

		// if it wasn't expired, set it and return
		if (te != null)
		{
			te.lastref = thisref;
			return texints[te.idx];
		}

		// if level > min, put it on the work queue,
		// and try the next level up
		queueKey(key);
		if (key.level > 7)
		{
			key.x >>= 1;
			key.y >>= 1;
			key.level -= 1;
			return getTextureInt(key);
		}

		// we must be at level 7, so use blank
		return texints[0];
	}

	/**
	  * Calls getTextureInt() and binds to the texture.
	  * Texture may not be the one you asked for, see getTextureInt().
	  */
	public int setTexture(int x, int y, int level)
	{
		TexKey key = priv_key;
		if (level > 7)
			key.set(x, y, level);
		else
			key.set(0, 0, 7);

		int texint = getTextureInt(key);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texint);

		return key.level;
	}

	public void printStatistics()
	{
		System.out.println();
		System.out.println("Entry map: " + entrymap);
		System.out.println("Free ints: " + freeints);
		System.out.println();
	}

	// FINALIZATION

	boolean beenFinalized = false;

	static List mercyQueue = new ArrayList();

	private static void euthanizeMercifully()
	{
		// todo: concurrent modification exception
		if (mercyQueue.size() > 0)
		{
			Iterator it = mercyQueue.iterator();
			while (it.hasNext())
			{
				((PlanetTextureCache) it.next()).close();
				it.remove();
			}
		}
	}

	public synchronized void close()
	{
		if (texints == null)
			return;
		gl.glDeleteTextures(NUM_TEXS, texints, 0);
		// kill update thread
		updthread = null;
		notify();
		System.err.println("**Closed " + this);
		texints = null;
	}

	public void finalize()
	{
		if (!beenFinalized)
		{
			// ask the next PTC to be merciful :)
			mercyQueue.add(this);
			beenFinalized = true;
		}
	}

	//

	boolean debug = false;
}
