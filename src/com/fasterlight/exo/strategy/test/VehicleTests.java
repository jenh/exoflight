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
package com.fasterlight.exo.strategy.test;

import java.util.List;

import junit.framework.*;

import com.fasterlight.exo.strategy.Vehicle;

public class VehicleTests
extends TestCase
{
	public VehicleTests(String name)
	{
		super(name);
	}

	//

	public void testVehicles()
	{
		List vehicles = Vehicle.getVehicleList();
		for (int j=0; j<vehicles.size(); j++)
		{
			Vehicle m = (Vehicle)vehicles.get(j);
			System.out.println("\t"+m);
		}
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(VehicleTests.class);
		return suite;
	}

}
