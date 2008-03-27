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
package com.fasterlight.exo.ship;

import com.fasterlight.vecmath.Vector3f;

/**
  * Interface for a capability that is able to control attitude
  * based on external commands.
  */
public interface AttitudeControlComponent
{
	/**
	  * Calulates the maximum moment this capability can impart
     * at full deflection, and sets 'moment'.  This value depends
     * on the current state of any dependent capabilities, and on the
     * position & orientation of the module.
     * Results in structure coords, rad/s^2.
	  */
	public void getMaxMoment(Vector3f moment);
	/**
	  * Same as getMaxMoment but for negative moment.
	  * (todo: what is this for?? nobody implements it)
	  */
	public void getMinMoment(Vector3f moment);
   /**
     * Commands this capability to impart a moment.
     * commanded moment = (min+max)/2 + strength*(max-min)/2.
     * If component has no power or is not armed, does nothing.
     * If strength is (0,0,0) or below min value, turns off component.
     * Each axis value ranges from -1 to 1.
     * 'strength' will be clamped to this range.
     */
	public void setStrength(Vector3f strength);
   /**
     * Is this capability armed -- that is, able to be commanded?
     */
	public boolean isArmed();
}
