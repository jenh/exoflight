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
  * Describes a physical object in the universe that has a
  * position (center), velocity, radius, mass, and trajectory.
  */
public interface UniverseThing
{
	public Trajectory getTrajectory();
	public void setTrajectory(Trajectory traj);

	public UniverseThing getParent(); // the thing we're orbiting around, or null

	public void addChild(UniverseThing child);
	public void removeChild(UniverseThing child);
	public Iterator getChildren();

	public Vector3d getPosition(UniverseThing ref, long time); // in km

	public Vector3d getVelocity(UniverseThing ref, long time); // in km/s

	public Orientation getOrientation(long time);

	public double getRadius(); // in km

	public float getVisibleRadius(); // in km

	public double getMass(); // in kg

	public double getMass(long time); // in kg

	public double getInfluenceRadius(long time); // in km

	public String getName();

	public Universe getUniverse();

	public Telemetry getTelemetry();

	// moved from SpaceShip or StructureThing

	/**
	  * Get the drag perturbation, or null if there is none
	  */
	public Perturbation getDragPerturbation(Trajectory traj);

}
