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

import com.fasterlight.exo.orbit.traj.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Various utilities for dealing with UniverseThings
  * and their like.
  */
public class UniverseUtil implements Constants
{

	public static Vector3d getLatLong(UniverseThing ut, long t)
	{
		Trajectory traj = ut.getTrajectory();
		if (traj == null)
			return null;
		if (traj instanceof LandedTrajectory)
		{
			return ((LandedTrajectory) traj).getLandPos();
		}
		else if (traj.getParent() instanceof Planet)
		{
			Planet p = (Planet) traj.getParent();
			Vector3d pos = traj.getPos(t);
			p.xyz2ijk(pos);
			p.ijk2llr(pos, t * (1d / TICKS_PER_SEC));
			return pos;
		}
		else
			return null;
	}

	/**
	  * Returns an orbit for a thing with either
	  * Cowell or Orbit trajectories.
	  * Uses galactic coordinate systme.
	  */
	public static Conic getConicFor(UniverseThing ut)
	{
		Trajectory traj = ut.getTrajectory();
		if (traj == null)
			return null;
		Conic o = null;
		long t = ut.getUniverse().getGame().time();
		// todo: too many!
		if (traj instanceof OrbitTrajectory)
			o = ((OrbitTrajectory) traj).getConic();
		else if (traj instanceof PolyElementsTrajectory)
			o = ((PolyElementsTrajectory) traj).getConic();
		else if (traj instanceof EphemerisTrajectory)
			o = ((EphemerisTrajectory) traj).getConic();
		else if (traj instanceof CowellTrajectory)
			o =
				new Conic(
					traj.getPos(t),
					traj.getVel(t),
					traj.getParent().getMass() * Constants.GRAV_CONST_KM,
					t * (1d / TICKS_PER_SEC));
		return o;
	}

	/**
	  * Returns an orbit for a thing with either
	  * Cowell or Orbit trajectories.
	  * Transforms to planet's coordinate systme.
	  */
	public static Conic getGeocentricConicFor(UniverseThing ut)
	{
		Conic o = getConicFor(ut);
		if (o != null && ut.getParent() instanceof Planet)
			o = ((Planet) ut.getParent()).xyz2ijk(o);
		return o;
	}

	public static double getMinRadius(UniverseThing ut)
	{
		if (ut instanceof Planet)
		{
			ElevationModel em = ((Planet) ut).getElevationModel();
			if (em != null)
			{
				return ut.getRadius() + em.getMinDisplacement();
			}
		}
		return ut.getRadius();
	}

	/**
	  * See if two objects (ut1 & ut2) can see each other without being
	  * obstructed by a third object (ut3)
	  * ut1 or ut2 can be null (representing universe origin), but not ut3.
	  * Source: Vallado pg 198-201
	  */
	public static boolean hasLOS(
		UniverseThing ut1,
		UniverseThing ut2,
		UniverseThing ut3,
		long time)
	{
		if (ut1 == ut2 || ut1 == ut3 || ut2 == ut3)
			return true;
		Vector3d r1 = ut3.getPosition(ut1, time);
		Vector3d r2 = ut3.getPosition(ut2, time);
		double rad = getMinRadius(ut3);
		// normalize r1 and r2
		r1.scale(1 / rad);
		r2.scale(1 / rad);
		double r1l2 = r1.lengthSquared();
		double r2l2 = r2.lengthSquared();
		double r1dr2 = r1.dot(r2);
		double tmin = (r1l2 - r1dr2) / (r1l2 + r2l2 - 2 * r1dr2);
		if (tmin < 0 || tmin > 1)
			return true;
		else
		{
			double cl2 = (1 - tmin) * r1l2 + r1dr2 * tmin;
			return (cl2 >= 1.0);
		}
	}

	/**
	  * See if two objects (ut1 & ut2) can see each other without being
	  * obstructed by a third object (ut3)
	  * Returns the ratio of ut2 that ut1 can see (0.0-1.0)
	  * ut2 must have a >0 radius.
	  * ut1, ut2, ut3 must all be nonnull.
	  * todo: doesn't work properly when occluding planet is far away
	  * Source: Vallado pg 198-201
	  */
	public static double getLOSArea(
		UniverseThing ut1,
		UniverseThing ut2,
		UniverseThing ut3,
		long time)
	{
		if (ut1 == ut2 || ut1 == ut3 || ut2 == ut3)
			return 1;
		Vector3d r1 = ut3.getPosition(ut1, time);
		Vector3d r2 = ut3.getPosition(ut2, time);
		double rad = getMinRadius(ut3);
		// normalize r1 and r2
		r1.scale(1 / rad);
		r2.scale(1 / rad);
		double r1l = r1.length();
		double r2l = r2.length();
		double r1dr2 = r1.dot(r2);
		// get angles
		double a12 = Math.acos(r1dr2 / (r1l * r2l));
		double a1 = Math.acos(1 / r1l);
		double a2 = Math.acos(1 / r2l);
		double sunang = Math.asin(ut2.getRadius() / (r2l * rad));
		double ang = a1 + a2 - a12;
		if (Double.isNaN(ang)) // ut1 & ut2 too close
			return 1;
		if (ang > sunang)
			return 1;
		else if (ang < -sunang)
			return 0;
		else
			return (ang + sunang) / (sunang * 2);
	}

