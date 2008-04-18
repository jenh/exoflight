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

import java.nio.IntBuffer;

import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.proctex.*;
import com.fasterlight.vecmath.Vector3f;

/**
  * Used to bake the lightmap textures onto another texture,
  * based on sun position and elevation map.
  */
public class BumpMapper
{
	static final float MIN_LAT_NML = 0.1f;

	float EXPOSURE = 255.45f;
	float BIAS = 0;
	float HEIGHT_MOD = 0.1f; // factor for "jagged" terminators

	boolean vflip = false;

	private float[] tmparr = new float[512];

	private final float sinX(int x)
	{
		return tmparr[x*2];
	}
	private final float cosX(int x)
	{
		return tmparr[x*2+1];
	}

	public void lightSphericalMap(
		byte[] emap, byte[] cldmap, int[] cmap, int[] dest,
		int w, int h, float xscale, float yscale,
		float lolat, float lolon, float hilat, float hilon,
		float sunlat)
	{
		int x,y,n,n2,n3;
		int r,r1;
		float latnml;
		Vector3f snml = new Vector3f();
		Vector3f lnml;
		float lat;
		float coslat,sinlat;
		int cld=0,pix;

		xscale *= (1f/255);
		yscale *= (1f/255);

		float cossunlat = (float)Math.cos(sunlat);
		float sinsunlat = (float)Math.sin(sunlat);

		for (x=0; x<w; x++)
		{
			float lon = lolon + x*(hilon-lolon)/w;
			tmparr[x*2] = (float)Math.sin(lon);
			tmparr[x*2+1] = (float)Math.cos(lon);
		}

		for (y=0; y<h; y++)
		{
			n = y*w;
			n3 = (y>0) ? n-w : n;

			// get the 1st pixel of this row
			if (Math.abs(hilon-lolon) > Math.PI*1.99)
				r1 = emap[n+w-1]&0xff;
			else
				r1 = emap[n]&0xff;
			lat = lolat + y*(hilat-lolat)/h;
			coslat = (float)Math.cos(lat);
			sinlat = (float)Math.sin(lat);

			for (x=0; x<w; x++)
			{
				float cosnml;

				float coslon = cosX(x);
				float sinlon = sinX(x);

  				r = emap[n]&0xff;
  				if (cldmap != null)
  				{
					cld = cldmap[n]&0xff;
					if (cld != 0)
						r += (255-r)*cld/512;
				}

  				// light normal depends on slope
  				float xslope = (r-r1)*xscale/(coslat+MIN_LAT_NML);
//  				float yslope = (r-(emap[n3+x]&0xff))*yscale;
  				r1 = r;

				// todo: get rid of sin,cos?
  				cosnml = (coslat*cossunlat*(
  						coslon*AstroUtil.cos5(xslope) + sinlon*AstroUtil.sin5(xslope)
  					) + sinlat*sinsunlat) + BIAS;
			/*
  				cosnml = (float)(coslat*AstroUtil.cos5(sunlat+yslope)*(
  						coslon*Astroutil.cos5(xslope) + sinlon*AstroUtil.sin5(xslope)
  					) + sinlat*Astroutil.sin5(sunlat+yslope));
  			*/

				if (cosnml > 0)
				{
					pix = cmap[n];
					if (cld != 0)
						pix = rgbmul(pix^0xffffff, 255-cld)^0xffffff;
					pix = rgbmul( pix, (int)(EXPOSURE*cosnml) );
				} else
					pix = 0;

				dest[n] = pix;
				n++;
			}
		}
	}

	private void buildTables(float xscale, float yscale,
		float lolat, float lolon, float hilat, float hilon,
		float sunlatf)
	{
		int x;

		for (x=0; x<256; x++)
		{
			float lon = lolon + x*(hilon-lolon)/256;
			tmparr[x*2] = (float)Math.sin(lon);
			tmparr[x*2+1] = (float)Math.cos(lon);
		}

		// define right triangle with adjacent = xscale or yscale
		// opposite = slope
		// thus tan(a) = slope/scale
		xscale = 1f/(255*xscale);
		yscale = 1f/(255*yscale);
		for (x=-255; x<=255; x++)
		{
			double atanx = Math.atan(x*xscale);
			double atany = Math.atan(x*yscale);
			sinxslope[x+256] = (float)Math.sin(atanx);
			sinyslope[x+256] = (float)Math.sin(atany + sunlatf);
			cosxslope[x+256] = (float)Math.cos(atanx);
			cosyslope[x+256] = (float)Math.cos(atany + sunlatf);
		}
	}

	private void checkBounds(int targlen, int len, String name)
	throws IllegalArgumentException
	{
		if (targlen > len)
			throw new IllegalArgumentException(name + " is too short (" + len + "<" + targlen + ")");
	}

	private float[] sinxslope = new float[512];
	private float[] cosxslope = new float[512];
	private float[] sinyslope = new float[512];
	private float[] cosyslope = new float[512];

