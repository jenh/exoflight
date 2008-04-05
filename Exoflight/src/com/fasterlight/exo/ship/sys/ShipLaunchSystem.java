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
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * Encapsulates the launch system for a ship,
  * determining launch time from incl and raan.
  */
public class ShipLaunchSystem
extends ShipSystem
implements PropertyAware, Constants
{
	private double incl, raan, azimuth;
	private long launch_time;
	private boolean dirty=true;
	private boolean targeting=true;
	private boolean ascend=true;

	private double targalt, targvel, targfpa;
	private double burntime;
	private double acclimit;
	private double argPeriapsis; // only matters when doing IGM

	private UniverseThing last_target;

	//

	public ShipLaunchSystem(SpaceShip ship)
	{
		super(ship);
	}

	private long recomputeLaunchTime()
	{
		if (ship.getParent() instanceof Planet)
		{
			Planet plan = (Planet)ship.getParent();
			double lon = getShipTelemetry().getLONG();
			// Vallado, pgs. 296-297
			double cosang = Math.cos(azimuth)/Math.sin(incl);
			double auxang = Math.acos(cosang);
//			if (ascend)
//				auxang = Math.PI*2-auxang;
			if (Double.isNaN(auxang))
				return INVALID_TICK;
			auxang = AstroUtil.fixAngle(auxang);
			double rot0 = plan.getRotation0(); // todo: rot0
			double rotper = plan.getRotationPeriodSecs();
			double anggst = raan + auxang - lon - rot0;
			anggst = AstroUtil.fixAngle(anggst);
			long t = getGame().time();
			long t0 = (long)( Math.floor(t/(rotper*TICKS_PER_SEC)) * rotper * TICKS_PER_SEC );
			long t1 = (long)(t0 + anggst*rotper*TICKS_PER_SEC/(Math.PI*2));
			t1 -= getBurnTime()*TICKS_PER_SEC;
			if (t1 < t)
				t1 += (long)(rotper*TICKS_PER_SEC);
			return t1;
		} else
			return INVALID_TICK;
	}

	private void recompute()
	{
		// if dirty flag not set, no need to recompute
		if (!dirty)
			return;

		// recompute target, if there is one
		UniverseThing target = getTarget();
		if (target != null)
		{
			Telemetry telem = target.getTelemetry();
			double newincl = telem.getINCL();
			double newraan = telem.getRAAN();
			setInclination(newincl);
			setRAAN(newraan);
		}

		// recompute azimuth
		double lat = getShipTelemetry().getLAT();
		// azimuth = asin(cos i/cos L)
      // from BMW p. 142
		azimuth = Math.asin(Math.cos(incl)/Math.cos(lat));
		if (!ascend)
			azimuth = Math.PI-azimuth;

		// recomppute launch time
		launch_time = recomputeLaunchTime();

		// reset dirty flag
		dirty = false;
	}

	public double getInclination()
	{
		recompute();
		return incl;
	}

	public void setInclination(double incl)
	{
		this.incl = incl;
		dirty = true;
	}

	public double getRAAN()
	{
		recompute();
		return raan;
	}

	public void setRAAN(double raan)
	{
		this.raan = raan;
		dirty = true;
	}

	public double getArgPeriapsis()
	{
		recompute();
		return argPeriapsis;
	}

	public void setArgPeriapsis(double argPeriapsis)
	{
		this.argPeriapsis = argPeriapsis;
		dirty = true;
	}

	public double getAzimuth()
	{
		recompute();
		return azimuth;
	}

	public void setAzimuth(double az)
	{
		this.ascend = Math.cos(az) >= 0;
		// sin(azimuth)*cos(lat) = cos(incl)
		double lat = getShipTelemetry().getLAT();
		this.incl = Math.acos(Math.sin(az)*Math.cos(lat));
		dirty = true;
	}

	public void setAscending(boolean asc)
	{
		this.ascend = asc;
		dirty = true;
	}

	public boolean getAscending()
	{
		return ascend;
	}

	public Conic getTargetOrbit()
	{
		// make sure conic will be vali!
		if (getTargetApoapsis() < getTargetAltitude())
			throw new PropertyRejectedException("Target apoapsis must be greater or equal to target altitude.");

		Planet planet = (Planet)ship.getParent();
		KeplerianElements ke = new KeplerianElements();
		ke.setPeriapsisAndApoapsis(
			getTargetAltitude() + planet.getRadius(),
			getTargetApoapsis() + planet.getRadius());
		ke.setInclination(getInclination());
		ke.setRAAN(getRAAN());
		if (argPeriapsis != 0)
			ke.setArgPeriapsis(argPeriapsis); // todo: can we calculate this?
		else {
			// TODO: calculate this?
			/*
			double argPeri =
				ship.getTelemetry().getARGPERI() +
				ship.getTelemetry().getMEANANOM();
			ke.setArgPeriapsis(argPeri);
			*/
		}
		ke.setMu(ship.getParent().getMass()*GRAV_CONST_KM);
		Conic conic = new Conic(ke);
		return planet.ijk2xyz(conic);
	}

	public long getLaunchTime()
	{
		recompute();
		return launch_time;
	}

	public double getTimeToLaunch()
	{
		recompute();
		if (launch_time == Game.INVALID_TICK)
			return Double.NaN;
		else
			return AstroUtil.tick2dbl(launch_time - getGame().time());
	}

	public void setTimeToLaunch(double ttl)
	{
		recompute();
		if (ship.getParent() instanceof Planet)
		{
			Planet plan = (Planet)ship.getParent();
			double lon = getShipTelemetry().getLONG();
			// Vallado, pgs. 296-297
			double cosang = Math.cos(azimuth)/Math.sin(incl);
			double auxang = Math.acos(cosang);
System.out.println("az=" + azimuth + ", incl=" + incl);
			if (Double.isNaN(auxang))
				throw new PropertyRejectedException("Could not set time-to-launch: auxang NaN");
			auxang = AstroUtil.fixAngle(auxang);
			double rot0 = plan.getRotation0();
			double rotper = plan.getRotationPeriodSecs();

			double t = AstroUtil.tick2dbl(getGame().time());
			double t1 = t + ttl + getBurnTime();

			double RA = -2*Math.PI*Math.floor(t/rotper) -
				auxang + lon + rot0 + 2*Math.PI*t1/rotper;
			setRAAN(RA);
System.out.println("RA = " + Util.toDegrees(RA));
System.out.println("ttl = " + getTimeToLaunch());
		} else
			throw new PropertyRejectedException("Could not set time-to-launch");
	}

	public String getLaunchTimeStr()
	{
		long lt = getLaunchTime();
		if (lt == INVALID_TICK)
			return "INVALID";
		return AstroUtil.formatDate( AstroUtil.gameTickToJavaDate(lt) );
	}

	protected Sequencer loadSequencer()
	{
		try {
			long t0 = getLaunchTime();
			if (t0 == INVALID_TICK)
				throw new UserException(
						"The target inclination may be invalid; " +
						"it must be greater than the launch vehicle's latitude.");
			ship.getShipManeuverSystem().updateLaunch(true);
			Sequencer seq = ship.loadProgram("launch");
			seq.setZeroTime(t0);
			seq.setVar("incl", new Double(incl));
			return seq;
		} catch (Exception e) {
			throw new UserException("Could not compute launch parameters: " + e.getMessage(), e);
		}
	}

	public boolean isTargeting()
	{
		return targeting;
	}

	public boolean getTargeting()
	{
		return targeting;
	}

	public void setTargeting(boolean b)
	{
		this.targeting = b;
		dirty = true;
	}

	public UniverseThing getTarget()
	{
		if (targeting) {
			UniverseThing targ = ship.getShipTargetingSystem().getTarget();
			if (targ != last_target)
				dirty = true;
			last_target = targ;
			return targ;
		} else
			return null;
	}

	public double getTimeToGo()
	{
		return (getLaunchTime()-getGame().time())*(1d/TICKS_PER_SEC) + getBurnTime();
	}

	public double getBurnTime()
	{
		return burntime;
	}

	public void setBurnTime(double burntime)
	{
		this.burntime = burntime;
		dirty = true;
	}

	public double getTargetAltitude()
	{
		return targalt;
	}

	public void setTargetAltitude(double targalt)
	{
		this.targalt = targalt;
	}

	public double getTargetVelocity()
	{
		return targvel;
	}

	public void setTargetVelocity(double targvel)
	{
		this.targvel = targvel;
	}

	public double getTargetApoapsis()
	{
		double U = ship.getParent().getMass()*GRAV_CONST_KM;
		double Rmin = targalt + ship.getParent().getRadius();
		return (Rmin*Rmin * targvel*targvel) / (2*U - Rmin*targvel*targvel) -
			ship.getParent().getRadius();
	}

	public void setTargetApoapsis(double targapo)
	{
		double U = ship.getParent().getMass()*GRAV_CONST_KM;
		double Rmin = targalt + ship.getParent().getRadius();
		double Rmax = targapo + ship.getParent().getRadius();
		this.targvel = Math.sqrt(2) * Math.sqrt(Rmax*U/(Rmin*(Rmax+Rmin)));
	}

	public double getTargetFlightPathAngle()
	{
		return targfpa;
	}

	public void setTargetFlightPathAngle(double targfpa)
	{
		this.targfpa = targfpa;
	}

	public double getAccelLimit()
	{
		return acclimit;
	}

	public double getGLimit()
	{
		return acclimit/EARTH_G;
	}

	public void setGLimit(double acclimit)
	{
		this.acclimit = acclimit*EARTH_G;
	}

	//

	public double getTargetRadialVelocity()
	{
		return targvel*Math.sin(targfpa);
	}

	public double getTargetDownrangeVelocity()
	{
		return targvel*Math.cos(targfpa);
	}

	public Vector3d getTargetPlaneNormal()
	{
		double si = Math.sin(incl);
		double sl = Math.sin(raan);
		double ci = Math.cos(incl);
		double cl = Math.cos(raan);
		Vector3d v = new Vec3d(-si*sl,si*cl,-ci);
		if (ship.getParent() instanceof Planet)
			((Planet)ship.getParent()).ijk2xyz(v);
//		if (getAscending())
//			v.scale(-1);
		return v;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipLaunchSystem.class);

	static {
		prophelp.registerGetSet("incl", "Inclination", double.class);
		prophelp.registerGetSet("raan", "RAAN", double.class);
		prophelp.registerGetSet("azimuth", "Azimuth", double.class);
		prophelp.registerGetSet("ascending", "Ascending", boolean.class);
		prophelp.registerGet("launchtime", "getLaunchTimeStr");
		prophelp.registerGetSet("timetolaunch", "TimeToLaunch", double.class);
		prophelp.registerGetSet("targeting", "Targeting", boolean.class);
		prophelp.registerGet("target", "getTarget");
		prophelp.registerGet("targorbit", "getTargetOrbit");
		prophelp.registerGetSet("burntime", "BurnTime", double.class);
		prophelp.registerGetSet("targalt", "TargetAltitude", double.class);
		prophelp.registerGetSet("targapo", "TargetApoapsis", double.class);
		prophelp.registerGetSet("targvel", "TargetVelocity", double.class);
		prophelp.registerGetSet("targfpa", "TargetFlightPathAngle", double.class);
		prophelp.registerGetSet("glimit", "GLimit", double.class);
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
