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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.io.IOUtil;

/**
  * A ProxyEphemeris performs lazy loading of one or more ephemeri.
  */
public class ProxyEphemeris
implements Ephemeris, Comparable
{
	private double start_time, end_time;
	private int bodymask;
	private String path;
	private Ephemeris delegat;

	//

	public ProxyEphemeris(String path, double start_time, double end_time, int bodymask)
	{
		this.start_time = start_time;
		this.end_time = end_time;
		this.bodymask = bodymask;
		this.path = path;
	}

	public double getStartTime()
	{
		return start_time;
	}

	public double getEndTime()
	{
		return end_time;
	}

	public int getBodiesSupported()
	{
		return bodymask;
	}

	public void getBodyStateVector(StateVector sv, int bodyIndex,
		double julianTime)
	{
		getDelegate().getBodyStateVector(sv, bodyIndex, julianTime);
	}

	public Ephemeris getDelegate()
	{
		if (delegat == null)
		{
			try {
				System.out.println("Reading " + path);
				delegat = (Ephemeris)IOUtil.readSerializedObject(path);
			} catch (Exception ioe) {
				throw new RuntimeException("Error reading ephemeris \"" + path + "\": " + ioe);
			}
		}
		return delegat;
	}

	//

	public String toString()
	{
		return "[ProxyEphemeris, JD:" + start_time + " to " + end_time +
			", flags=" + Integer.toString(getBodiesSupported(),16) + " -> " +
			delegat + "]";
	}

	public int compareTo(Object o)
	{
		Ephemeris eph = (Ephemeris)o;
		return AstroUtil.sign(getStartTime() - eph.getStartTime());
	}
}
