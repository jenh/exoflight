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
package com.fasterlight.exo.orbit.test;

import junit.framework.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.testing.*;
import com.fasterlight.vecmath.Vector3d;

public class EphemerisTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 20000;

	// JUnit sucks.
	static SpaceGame game;

	//

	public void setUp()
	{
		if (game == null)
		{
			game = new SpaceGame();
			game.start();
		}
	}

	//

	public EphemerisTests(String name)
	{
		super(name);
		THRESHOLD = 1e-8;
//		num_debug = true;
	}

	//

	public void testJulian()
	{
		double jd = 2449493.333;
		double jd2 = AstroUtil.dbl2julian(AstroUtil.julian2dbl(jd));
		assertTrue(compare(jd, jd2));
	}

	public void testJulian2()
	{
		double jd = 2449493.333;
		long t2 = AstroUtil.dbl2tick(AstroUtil.julian2dbl(jd));
		double jd2 = AstroUtil.dbl2julian(AstroUtil.tick2dbl(t2));
		double jd3 = AstroUtil.tick2julian(t2);
		assertTrue(compare(jd, jd2));
		assertTrue(compare(jd, jd3));
	}

	public void testJupiterExample()
	{
		// Vallado pg 190, example
		Planet jupiter = (Planet)game.getBody("Jupiter");
		long time = AstroUtil.dbl2tick(AstroUtil.julian2dbl(2449493.333));
		System.out.println("time=" + time);
		// get state vector
		Vector3d pos = jupiter.getPosition(null, time);
		Vector3d vel = jupiter.getVelocity(null, time);
		// convert to AU
		pos.scale(1d/Constants.AU_TO_KM);
		// convert to AU/TU
		vel.scale(1d/Constants.AUTU_TO_KMSEC);
		// expected
		Vector3d exppos = new Vector3d(-4.075932,-3.578306,0.105970);
		Vector3d expvel = new Vector3d(0.004889,-0.005320,-0.000087);
		// compare
		if (!compare(pos, exppos) || !compare(vel, expvel))
		{
			System.out.println("\npos=" + pos + "\nexppos=" + exppos);
			System.out.println("\nvel=" + vel + "\nexpvel=" + expvel);
			System.out.println("\n|vel|=" + vel.length() + "\n|expvel|=" + expvel.length());
			fail("expected position or velocity different");
		}
	}

	public void testMoon()
	{
		Planet luna = (Planet)game.getBody("Luna");
		Planet earth = (Planet)game.getBody("Earth");
		long time = AstroUtil.julian2tick(2449000.5);
		System.out.println("TIME = " + AstroUtil.gameTickToJavaDate(time));
		Vector3d pos = luna.getPosition(earth, time);
		earth.ijk2xyz(pos);
		System.out.println("moonr=" + pos);
		System.out.println("moonv=" + luna.getVelocity(earth, time));
		System.out.println("earthr=" + earth.getPosition(null, time));
		System.out.println("earthv=" + earth.getVelocity(null, time));
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(EphemerisTests.class);
		return suite;
	}

}
