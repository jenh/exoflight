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

import com.fasterlight.vecmath.Vector3d;

/**
  * An interface to an object which describes the trajectory
  * of an object relative to another object.
  */
public interface Trajectory
{
	/**
	  * Get the reference object (parent)
	  */
	public UniverseThing getParent();
	/**
	  * Activate the trajectory for use with a UniverseThing at the
	  * current game time.
	  */
	public void activate(UniverseThing subject);
	/**
	  * End use of this trajectory with a UniverseThing.
	  * Must be called after activate().
	  */
	public void deactivate();
	/**
	  * Returns the UniverseThing that this Trajectory is activated
	  * upon (passed in the activate method), or null if it is not activated.
	  */
	public UniverseThing getThing();
	/**
	  * Get the position of the object at a given time.
	  * May not be valid for all values of 'time', depends on the
	  * individual implementation.
	  */
	public Vector3d getPos(long time);
	/**
	  * Get the velocity of the object at a given time.
	  */
	public Vector3d getVel(long time);
	/**
	  * Get the orientation of the object at a given time.
	  */
	public Orientation getOrt(long time);
	/**
	  * Gets the instantaneous angular velocity.
	  */
	public Vector3d getAngularVelocity();
}
