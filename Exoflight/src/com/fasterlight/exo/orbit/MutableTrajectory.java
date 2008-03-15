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

import java.util.Iterator;

import com.fasterlight.vecmath.Vector3d;

/**
  * A Trajectory that can be "perturbed", modified,
  * and potentially be replaced with other classes of MutableTrajectory
  * when neccessary.
  *
  * For instance, a LandedTrajectory is mutable because if a thrust
  * is applied, it is replaced by Cowell, and back to Landed again when
  * it lands on the surface and comes to a stop.
  */
public interface MutableTrajectory
extends Trajectory
{
	/**
	  * Set the initial conditions for this trajectory.
	  */
	public void set(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort);
	/**
	  * Adds a perturbation. This may cause the trajectory to
	  * be changed if it is activated.
	  */
	public void addPerturbation(Perturbation p);
	/**
	  * Removes a perturbation. This may cause the trajectory to
	  * be changed if it is activated.
	  */
	public void removePerturbation(Perturbation p);
	/**
	  * Iterate over all perturbations.
	  */
	public Iterator getPerturbations();
	/**
	  * Sets the angular velocity.
	  */
	public void setAngularVelocity(Vector3d angvel);
	/**
	  * If a Perturbation has changed that would affect
	  * the trajectory prior to the next update, call this
	  * method to update the state of the trajectory
	  */
	public void refresh();
	/**
	  * If we have to create a new trajectory, we need to copy the
	  * user perturbations (those not automagically added) to the new trajectory.
	  */
	public void addUserPerturbations(MutableTrajectory traj);
}
