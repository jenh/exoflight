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

import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Encapsulates a position and velocity vector.
  */
public class StateVector
implements PropertyAware
{
	public Vec3d r, v;

	//

	public StateVector()
	{
		this.r = new Vec3d();
		this.v = new Vec3d();
	}

	public StateVector(Vector3d rr, Vector3d vv)
	{
		this.r = new Vec3d(rr);
		this.v = new Vec3d(vv);
	}

	public StateVector(StateVector sv)
	{
		this(sv.r, sv.v);
	}

	public Vec3d getPosition()
	{
		return r;
	}

	public void setPosition(Vec3d pos)
	{
		this.r.set(pos);
	}

	public Vec3d getVelocity()
	{
		return v;
	}

	public void setVelocity(Vec3d vel)
	{
		this.v.set(vel);
	}

	public int hashCode()
	{
		return r.hashCode() ^ v.hashCode();
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof StateVector))
			return false;
		StateVector sv = (StateVector)o;
		return (sv.r.equals(r) && sv.v.equals(v));
	}

	public String toString()
	{
		return "[" + r + ',' + v + ']';
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(StateVector.class);

	static {
		prophelp.registerGetSet("pos", "Position", Vec3d.class);
		prophelp.registerGetSet("vel", "Velocity", Vec3d.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}
}

