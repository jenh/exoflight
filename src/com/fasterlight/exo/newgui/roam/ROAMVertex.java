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
import com.fasterlight.vecmath.*;

/**
  * A vertex which also contains texture coords
  */
public class ROAMVertex
extends Vector3f
{
	public int tx,ty;
//	public float nmlx,nmly,nmlz; // normal

	public ROAMVertex(Tuple3f p, Point2i t)
	{
		set(p);
		set(t);
	}

	public ROAMVertex(float px, float py, float pz, int tx, int ty)
	{
		set(px, py, pz);
		set(tx, ty);
	}

	public void set(int tx, int ty)
	{
		this.tx = tx;
		this.ty = ty;
	}

	public void set(Point2i t)
	{
		tx = t.x;
		ty = t.y;
	}

	public boolean equals(Object o)
	{
		ROAMVertex vtx = (ROAMVertex)o;
		return (vtx.tx==tx && vtx.ty==ty);
	}

	public int hashCode()
	{
		return tx ^ (ty*7);
	}

	public String toString()
	{
		return super.toString() + '[' + tx + ',' + ty + ']';
	}
}
