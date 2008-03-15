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

import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Simple Perturbation object used mainly for thrust
  */
public class SimplePerturbation
implements Perturbation
{
	Vector3d f = new Vec3d();

	public SimplePerturbation()
	{
	}
	public void setForce(Vector3d f)
	{
		this.f.set(f);
	}
	public Vector3d getForce()
	{
		return f;
	}
	public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
		Orientation ort, Vector3d w, long time)
	{
		force.f.add(f);
	}
}
