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
  * Perturbation for 3rd body perturbations (besides the primary body).
  * Vallado, pg. 515
  */
public class ThirdBodyPerturbation
implements Perturbation
{
	UniverseThing ref, third;
	double U;
	double third_infrad2;
	boolean adjustForParent;
	boolean inside_infrad;

	//

	/**
	  * @param ref - the body that the perturbed object is orbiting.
	  * @param third - the body that is influencing the perturbed object.
	  */
	public ThirdBodyPerturbation(UniverseThing ref, UniverseThing third)
	{
		this.ref = ref;
		this.third = third;
		this.U = third.getMass()*Constants.GRAV_CONST_KM;
		this.third_infrad2 = AstroUtil.sqr(third.getInfluenceRadius(ref.getUniverse().getGame().time()));
	}

	public ThirdBodyPerturbation(UniverseThing ref, UniverseThing third,
		boolean adjustForParent)
	{
		this(ref, third);
		this.adjustForParent = adjustForParent;
	}

	public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
		Orientation ort, Vector3d w, long time)
	{
		double r2;

		// compute vector from third body to ref center (e-m)
		Vector3d ref_to_third = third.getPosition(ref, time);

		// vector from third body to 'r'	(m-b)
		Vector3d acc = new Vector3d(ref_to_third);
		acc.sub(r);
		r2 = acc.lengthSquared();
		inside_infrad = (r2 < third_infrad2);
      acc.scale(U/(r2*Math.sqrt(r2)));
      force.a.add(acc); // -U
//System.out.println("  thing -> " + third + ": " + r2 + ", " + force.a);

      // do the same for ref to third body (e-m)
      if (adjustForParent)
      {
			acc.set(ref_to_third);
			r2 = acc.lengthSquared();
      	acc.scale(-U/(r2*Math.sqrt(r2)));
	      force.a.add(acc); // U
	   }
//System.out.println("  " + third + " -> " + ref + ": " + r2 + ", " + force.a);
	}

	public boolean isInsideInfluence()
	{
		return inside_infrad;
	}

	public UniverseThing getReferenceBody()
	{
		return ref;
	}

	public UniverseThing getThirdBody()
	{
		return third;
	}
}
