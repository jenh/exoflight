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
import com.fasterlight.exo.ship.*;
import com.fasterlight.exo.strategy.*;
import com.fasterlight.vecmath.*;

public class WindTunnelTest
implements Constants
{
	String vehiclename = "X-15";
	Vehicle vehicle;
	Agency agency;
	SpaceGame game;

	Structure struct;
	Orientation sort = new Orientation();
	Vector3d angvel = new Vector3d();

	Vector3f airvel = new Vector3f(0,0,1);
	float mach = 0.5f;

	double dt = 1d/60;

   //

  	public void doTest()
  	throws Exception
  	{
  		game = new SpaceGame();
  		agency = game.getAgency();

  		vehicle = Vehicle.getVehicle(vehiclename);
  		if (vehicle == null)
  			throw new Exception("Vehicle " + vehiclename + " not found!");

  		struct = vehicle.toStructure(agency);
		System.out.println("Structure: " + struct);
		System.out.println("Inertia: " + struct.getInertiaVector());
		double mass = struct.getMass();

		while (true)
		{
			Vector3f vel = new Vector3f(airvel);
			sort.invTransform(vel);
			AeroForces af = struct.calculateDragCoeff(vel, mach, game.time());

System.out.println();
			System.out.println("ort: " + sort);
			System.out.println("angvel: " + angvel);
			System.out.println("vel: " + vel);
			System.out.println("BC: " + af.BC + " area: " + af.area + " pf: " + af.pf);
			angvel.scaleAdd(dt/mass, af.pf.T, angvel);
			sort.mul(angvel, dt);
		}
  	}

  	boolean debug = false;


   public static void main(String[] args)
   throws Exception
   {
   	WindTunnelTest os = new WindTunnelTest();
   	for (int i=0; i<args.length; i++)
   	{
   		String s = args[i];
	   	if (args[i].equals("-d"))
   			os.debug = true;
   		else if (args[i].equals("-v"))
   		{
   			s = args[++i];
   			s = s.replace('_',' ');
   			os.vehiclename = s;
   		}
   	}
   	os.doTest();
   }
}
