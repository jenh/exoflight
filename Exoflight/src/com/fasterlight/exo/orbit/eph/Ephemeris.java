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
package com.fasterlight.exo.orbit.eph;

import com.fasterlight.exo.orbit.StateVector;

/**
  * Interface for the ephemeris classes that allows
  * retrieval of state vectors for the planets and moon.
  */
public interface Ephemeris
{
	/**
	  * Gets the minimum Julian time for this ephemeris.
	  * All dates passed in must be >= this value.
	  */
	public double getStartTime();

	/**
	  * Gets the maximum Julian time for this ephemeris.
	  * All dates passed in must be < this value.
	  */
	public double getEndTime();

	/**
	  * Returns a bit mask.
	  */
	public int getBodiesSupported();

	/**
	  * Gets the state vector for a given body at a Julian date.
	  * Body index must be part of the flag set returned by
	  * getBodiesSupported().
	  */
	public void getBodyStateVector(StateVector sv, int bodyIndex,
		double julianTime);

}
