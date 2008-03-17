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
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * An OrbitTrajectory describes a body orbiting around another body.
  * todo: wassup with these initializers?
  */
public class OrbitTrajectory extends DefaultMutableTrajectory
{
	Conic orbit;

	public OrbitTrajectory()
	{
	}
	public OrbitTrajectory(UniverseThing ref, Conic o)
	{
		this.set(ref, o);
	}
	public OrbitTrajectory(UniverseThing ref, Conic o, Orientation ort)
	{
		this.set(ref, o, ort);
	}
	public OrbitTrajectory(UniverseThing ref, Vector3d r0, Vector3d v0, long t0)
	{
		this.set(ref, r0, v0, t0, null);
	}
	public OrbitTrajectory(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort)
	{
		this.set(ref, r0, v0, t0, ort);
	}

	public void set(UniverseThing ref, Conic o)
	{
		set(ref, o, null);
	}
	public void set(UniverseThing ref, Conic o, Orientation ort)
	{
		StateVector sv = o.getStateVectorAtEpoch();
		set(ref, sv.r, sv.v, (long) (o.getInitialTime() * TICKS_PER_SEC), ort);
	}
	public void set(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort)
	{
		if (ref == null)
			throw new IllegalArgumentException("ref == null");
		boolean act = isActive();
		if (act)
			thing.setTrajectory(null);
		super.set(ref, r0, v0, t0, ort);
		this.orbit = new Conic(r0, v0, ref.getMass() * GRAV_CONST_KM, t0 * (1d / TICKS_PER_SEC));
		if (act)
			thing.setTrajectory(this);
	}

	public Conic getConic()
	{
		return orbit;
	}

	public void setConic(Conic o)
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		set(ref, o);
	}

	public KeplerianElements getInertialOrbitElements()
	{
		return (orbit != null) ? orbit.getElements() : null;
	}

	public void setInertialOrbitElements(KeplerianElements elem)
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		elem.setMu(ref.getMass() * GRAV_CONST_KM);
		set(ref, elem.getConic());
	}

	// really should be "equitorial", not geocentric
	public KeplerianElements getGeocentricOrbitElements()
	{
		if (orbit == null)
			return null;
		if (ref instanceof Planet)
			return ((Planet) ref).xyz2ijk(orbit).getElements();
		else
			return orbit.getElements();
	}

	public void setGeocentricOrbitElements(KeplerianElements elem)
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		elem.setMu(ref.getMass() * GRAV_CONST_KM);
		if (ref instanceof Planet)
			set(ref, ((Planet) ref).ijk2xyz(elem.getConic()));
		else
			set(ref, elem.getConic());
	}

	public Vector3d getInitialPos()
	{
		return new Vector3d(orbit.getStateVectorAtEpoch().r);
	}
	public Vector3d getInitialVel()
	{
		return new Vector3d(orbit.getStateVectorAtEpoch().v);
	}
	public long getInitialTime()
	{
		return (long) (orbit.getInitialTime() * TICKS_PER_SEC);
	}

	private StateVector solveKepler(long time)
	{
		double thistime = time * (1d / TICKS_PER_SEC);
		StateVector res = orbit.getStateVectorAtTime(thistime);
		return res;
	}

	public Vector3d getPos(long time)
	{
		StateVector res = solveKepler(time);
		return res.r;
	}

	public Vector3d getVel(long time)
	{
		StateVector res = solveKepler(time);
		return res.v;
	}

	public void activate(UniverseThing subject)
	{
		if (orbit == null)
			throw new RuntimeException(this +" has no orbit!");
		super.activate(subject);
	}

	/**
	  * If we have any perturbs, go to Cowell
	  * TODO: check for upwards velocity
	  */
	public boolean checkPerturbs()
	{
		if (isActive() && !isOrbitable(false))
		{
			convertToCowell();
			return true;
		} else
			return false;
	}

	protected void convertToCowell()
	{
		long t = getGame().time();
		CowellTrajectory traj = new CowellTrajectory(ref, getPos(t), getVel(t), t, getOrt(t));
		addUserPerturbations(traj);
		thing.setTrajectory(traj);
	}

	protected void addDefaultPerturbs()
	{
		// none!
	}

	public Conic getClonedConic()
	{
		long t = getGame().time();
		return new Conic(getPos(t), getVel(t), orbit.getMu(), t * (1d / TICKS_PER_SEC));
	}

	public String getType()
	{
		return "orbit";
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try
		{
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException pre)
		{
			super.setProp(key, value);
		}
	}

	static PropertyHelper prophelp = new PropertyHelper(OrbitTrajectory.class);

	static {
		prophelp.registerGetSet("conic", "Conic", Conic.class);
		prophelp.registerGetSet("orbelemgeo", "GeocentricOrbitElements", KeplerianElements.class);
		prophelp.registerGetSet("orbelemintrl", "InertialOrbitElements", KeplerianElements.class);
		prophelp.registerGet("clonedconic", "getClonedConic");
	}
}
