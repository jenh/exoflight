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
import com.fasterlight.exo.ship.*;
import com.fasterlight.exo.ship.sys.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * A guidance program that implements a linear parametric
  * guidance algorithm as described in NASA-TM-X-67218,
  * "Project/Space Shuttle - Space shuttle guidance, navigation and
  * control design equations, volume 1"
  *
  * pitch = A + B*t
  * yaw = C + D*t
  */
public class IGMProgram
extends PYRProgram
implements GuidanceProgram, Constants, PropertyAware
{
	Vector3d r = new Vec3d();
	Vector3d v = new Vec3d();
	Vector3d i_q = new Vec3d();
	Vector3d i_r = new Vec3d();
	Vector3d i_z = new Vec3d();
	Vector3d i_y = new Vec3d();
	Vector3d vg = new Vec3d();
	Vector3d aTry = new Vec3d();
	Vector3d thrustvec = new Vec3d();

	long last_time = INVALID_TICK; // last time when tgo was set
	long t0; // time since A,B,C,D were set
//	long interval = TICKS_PER_SEC/2; // when to call guidance algo

	float toff = 0.5f; // tgo < seconds, we stop calculating coeffs.
	double tgo,tlim;
	double A,B,C,D;
	double U,rl,y,rdot,ydot,zdot,geff,vgl,tau;

	double tau_bias;
	double kcoast = 1; // scale factor for glimit

	boolean rinhibit; // vertical-inhibit mode
	boolean yinhibit; // crossrange-inhibit mode

	Conic targorbit; // target orbit - optional mode
	double targorbtime;
	double targorbmult;

	double targ_rl;
	double targ_rdot;
	double targ_zdot;
	Vector3d targ_plane_nml = new Vec3d();

	double RECALC_DAMPING_RATE = 1.0;
	double RECALC_START_FACTOR = 0.25;

	//

	public IGMProgram()
	{
	}

	public double getTimeToGo()
	{
		return tgo;
	}

	public double getTimeToLimit()
	{
		return tlim;
	}

	public float getTimeToFreeze()
	{
		return toff;
	}

	public void setTimeToFreeze(float toff)
	{
		this.toff = toff;
	}

	public double getCrossRangeDistance()
	{
		return y;
	}

	public double getRadialRate()
	{
		return rdot;
	}

	public double getCrossRangeRate()
	{
		return ydot;
	}

	public double getDownRangeRate()
	{
		return zdot;
	}

	public double getVelocityToBeGained()
	{
		return vgl;
	}

	public double getTau()
	{
		return tau;
	}

	public boolean getRadialInhibit()
	{
		return rinhibit;
	}

	public void setRadialInhibit(boolean rinhibit)
	{
		this.rinhibit = rinhibit;
	}

	public boolean getCrossRangeInhibit()
	{
		return yinhibit;
	}

	public void setCrossRangeInhibit(boolean yinhibit)
	{
		this.yinhibit = yinhibit;
	}

	public Conic getTargetOrbit()
	{
		return targorbit;
	}

	public void setTargetOrbit(Conic targorbit)
	{
		this.targorbit = targorbit;
		this.targorbtime = 0;
	}

	//

	// todo: separate into blocks, different methods

	void computeTargetOrbit(SpaceShip ship)
	{
		ShipManeuverSystem sms = ship.getShipManeuverSystem();

		if (sms.getUseArgPeriapsis())
		{
			Planet planet = (Planet)ship.getParent();

			// find targorbtime, which is the time at which we will
			// evaluate the target orbit and snarf its state vector at
			// that time.
			// we do this by retrieving the _mean argument of latitude_ of
			// spacecraft at time curtime (mean anom + arg of peri)
			// convert that to the mean anomoly of the target orbit,
			// and then evaluate the target orbit at (curtime + tgo)
			// (todo)
			long curtime = ship.getUniverse().getGame().time();
			Telemetry telem = ship.getTelemetry();
			// true argument of latitude (arg of periapsis + true anomoly)
			// Vallado pg 137
			double trueArgLat = telem.getARGPERI() + telem.getTRUEANOM();
			// get true anomoly in target orbit (trueArgLat - argPeriapsis)

			KeplerianElements ke = new KeplerianElements(sms.getElements());
			targorbit = new Conic(ke);
				// targorbit.getElements();
			double targTrueAnom = AstroUtil.fixAngle(trueArgLat - ke.getArgPeriapsis());
			ke.setTrueAnomaly(targTrueAnom);
			double targMean = ke.getMeanAnomaly();
			double t0 = targorbit.getTimeAtMeanAnomaly2(targMean);

//System.out.println("  " + trueArgLat + " " + targTrueAnom + " " + targMean + " " + (t0-AstroUtil.tick2dbl(curtime)));

			if (targorbtime == 0)
			{
				targorbtime = t0;
				targorbmult = RECALC_START_FACTOR;
			} else {
				double ttime = t0 + tgo;
				targorbtime += (ttime - targorbtime)*targorbmult; // Math.min(0.5, tgo/10000);
				targorbmult *= RECALC_DAMPING_RATE;
			}

			StateVector sv = targorbit.getStateVectorAtTime(targorbtime);
			Conic conic = planet.xyz2ijk(targorbit);
			KeplerianElements elem = conic.getElements();

			targ_plane_nml.set(conic.getPlaneNormal());
			targ_rl = sv.r.length();

			double targvel = sv.v.length();
			double dot = sv.v.dot(sv.r)/(sv.v.length()*sv.r.length());
			double fpa = Math.asin(dot);

			targ_rdot = targvel*dot;
			targ_zdot = targvel*Math.cos(fpa);
		} else {
			// if useargperi == false, we just use the periapsis value
			// and the max. velocity
			// optz: only really need 1nce
			KeplerianElements ke = sms.getElements();
			Conic conic = new Conic(ke);
			targorbit = conic;

			targ_plane_nml.set(conic.getPlaneNormal());
			targ_rl = conic.getPeriapsis();
			targ_rdot = 0; //todo: use FPA?
			targ_zdot = conic.getMaxVelocity();
		}

//System.out.println(targ_plane_nml + " " + targ_rl + " " + targ_rdot + " " + targ_zdot);

		targ_plane_nml.normalize();
	}

	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		computeTargetOrbit(ship);

		Telemetry telem = ship.getTelemetry();
		ShipLaunchSystem sls = ship.getShipLaunchSystem();
		UniverseThing parent = ship.getParent();
		U = parent.getMass()*Constants.GRAV_CONST_KM;
		long time = ship.getUniverse().getGame().time();

		double thrust = ship.getStructure().getCurrentThrust();
		double mass = ship.getStructure().getMassAtTime(time);
		double aT = thrust/mass;

		// todo: if no thrust, get expected thrust
		if (aT < 1e-10)
		{
			thrust = ship.getMaxThrust();
			aT = thrust/mass;
		}

		// now do thrust limiting, if applic.
		double aTL = sls.getAccelLimit(); //todo
		if (aTL > 0 && aT > aTL)
		{
			// todo: doesnt work
			double aMAX = ship.getMaxAccel();
			float throt = (float)(aTL/aMAX);
			if (throt > 0)
			{
				ship.getShipAttitudeSystem().setAutoThrottle(throt);
			}
			if (debug)
				System.out.println("aMAX=" + aMAX + ", throt=" + throt);
		}

		double aTLk = aTL*kcoast;

		double flowrate = ship.getStructure().getMassFlowRate();
		if (flowrate < 1e-5)
			flowrate = 1e-5; //todo-yuk!
		double ve = thrust/flowrate;
		double tau = mass/flowrate + tau_bias;
		this.tau = tau;

		if (Double.isInfinite(tau) || Double.isInfinite(ve))
		{
			ship.getShipWarningSystem().setWarning("GUID-IGM", "IGM overflow");
			return;
		}

		if (debug) {
			System.out.println("thrust=" + thrust + ", flow=" + flowrate + ", mass=" + mass);
			System.out.println("  ve=" + ve + ", tau=" + tau + ", aT=" + aT + ", aTL=" + aTL);
		}

		// pg. 9.4-12
		// local position & velocity

		r.set(telem.getCenDistVec());
		v.set(telem.getVelocityVec());
		i_q.set(targ_plane_nml);
		i_r.set(r);
		i_r.normalize();
		i_z.cross(i_r, i_q);
		i_y.cross(i_z, i_r);
		rl = telem.getCENDIST();
		y = rl*Math.asin(i_r.dot(i_q));
		rdot = v.dot(i_r);
		ydot = v.dot(i_y);
		zdot = v.dot(i_z); // i did the abs()

		if (debug)
		{
			System.out.println("rdot=" + rdot + ", ydot=" + ydot + ", zdot=" + zdot + ", y=" + y);
		}

		// effective gravity

		Vector3d rxv = new Vec3d();
		rxv.cross(r, v);
		geff = (-U + rxv.lengthSquared()/rl)/(rl*rl);

		// velocity to-be-gained

		boolean firsttime = (last_time == INVALID_TICK);
		if (firsttime)
			tgo = sls.getTimeToGo();
		else
			tgo -= (time - last_time)*(1d/TICKS_PER_SEC);
		last_time = time;

		vg.set(i_r);
		vg.scale(targ_rdot - rdot);
		vg.scaleAdd(-ydot, i_y, vg);
		vg.scaleAdd(targ_zdot - zdot, i_z, vg);

		vg.scaleAdd(-0.5*geff*tgo, i_r, vg);
		vgl = vg.length();

		if (debug)
			System.out.println("geff=" + geff + ", tgo=" + tgo + ", vgl=" + vgl);

		// time-to-go prediction
		// 9.4-13

//		tgo = tau*(vgl/ve)*(1 - 0.5*vgl/ve); // crap!
		tgo = tau*(1-Math.exp(-vgl/ve));
		tlim = (aTL > 0) ? (tau - ve/aTL) : 0;
		if (aTL > 0 && tgo > tlim)
		{
			tgo = tlim + (vgl + ve*Math.log(1 - tlim/tau))/aTLk;
		}

		if (tgo <= 0)
		{
			ship.getShipWarningSystem().setWarning("GUID-TGO", "Desired velocity already attained");
			return; // we're burnt out, man
		}

		if (debug)
			System.out.println("tlim=" + tlim + ", newtgo=" + tgo);

		// linear control law
		// 9.4-14,15

		if (tgo > toff)
		{
   		double a11,a12,a21,a22;
   		if (aTLk <= 0 || tgo < tlim) // accel limit will not be reached
   		{
   			double log = Math.log(1 - tgo/tau);
   			a11 = -ve*log;
   			a12 = -ve*tau*log - ve*tgo;
   			a21 = ve*(tau-tgo)*log + ve*tgo;
   			a22 = ve*tau*(tau-tgo)*log + ve*tgo*(tau - 0.5*tgo);
   		}
   		else if (0 < tlim && tlim < tgo) // accel limit will be reached
   		{
   			double log = Math.log(1 - tlim/tau);
   			a11 = -ve*log + aTLk*(tgo-tlim);
   			a12 = -ve*tau*log - ve*tlim + 0.5*aTLk*(tgo*tgo-tlim*tlim);
   			a21 = ve*(tau-tgo)*log + ve*tlim + 0.5*aTLk*(tgo-tlim)*(tgo-tlim);
   			a22 = ve*tau*(tau-tgo)*log + ve*tlim*(tau + 0.5*tlim - tgo) +
   				(1d/6)*aTLk*(tgo*tgo*tgo - 3*tgo*tlim*tlim + 2*tlim*tlim*tlim);
   		}
   		else // accel limit has been reached
   		{
   			a11 = aTLk*tgo;
   			a12 = a21 = 0.5*aTLk*tgo*tgo;
   			a22 = (1d/6)*aTLk*tgo*tgo*tgo;
   		}

   		double J = 1/(a11*a22-a12*a21);
   		A = J*(-a12*(targ_rl - rl - rdot*tgo) + a22*(targ_rdot - rdot));
   		B = J*(a11*(targ_rl - rl - rdot*tgo) + a21*(targ_rdot - rdot));
   		C = J*(a12*(y + ydot*tgo) - a22*ydot);
   		D = J*(-a11*(y + ydot*tgo) - a21*ydot);
   		t0 = time;

			if (debug)
				System.out.println("J=" + J + " A=" + A + " B=" + B + " C=" + C + " D=" + D);
   	}


		// pg 9.4-13

		double dt = (time-t0)*(1d/TICKS_PER_SEC);
		double aTr = (rinhibit ? 0 : aT * (A + B*dt)) - geff;
		double aTy = (yinhibit ? 0 : aT * (C + D*dt));

		// required thrust direction
		// pg 9.4-15

		aTry.set(i_r);
		aTry.scale(aTr);
		aTry.scaleAdd(aTy, i_y, aTry);

		// pg 9.5-16

		double aTry_l = aTry.length();
		double aTz;
		if (aT < aTry_l)
		{
			aTry.scale(aT/aTry_l);
			aTz = 0;
		} else {
			aTz = Math.sqrt(aT*aT - aTry_l*aTry_l) * AstroUtil.sign(targ_zdot - zdot); //??zdot?
		}

		thrustvec.scaleAdd(aTz, i_z, aTry);
		thrustvec.normalize();

		if (debug)
			System.out.println("\taTr=" + aTr + ", aTy=" + aTy + ", aTz=" + aTz + ", tvec=" + thrustvec);

		// set att ctrl params

		if (firsttime)
			return; // we will be invalid the first time, so don't

		Orientation targort = new Orientation(thrustvec, r);

		// do roll, if nonzero
		if (pyr.lengthSquared() != 0)
		{
			// convert pyr coords to planet-reference frame
			Orientation planetort = telem.getPlanetRefOrientation();
			targort.mulInverse(planetort);
			Orientation pyrort = new Orientation();
			pyrort.setEulerPYR(pyr);
			pyrort.concat(planetort);
			targort.mul(pyrort);
		}

		attctrl.setTargetOrientation(targort);

	}

	//

	public double getA()
	{
		return A;
	}

	public void setA(double A)
	{
		this.A = A;
	}

	public double getB()
	{
		return B;
	}

	public void setB(double B)
	{
		this.B = B;
	}

	public double getC()
	{
		return C;
	}

	public void setC(double C)
	{
		this.C = C;
	}

	public double getD()
	{
		return D;
	}

	public void setD(double D)
	{
		this.D = D;
	}

	public double getTauBias()
	{
		return tau_bias;
	}

	public void setTauBias(double tau_bias)
	{
		this.tau_bias = tau_bias;
	}

	public double getKCoast()
	{
		return kcoast;
	}

	public void setKCoast(double kcoast)
	{
		this.kcoast = kcoast;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(IGMProgram.class);

	static {
		prophelp.registerGetSet("tau_bias", "TauBias", double.class);
		prophelp.registerGetSet("kcoast", "KCoast", double.class);
		prophelp.registerGetSet("A", "A", double.class);
		prophelp.registerGetSet("B", "B", double.class);
		prophelp.registerGetSet("C", "C", double.class);
		prophelp.registerGetSet("D", "D", double.class);
		prophelp.registerGet("tgo", "getTimeToGo");
		prophelp.registerGet("tlim", "getTimeToLimit");
		prophelp.registerGetSet("toff", "TimeToFreeze", float.class);
		prophelp.registerGet("y", "getCrossRangeDistance");
		prophelp.registerGet("rdot", "getRadialRate");
		prophelp.registerGet("ydot", "getCrossRangeRate");
		prophelp.registerGet("zdot", "getDownRangeRate");
		prophelp.registerGet("vgl", "getVelocityToBeGained");
		prophelp.registerGet("tau", "getTau");
		prophelp.registerGetSet("rinhibit", "RadialInhibit", boolean.class);
		prophelp.registerGetSet("yinhibit", "CrossRangeInhibit", boolean.class);
		prophelp.registerGetSet("targorbit", "TargetOrbit", Conic.class);
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
		} catch (PropertyRejectedException pre) {
			super.setProp(key, value);
		}
	}

	//

	static boolean debug = false;
}
