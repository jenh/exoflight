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

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.eph.Ephemeris;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * An EphemerisTrajectory is a trajectory which uses the DE405
  * (or other) JPL ephemeris.
  * @see com.fasterlight.exo.orbit.eph.Ephemeris
  */
public class EphemerisTrajectory
extends StaticTrajectory
implements Trajectory, PropertyAware
{
	double mu;
	int bodyIndex;

	Conic last_conic;
	long last_conic_time = Game.INVALID_TICK;

	StateVector last_sv = new StateVector();
	long last_sv_time = Game.INVALID_TICK;

	// for Luna, it's in IJK coordinates (geocentric)
	// so we gotta convert to XYZ (ecliptic, inertial)
	Planet refbody;

	//

	public EphemerisTrajectory()
	{
		getDefaultEphemeris();
	}

	public int getBodyIndex()
	{
		return bodyIndex;
	}

	public void setBodyIndex(int bodyIndex)
	{
		this.bodyIndex = bodyIndex;
	}

	public void activate(UniverseThing subject)
	{
		mu = ref.getMass()*Constants.GRAV_CONST_KM;
		super.activate(subject);
		if (false && bodyIndex == 10) // Luna
			refbody = (Planet)((SpaceGame)getGame()).getBody("Earth");
	}

	public Conic getConic()
	{
		return getConic(getGame().time());
	}

	public Conic getConic(long time)
	{
		if (time != last_conic_time)
		{
			StateVector sv = getStateVector_unsafe(time);
			last_conic = new Conic(sv, mu, AstroUtil.tick2dbl(time));
			last_conic_time = time;
		}
		return last_conic;
	}

	public StateVector getStateVector_unsafe()
	{
		return getStateVector(getGame().time());
	}

	public StateVector getStateVector()
	{
		return new StateVector(getStateVector_unsafe());
	}

	public StateVector getStateVector_unsafe(long time)
	{
		if (time != last_sv_time)
		{
			double julianTime = AstroUtil.tick2julian(time);
			StateVector sv = last_sv;
			defaultEphemeris.getBodyStateVector(sv, bodyIndex, julianTime);
			// km/day to km/s
			sv.v.scale(1d/86400);
			last_sv_time = time;
		}
		return last_sv;
	}

	public StateVector getStateVector(long time)
	{
		return new StateVector(getStateVector_unsafe(time));
	}

	public Vector3d getPos(long time)
	{
		StateVector sv = getStateVector_unsafe(time);
		return new Vec3d(sv.r);
	}

	public Vector3d getVel(long time)
	{
		StateVector sv = getStateVector_unsafe(time);
		return new Vec3d(sv.v);
	}

	public String getType()
	{
		return "polyelements";
	}

	public void setParent(UniverseThing parent)
	{
//		assertActive(false);
		this.ref = parent;
	}

	//

	private static Ephemeris defaultEphemeris;

	/**
	  * A singleton Ephemeris.
	  */
	public static Ephemeris getDefaultEphemeris()
	{
		if (defaultEphemeris == null)
		{
			defaultEphemeris = makeDefaultEphemeris();
		}
		return defaultEphemeris;
	}

	public static final String DEFAULT_EPHEMERIS_CLASSNAME =
		"com.fasterlight.exo.orbit.eph.Ephemeris1960to2020";

	private static Ephemeris makeDefaultEphemeris()
	{
		String clazzname = Settings.getString("Ephemeris", "EphemerisClass",
			DEFAULT_EPHEMERIS_CLASSNAME);
		try {
			return (Ephemeris)Class.forName(clazzname).newInstance();
		} catch (Exception exc) {
			throw new RuntimeException("Could not find ephemeris class: " + clazzname);
		}
	}


	// PROPERTIES

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	static PropertyHelper prophelp = new PropertyHelper(EphemerisTrajectory.class);

	static {
		prophelp.registerGet("conic", "getConic");
		prophelp.registerGet("statevector", "getStateVector");
		prophelp.registerGetSet("parent", "Parent", UniverseThing.class);
		prophelp.registerGetSet("bodyindex", "BodyIndex", int.class);
	}
}
