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
package com.fasterlight.exo.orbit;

import java.util.StringTokenizer;

import com.fasterlight.proctex.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.Vector3d;

/**
  * An elevation model that uses a hierarchy of bitmaps to map pixels
  * to elevation displacements.  The bitmaps in each level are the
  * same height and width, with the exception of the top level,
  * which has 2x the width (usually level 7, so it is 256x128).
  *
  * Longitude goes from (-180-180 degrees) along the X axis,
  * and latitude goes from (-90-90 degrees) along the Y axis.
  * If precision is exceeded, values are mapped with a bilinear
  * interpolation method.
  */
public class RasterLODElevationModel implements ElevationModel, PropertyAware
{
	float minrad, maxrad;

	int maxprec;

	static final int minprec = 7;

	ProcTexProvider ptp = new ProcTexProvider();

	//

	public RasterLODElevationModel()
	{
	}

	public RasterLODElevationModel(String fn, int maxprec, float minrad, float maxrad)
	{
		setFilePrefix(fn);
		setMaxPrecision(maxprec);
		setMinDisplacement(minrad);
		setMaxDisplacement(maxrad);
	}

	public RasterLODElevationModel(String spec)
	{
		StringTokenizer st = new StringTokenizer(spec, ";");
		setFilePrefix(st.nextToken());
		setMaxPrecision(Integer.parseInt(st.nextToken()));
		setMinDisplacement(Util.parseFloat(st.nextToken()));
		setMaxDisplacement(Util.parseFloat(st.nextToken()));
	}

	public void setFilePrefix(String fp)
	{
		ptp.setPathPrefix(fp);
		ptp.setPixelConfabulator(new RandomNeighborPixelConfabulator());
		ptp.setCacheAll(true);
	}

	public String getFilePrefix()
	{
		return ptp.getPathPrefix();
	}

	public ProcTexProvider getProcTexProvider()
	{
		return ptp;
	}

	public int getMinPrecision()
	{
		return minprec;
	}

	public int getMaxPrecision()
	{
		return maxprec;
	}
	public void setMaxPrecision(int maxprec)
	{
		this.maxprec = maxprec;
	}

	public float getMinDisplacement()
	{
		return minrad;
	}
	public void setMinDisplacement(float minrad)
	{
		this.minrad = minrad;
		ptp.setMinMax(minrad, maxrad);
	}

	public float getMaxDisplacement()
	{
		return maxrad;
	}
	public void setMaxDisplacement(float maxrad)
	{
		this.maxrad = maxrad;
		ptp.setMinMax(minrad, maxrad);
	}

	//

	//

	private TexQuad last_tq = null;
	private int last_x, last_y, last_level = -1;

	public TexQuad getTexQuad(int x, int y, int level)
	{
		if (x == last_x && y == last_y && level == last_level)
		{
			return last_tq;
		}
		TexQuad tq = ptp.getTexQuad(x, y, level);
		last_tq = tq;
		last_x = x;
		last_y = y;
		last_level = level;
		return tq;
	}

	public TexQuad getTexQuad(double x, double y, int level)
	{
		int xx = (int) (x * (1 << (level - minprec)));
		int yy = (int) (y * (1 << (level - minprec)) / 2);
		return getTexQuad(xx, yy, level);
	}

	public int getAbsPixel(int x, int y, int prec)
	{
		int s = (prec - minprec);
		int mx = (1 << s) - 1;
		int my = mx >> 1;
		int sc = 8 + prec - maxprec;
		int qx = (x >> sc) & mx;
		int qy = (y >> sc) & my;
		TexQuad mq = getTexQuad(qx, qy, prec);
		//		System.out.print(qx + "," + qy + " " + x + "," + y + "; ");
		return getPixel(mq, x, y);
	}

	public final int getPixel(TexQuad tq, int x, int y)
	{
		return tq.getByteData()[(x & 0xff) + ((y & 0xff) << 8)] & 0xff;
	}

	public final int getPixel(TexQuad tq, double x, double y)
	{
		return getPixel(
			tq,
			(int) (x * (ptp.getWidth(tq) - 2)) + 1,
			(int) (y * (ptp.getHeight(tq) - 2)) + 1);
	}

	public final int getPixelMinLevel(TexQuad tq, double x, double y)
	{
		return getPixel(tq, (int) (x * ptp.getWidth(tq)), (int) (y * ptp.getHeight(tq)));
	}

