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
package com.fasterlight.exo.orbit.nav;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * A RK4 integrator that works on 3-vectors (pos & vel).
  * Integrates given a specific acceleration function,
  * from a start to end time, and using a given number of
  * steps.  Can terminate when a user-defined condition is
  * reached.  Has an adaptive-timestep option (autointegrate method).
  */
public class PlanetIntegrator3d extends Integrator3d implements Constants
{
	Planet planet;
	double U;
	Atmosphere atmo;

	double BC;
	double lift;

	//

	public void setPlanet(Planet p)
	{
		this.planet = p;
		this.U = p.getMass() * GRAV_CONST_KM;
		this.atmo = p.getAtmosphere();
	}

	public void setPlanet(double U)
	{
		this.planet = null;
		this.U = U;
		this.atmo = null;
	}

	/**
	  * BC is (Cd/mass)
	  */
	public void setBC(double BC)
	{
		this.BC = BC;
	}

	public boolean getAccel(double t, Vector3d r, Vector3d v, Vector3d a)
	{
		// first compute gravity
		a.set(r);
		double rl2 = r.lengthSquared();
		double rl = Math.sqrt(rl2);
		a.scale(-U / (rl2 * rl)); // -U/r^3

		// now, lift & drag
		if (planet != null && atmo != null && BC != 0)
		{
			float alt = (float) (rl - planet.getRadius());
			if (alt <= 0)
				alt = 0;
			Atmosphere.Params res = atmo.getParamsAt(alt);
			Vector3d vd = new Vector3d(v);
			vd.sub(planet.getAirVelocity(r));

			double vvl = vd.length();
			double v2 = vvl * vvl * 1000 * 1000; // convert to m
			// todo: this should be 0.5, but this works better (!?!?!)
			double Q = 0.5 * res.density * v2;
			double T = Q * BC; // to km

			a.scaleAdd(-T / vvl, vd, a);
			if (debug)
				System.err.println("alt=" + alt + ", vvl=" + vvl + ", T=" + T);
		}

		return true;
	}

	//

	/***
		public static void main(String[] args)
		throws Exception
		{
			Game game = new Game();
			Universe u = new Universe(game);
			SolarSystem ss = new SolarSystem("solarsystem", game, u);
			Planet planet = (Planet)ss.getBody("Earth");

			PlanetIntegrator3d pi3d = new PlanetIntegrator3d();
			pi3d.setDebug(true);
			pi3d.setPlanet(planet);

			Vector3d r0,v0;
			r0 = new Vector3d(0,6378+100,0);
			v0 = new Vector3d(8.1,-8.1,0);
			r0 = new Vector3d(0,6378+100,0);
			v0 = new Vector3d(11.1,-11.1,0);
			r0 = new Vector3d(-541.808,-4187.825,4848.743);
			v0 = new Vector3d(4.4344,-0.86915,-0.82630);
			pi3d.setState(r0,v0);
			pi3d.setTime(0);
			pi3d.setBC(3.4438e-5);

			int iters = 1000;
			double dt = 5;
			for (int i=0; i<iters; i++)
			{
				pi3d.autointegrate();
				RKState s = pi3d.getState();
				System.out.println(s.a.x + " " +  s.a.y);
				Vector3d v = new Vector3d(s.a);
				v.scale(planet.getRadius()/v.length());
				System.out.println(v.x + " " + v.y);
				System.err.println("A=" + pi3d.getLastForce().length()/Constants.EARTH_G);
			}
		}
	***/

}
