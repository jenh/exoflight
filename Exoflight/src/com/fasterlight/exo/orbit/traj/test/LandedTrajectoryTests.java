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
import com.fasterlight.exo.orbit.traj.LandedTrajectory;
import com.fasterlight.testing.*;
import com.fasterlight.vecmath.Vector3d;

public class LandedTrajectoryTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 5000;

	//

	public LandedTrajectoryTests(String name)
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

	public void testLanded1()
	{
		SpaceGame game = new SpaceGame();
		game.start();

		for (int i=0; i<NUM_ITERS; i++)
		{
			Planet planet = (Planet)game.getBody("Earth");

			LandedTrajectory traj = new LandedTrajectory();
			traj.setParent(planet);

			Vector3d landpos = rnd.rndvec();
			landpos.z = planet.getRadius();
			traj.setLandPos(landpos);

			compareAssert(landpos, traj.getLandPos());

			// make sure sticking up
			{
				Vector3d r1 = traj.getPos(game.time());
				Vector3d r2 = traj.getOrt(game.time()).getDirection();
				compareAssert(r1.dot(r2)/r1.length(), 1);
			}

			Orientation o1 = getRandomOrt();
//			if (!Double.isNaN(o1.x+o1.y+o1.z+o1.w))
			{
				traj.setOrientation(o1);
				compareAssert(traj.getOrt(game.time()).getDirection(), o1.getDirection());

				double dot1 = traj.getPos(game.time()).dot(o1.getDirection());

				game.update(100000);
				Orientation o2 = traj.getOrt(game.time());
				double dot2 = traj.getPos(game.time()).dot(o2.getDirection());

//System.out.println(dot1 + " " + dot2);

				compareAssert(dot1, dot2);
			}
		}
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(LandedTrajectoryTests.class);
		return suite;
	}

}
