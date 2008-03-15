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
package com.fasterlight.exo.orbit.traj.test;

import junit.framework.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.CowellTrajectory;
import com.fasterlight.testing.*;
import com.fasterlight.vecmath.Vector3d;

public class MutableTrajectoryTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 5000;

	//

	public MutableTrajectoryTests(String name)
	{
		super(name);
		THRESHOLD = 1e-5;
	}

	public Orientation getRandomOrt()
	{
		Vector3d a = rnd.rndvec();
		Vector3d b = rnd.rndvec();
		return new Orientation(a,b);
	}

	//

	public void testMutable1()
	{
		SpaceGame game = new SpaceGame();
		game.start();

		for (int i=0; i<NUM_ITERS; i++)
		{
			Planet planet = (Planet)game.getBody("Earth");

			CowellTrajectory traj = new CowellTrajectory();
			traj.setParent(planet);

			Vector3d r = rnd.rndvec();
			Vector3d v = rnd.rndvec();
			traj.setStateVector(new StateVector(r, v));

			Orientation o1 = getRandomOrt();
			traj.setVelRefOrientation(o1);
			Orientation o2 = traj.getVelRefOrientation();

			compareAssert(o1.getDirection(), o2.getDirection());
		}
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(MutableTrajectoryTests.class);
		return suite;
	}

}
