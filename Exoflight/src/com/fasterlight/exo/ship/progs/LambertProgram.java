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
package com.fasterlight.exo.ship.progs;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.nav.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * Uses the Lambert equation to transfer to a rendezvous orbit,
  * modifying the orbit as the maneuver is underway.
  */
public class LambertProgram
implements GuidanceProgram, Constants, PropertyAware
{
	Vector3d targetpos;
	long targtime = INVALID_TICK;
	long timebias;
	long min_tti;
	UniverseThing ref, target;
	boolean init = false;
	boolean tvdotpos;
	double lastdv=1e8;
	double minalt;
	boolean continuous = false;
	boolean longway;
	int direction;

	LambertResult lastgr;

	double drift_factor = TICKS_PER_SEC;

	public long getTargetTime()
	{
		return targtime;
	}

	public void setTargetTime(long targtime)
	{
		this.targtime = targtime;
	}

	public double getTimeBias()
	{
		return timebias*(1d/TICKS_PER_SEC);
	}

	public void setTimeBias(double timebias)
	{
		this.timebias = (long)(timebias*TICKS_PER_SEC);
	}

	public double getMinTTI()
	{
		return min_tti*(1d/TICKS_PER_SEC);
	}

	public void setMinTTI(double min_tti)
	{
		this.min_tti = (long)(min_tti*TICKS_PER_SEC);
	}

	public Vector3d getDestPos(long t)
	{
		if (target != null) {
			Vector3d dpos = target.getPosition(ref, t);
			return dpos;
		} else
			return targetpos;
	}

	public Vector3d getDestVel(long t)
	{
		if (target != null) {
			Vector3d vpos = target.getVelocity(ref, t);
			return vpos;
		} else
			return null; //todo?
	}

	class LambertResult
	{
		Vector3d v,v2;
		double vl,vl2,vt;
		Conic orbit;
		double peri;
	}

	LambertResult tryLambert(Game game, Vector3d r1, Vector3d v1, long timeadd, boolean otherway)
	{
		long t = game.time();

		Lambert g = new Lambert();
		double dt = (targtime+timebias+timeadd-t)*(1d/TICKS_PER_SEC);
		Vector3d r2 = getDestPos(targtime+timebias+timeadd);
		Vector3d v2 = getDestVel(targtime+timebias+timeadd);
		// Vallado, pg. 456
		//boolean longway = g.useLongWay(r1,v1,r2,v2);
		try {
			g.solve(r1, r2, ref.getMass()*GRAV_CONST_KM, dt, longway ^ otherway);

   		LambertResult gr = new LambertResult();
   		gr.orbit = g.getConic();
   		gr.v = new Vector3d(g.v1);
   		gr.v.sub(v1);
   		gr.vl = gr.v.length();
   		gr.v2 = new Vector3d(g.v2);
   		gr.v2.sub(v2);
   		gr.vl2 = gr.v2.length();
   		gr.vt = gr.vl+gr.vl2;
   		gr.peri = gr.orbit.getPeriapsis();

   		return gr;
		} catch (LambertException le) {
			return null;
		}

	}

	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		if (ship == null)
			return;

		if (targtime == INVALID_TICK)
		{
			targtime = ship.getShipTargetingSystem().getTargetTick();
		}

		target = ship.getShipTargetingSystem().getTarget();
		// make sure there is a target
		if (target == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for Lambert program");
			return;
		}

		Telemetry telem = ship.getTelemetry();
		Game game = ship.getUniverse().getGame();
		ref = ship.getParent();
		minalt = ref.getRadius() + ship.getShipTargetingSystem().getMinAltitude();

		long t = game.time();
		Vector3d r1 = ship.getPosition(ref, t);
		Vector3d v1 = ship.getVelocity(ref, t);

		// keep our time to target > min time
		if (targtime+timebias-t < min_tti)
		{
			timebias = 0;
			targtime = t+min_tti;
		}

		// todo: what if TOF is negative?

		long tryd = (long)(drift_factor*getDVRemaining());
		LambertResult gr = tryLambert(game, r1, v1, 0, false);

		if (gr == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-LAMBERTFAIL", "Lambert failed");
			return;
		}

		/*
		if (gr.peri < minalt)
			tryd = (long)drift_factor;
		*/

		LambertResult grm1 = tryLambert(game, r1, v1, -tryd, false);
		LambertResult grp1 = tryLambert(game, r1, v1, tryd, false);
		LambertResult grow = null;
		// only try the "other way" if dV is high (todo?)
		if (gr.vt > 0.1)
			grow = tryLambert(game, r1, v1, 0, true);

		if (grow != null && grow.vt < gr.vt)
		{
			gr = grow;
			longway = !longway;
		}
		else if (gr.peri < minalt && grm1 != null && grp1 != null)
		{
			if (grm1.peri > gr.peri && grm1.peri > grp1.peri) {
				gr = grm1;
				timebias -= tryd;
//				System.out.println(timebias + "\t" + gr.peri);
			}
			else if (grp1.peri > gr.peri) {
				gr = grp1;
				timebias += tryd;
//					System.out.println(timebias + "\t" + gr.peri);
			}
		}
		else if (grm1 != null && grp1 != null &&
			grm1.vt < gr.vt && grm1.vt < grp1.vt && grm1.peri > minalt)
		{
			gr = grm1;
			timebias -= tryd;
		}
		else if (grp1 != null && grp1.vt < gr.vt && grp1.peri > minalt)
		{
			gr = grp1;
			timebias += tryd;
		}

		lastgr = gr;
		direction = AstroUtil.sign2(v1.dot(gr.v));

		// compensate for gravity
		// todo: not right
		Vector3d fv = new Vector3d(telem.getCentripetalAccel());
		fv.add(telem.getGravAccelVec());
		fv.scale(-1);
		double k = ship.getMaxAccel() - fv.length();
		if (k > 0)
			fv.scaleAdd(k/gr.v.length(), gr.v, fv);

		// set orientation

		Orientation ort = new Orientation(fv, r1);
		attctrl.setTargetOrientation(ort);
	}

	public String toString()
	{
		return "Lambert trajectory to " + target;
	}

	public double getDVRemaining()
	{
		return (lastgr != null) ? lastgr.vl : Double.NaN;
	}

	public double getTotalDVRemaining()
	{
		return (lastgr != null) ? lastgr.vt : Double.NaN;
	}

	public double getTimeRemaining()
	{
		if (ref == null)
			return Double.NaN;
		Game game = ref.getUniverse().getGame();
		return (getTargetTime() - game.time() + timebias)*(1d/TICKS_PER_SEC);
	}

	public int getDirection()
	{
		return direction;
	}

	public boolean getLongWay()
	{
		return longway;
	}

	public Conic getTransferOrbit()
	{
		return (lastgr != null) ? lastgr.orbit : null;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(LambertProgram.class);

	static {
		prophelp.registerGetSet("targtime", "TargetTime", long.class);
		prophelp.registerGet("dvrem", "getDVRemaining");
		prophelp.registerGet("totaldvrem", "getTotalDVRemaining");
		prophelp.registerGet("timerem", "getTimeRemaining");
		prophelp.registerGet("tranorbit", "getTransferOrbit");
		prophelp.registerGet("direction", "getDirection");
		prophelp.registerGet("longway", "getLongWay");
		prophelp.registerGetSet("timebias", "TimeBias", double.class);
		prophelp.registerGetSet("mintti", "MinTTI", double.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
