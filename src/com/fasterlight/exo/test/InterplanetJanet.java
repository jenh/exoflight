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
package com.fasterlight.exo.test;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.nav.*;
import com.fasterlight.vecmath.*;


public class InterplanetJanet
implements Constants
{
	SpaceGame game;
	Universe u;

	//

	public void solve(Planet src, Planet dest,
		long tod, long tof, double start_alt, double infrad)
	throws NavigationException
	{
		Star sun = (Star)game.getBody("Sun");
		double sunU = sun.getMass()*GRAV_CONST_KM;
		double srcU = src.getMass()*GRAV_CONST_KM;

		// get pole of source planet
		Vector3d src_pole = src.getSpinAxis();
		System.out.println("src_pole = " + src_pole);
		//new Vector3d(0,0,1);
		//src.getOrientation(tod).transform(src_pole);

		double start_rad = start_alt + src.getRadius();

		Vector3d pos_src = src.getPosition(sun, tod);
		Vector3d pos_dest = dest.getPosition(sun, tod+tof);
		Vector3d vel_src = src.getVelocity(sun, tod);
		Vector3d vel_dest = dest.getVelocity(sun, tod+tof);

		Lambert lambert = new Lambert();
		lambert.setThreshold(1e-5);
		lambert.solve(pos_src, pos_dest, sunU, AstroUtil.tick2dbl(tof), false);

		Vector3d velreqd = new Vector3d(lambert.v1);
//		velreqd.sub(lambert.v1);
		System.out.println("Vel reqd = " + velreqd.length() + " km/s");

		Vector3d vinf = new Vector3d(velreqd);
		vinf.sub(vel_src);
		Vector3d vinfUnit = new Vector3d(vinf);
		vinfUnit.normalize();
		System.out.println("|vinf| = " + vinf.length() + " km/s");

		System.out.println("vinf = " + vinf);

		double decl = Math.PI/2 - Math.acos(vinfUnit.dot(src_pole));
		System.out.println("decl = " + Math.toDegrees(decl));

		//Now what speed to we need at peiapse in order to get the
	   //necessary speed for vInfinity?
      //NOTE: C3 = v^2  - (2mu/r)  =  vInfinity ^2
		//Assume we want to leave from a 300km altitude orbit around the earth...

      double rPeriapse = start_rad;
      double vSquared = vinf.lengthSquared()+ (2*srcU/rPeriapse);
      double vCirc = Math.sqrt(srcU/(rPeriapse)); // circular orbit velocity
      double dv = Math.sqrt(vSquared) - vCirc;

      System.out.println("vCirc = " + vCirc + " km/s");
      System.out.println("delta v = " + dv + " km/s");

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
	      double oneOverSMA = -1.0 * vinf.lengthSquared()/srcU;
   	   sma = 1.0/oneOverSMA;
		} else {
			sma = srcU*infrad/(2*srcU - infrad*vinf.lengthSquared());
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
		raanDirection.cross(vinf, src_pole);
		raanDirection.normalize();
	/*
		// todo??
      if (vinfUnit.dot(src_pole) < 0.0 )
        raanDirection.scale(-1);
   */

      System.out.println("vinfUnit.dot(src_pole) = " + vinfUnit.dot(src_pole));

      System.out.println("raanDir = " + raanDirection);

      Vector3d angMomDirection = new Vector3d();
		angMomDirection.cross(raanDirection, vinf);
		angMomDirection.normalize();

		System.out.println("angMomDirection = " + angMomDirection);

      //rotate vector from vinf direction, counterclockwise
      //around angMomDirection to find argPeriapse location:
      AxisAngle4d aa = new AxisAngle4d(angMomDirection, -maxTrueAnom);
      Vector3d argPerLocation = new Vector3d(vinfUnit);
      Matrix3d mat = new Matrix3d();
      mat.set(aa);
		mat.transform(argPerLocation);

System.out.println("maxTrueAnom = " + Math.toDegrees(maxTrueAnom));
System.out.println("argPerLocation = " + argPerLocation);

      //We know where arPerLocation is, and orbital plane. Find
      //argument of periapse, by first finding the preDeparture
      //orbit:
      Vector3d circVelDirection = new Vector3d();
      circVelDirection.cross(angMomDirection, argPerLocation);
      circVelDirection.normalize();

      Vector3d preMnvrVel = new Vector3d(circVelDirection);
     	preMnvrVel.scale(vCirc);

      Vector3d periapse = new Vector3d(argPerLocation);
      periapse.scale(start_rad/periapse.length());

      double jdNow = AstroUtil.tick2dbl(tod);

      Conic preDepart = new Conic(periapse, preMnvrVel, srcU, jdNow);
      preDepart = src.xyz2ijk(preDepart);

      Vector3d postMnvrVel = new Vector3d(circVelDirection);
      postMnvrVel.scale(vCirc+dv);

      Conic postDepart = new Conic(periapse, postMnvrVel, srcU, jdNow);
      postDepart = src.xyz2ijk(postDepart);

		System.out.println("\nConic: " + lambert.getConic().getElements());
      System.out.println("\nPre-elems: " + preDepart.getElements());
      System.out.println("\nPost-elems: " + postDepart.getElements());

      System.out.println("Depart date: " + AstroUtil.gameTickToJavaDate(
      	AstroUtil.dbl2tick(postDepart.getInitialTime())));
	}

   //Construct the application
   public InterplanetJanet()
   throws Exception
   {
      game = new SpaceGame();
//		game.setGameStartTime(new Date(69,7,16,12,26,00));
		game.setGameStartTime(-984342142772l + 20*60*1024);
		game.start();
		System.out.println("game tick = " + game.time());
    	u = game.getUniverse();

      // solve
      solve(
      	(Planet)game.getBody("Earth"),
      	(Planet)game.getBody("Mars"),
      	game.time(), AstroUtil.dbl2tick(260.0*86400),
      	300.0,
      	game.getBody("Earth").getInfluenceRadius(game.time()));

   }


	public static void main(String[] args)
	throws Exception
	{
		new InterplanetJanet();
	}

}