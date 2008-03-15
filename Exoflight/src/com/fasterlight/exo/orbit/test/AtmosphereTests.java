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

public class AtmosphereTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 100;

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

	public AtmosphereTests(String name)
	{
		super(name); //"Atmosphere Orbit Test");
		THRESHOLD = 1e-8;
//		num_debug = true;
	}

	public void testAtmo(String planet)
	{
		Atmosphere atmo = ((Planet)game.getBody(planet)).getAtmosphere();
		for (int i=-200; i<=2000; i+=10)
		{
			Atmosphere.Params p = atmo.getParamsAt(i);
//			System.out.println(i + ": " + p);
		}
	}

	public void testEarth()
	{
		testAtmo("Earth");
	}

	public void testMars()
	{
		testAtmo("Mars");
	}

	public void testVenus()
	{
		testAtmo("Venus");
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(AtmosphereTests.class);
		return suite;
	}

}
