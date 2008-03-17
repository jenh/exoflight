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

import junit.framework.*;

import com.fasterlight.exo.orbit.traj.test.*;

public class AllTests
extends TestSuite
{
	public AllTests(String name)
	{
		super(name);
	}

	public static Test suite()
	{
		AllTests suite = new AllTests("All Exoflight tests");

		suite.addTest(com.fasterlight.util.test.StringUtilTests.suite());
		suite.addTest(com.fasterlight.util.test.UnitConverterTests.suite());

		suite.addTest(com.fasterlight.io.test.IOTests.suite());

		suite.addTest(com.fasterlight.exo.orbit.test.ConicTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.KeplerTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.PlanetTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.EphemerisTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.AtmosphereTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.DE405Tests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.UtilTests.suite());
		suite.addTest(com.fasterlight.exo.orbit.test.OrientationTests.suite());

		suite.addTest(LandedTrajectoryTests.suite());
		suite.addTest(MutableTrajectoryTests.suite());

		suite.addTest(com.fasterlight.exo.strategy.test.MissionTests.suite());
		suite.addTest(com.fasterlight.exo.strategy.test.VehicleTests.suite());

		suite.addTest(com.fasterlight.exo.test.JUnitMissionTest.suite());

		return suite;
	}
}
