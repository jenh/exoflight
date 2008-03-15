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
package com.fasterlight.exo.orbit.nav;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.vecmath.*;

/**
  * A more full-featured Lambert optimizer that
  * handles interplanetary maneuvers.
  */
public class Navigator
implements Constants
{
	UniverseThing src, dest;
	long tod, tof;
	boolean longway;

	Vector3d r1,v1; // pos, vel of src
	Vector3d r2,v2; // pos, vel of dest

	Vector3d vt1,vt2; // vel of transfer orbit start, finish
	Conic tranorbit;
	double dv1;		// delta-v on first leg
	double dv2;		// delta-v on second leg

	//

	public void setSourceBody(UniverseThing body)
	{
		this.src = body;
	}

	public UniverseThing getSourceBody()
	{
		return src;
	}

	public void setTargetBody(UniverseThing body)
	{
		this.dest = body;
	}

	public UniverseThing getTargetBody()
	{
		return dest;
	}


	public long getTimeOfDeparture()
	{
		return tod;
	}

	public void setTimeOfDeparture(long tod)
	{
		this.tod = tod;
	}

	public long getFlightTime()
	{
		return tof;
	}

	public void setFlightTime(long tof)
	{
		this.tof = tof;
	}

	public void setLongWay(boolean longway)
	{
		this.longway = longway;
	}

	public boolean getLongWay()
	{
		return longway;
	}

	public Conic getTransferOrbit()
	{
		return tranorbit;
	}

	public double getDepartureDV()
	{
		return dv1;
	}

	public double getArrivalDV()
	{
		return dv2;
	}

	public double getTotalDV()
	{
		return dv1+dv2;
	}

	//

	public void solve()
	throws LambertException
	{
		UniverseThing srcparent = src.getParent();
		UniverseThing destparent = dest.getParent();

		if (srcparent.getParent() == destparent)
			solve2();
		else
			solve1();
//			throw new RuntimeException("Can't solve this targeting case");
	}

	// case where both bodies have the same parent
	public void solve1()
	throws LambertException
	{
		UniverseThing parent = src.getParent();
		double U = parent.getMass()*Constants.GRAV_CONST_KM;

		r1 = src.getPosition(parent, tod);
		r2 = dest.getPosition(parent, tod+tof);
		v1 = src.getVelocity(parent, tod);
		v2 = dest.getVelocity(parent, tod+tof);

		Lambert gauss = new Lambert();
		gauss.solve(r1, r2, U, AstroUtil.tick2dbl(tof), longway);

		tranorbit = gauss.getConic();
		tranorbit.setInitialTime(AstroUtil.tick2dbl(tod));
		vt1 = gauss.v1;
		vt2 = gauss.v2;
		dv1 = AstroUtil.vecdist(v1, vt1);
		dv2 = AstroUtil.vecdist(v2, vt2);
	}

	// case where dest body has same parent as src parent's parent
	// (Earth-Mars)
	public void solve2()
	throws LambertException
	{
		UniverseThing earth = src.getParent();
		UniverseThing sun = earth.getParent();

		double infrad = earth.getInfluenceRadius(tod);

		double sunU = sun.getMass()*GRAV_CONST_KM;
		double earthU = earth.getMass()*GRAV_CONST_KM;

		// get pole of source planet
		Vector3d earth_pole = ((Planet)earth).getSpinAxis();

		double start_rad = src.getTelemetry().getCENDIST(); //todo?

		Vector3d r1 = earth.getPosition(sun, tod);
		Vector3d r2 = dest.getPosition(sun, tod+tof);
		Vector3d v1 = earth.getVelocity(sun, tod);
		Vector3d v2 = dest.getVelocity(sun, tod+tof);

		Lambert gauss = new Lambert();
		gauss.solve(r1, r2, sunU, AstroUtil.tick2dbl(tof), longway);

		Vector3d velreqd = new Vector3d(gauss.v1);
		if (debug)
			System.out.println("Vel reqd = " + velreqd.length() + " km/s");

		Vector3d vinf = new Vector3d(velreqd);
		vinf.sub(v1);
		Vector3d vinfUnit = new Vector3d(vinf);
		vinfUnit.normalize();
		if (debug) {
			System.out.println("|vinf| = " + vinf.length() + " km/s");
			System.out.println("vinf = " + vinf);
		}

//		double decl = Math.PI/2 - Math.acos(vinfUnit.dot(earth_pole));
//		System.out.println("decl = " + Util.toDegrees(decl));

		//Now what speed to we need at peiapse in order to get the
	   //necessary speed for vInfinity?
      //NOTE: C3 = v^2  - (2mu/r)  =  vInfinity ^2
		//Assume we want to leave from a 300km altitude orbit around the earth...

      double rPeriapse = start_rad;
      double vSquared = vinf.lengthSquared()+ (2*earthU/rPeriapse);
      double vCirc = Math.sqrt(earthU/(rPeriapse)); // circular orbit velocity
      double dv = Math.sqrt(vSquared) - vCirc;

      if (debug) {
	      System.out.println("vCirc = " + vCirc + " km/s");
   	   System.out.println("delta v = " + dv + " km/s");
   	}

      //We know a lot about the departure orbit: The inclination can
      //be defined with the vInfinity vector, if we don't want a
      //plane change.  The RAAN is not yet determined.
      //The rPeriapse is user specified (above). We want to leave
      //from a circ orbit and we know the DV magnitude.
      //One thing we don't know is the  periapse location.
      //We need to find the angle from
      //periapse to the vInfinity asymptote.  Now,
      //maxTrueAnom = 180deg - acos(1/e) [Vallado 4-28]
      //Determine maxTrueAnom:
      //(1) Compute sma and ecc first:
      //Compute sma using vis-viva equation v^2 = mu (2/r - 1/a):

		double sma;
		if (Double.isInfinite(infrad))
		{
	      double oneOverSMA = -1.0 * vinf.lengthSquared()/earthU;
   	   sma = 1.0/oneOverSMA;
		} else {
			sma = earthU*infrad/(2*earthU - infrad*vinf.lengthSquared());
		}

      //Compute ecc  from rp = a(1-e) or r = a(1-e^2)/(1+ecosF).
      //Note that using that latter with true anomaly = 0 is the
      //same as the first.
      double oneMinusEcc = rPeriapse/sma;
      double ecc = 1.0 - oneMinusEcc;

      //(2) Determine maxTrueAnom:
      double maxTrueAnom = Math.PI - Math.acos(1.0/ecc);

      //(3) We would like to determine the argPeriapse and the RAAN:
      //We know
      //(a) vInfinity cross northPole points along RAAN
      //(b) orbital plane inertially defined by RAAN,vInfinity vectors
      //(c) argPeriapse dot vInfinity must be maxTrueAnom
      Vector3d raanDirection = new Vector3d();
		raanDirection.cross(vinf, earth_pole);
		raanDirection.normalize();
	/*
		// todo??
      if (vinfUnit.dot(earth_pole) < 0.0 )
        raanDirection.scale(-1);
   */

      if (debug) {
	      System.out.println("vinfUnit.dot(earth_pole) = " + vinfUnit.dot(earth_pole));
	      System.out.println("raanDir = " + raanDirection);
	   }

      Vector3d angMomDirection = new Vector3d();
		angMomDirection.cross(raanDirection, vinf);
		angMomDirection.normalize();

		if (debug)
			System.out.println("angMomDirection = " + angMomDirection);

      //rotate vector from vinf direction, counterclockwise
      //around angMomDirection to find argPeriapse location:
      AxisAngle4d aa = new AxisAngle4d(angMomDirection, -maxTrueAnom);
      Vector3d argPerLocation = new Vector3d(vinfUnit);
      Matrix3d mat = new Matrix3d();
      mat.set(aa);
		mat.transform(argPerLocation);

if (debug) {
	System.out.println("maxTrueAnom = " + AstroUtil.toDegrees(maxTrueAnom));
	System.out.println("argPerLocation = " + argPerLocation);
}

      //We know where arPerLocation is, and orbital plane. Find
      //argument of periapse, by first finding the preDeparture
      //orbit:
      Vector3d circVelDirection = new Vector3d();
      circVelDirection.cross(angMomDirection, argPerLocation);
      circVelDirection.normalize();

      Vector3d periapse = new Vector3d(argPerLocation);
      periapse.scale(start_rad/periapse.length());

      Vector3d postMnvrVel = new Vector3d(circVelDirection);
      postMnvrVel.scale(vCirc+dv);

      double jdNow = AstroUtil.tick2dbl(tod);
      tranorbit = new Conic(periapse, postMnvrVel, earthU, jdNow);

		vt1 = gauss.v1; // todo; wrong
		vt2 = gauss.v2;
		dv1 = dv;
		dv2 = AstroUtil.vecdist(v2, vt2);
	}

//

	boolean debug = false;
}