	public void lightSphericalMapIndexed(
		byte[] cimap, int[] palints, byte[] emap, byte[] cldmap, int[] dest,
		int w, int h, float xscale, float yscale,
		float lolat, float lolon, float hilat, float hilon,
		float sunlat)
	{
		int x,y,n,n3;
		int r,r1;
		float lat;
		float coslat,sinlat;
		int cld=0,pix;

		xscale *= (1f/255);
		yscale *= (1f/255);

		int bmaplen = w*h;
		checkBounds(bmaplen, cimap.length, "cimap");
		checkBounds(bmaplen, emap.length, "emap");
		checkBounds(bmaplen, dest.length, "dest");
		if (cldmap != null)
			checkBounds(bmaplen, cldmap.length, "cldmap");

		// build some tables...

		float cossunlat = (float)Math.cos(sunlat);
		float sinsunlat = (float)Math.sin(sunlat);
		float sunlatf = sunlat;

		buildTables(xscale, yscale, lolat, lolon, hilat, hilon, sunlatf);

		// iterate over all pixs

		for (y=0; y<h; y++)
		{
			n = y*w;
			n3 = (y>0) ? n-w : n;

			// get the 1st pixel of this row
			if (Math.abs(hilon-lolon) > Math.PI*1.99)
				r1 = emap[n+w-1]&0xff;
			else
				r1 = emap[n]&0xff;
			lat = lolat + y*(hilat-lolat)/h;
			coslat = (float)Math.cos(lat);
			sinlat = (float)Math.sin(lat);

			for (x=0; x<w; x++)
			{
				float cosnml;
				pix=0;

				float coslon = cosX(x);
				float sinlon = sinX(x);

				float sundot = coslat*cossunlat*coslon + sinlat*sinsunlat;

				if (sundot > 0) // todo: make limit depend on elevation?
				{
     				r = emap[n]&0xff;
     				if (cldmap != null)
     				{
   					cld = cldmap[n]&0xff;
   					if (cld != 0)
   						r += ((255-r)*cld) >> 9;
   				}

     				// light normal depends on slope
     				int xislope = (r-r1)+256;
//     				int yislope = (r-(emap[n3+x]&0xff))+256;
     				r1 = r;

   				// todo: get rid of sin,cos?
   			/*
     				cosnml = coslat*cosyslope[yislope]*(
     						coslon*cosxslope[xislope] + sinlon*sinxslope[xislope]
     					) + sinlat*sinyslope[yislope];
     			*/
     				cosnml = coslat*(
     						coslon*cosxslope[xislope] + sinlon*sinxslope[xislope]
     					);

   				if (cosnml > 0)
   				{
   					pix = palints[cimap[n]&0xff];
   					if (cld != 0)
   						pix = rgbmul(pix^0xffffff, 255-cld)^0xffffff;
   					pix = rgbmul( pix, (int)(EXPOSURE*cosnml) );
   					pix = (pix & 0xffffff) | ((cld)<<24);
   				}
   			}

				dest[n] = pix;
				n++;
			}
		}
	}

	public void lightSphericalMapBumpOnly(
		byte[] emap, int[] palints, int[] dest,
		int w, int h, float xscale, float yscale,
		float lolat, float lolon, float hilat, float hilon,
		float sunlat)
	{
		int x,y,n,n3;
		int r,r1;
		float lat;
		float coslat,sinlat;
		int pix;

		xscale *= (1f/255);
		yscale *= (1f/255);

		int bmaplen = w*h;
		checkBounds(bmaplen, emap.length, "emap");
		checkBounds(bmaplen, dest.length, "dest");

		// build some tables...

		float cossunlat = (float)Math.cos(sunlat);
		float sinsunlat = (float)Math.sin(sunlat);
		float sunlatf = sunlat;

		buildTables(xscale, yscale, lolat, lolon, hilat, hilon, sunlatf);

		// iterate over all pixs

		int pallen = palints.length;
		float heightmod = HEIGHT_MOD/255; // todo?

		for (y=0; y<h; y++)
		{
			n = y*w;
			n3 = (y>0) ? n-w : n;

			// get the 1st pixel of this row
			if (Math.abs(hilon-lolon) > Math.PI*1.99)
				r1 = emap[n+w-1]&0xff;
			else
				r1 = emap[n]&0xff;
			lat = lolat + y*(hilat-lolat)/h;
			coslat = (float)Math.cos(lat);
			sinlat = (float)Math.sin(lat);

			for (x=0; x<w; x++)
			{
				float cosnml;

				pix = 0;
  				r = emap[n]&0xff;

				float coslon = cosX(x) - r*heightmod;
				float sinlon = sinX(x) + r*heightmod;

				float sundot = coslat*cossunlat*coslon + sinlat*sinsunlat;
				if (sundot > 0)
				{
     				// light normal depends on slope
     				int xislope = (r-r1)+256;
     				int yislope = (r-(emap[n3+x]&0xff))+256;
     				r1 = r;

   				// todo: get rid of sin,cos?
     				cosnml = coslat*cosyslope[yislope]*(
     						coslon*cosxslope[xislope] + sinlon*sinxslope[xislope]
     					) + sinlat*sinyslope[yislope];
					if (cosnml < 0)
						cosnml = sundot;
     			} else {
     				cosnml = sundot;
     			}

				int idx = Math.max(0, Math.min(pallen-1, (int)((cosnml+1)*(pallen/2))));
				pix = palints[idx];
				dest[n] = pix;
				n++;
			}
		}
	}


