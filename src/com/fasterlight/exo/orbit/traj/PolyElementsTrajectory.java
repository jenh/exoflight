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
import com.fasterlight.game.Game;
import com.fasterlight.math.*;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * An PolyElementsTrajectory is a trajectory which uses variation
  * of orbital elements.  Each element is assigned a 1-d function
  * (usually a polynomial of some kind)
  */
public class PolyElementsTrajectory extends StaticTrajectory implements Trajectory, PropertyAware
{
	double mu;
	// orbital params
	Func1d[] params = new Func1d[6];

	// cached stuff

	KeplerianElements last_ke;
	long last_ke_time = Game.INVALID_TICK;

	Conic last_conic;
	long last_conic_time = Game.INVALID_TICK;

	StateVector last_sv;
	long last_sv_time = Game.INVALID_TICK;

	//

	public PolyElementsTrajectory()
	{
	}

	public void activate(UniverseThing subject)
	{
		mu = ref.getMass() * Constants.GRAV_CONST_KM;
		super.activate(subject);
	}

	public void setParameter(int pi, Func1d func)
	{
		params[pi] = func;
	}

	public void setParameter(int pi, String funcspec)
	{
		params[pi] = CurveParser.parseCurve1d(funcspec);
	}

	/**
	  * @param pi - element parameter (a,e,i...)
	  * @param t - UT1 time, Julian centuries
	  */
	public double getParameter(int pi, double t)
	{
		return params[pi].f(t);
	}

	public KeplerianElements getElements(long time)
	{
		if (last_ke_time != time)
		{
			double dt =
				time * (1d / (Constants.TICKS_PER_SEC * 86400 * Constants.JULIAN_DAYS_PER_CENT));
			// convert arguments to canonical
			// Valldo pg 189
			double RAAN = getParameter(3, dt);
			double trueArg = getParameter(4, dt);
			double argPeri = trueArg - RAAN;
			double meanAnom = getParameter(5, dt) - trueArg;
			last_ke =
				new KeplerianElements(
					getParameter(0, dt),
					getParameter(1, dt),
					getParameter(2, dt),
					RAAN,
					argPeri,
					0,
					mu,
					dt);
			last_ke.setMeanAnomaly(meanAnom);
			last_ke_time = time;
		}
		return last_ke;
	}

	public Conic getConic()
	{
		return getConic(getGame().time());
	}

	public Conic getConic(long time)
	{
		if (time != last_conic_time)
		{
			KeplerianElements ke = getElements(time);
			last_conic = new Conic(ke);
			last_conic_time = time;
		}
		return last_conic;
	}

	public StateVector getStateVector()
	{
		return getStateVector(getGame().time());
	}

	public StateVector getStateVector(long time)
	{
		if (time != last_sv_time)
		{
			KeplerianElements ke = getElements(time);
			last_sv = ke.getStateVectorAtEpoch();
			last_sv_time = time;
		}
		return last_sv;
	}

	public Vector3d getPos(long time)
	{
		StateVector sv = getStateVector(time);
		return new Vector3d(sv.r);
	}

	public Vector3d getVel(long time)
	{
		StateVector sv = getStateVector(time);
		return new Vector3d(sv.v);
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

	// PROPERTIES

	public Object getProp(String key)
	{
		if (key.startsWith("param#"))
		{
			int pi = Integer.parseInt(key.substring(6));
			return params[pi];
		}
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		if (key.startsWith("param#"))
		{
			int pi = Integer.parseInt(key.substring(6));
			setParameter(pi, PropertyUtil.toString(value));
		} else
			prophelp.setProp(this, key, value);
	}

	static PropertyHelper prophelp = new PropertyHelper(PolyElementsTrajectory.class);

	static {
		prophelp.registerGet("conic", "getConic");
		prophelp.registerGet("statevector", "getStateVector");
		prophelp.registerGetSet("parent", "Parent", UniverseThing.class);
	}
}
