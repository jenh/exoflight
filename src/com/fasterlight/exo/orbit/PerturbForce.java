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

import com.fasterlight.vecmath.*;

/**
  * Represents acceleration, force and net torque.
  * This enables calculations that do not take mass into account
  * (for instance, gravity) to have an acceleration vector,
  * and a force vector for everything else.  For rotation, there
  * is only torque.
  */
public class PerturbForce
{
	public Vector3d a; // the acceleration, units
	public Vector3d f; // the force
	public Vector3d T; // the torque

	public PerturbForce()
	{
		a = new Vector3d();
		f = new Vector3d();
		T = new Vector3d();
	}

	public PerturbForce(PerturbForce pf)
	{
		a = new Vector3d(pf.a);
		f = new Vector3d(pf.f);
		T = new Vector3d(pf.T);
	}

	public void clear()
	{
		a.set(0,0,0);
		f.set(0,0,0);
		T.set(0,0,0);
	}
	/**
	  * Add all fields of another PerturbForce instance to this instance.
	  */
	public void add(PerturbForce pf)
	{
		a.add(pf.a);
		f.add(pf.f);
		T.add(pf.T);
	}
	/**
	  * Add a force, offset by the CM -- so there's a torque
	  */
	public void addOffsetForce(Vector3d df, Vector3d pt)
	{
		f.add(df);
		Vector3d v = new Vector3d(); // torque = cross(r,pt)
		v.cross(pt,df);
		T.add(v);
	}
	/**
	  * Add a force, offset by the CM -- so there's a torque
	  */
	public void addOffsetForce(Vector3d df, Vector3d pt, double scale)
	{
		f.scaleAdd(scale, df, f);
		Vector3d v = new Vector3d(); // torque = cross(r,pt)
		v.cross(pt,df);
		T.scaleAdd(scale, v, T);
	}
	/**
	  * Add a force, offset by the CM -- so there's a torque
	  */
	public void addOffsetForce(Vector3f df, Vector3f pt)
	{
		f.x += df.x;
		f.y += df.y;
		f.z += df.z;
		Vector3f v = new Vector3f(); // torque = cross(r,pt)
		v.cross(pt,df);
		T.x += v.x;
		T.y += v.y;
		T.z += v.z;
	}
	/**
	  * Add a force, offset by the CM -- so there's a torque
	  */
	public void addOffsetForce(Vector3f df, Vector3f pt, float scale)
	{
		f.x += df.x*scale;
		f.y += df.y*scale;
		f.z += df.z*scale;
		Vector3f v = new Vector3f(); // torque = cross(r,pt)
		v.cross(pt,df);
		T.x += v.x*scale;
		T.y += v.y*scale;
		T.z += v.z*scale;
	}
	/**
	  * Transform by an orientation
	  */
	public void transform(Orientation ort)
	{
		ort.transform(a);
		ort.transform(f);
		ort.transform(T);
	}
	/**
	  * Transform by an orientation, inverse
	  */
	public void invTransform(Orientation ort)
	{
		ort.invTransform(a);
		ort.invTransform(f);
		ort.invTransform(T);
	}

	public String toString()
	{
		return "(a=" + a + ",f=" + f + ",T=" + T + ")";
	}
}