	private final static int mul(int x, int s)
	{
		x = (((x&0xff)*s)+(s>>1));
		if (x<0) return 0;
		if (x>0xffff) return 0xff;
		return (x>>8);
	}

	private final static int rgbmul(int x, int s)
	{
		int s2=s>>1;
		return
			(mul((x)&0xff, s) ) +
			(mul((x>>8)&0xff, s)<<8 ) +
			(mul((x>>16)&0xff, s)<<16 );
	}

	//

	public static void main(String[] args)
	{
		BumpMapper bmap = new BumpMapper();

		byte[] cimap = new byte[0x10000];
		byte[] emap = new byte[0x10000];
		byte[] cldmap = new byte[0x10000];
		int[] palints = new int[256];
		int[] dest = new int[0x10000];

		for (int i=0; i<3; i++)
		{
			long t1 = System.currentTimeMillis();
			bmap.lightSphericalMapIndexed(
				cimap, palints, emap, cldmap, dest,
				256, 256, 0.1f, 0.1f,
				0, (float)Math.PI/4, 0, (float)Math.PI/4,
				(float)Math.PI/8);
			long t2 = System.currentTimeMillis();
			System.out.println("Iteration " + i + ": " + (t2-t1) + " msec");
		}
	}
	public void makeNormalMap(TexQuad srcquad, IntBuffer destbuf,
			int imgw, int imgh,
			float radius,
			float lolat, float lolon, float hilat, float hilon)
	{
		// compute range of elevations
		float range = srcquad.maxvalue - srcquad.minvalue;
		// TODO: account for border
		// TODO: scale?
		float xscale,yscale;
		// compute 1/distance between texels at this level
		// distance = circumference/(texelwidth*range)
		xscale = (radius * 2 * 3.141592f) / ((2 << srcquad.level) * range);
		yscale = (float)(xscale * Math.cos((lolat+hilat)/2));
		//System.out.println(xscale + " " + yscale + " " + range);
		buildTables(xscale, yscale, lolat, lolon, hilat, hilon, 0);
		byte[] arr = srcquad.getByteData();
		for (int y=0; y<imgh; y++)
		{
			float lat = lolat + y*(hilat-lolat)/imgh;
			float sinlat = (float)Math.sin(lat);
			float coslat = (float)Math.cos(lat);
			int yy = Math.max(1, Math.min(imgh-2, y));
			int i = (yy*imgw)+1;
			for (int x=1; x<imgw-1; x++)
			{
				int n1 = (arr[i-imgw-1] & 0xff);
				int n2 = (arr[i-imgw] & 0xff);
				int n3 = (arr[i-imgw+1] & 0xff);
				int n4 = (arr[i-1] & 0xff);
				int n6 = (arr[i+1] & 0xff);
				int n7 = (arr[i+imgw-1] & 0xff);
				int n8 = (arr[i+imgw] & 0xff);
				int n9 = (arr[i+imgw+1] & 0xff);
				// 7/5 is close
				int xslope = ((n6-n4)*10 + (n3+n9-n1-n7)*7);
				int yslope = ((n2-n8)*10 + (n1+n3-n7-n9)*7);
				xslope /= 17;
				yslope /= 17;
				float coslon = cosX(x);
				float sinlon = sinX(x);
				Vector3f nml = new Vector3f(
						coslon * coslat,
						sinlon * coslat,
						sinlat
				);
				// TODO: do the Y vector as well
				/*
				nml.set(
						nml.x,
						nml.y * cosyslope[yslope+256] - nml.z * sinyslope[yslope+256],
						-nml.y * sinyslope[yslope+256] + nml.z * cosyslope[yslope+256]
				);
				*/
				nml.set(
						nml.x * cosxslope[xslope+256] - nml.y * sinxslope[xslope+256],
						-nml.x * sinxslope[xslope+256] + nml.y * cosxslope[xslope+256],
						nml.z
				);
				//Vector3f nml = new Vector3f(xslope, yslope, 256*17/2);
				//nml.normalize();
				//System.out.println(x + " " + y + " " + nml);
				int r = 128 + Math.round(nml.x*127);
				int g = 128 + Math.round(nml.y*127);
				int b = 128 + Math.round(nml.z*127);
				int rgb = r + (g<<8) + (b<<16);
				destbuf.put(rgb);
				if (x==1 || x==imgw-2)
					destbuf.put(rgb);
				i++;
			}
		}
	}

}
