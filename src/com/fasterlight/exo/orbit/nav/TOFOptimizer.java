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
import com.fasterlight.util.Rect4f;
import com.fasterlight.vecmath.Vector3d;

/**
  * An Optimizer class that computes the best Lambert trajectory
  * for a particular set of constraints.
  */
public class TOFOptimizer extends Optimizer implements Constants
{
	public UniverseThing src, dest, ref;
	public long t1l, t1h, t2l, t2h;
	public double minrad;

	public static final double MAX_COST = 999;

	public TOFOptimizer(
		UniverseThing src,
		UniverseThing dest,
		UniverseThing ref,
		long t1l,
		long t1h,
		long t2l,
		long t2h)
	{
		super(new Rect4f(0, 0, 1, 1));
		this.t1l = t1l;
		this.t1h = t1h;
		this.t2l = t2l;
		this.t2h = t2h;
		this.src = src;
		this.dest = dest;
		this.ref = ref;
	}

	public void setMinRadius(double minrad)
	{
		this.minrad = minrad;
	}

	// transient-- danger-- todo
	long t1, t2;
	Vector3d r1, r2, v1, v2;

	Navigator solveNavigatorFor(float x, float y) throws NavigationException
	{
		// x is t1, y is t2
		t1 = x2time(x);
		t2 = y2time(y);

		// Vallado, pg. 456
		Navigator g = new Navigator();
		g.setSourceBody(src);
		g.setTargetBody(dest);
		g.setTimeOfDeparture(t1);
		g.setFlightTime(t2);
		//		g.setThreshold(0.001);
		boolean longway = Lambert.useLongWay(r1, v1, r2, v2);
		g.setLongWay(longway);
		g.solve();
		return g;
		/*
			Navigator[] garr = new Navigator[2];
			for (int i=0; i<2; i++)
			{
				Navigator g = new Navigator();
				g.setThreshold(0.001);
				boolean longway = (i==1);
				g.solve(r1, r2, ref.getMass()*GRAV_CONST_KM,
					t2*(1d/TICKS_PER_SEC), longway);
				garr[i] = g;
			}
			double cost0 = computeCost(garr[0]);
			double cost1 = computeCost(garr[1]);
			return (cost0 < cost1) ? garr[0] : garr[1];
		*/
	}

	public double computeCost(float x, float y)
	{
		try
		{
			Navigator g = solveNavigatorFor(x, y);
			return computeCost(g);
		} catch (NavigationException nave)
		{
			return MAX_COST;
		}
	}

	protected double computeCost(Navigator g)
	{
		// check if < min alt -- if so, return maximum cost
		double peri = g.getTransferOrbit().getPeriapsis();
		if (peri < minrad)
		{
			return MAX_COST + (minrad - peri);
		} else
		{
			//System.out.println(g.getConic().getPeriapsis());
		}
		// total cost is delta v (todo: support other constraints)
		return g.getTotalDV();
	}

	public Conic getConicFor(float x, float y) throws NavigationException
	{
		Navigator g = solveNavigatorFor(x, y);
		return g.getTransferOrbit();
	}

	public long x2time(float x)
	{
		return t1l + (long) ((t1h - t1l) * x);
	}

	public long y2time(float y)
	{
		return t2l + (long) ((t2h - t2l) * y);
	}

	public float time2x(long tick)
	{
		return (tick - t1l) * 1.0f / (t1h - t1l);
	}

	public float time2y(long tick)
	{
		return (tick - t2l) * 1.0f / (t2h - t2l);
	}

}
