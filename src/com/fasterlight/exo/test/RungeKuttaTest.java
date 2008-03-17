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

import java.io.*;

import com.fasterlight.exo.orbit.Constants;
import com.fasterlight.vecmath.Vector3d;

public class RungeKuttaTest
implements Constants
{
	PrintWriter out;

	State y0;
	double t0; // cur time
	double T; // end thrust
	double U; // U=grav
	double h; // step size
	double thrust;
	double BC=5;

	void print(Vector3d r0)
	{
		out.println(r0.x + "\t" + r0.y);
		Vector3d r1 = new Vector3d(r0);
		r1.normalize();
		out.println(r1.x + "\t" + r1.y);
//		out.println(t0 + "\t" + r0.length());
		out.flush();
	}

	class State
	{
		Vector3d r,v;
		State(Vector3d r, Vector3d v) { this.r=r; this.v=v; }
		State(DState ds) { this(new Vector3d(ds.v), new Vector3d(ds.a)); }
		void add(DState s) { r.add(s.v); v.add(s.a); }
		void add(State s) { r.add(s.r); v.add(s.v); }
		void scale(double x) { r.scale(x); v.scale(x); }
	}

	class DState
	{
		Vector3d v,a;
		DState(Vector3d v, Vector3d a) { this.v=v; this.a=a; }
		DState(DState ds) { this(new Vector3d(ds.v), new Vector3d(ds.a)); }
		void add(DState s) { v.add(s.v); a.add(s.a); }
		void add(State s) { v.add(s.r); a.add(s.v); }
		void scale(double x) { v.scale(x); a.scale(x); }
	}

	DState f(double t, State y)
	{
		// first do gravity
		Vector3d dv = new Vector3d(y.r);
		double r2 = dv.lengthSquared();
		dv.scale(-U/(r2*Math.sqrt(r2)));
		// now thrust (gravity turn)
		if (t0 < T)
		{
			double tdivT = 1-t0/T;
			double rang = Math.atan2(y.r.x, y.r.y);
			double dot = y.r.dot(y.v)/(y.v.length()*y.r.length());
			double theta = Math.asin(dot);
			double phi = Math.atan(tdivT*Math.tan(theta));
/*			if (phi < 0)
				phi = 0;*/
			Vector3d ta = new Vector3d(Math.cos(phi)*thrust, Math.sin(phi)*thrust, 0);
			if (debug)
				System.out.println("theta=" + Math.toDegrees(theta) +
					", phi=" + Math.toDegrees(phi) +
					", rang=" + Math.toDegrees(rang) +
					", thrust=" + ta);
			dv.add(ta);
		}
		// now drag
		Vector3d drag = new Vector3d(y.v);
		double density = Math.exp(1-r2);
		drag.scale(-BC*density*y.v.length());
		dv.add(drag);
		if (debug)
			System.out.println("density=" + density + ", drag=" + drag);

		return new DState(y.v, dv);
	}

	public void iterate(double thish)
	{
  		thrust = 1.45e-3;
  		T = 100;
  		y0 = new State(new Vector3d(0,1,0), new Vector3d(1e-5,1e-3,0));
  		t0=0;
  		U = 1e-3;
  		h = thish;
  		System.out.println("time step = " + h);

  		T = 0;
  		y0 = new State(new Vector3d(0,3,0), new Vector3d(2e-2,0,0));

  		double maxtime = 50000;

  		// loop

		while (t0 < maxtime)
		{
	  		print(y0.r);

  			// runge-kutta
  			DState dy1 = f(t0, y0);

	  		State ry2 = new State(dy1);
  			ry2.scale(h/2);
  			ry2.add(y0);
	  		DState dy2 = f(t0+h/2, ry2);

  			State ry3 = new State(dy2);
  			ry3.scale(h/2);
	  		ry3.add(y0);
  			DState dy3 = f(t0+h/2, ry3);

	  		State ry4 = new State(dy3);
  			ry4.scale(h);
  			ry4.add(y0);
	  		DState dy4 = f(t0+h, ry4);

  			DState term = new DState(dy1);
  			term.add(dy4);
	  		term.scale(1d/2);
  			term.add(dy2);
  			term.add(dy3);
	  		term.scale(h/3);

  			y0.add(term);
  			if (y0.r.lengthSquared() < (1-1e-5))
	  		{
  				System.out.println("Crashed at t=" + t0);
  				break;
	  		}
  			t0 += h;
		}
	}

  	public void doTest()
  	throws Exception
  	{
  		out = new PrintWriter( new BufferedWriter(new FileWriter("/tmp/gplot.out")));

		iterate(4);
		iterate(8);
		iterate(16);
		iterate(32);

   	out.close();
  	}

  	boolean debug = false;

   public static void main(String[] args)
   throws Exception
   {
   	RungeKuttaTest os = new RungeKuttaTest();
   	if (args.length > 0 && args[0].equals("-d"))
   		os.debug = true;
   	os.doTest();
   }
}