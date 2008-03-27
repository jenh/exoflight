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

import java.util.*;

import com.fasterlight.exo.orbit.StateVector;

/***
Uses multiple Ephemeri to make a big ephemeris.
***/

public class CompositeEphemeris
implements Ephemeris
{
	List ephemeri = new ArrayList();

	double start_time, end_time;
	int bodies_supported = -1;
	int last_eph_index;

	//

	public CompositeEphemeris()
	{
		start_time = Double.MAX_VALUE;
		end_time = Double.MIN_VALUE;
	}

	public void addEphemeris(Ephemeris ephem)
	{
		ephemeri.add(ephem);

		bodies_supported &= ephem.getBodiesSupported();
		start_time = Math.min(start_time, ephem.getStartTime());
		end_time = Math.max(end_time, ephem.getEndTime());

		Collections.sort(ephemeri);
	}

	/**
	  * Gets the minimum Julian time for this ephemeris.
	  * All dates passed in must be >= this value.
	  */
	public double getStartTime()
	{
		return start_time;
	}

	/**
	  * Gets the maximum Julian time for this ephemeris.
	  * All dates passed in must be < this value.
	  */
	public double getEndTime()
	{
		return end_time;
	}

	// which indexes are supported?
	public int getBodiesSupported()
	{
		return bodies_supported;
	}

	public void getBodyStateVector(StateVector sv, int bodyIndex, double julianTime)
	{
		int l = ephemeri.size();
		int li = last_eph_index;
		for (int i=li; i<li+l; i++)
		{
			int ii = i%l;
			Ephemeris ephem = (Ephemeris)ephemeri.get(ii);
			if (julianTime >= ephem.getStartTime() && julianTime < ephem.getEndTime()
				&& (ephem.getBodiesSupported() & (1<<bodyIndex)) != 0 )
			{
				ephem.getBodyStateVector(sv, bodyIndex, julianTime);
				last_eph_index = ii;
				return;
			}
		}
		throw new RuntimeException("Could not find state for body " + bodyIndex + ", JED=" + julianTime);
	}

	//

	public String toString()
	{
		return "[CompositeEphemeris, JD:" + start_time + " to " + end_time +
			" " + ephemeri;
	}

}