	public final float getPixelInterp(TexQuad tq, double x, double y)
	{
		x = x * ptp.getUsableTexSize() + ptp.getUsableTexSize();
		y = y * ptp.getUsableTexSize() + ptp.getUsableTexSize();
		int xx = (int) Math.floor(x);
		int yy = (int) Math.floor(y);
		double fx = x - xx;
		double fy = y - yy;
		double fx1 = 1 - fx;
		double fy1 = 1 - fy;
		int i1 = getPixel(tq, xx, yy);
		int i2 = getPixel(tq, xx + 1, yy);
		int i3 = getPixel(tq, xx, yy + 1);
		int i4 = getPixel(tq, xx + 1, yy + 1);
		double f1 = i1 * fx1 * fy1 + i2 * fx * fy1 + i3 * fx1 * fy + i4 * fx * fy;
		return (float) f1;
	}

	public final float getPixelInterpMinLevel(TexQuad tq, double x, double y)
	{
		int w = ptp.getWidth(tq);
		int h = ptp.getHeight(tq);
		x = x * w;
		y = y * h;
		int xx = (int) Math.floor(x);
		int yy = (int) Math.floor(y);
		double fx = x - xx;
		double fy = y - yy;
		double fx1 = 1 - fx;
		double fy1 = 1 - fy;
		int xx1 = (xx + 1) & (w - 1);
		int yy1 = (yy + 1) & (h - 1);
		int i1 = getPixel(tq, xx, yy);
		int i2 = getPixel(tq, xx1, yy);
		int i3 = getPixel(tq, xx, yy1);
		int i4 = getPixel(tq, xx1, yy1);
		double f1 = i1 * fx1 * fy1 + i2 * fx * fy1 + i3 * fx1 * fy + i4 * fx * fy;
		return (float) f1;
	}

	public float getDisplacement(double lat, double lon, int precision)
	{
		double yy = (Math.PI / 2 - lat) / (Math.PI + 1e-15);
		// **we add PI+1e-15 so that we can get -90 and +90 degrees
		double xx = (lon + Math.PI) / (Math.PI * 2);
		// truncate to range 0 <= x < 1
		xx = xx - Math.floor(xx);
		yy = yy - Math.floor(yy);
		float i;
		TexQuad mq;
		if (precision <= minprec)
		{
			mq = getTexQuad(0, 0, minprec);
			i = getPixelMinLevel(mq, xx, yy);
		}
		else if (precision > maxprec)
		{
			if (minprec != maxprec)
			{
				double x = xx * (1 << (maxprec - minprec));
				double y = yy * (1 << (maxprec - minprec)) / 2;
				mq = getTexQuad((int) x, (int) y, maxprec);
				i = getPixelInterp(mq, x - (int) Math.floor(x), y - (int) Math.floor(y));
			}
			else
			{
				mq = getTexQuad(0, 0, maxprec);
				i = getPixelInterpMinLevel(mq, xx, yy);
			}
		}
		else
		{
			double x = xx * (1 << (precision - minprec));
			double y = yy * (1 << (precision - minprec)) / 2;
			mq = getTexQuad((int) x, (int) y, precision);
			i = getPixel(mq, x - (int) Math.floor(x), y - (int) Math.floor(y));
		}
		return mq.byteToValue(i);
	}

	// return normal to surface, in SEZ coords
	// the normal is not normalized!! hah!!
	public void getNormal(double lat, double lon, int precision, Vector3d nml)
	{
		// get heights of corners of triangle, and compute normal from that
		// inaccurate but easy
		//		double sqrt2 = 1.4142136;
		double small = 1e-4; // 1 m
		float d1 = getDisplacement(lat + small, lon - small, precision);
		float d2 = getDisplacement(lat + small, lon + small, precision);
		float d3 = getDisplacement(lat - small, lon - small, precision);
		float d4 = getDisplacement(lat - small, lon + small, precision);
		nml.x = (d2 - d1 + d4 - d3) / 2;
		nml.y = (d1 - d3 + d2 - d4) / 2;
		nml.z = small * 2;
	}

	//

	//

	public static void main(String[] args) throws Exception
	{
		RasterLODElevationModel bmconv =
			new RasterLODElevationModel("elevtexs/Earth/Earth-elev", 11, 0, 8.5f);

		double lat = Util.toRadians(49d);
		double lon = Util.toRadians(-110d);
		Vector3d nml = new Vector3d();

		for (int prec = bmconv.minprec; prec <= bmconv.maxprec + 1; prec++)
		{
			System.out.println(bmconv.getDisplacement(lat, lon, prec));
			bmconv.getNormal(lat, lon, prec, nml);
			System.out.println(nml);
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(RasterLODElevationModel.class);

	static {
		prophelp.registerGetSet("fileprefix", "FilePrefix", String.class);
		prophelp.registerGetSet("mindisplacement", "MinDisplacement", float.class);
		prophelp.registerGetSet("maxdisplacement", "MaxDisplacement", float.class);
		prophelp.registerGetSet("maxprecision", "MaxPrecision", int.class);
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
