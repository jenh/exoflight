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

import com.fasterlight.util.Point2i;
import com.fasterlight.vecmath.Vector3f;

class TriNode
//implements Comparable
{
	TriNode lc, rc; // left child, right child
	TriNode b, l, r;	// base neighbor, left neighbor, right neighbor
	ROAMVertex p1, p2, p3; // p1 = apex, p2-p3 = base
	float nmlx, nmly, nmlz; // normal of triangle
	float midpl; // displacement of p1 from parent edge ^ 2
	int vtindex; // used to index into variance tree -- if negative, don't
	byte texlevel; // note: leafs do not have a tex level
	byte flags; // see ROAMPlanet for flags
	byte frustflags; // "is in" flags for planes 0-5
	byte firstplane; // first plane to check when iterating (perf improv)

	//

	TriNode()
	{
	}
	void init()
	{
		lc=rc=b=l=r=null;
		flags=0;
		texlevel=-1;
		setNormal();
	}
	void set(TriNode b, TriNode l, TriNode r)
	{
		this.b = b;
		this.l = l;
		this.r = r;
	}
	void set(TriNode l, TriNode r)
	{
		this.l = l;
		this.r = r;
	}
	void split(TriNode lc, TriNode rc)
	{
		this.lc = lc;
		this.rc = rc;
	}
	boolean isSplit()
	{
		return (lc != null) || (rc != null);
	}
	boolean isDiamond()
	{
		// make sure base neighbor's neighbor is tn
		if (b != null && b.b == this)
		{
			// Check both tris actually have children
			if (lc != null && b.lc != null)
			{
				// If both have children in the triangulation, then it is a diamond.
				if (lc.lc == null && rc.lc == null &&
					b.lc.lc == null && b.rc.lc == null)
					return true;
			}
		}
		return false;
	}
	boolean isVisible()
	{
		return (flags & (ROAMPlanet.INVISIBLE | ROAMPlanet.OUTOF_FRUSTUM)) == 0;
	}
	ROAMVertex getVertex(int i)
	{
		switch (i) {
			case 0: return p1;
			case 1: return p2;
			case 2: return p3;
			default : throw new IllegalArgumentException();
		}
	}
	Vector3f getCenterPt()
	{
		Vector3f v = new Vector3f(p1);
		v.add(p2);
		v.add(p3);
		v.scale(1f/3);
		return v;
	}
	float getMaxDim()
	{
		float mx = Math.max(Math.max(Math.abs(p1.x-p2.x), Math.abs(p2.x-p3.x)), Math.abs(p3.x-p1.x));
		float my = Math.max(Math.max(Math.abs(p1.y-p2.y), Math.abs(p2.y-p3.y)), Math.abs(p3.y-p1.y));
		float mz = Math.max(Math.max(Math.abs(p1.z-p2.z), Math.abs(p2.z-p3.z)), Math.abs(p3.z-p1.z));
		return Math.max(mx, Math.max(my, mz));
	}
	Point2i getTexCenter()
	{
		return new Point2i(
			(p1.tx+p2.tx+p3.tx)/3,
			(p1.ty+p2.ty+p3.ty)/3);
	}
	private void setNormal()
	{
		float v1x = p2.x-p1.x;
		float v1y = p2.y-p1.y;
		float v1z = p2.z-p1.z;
		float v2x = p1.x-p3.x;
		float v2y = p1.y-p3.y;
		float v2z = p1.z-p3.z;
		nmlx = v1y*v2z - v1z*v2y;
		nmly = v1z*v2x - v1x*v2z;
		nmlz = v1x*v2y - v1y*v2x;
	}
	float getArea()
	{
		Vector3f a = new Vector3f(p3);
		a.sub(p2);
		float base = a.length();
		a.scale(0.5f);
		a.add(p2);
		a.sub(p1);
		float apex = a.length();
		return base*apex*0.5f;
	}
	public String toString()
	{
		return "[TriNode " + vtindex + " 0x" +
			Integer.toString(flags&0xff,16) + " " +
			texlevel + " " +
			(isSplit() ? "S" : "") + (isDiamond() ? "D" : "") +
			"]";
	}
/*
	public int compareTo(Object o)
	{
		float oprio = ((TriNode)o).prio;
		if (oprio > prio) return 1;
		if (oprio < prio) return -1;
		// todo: what if overflow?
		return vtindex - ((TriNode)o).vtindex;
	}
*/
/*
	public boolean equals(Object o)
	{
		return (o instanceof TriNode) && ((TriNode)o).vtindex == vtindex;
	}
	// this is for renderSceneryObjects
	public int hashCode()
	{
		return nml.hashCode();
	}
*/
}

