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
package com.fasterlight.exo.ship.sys;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.nav.PlanetIntegrator3d;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * Performs reentry support for a ship,
  * calculating landing position, time, max-G, etc.
  * todo: when data not valid, return NaN
  */
public class ShipReentrySystem
extends ShipSystem
implements PropertyAware
{
	double entry_ang; // in radians

	double integ_ang; // in radians
	double integ_maxg;
	long integ_time;
	Vec3d integ_landpos = new Vec3d();
	Vec3d integ_landllr = new Vec3d();

	long last_integ_time = Constants.INVALID_TICK;
	long min_upd_time = Constants.TICKS_PER_SEC;

	double int_alt = 100; // km

	//

	public ShipReentrySystem(SpaceShip ship)
	{
		super(ship);
	}

	public long getBurnTime()
	{
		// todo
		return getGame().time();
	}

	protected Sequencer loadSequencer()
	{
		Sequencer seq = ship.loadProgram("reentry");
		long t0 = getBurnTime();
		if (t0 == INVALID_TICK)
			throw new PropertyRejectedException("Could not compute burn time");
		if (t0 < getGame().time())
			throw new PropertyRejectedException("Burn time is in the past");

		seq.setZeroTime(t0);
		return seq;
	}

/*
	public Vector3d getPredictedEntryPoint()
	{
		double intalt = getInterfaceAltitude();
		Vector3d r0 = ship.getParent().getRadius() + intalt;
		Vector3d v0 =
	}
*/

	/**
	  * Returns time at which orbit intersects the atmosphere
	  * sufficiently to cause drag above 0.05 G
	  */
	public double getInterfaceAltitude()
	{
		return int_alt;
	}

	public void setInterfaceAltitude(double intalt)
	{
		this.int_alt = intalt;
	}

	public long getInterfaceTime()
	{
		double intalt = getInterfaceAltitude();
		if (!Double.isNaN(intalt))
		{
			Conic orbit = UniverseUtil.getConicFor(ship);
			if (orbit != null)
			{
				if (ship.getParent() instanceof Planet)
				{
					long tnow = getGame().time();
					// if we are below the interface alt, interface time == now
					if (getShipTelemetry().getALT() <= intalt)
						return tnow;

					double pr = ship.getParent().getRadius();
					long t = AstroUtil.getNearestTime(orbit, intalt + pr, tnow);
					return t;
				}
			}
		}
		return Constants.INVALID_TICK;
	}

	public double getTimeToInterface()
	{
		long t = getInterfaceTime();
		if (t == Constants.INVALID_TICK)
			return Double.NaN;
		else
			return AstroUtil.tick2dbl(t - getGame().time());
	}

	double getBC(Vector3f vv, float mach)
	{
		AeroForces af = ship.getStructure().calculateDragCoeff(
			vv, mach, getGame().time());
		double BC = af.BC/(1000*ship.getMass());
		return BC;
	}

	boolean integrateReentryTrajectory()
	{
		integ_ang = Double.NaN;
		integ_maxg = Double.NaN;
		integ_landpos.set(Double.NaN, Double.NaN, Double.NaN);
		integ_landllr.set(Double.NaN, Double.NaN, Double.NaN);

		long intt = getInterfaceTime();
		if (intt == INVALID_TICK)
			return false;
		Planet planet = (Planet)ship.getParent();
		Atmosphere atmo = planet.getAtmosphere();
		Conic orbit = UniverseUtil.getConicFor(ship);
		double dt = AstroUtil.tick2dbl(intt);
		StateVector sv = orbit.getStateVectorAtTime(dt);
		Vector3d r = sv.r;
		Vector3d v = sv.v;

		PlanetIntegrator3d pint = new PlanetIntegrator3d();
		pint.setState(r, v);
		pint.setPlanet(planet);

		// calculate drag forces
		// todo: precalculate relative velocity vec
		Vector3f vvf = new Vector3f(getShipTelemetry().getAirRefVelocityShipRef());
		vvf.normalize();

		double lowr = planet.getRadius() + 10; // todo: const

		int maxiters = 1000;
		int iter=0;
		integ_maxg = 0;
//		pint.setDebug(true);

		// get speed of sound at sea level
		float mach1sl = (atmo != null) ? atmo.getParamsAt(0).airvel/1000 : 0;
		float mach;
		float last_mach = -1;
		float MACH_STEP = 0.1f; //todo: const
		float MIN_MACH = 5.0f;

		// intergrate the trajectory
		while (++iter < maxiters &&
			pint.getR1().lengthSquared() > lowr*lowr)
		{
			if (atmo != null)
			{
				// if mach changes by MACH_STEP, recompute
				mach = (float)pint.getV1().length()/mach1sl;
				mach = Math.max(MIN_MACH, mach);
				if (Math.abs(mach-last_mach) > MACH_STEP)
				{
					pint.setBC(getBC(vvf, mach));
					last_mach = mach;
				}
			}
			pint.autointegrate();
			double gforce = pint.getLastForce().length()/Constants.EARTH_G;
			if (gforce > integ_maxg)
				integ_maxg = gforce;
		}

		// todo: too many iters (error conditions)

		Game game = getGame();
		long tnow = game.time();

		Vector3d r0 = ship.getPosition(planet, tnow);
		integ_ang = r0.angle(pint.getR1());
		integ_time = (long)(pint.getTime()*TICKS_PER_SEC) + intt;

		integ_landpos.set(pint.getR1());
		integ_landllr.set(pint.getR1());
		planet.xyz2ijk(integ_landllr);
		planet.ijk2llr(integ_landllr, dt + pint.getTime());

		return true;
	}

	void reintegrate()
	{
		long t = getGame().time();
		if (last_integ_time == Constants.INVALID_TICK ||
			t >= last_integ_time + min_upd_time)
		{
			integrateReentryTrajectory();
			last_integ_time = t;
		}

	}

	// todo: call integrateReentryTrajectory when needed

	public double getMaxG()
	{
		reintegrate();
		return integ_maxg;
	}

	public double getReentryRange()
	{
		reintegrate();
		return integ_ang*ship.getParent().getRadius();
	}

	public Vector3d getLandingLLR()
	{
		reintegrate();
		return integ_landllr;
	}

	public double getEntryAngle()
	{
		return entry_ang;
	}

	public void setEntryAngle(double entry_ang)
	{
		this.entry_ang = entry_ang;
	}

	Vector3d getTargetMissVec()
	{
		reintegrate();
		if (integ_landpos == null)
			return null;

		UniverseThing ut = ship.getShipTargetingSystem().getTarget();
		if (ut == null)
			return null;

		// rt = position of landing site at predicted landing time
		Vector3d rt = ut.getPosition(ut.getParent(), integ_time);
		if (rt == null)
			return null;

		return rt;
	}

	public double getDownrangeError()
	{
		return getDownrangeErrorAngle() * ship.getParent().getRadius();
	}

	public double getCrossrangeError()
	{
		return getCrossrangeErrorAngle() * ship.getParent().getRadius();
	}

	// todo: slow
	public double getDownrangeErrorAngle()
	{
		Vector3d rt = getTargetMissVec();
		if (rt == null)
			return Double.NaN;

		Orientation sort = new Orientation(
			getShipTelemetry().getVelocityVec(), rt);

		sort.invTransform(rt);
		rt.x = 0;

		Vector3d lpos = new Vector3d(integ_landpos);
		sort.invTransform(lpos);
		lpos.x = 0;

		// spherical trig - right tri
		// TL = ang from target to landing site
		// AL = ang between VT and VL
		// AT = downrange dist
		// cos TL = cos AT * cos AL
		// so AT = acos(cos TL/cos AL)

		Vector3d V = getShipTelemetry().getCenDistVec();
		sort.invTransform(V);
		boolean neg = V.angle(rt) > V.angle(lpos);

		return rt.angle(lpos) * (neg?-1:1);
	}

	// todo: slow
	public double getCrossrangeErrorAngle()
	{
		Vector3d rt = getTargetMissVec();
		if (rt == null)
			return Double.NaN;

		Orientation sort = new Orientation(
			getShipTelemetry().getVelocityVec(), rt);

		Vector3d lpos = new Vector3d(integ_landpos);
		sort.invTransform(lpos);

		double r = ship.getParent().getRadius();
		return -lpos.x/r;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipReentrySystem.class);

	static {
		prophelp.registerGetSet("entryang", "EntryAngle", double.class);
		prophelp.registerGet("ttint", "getTimeToInterface");
		prophelp.registerGetSet("intalt", "InterfaceAltitude", double.class);
		prophelp.registerGet("entrange", "getReentryRange");
		prophelp.registerGet("down_errorang", "getDownrangeErrorAngle");
		prophelp.registerGet("cross_errorang", "getCrossrangeErrorAngle");
		prophelp.registerGet("down_error", "getDownrangeError");
		prophelp.registerGet("cross_error", "getCrossrangeError");
		prophelp.registerGet("maxg", "getMaxG");
		prophelp.registerGet("landllr", "getLandingLLR");
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try {
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}
}