	/**
	  * See if two objects (ut1 & ut2) can see each other without being
	  * obstructed by a third object (ut3)
	  * ut1 or ut2 can be null (representing universe origin), but not ut3.
	  * Source: Vallado pg 198-201
	  */
	public static boolean hasLOS(Vector3d r1, UniverseThing ut2, UniverseThing ut3, long time)
	{
		if (ut2 == ut3)
			return true;
		r1 = new Vector3d(r1);
		Vector3d r2 = ut3.getPosition(ut2, time);
		double rad = getMinRadius(ut3);
		// normalize r1 and r2
		r1.scale(1 / rad);
		r2.scale(1 / rad);
		double r1l2 = r1.lengthSquared();
		double r2l2 = r2.lengthSquared();
		double r1dr2 = r1.dot(r2);
		double tmin = (r1l2 - r1dr2) / (r1l2 + r2l2 - 2 * r1dr2);
		if (tmin < 0 || tmin > 1)
			return true;
		else
		{
			double cl2 = (1 - tmin) * r1l2 + r1dr2 * tmin;
			return (cl2 >= 1.0);
		}
	}

	/**
	  * See if two objects (r1 & ut2) can see each other without being
	  * obstructed by a third object (ut3)
	  * Returns the ratio of ut2 that ut1 can see (0.0-1.0)
	  * ut2 must have a >0 radius.
	  * ut1, ut2, ut3 must all be nonnull.
	  * todo: doesn't work properly when occluding planet is far away
	  * Source: Vallado pg 198-201
	  */
	public static double getLOSArea(Vector3d r1, UniverseThing ut2, UniverseThing ut3, long time)
	{
		if (ut2 == ut3)
			return 1;
		r1 = new Vector3d(r1);
		Vector3d r2 = ut3.getPosition(ut2, time);
		double rad = getMinRadius(ut3);
		// normalize r1 and r2
		r1.scale(1 / rad);
		r2.scale(1 / rad);
		double r1l = r1.length();
		double r2l = r2.length();
		double r1dr2 = r1.dot(r2);
		// get angles
		double a12 = Math.acos(r1dr2 / (r1l * r2l));
		double a1 = Math.acos(1 / r1l);
		double a2 = Math.acos(1 / r2l);
		double sunang = Math.asin(ut2.getRadius() / (r2l * rad));
		double ang = a1 + a2 - a12;
		if (Double.isNaN(ang)) // ut1 & ut2 too close
			return 1;
		if (ang > sunang)
			return 1;
		else if (ang < -sunang)
			return 0;
		else
			return (ang + sunang) / (sunang * 2);
	}

	/**
	  * Gets the common parent of two objects A and B.
	  * @returns null if there is no common parent
	  */
	public static UniverseThing getCommonParent(UniverseThing a, UniverseThing b)
	{
		if (a == null || b == null)
			return null;
		UniverseThing p = a.getParent();
		while (p != null)
		{
			if (p == b.getParent())
				return p;
			p = p.getParent();
		}
		return getCommonParent(a, b.getParent());
	}

	/**
	  * Returns the first ancestor up the tree that is an instance of Planet.
	  * @returns null if there is no Planet in the ancestor list
	  */
	public static Planet getFirstPlanet(UniverseThing a)
	{
		UniverseThing p = a;
		while (p != null)
		{
			p = p.getParent();
			if (p instanceof Planet)
				return (Planet) p;
		}
		return null;
	}

	public static void setPositionInOrbit(
		UniverseThing thing,
		UniverseThing body,
		KeplerianElements elements,
		long time)
	{
		CowellTrajectory traj = new CowellTrajectory();
		traj.setParent(body);
		elements.setEpoch(AstroUtil.tick2dbl(time));
		traj.setGeocentricOrbitElements(elements);
		Trajectory curtraj = thing.getTrajectory();
		if (curtraj instanceof MutableTrajectory)
			traj.addUserPerturbations((MutableTrajectory) curtraj);
		thing.setTrajectory(traj);
	}

	public static void setPositionOnGround(
		UniverseThing thing,
		UniverseThing body,
		Vector3d llr,
		long time)
	{
		llr = new Vec3d(llr);
		LandedTrajectory traj = new LandedTrajectory(body, llr, time);
		Trajectory curtraj = thing.getTrajectory();
		if (curtraj instanceof MutableTrajectory)
			traj.addUserPerturbations((MutableTrajectory) curtraj);
		thing.setTrajectory(traj);
	}

	public static void setPositionOnGroundWithVel(
		UniverseThing thing,
		Planet planet,
		Vector3d llr,
		long time,
		Vector3d aed)
	{
		setPositionOnGround(thing, planet, llr, time);

		// now add the velocity
		Vector3d vel = planet.aed2sez(aed);
		planet.rotateVecByLL(vel, llr, AstroUtil.tick2dbl(time));
		// free to cowell (we know it's a landed)
		LandedTrajectory traj = (LandedTrajectory) thing.getTrajectory();
		traj.free(traj.getPos(time), vel);
	}

}
