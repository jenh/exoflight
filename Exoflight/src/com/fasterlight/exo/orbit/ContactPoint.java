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

import java.util.*;

import com.fasterlight.util.Util;
import com.fasterlight.vecmath.Vector3f;

/**
  * Describes the point on a thing where it may contact
  * the ground (landing gear, skids, wingtips, etc).
  * Has various attributes for elasticity and compression, etc.
  */
public class ContactPoint
{
	/**
	  * Offset in structure-CM coordinates of contact point,
	  * fully extended
	  * units in km
	  */
	public Vector3f extpos;
	/**
	  * Offset in structure-CM coordinates of compression direction
	  * Default is (0,1,0) -- straight up
	  * units in km
	  */
	public Vector3f compdir;

	public float Kspring; // spring force constant
	public float Kdamping; // spring damping constant
	public float maxCompress; // max spring compress (km)
	public float Krolling; // rolling friction constant (friction in Z dir)
	public float Kstatic; // static friction constant (friction in X dir)

	//

	public ContactPoint()
	{
		maxCompress = 0.001f;
	}
	public ContactPoint(ContactPoint cp)
	{
		extpos = new Vector3f(cp.extpos);
		compdir = new Vector3f(cp.compdir);
		Kspring = cp.Kspring;
		Kdamping = cp.Kdamping;
		Kstatic = cp.Kstatic;
		Krolling = cp.Krolling;
		maxCompress = cp.maxCompress;
	}
	public ContactPoint(String spec)
	{
		this();
		StringTokenizer st = new StringTokenizer(spec, ";");
		extpos = new Vector3f(AstroUtil.parseVector(st.nextToken().trim()));
		extpos.scale(1f / 1000);
		try
		{
			String s;
			s = st.nextToken().trim();
			if (s.length() > 0)
			{
				compdir = new Vector3f(AstroUtil.parseVector(s));
				compdir.scale(1f / 1000);
			}
			else
				compdir = new Vector3f(0, 1, 0);
			Kspring = Util.parseFloat(st.nextToken()) * 1000;
			Kdamping = Util.parseFloat(st.nextToken()) * 1000;
			maxCompress = Util.parseFloat(st.nextToken()) / 1000;
			Krolling = Kstatic = Util.parseFloat(st.nextToken());
			if (st.hasMoreTokens())
				Kstatic = Util.parseFloat(st.nextToken());
			//System.out.println(this);
		}
		catch (NoSuchElementException nsee)
		{
			System.out.println(nsee + ": " + spec);
		}
	}

	public String toString()
	{
		return "[ContactPoint:"
			+ extpos
			+ ";"
			+ compdir
			+ ";"
			+ Kspring
			+ ";"
			+ Kdamping
			+ ";"
			+ maxCompress
			+ ";"
			+ Krolling
			+ ";"
			+ Kstatic
			+ "]";
	}
}
