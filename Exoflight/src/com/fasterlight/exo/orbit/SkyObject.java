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

import java.io.*;
import java.util.*;

import com.fasterlight.util.Util;
import com.fasterlight.vecmath.*;

/**
  * Represents a star, used to build the star dome.
  */
public class SkyObject
implements Constants
{
	float j2long, j2lat, mag;
	Vector3f galpos;
	char specclass;

	static Matrix3d jgal = new Matrix3d(
		-0.054875539726,-0.873437108010,-0.483834985808,
       0.494109453312,-0.444829589425, 0.746982251810,
      -0.867666135858,-0.198076386122, 0.455983795705);

	/**
	  * Initialize from line of ADC catalog
	  **/
	public SkyObject(String s, float r)
	{
		float hr,min,sec;

		hr = Integer.parseInt(s.substring(75,77));
		min = Integer.parseInt(s.substring(77,79));
		sec = Util.parseFloat(s.substring(79,83));
		j2long = (float)((Math.PI*2.0/86400)*(hr*60*60+min*60+sec));

		hr = Integer.parseInt(s.substring(84,86));
		if (s.charAt(83)=='-')
			hr = -hr;
		min = Integer.parseInt(s.substring(86,88));
		sec = Util.parseFloat(s.substring(88,90));
		j2lat = (float)Util.toRadians(hr+min/60+sec/3600);

		mag = Util.parseFloat(s.substring(102,107).trim());
		Vector3d galpos2 = new Vector3d(
			(float)(Math.cos(j2long)*Math.cos(j2lat)*r),
			(float)(Math.sin(j2long)*Math.cos(j2lat)*r),
			(float)(Math.sin(j2lat)*r));

//		jgal.transform(galpos2);
		galpos = new Vector3f(galpos2);

		specclass = s.charAt(129);
	}

	public Vector3f getGalPos()
	{
		return galpos;
	}

	public float getMag()
	{
		return mag;
	}

	public char getSpectralClass()
	{
		return specclass;
	}

	public static List readSkyObjects(BufferedReader in)
	throws IOException
	{
		String line;
		List v = new ArrayList();
		while ((line=in.readLine()) != null)
		{
			try {
				SkyObject skyobj = new SkyObject(line, 1);
				v.add(skyobj);
			} catch (Exception e) {
				System.err.println(line + " - " + e);
			}
		}
		return v;
	}
}
