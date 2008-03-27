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

import com.fasterlight.game.Settings;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * A RK4 integrator that works on 3-vectors (pos & vel).
  * Integrates given a specific acceleration function,
  * from a start to end time, and using a given number of
  * steps.  Can terminate when a user-defined condition is
  * reached.
  */
public abstract class Integrator3d
{
	RKState curstate = new RKState();
	double timestep = 1.0;
	double min_timestep = Settings.getDouble("Integrator", "MinTimestep", 1d/256);
	double max_timestep = Settings.getDouble("Integrator", "MaxTimestep", 256);
	double error_thresh = Settings.getDouble("Integrator", "Threshold", 1e-7);
	Vector3d lastforce = new Vector3d();
	double curtime;
	boolean stopped;

	//

	/**
	  * Get the acceleration for a Runge-Kutta step.
	  * Resulting acceleration will be set in 'a'
	  * [ example: a.set(v) ]
	  * Return false if must stop integration.
	  */
	public abstract boolean getAccel(double t, Vector3d r, Vector3d v, Vector3d a);

	protected RKState getF(double t, RKState s)
	{
		RKState s2 = new RKState(s.b);
		if (!getAccel(t, s.a, s.b, s2.b))
			stopped = true;
		return s2;
	}

	public Vector3d getR1()
	{
		return (curstate!=null) ? curstate.a : null;
	}

	public Vector3d getV1()
	{
		return (curstate!=null) ? curstate.b : null;
	}

	public void setState(Vector3d r, Vector3d v)
	{
		curstate.a.set(r);
		curstate.b.set(v);
	}

	public RKState getState()
	{
		return curstate;
	}

	public void setTime(double t)
	{
		this.curtime = t;
	}

	public double getTime()
	{
		return curtime;
	}

	public Vector3d getLastForce()
	{
		return lastforce;
	}

	public double getTimeStep()
	{
		return timestep;
	}

	public void setTimeStep(double ts)
	{
		this.timestep = Math.max(min_timestep, Math.min(max_timestep, ts));
	}

	public double getMinTimeStep()
	{
		return min_timestep;
	}

	public void setMinTimeStep(double ts)
	{
		this.min_timestep = ts;
	}

	public double getMaxTimeStep()
	{
		return max_timestep;
	}

	public void setMaxTimeStep(double ts)
	{
		this.max_timestep = ts;
	}

	public double getErrorThreshold()
	{
		return error_thresh;
	}

	public void setErrorThreshold(double error_thresh)
	{
		this.error_thresh = error_thresh;
	}

	//

	public void autointegrate()
	{
		RKState dy1 = getF(curtime, curstate);
		lastforce.set(dy1.b);
		RKState newstate = new RKState(curstate);

		// curts is the actual time step calculated in this step
		// while 'timestep' is the time step used for the next iteration
		double curts = timestep;
		do {
			// do crappy integration first
			// s = s + v*t + 0.5*a*t^2
			Vector3d r = new Vector3d(curstate.a);
			r.scaleAdd(0.5*curts*curts, dy1.b, r);
			r.scaleAdd(curts, dy1.a, r);

			// now do RK step
			solve(newstate, dy1, curtime, curts);

			// compute error
			r.sub(newstate.a);
			double error = r.lengthSquared();
			if (debug)
				System.err.println("t=" + curtime + ", ts=" + curts + ", error=" + error);

			// if error is above threshold, halve time step and repeat
			if (error > error_thresh) {
				if (curts/2 < min_timestep)
					break;
				curts /= 2;
				setTimeStep(curts);
				newstate.a.set(curstate.a);
				newstate.b.set(curstate.b);
			} else {
				// if error is below threshold/16, double time step
				if (error < error_thresh/16)
					setTimeStep(timestep*2);
				// in any case, leave loop
				break;
			}
		} while (true);

		// set curstate == newstate
		curstate.a.set(newstate.a);
		curstate.b.set(newstate.b);
		curtime += curts;
	}

	public void integrate(double h)
	{
		RKState dy1 = getF(curtime, curstate);
		lastforce.set(dy1.b);
		solve(curstate, dy1, curtime, h);
		curtime += h;
	}

	private void solve(RKState y0, RKState dy1, double t, double h)
	{
 		RKState ry2 = new RKState(dy1);
		ry2.scale(h/2);
		ry2.add(y0);
  		RKState dy2 = getF(t+h/2, ry2);

  		RKState ry3 = new RKState(dy2);
  		ry3.scale(h/2);
	  	ry3.add(y0);
  		RKState dy3 = getF(t+h/2, ry3);

	  	RKState ry4 = new RKState(dy3);
  		ry4.scale(h);
  		ry4.add(y0);
	  	RKState dy4 = getF(t+h, ry4);

  		RKState term = new RKState(dy1);
  		term.add(dy4);
	  	term.scale(1d/2);
  		term.add(dy2);
  		term.add(dy3);
	  	term.scale(h/3);

  		y0.add(term);
	}

	//

	/**
	  * Stores info about a Runge-Kutta step.
	  * (todo : merge with CowellTrajectory)
	  */
	class RKState
	{
		Vector3d a,b; // pos/vel, or vel/accel
		RKState()
		{
			this.a = new Vec3d();
			this.b = new Vec3d();
		}
		RKState(Vector3d a)	{
			this.a = new Vec3d(a);
			this.b = new Vec3d();
		}
		RKState(Vector3d a, Vector3d b)	{
			this.a = new Vec3d(a);
			this.b = new Vec3d(b);
		}
		RKState(RKState s) { this(s.a, s.b); }
		void add(RKState s) { a.add(s.a); b.add(s.b); }
		void scale(double x) { a.scale(x); b.scale(x); }
		public String toString() { return "[" + a + ", " + b + "]"; }
	}

	//

	boolean debug = false;

	public void setDebug(boolean b)
	{
		this.debug = b;
	}
}
