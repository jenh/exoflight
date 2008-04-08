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

import com.fasterlight.exo.strategy.Mission;

public class MissionTests
extends TestCase
{
	public MissionTests(String name)
	{
		super(name);
	}

	//

	public void testCategories()
	{
		List categs = Mission.getCategories();
		for (int i=0; i<categs.size(); i++)
		{
			String categ = (String)categs.get(i);
			System.out.println(categ);
			List missions = Mission.getMissions(categ);
			for (int j=0; j<missions.size(); j++)
			{
				Mission m = (Mission)missions.get(j);
				System.out.println("\t"+m);
			}
		}
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(MissionTests.class);
		return suite;
	}

}
