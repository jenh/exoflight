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
import com.fasterlight.exo.orbit.nav.*;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * Encapsulates the targeting system for a ship.
  * Set/get the target, and query various parameters
  * with respect to the source ship.
  */
public class ShipTargetingSystem
extends ShipSystem
implements PropertyAware
{
	private UniverseThing target;

	private double minalt = 150; // min altitude allowed
	private double maxtod = 3600*12; // 12 hrs
	private double maxtof = 86400*3; // 3 days
	private double mintof = 60*15; // 15 mins
	private double mintod = 60; //2 mins

	private TOFOptimizer opt;
	private long t1l,t1h,t2l,t2h;

	private String progtype = "intercept";
	private Vector3d lastdockofs = new Vec3d();
	private Vector3d lastdockdir = new Vec3d();

	private Conic xferorbit;

	// todo: const
	long compute_interval = TICKS_PER_SEC/4;
	int compute_iters = 12;
	int compute_maxiters = 1000;

	Vector3f currentpt = new Vector3f();

	//

	public ShipTargetingSystem(SpaceShip ship)
	{
		super(ship);
	}

	// can be null
	public TOFOptimizer getTOFOptimizer()
	{
		return opt;
	}

	private Telemetry getTargetTelemetry()
	{
		if (getTarget() == null)
			throw new RuntimeException("Target is null");
		else
			return getTarget().getTelemetry();
	}

	public UniverseThing getTarget()
	{
		// if target is invalid (removed) set to null
		if (target != null && target.getTrajectory() == null)
			target = null;
		return target;
	}

	public void setTarget(UniverseThing target)
	{
		if (target == ship)
			throw new RuntimeException("Target cannot be same as ship!");
		this.target = target;
	}

	public double getMinAltitude()
	{
		return minalt;
	}

	public void setMinAltitude(double minalt)
	{
		this.minalt = minalt;
	}

	public double getMaxTOD()
	{
		return maxtod;
	}

	public void setMaxTOD(double maxtod)
	{
		this.maxtod = maxtod;
	}

	public double getMaxTOF()
	{
		return maxtof;
	}

	public void setMaxTOF(double maxtof)
	{
		this.maxtof = maxtof;
	}

	// LAMBERT COMPUTING

	public boolean isComputing()
	{
		return (computeevent != null);
	}

	void startComputing()
	{
		if (isComputing())
			return;

		Game game = getGame();
		UniverseThing destbody = getTarget();
		if (ship == null || destbody == null)
		{
			ship.getShipWarningSystem().setWarning("NAV-COMPUTE", "No target for trajectory computation");
			return;
		}

		// todo: what if different parents?
		Telemetry tel1 = getShipTelemetry();
		Telemetry tel2 = getTargetTelemetry();
		double per1 = tel1.getPERIOD();
		double per2 = tel2.getPERIOD();

		// compute TOD and TOF times
		// TOD = 2 min - syn period
		// TOF = 2 min - (period 1 + period 2)
		double synper = Math.min(maxtod, getSynodicPeriod());
		if (Double.isNaN(synper))
			synper = maxtod;
		double minper = mintod;
		long t1l = (long)(minper*TICKS_PER_SEC);
		long t1h = (long)(synper*TICKS_PER_SEC);
		long t2l = (long)(mintof*TICKS_PER_SEC); // todo: const?
		double hitof = (Math.max(per1,per2)/2);
		hitof = Math.min(maxtof, hitof);
		long t2h = (long)(hitof*TICKS_PER_SEC);

		long t = game.time();
		opt = new TOFOptimizer(
			ship, destbody, destbody.getParent(),
			t+t1l, t+t1h, t2l, t2h);
		opt.setMinXSize(1f/256);
		opt.setMinYSize(1f/256);
		opt.setMinRadius(minalt + destbody.getParent().getRadius());

		computeevent = new ComputeEvent(game.time());
		game.postEvent(computeevent);
	}

	void stopComputing()
	{
		if (!isComputing())
			return;

		getGame().cancelEvent(computeevent);
		computeevent = null;
	}

	public void setComputing(boolean b)
	{
		if (b) {
			startComputing();
		} else {
			stopComputing();
		}
	}

	ComputeEvent computeevent;

	class ComputeEvent
	extends GameEvent
	{
		ComputeEvent(long time)
		{
			super(time);
		}
		public void handleEvent(Game game)
		{
			compute(compute_iters);

			Tuple2f bestpt = opt.getBestPoint();
			if (bestpt != null)
			{
				currentpt.x = bestpt.x;
				currentpt.y = bestpt.y;
				updateTransferOrbit();
			}

			if (opt.getSize() < compute_maxiters)
			{
				eventtime += compute_interval;
				game.postEvent(this);
			} else {
				computeevent = null;
			}
		}
	}

	void updateTransferOrbit()
	{
		try {
			xferorbit = opt.getConicFor(currentpt.x, currentpt.y);
		} catch (NavigationException nave) {
			ship.getShipWarningSystem().setWarning("NAV-COMPUTE", nave.getMessage());
		}
	}

	void compute(int iters)
	{
		for (int i=0; i<iters; i++)
		{
			opt.iterate();
		}
	}

	public double getMinDeltaV()
	{
		if (opt == null)
			return Double.NaN;
		double c = opt.getMinCost();
		if (c >= TOFOptimizer.MAX_COST)
			return Double.NaN;
		else
			return c;
	}

	// TARGETING QUERIES

	public Vector3d getTargetPosVec()
	{
		if (getTarget() == null)
			return null;
		else {
			Vector3d v = new Vec3d(target.getPosition(ship, getGame().time()));
			ship.getOrientation(getGame().time()).invTransform(v);
			return v;
		}
	}

	public Vector3d getTargetDockingVec()
	{
		if (getTarget() == null)
			return null;
		else {
			return lastdockofs;
		}
	}

	public Vector3d getTargetDockingDir()
	{
		if (getTarget() == null)
			return null;
		else {
			return lastdockdir;
		}
	}

	public Orientation getTargetDockingOrt()
	{
		if (getTarget() == null)
			return null;
		else {
			return new Orientation(lastdockdir); //todo!
		}
	}

	public Vector3d getTargetVelVec()
	{
		if (getTarget() == null)
			return null;
		else {
			Vector3d v = new Vec3d(target.getVelocity(ship, getGame().time()));
			ship.getOrientation(getGame().time()).invTransform(v);
			return v;
		}
	}

	// todo: angular dist
	public double getTargetLinearDistance()
	{
		if (getTarget() == null)
			return Double.NaN;
		else
			return getTargetPosVec().length();
	}

	public double getTargetRange()
	{
		if (getTarget() == null || ship.getParent() != getTarget().getParent())
			return Double.NaN;
		else {
			return getPhaseAngle() * getTargetTelemetry().getCENDIST();
		}
	}

	// todo: angular rate
	public double getTargetClosure()
	{
		if (getTarget() == null)
			return Double.NaN;
		else
			return getTargetVelVec().length();
	}

	public double getTargetInterceptTime()
	{
		return getTargetRange()/getTargetClosure();
	}

	// todo: >180 when moving away
	public double getPhaseAngle()
	{
		if (getTarget() == null)
			return Double.NaN;

		Telemetry t1 = getShipTelemetry();
		Telemetry t2 = getTargetTelemetry();

		// take dot product of pos vectors
		Vector3d p1 = t1.getCenDistVec();
		Vector3d p2 = t2.getCenDistVec();
		double ang = p1.angle(p2);
		return ang;
	}

	public double getSynodicPeriod()
	{
		if (getTarget() == null)
			return Double.NaN;

		Telemetry t1 = getShipTelemetry();
		Telemetry t2 = getTargetTelemetry();

		double p1 = 1/t1.getPERIOD();
		double p2 = 1/t2.getPERIOD();

		return 1/Math.abs(p1-p2);
	}

	public double getCatchTime()
	{
		if (getTarget() == null)
			return Double.NaN;

		return getSynodicPeriod()*getPhaseAngle()/(Math.PI*2);
	}

	public double getLeaveTime()
	{
		if (opt != null)
		{
			long seltime = opt.x2time(currentpt.x);
			return (seltime - getGame().time())*(1d/TICKS_PER_SEC);
		}
		return Double.NaN;
	}

	public void setLeaveTime(double t)
	{
		if (opt != null)
		{
			currentpt.x = opt.time2x(AstroUtil.dbl2tick(t)-getGame().time());
			updateTransferOrbit();
		}
	}

	public double getFlightTime()
	{
		if (opt != null)
		{
			long seltof = opt.y2time(currentpt.y);
			return (seltof)*(1d/TICKS_PER_SEC);
		}
		return Double.NaN;
	}

	public void setFlightTime(double t)
	{
		if (opt != null)
		{
			currentpt.y = opt.time2y(AstroUtil.dbl2tick(t));
			updateTransferOrbit();
		}
	}

	public long getTargetTick()
	{
		if (opt != null)
		{
			long seltime = opt.x2time(currentpt.x);
			long seltof = opt.y2time(currentpt.y);
			return (seltof+seltime);
		}
		return Constants.INVALID_TICK;
	}

	public String getProgramType()
	{
		return progtype;
	}

	public void setProgramType(String progtype)
	{
		this.progtype = progtype;
	}

	public void activateProgram(String progtype)
	{
		setActive(false);
		setProgramType(progtype);
		setActive(true);
	}

	public Conic getTransferOrbit()
	{
		return xferorbit;
	}

	protected Sequencer loadSequencer()
	{
		if ("intercept".equals(progtype))
		{
			if (opt == null || getTarget() == null)
				return null;

			long seltime = opt.x2time(currentpt.x);
			long seltof = opt.y2time(currentpt.y);

			Sequencer seq = ship.loadProgram("intercept");
			seq.setVar("target", getTarget());
			seq.setVar("targtime", new Long(seltof+seltime));
			seq.setZeroTime(seltime);
			return seq;
		}
		else if ("landing".equals(progtype))
		{
			if (getTarget() == null)
				return null;

			Sequencer seq = ship.loadProgram("landing");
			seq.setZeroTime(getGame().time() + TICKS_PER_SEC*60); // todo??
			return seq;
		}
		else if ("docking".equals(progtype))
		{
			if (getTarget() == null)
				return null;

			Sequencer seq = ship.loadProgram("docking");
			seq.setZeroTime(getGame().time()); // todo??
			return seq;
		}
		else
		{
			System.out.println("Couldn't parse progtype " + progtype);
			return null;
		}
	}

	public void setLastDockingOffset(Vector3d ofs)
	{
		this.lastdockofs.set(ofs);
	}

	public void setLastDockingDirection(Vector3d ofs)
	{
		this.lastdockdir.set(ofs);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipTargetingSystem.class);

	static {
		prophelp.registerGetSet("target", "Target", UniverseThing.class);
		prophelp.registerGetSet("minalt", "MinAltitude", double.class);
		prophelp.registerGetSet("progtype", "ProgramType", String.class);
		prophelp.registerSet("activateprog", "activateProgram", String.class);
		prophelp.registerGet("mindv", "getMinDeltaV");
		prophelp.registerGetSet("leavetime", "LeaveTime", double.class);
		prophelp.registerGetSet("flighttime", "FlightTime", double.class);
		prophelp.registerGetSet("maxtod", "MaxTOD", double.class);
		prophelp.registerGetSet("maxtof", "MaxTOF", double.class);
		prophelp.registerGet("computing", "isComputing");
		prophelp.registerSet("computing", "setComputing", boolean.class);
		prophelp.registerGet("posvec", "getTargetPosVec");
		prophelp.registerGet("dockvec", "getTargetDockingVec");
		prophelp.registerGet("dockort", "getTargetDockingOrt");
		prophelp.registerGet("velvec", "getTargetVelVec");
		prophelp.registerGet("distance", "getTargetLinearDistance");
		prophelp.registerGet("range", "getTargetRange");
		prophelp.registerGet("closure", "getTargetClosure");
		prophelp.registerGet("intertime", "getTargetInterceptTime");
		prophelp.registerGet("phaseang", "getPhaseAngle");
		prophelp.registerGet("synperiod", "getSynodicPeriod");
		prophelp.registerGet("catchtime", "getCatchTime");
		prophelp.registerGet("tranorbit", "getTransferOrbit");
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
