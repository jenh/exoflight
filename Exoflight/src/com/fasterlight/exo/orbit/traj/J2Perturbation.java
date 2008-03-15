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
package com.fasterlight.exo.orbit.traj;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * Perturbation for the J2 harmonic.
  * Vallado, pg. 528
  */
public class J2Perturbation
implements Perturbation
{
	Planet ref;
	double mu;
	double J2;
	double refrad2;

	//

	/**
	  * @param ref - the body that the perturbed object is orbiting.
	  * @param third - the body that is influencing the perturbed object.
	  */
	public J2Perturbation(Planet ref, double J2)
	{
		this.ref = ref;
		this.mu = ref.getMass()*Constants.GRAV_CONST_KM;
		this.J2 = J2;
		this.refrad2 = ref.getRadius();
		refrad2 *= refrad2;
	}

	public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
		Orientation ort, Vector3d w, long time)
	{
		// first transform r to ijk
		Vector3d ijk = new Vector3d(r);
		ref.xyz2ijk(ijk);
		double ri = ijk.x;
		double rj = ijk.y;
		double rk = ijk.z;

		double rl2 = r.lengthSquared();
		double rl = Math.sqrt(rl2);
		double rln5 = rl2*rl2*rl; // r^5
		double term1 = -1.5d*J2*mu*refrad2/rln5;
		double term2 = 5*rk*rk/rl2;

		double ai = term1*(1-term2)*ri;
		double aj = term1*(1-term2)*rj;
		double ak = term1*(3-term2)*rk;
		ijk.set(ai,aj,ak);

		// now back to xyz
		ref.ijk2xyz(ijk);
		force.a.add(ijk);
	}

	public UniverseThing getReferenceBody()
	{
		return ref;
	}

	public double getJ2()
	{
		return J2;
	}
}
