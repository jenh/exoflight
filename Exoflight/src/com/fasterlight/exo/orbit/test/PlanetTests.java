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

import java.util.Iterator;

import junit.framework.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.Planet;
import com.fasterlight.testing.*;
import com.fasterlight.vecmath.Vector3d;

public class PlanetTests
extends TestSuite
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 100;

	// JUnit sucks.
	static SpaceGame game;

	//

	public static void setUpTest()
	{
		if (game == null)
		{
			game = new SpaceGame();
			game.start();
		}
	}

	public void setUp()
	{
		setUpTest();
	}

	//

	public PlanetTests(String name)
	{
		super(name);
	}
	
	//

	class PlanetXYZTest
	extends NumericTestCase
	{
		Planet p;
		PlanetXYZTest(Planet p)
		{
			super(p.getName() + " XYZ test");
			this.p = p;
		}
		public void runTest()
		{
			assertTrue(p != null);
//			System.out.println(p + " " + p.geo2llr(new Vector3d(0,1,0), 0));
			for (int i=0; i<NUM_ITERS; i++)
			{
				Vector3d v1 = rnd.rndvec();
				Vector3d v2 = new Vector3d(v1);
				p.xyz2ijk(v2);
				p.ijk2llr(v2, 0);
				compareAssert(p.geo2llr(v1, 0), v2);
				compareAssert(p.llr2geo(v2, 0), v1);

				v2.set(v1);
				p.xyz2ijk(v2);
				p.ijk2xyz(v2);
				compareAssert(v1, v2);
				double time = rnd.rnd(-1e10, 1e10);
				p.ijk2llr(v2, time);
				p.llr2ijk(v2, time);
				compareAssert(v1, v2);
				p.ijk2llr(v2);
				p.llr2ijk(v2);
				compareAssert(v1, v2);

			}
		}
	}

	//

	class PlanetVelVecTest
	extends NumericTestCase
	{
		Planet p;
		PlanetVelVecTest(Planet p)
		{
			super(p.getName() + " velvec test");
			this.p = p;
		}
		public void runTest()
		{
			assertTrue(p != null);
			for (int i=0; i<NUM_ITERS; i++)
			{
				Vector3d v1 = rnd.rndvec();
				{
					Vector3d vel1 = p.getVelocityVector(v1);
					Vector3d vel2 = p.getVelocityVector2(v1);
					compareAssert(vel1, vel2);
				}
				{
					Vector3d vel1 = p.getAirVelocity(v1);
					Vector3d vel2 = p.getAirVelocity2(v1);
					compareAssert(vel1, vel2);
				}
			}
		}
	}

	void addPlanetTests()
	{
		setUp();

		Iterator it = game.getPlanets().iterator();
		while (it.hasNext())
		{
			Planet p = (Planet)it.next();
			addTest(new PlanetXYZTest(p));
			addTest(new PlanetVelVecTest(p));
		}
	}

	//

	public static Test suite()
	{
		PlanetTests suite = new PlanetTests(PlanetTests.class.getName());
		suite.addPlanetTests();
		return suite;
	}

}
