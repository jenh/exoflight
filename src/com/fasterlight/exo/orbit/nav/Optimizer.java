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

import java.util.*;

import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Optimizes TOF problems using a grid approach.
  * Tries to find the best combination of two variables
  * for a given constraint.
  *
  * The algorithm is:
  * 1. add the max search space (rectangle) to the queue
  * 2. compute the value of each corner
  * 3. assign a cost to that space, the sum of the costs of each corner / area
  * 4. pop the best (cheapest cost) node off of the queue
  * 5. subdivide into 4 quadrants, compute each
  */
public abstract class Optimizer
{
	Rect4f bounds;
	PriorityQueueVector pqv = new PriorityQueueVector();
	//	HashMap costs = new HashMap();
	Quadtree costs;
	Tuple2f bestpoint;
	double bestcost, maxcost;
	float minx, miny;
	int niters = 0;

	boolean debug = false;

	class SearchSpace implements Comparable
	{
		Rect4f r;
		double total;
		SearchSpace(Rect4f r)
		{
			this.r = r;
			double c1 = getCost(r.x1, r.y1);
			double c2 = getCost(r.x2, r.y1);
			double c3 = getCost(r.x1, r.y2);
			double c4 = getCost(r.x2, r.y2);
			double cc = getCost((r.x1 + r.x2) / 2, (r.y1 + r.y2) / 2);
			/*
			double avg = (c1+c2+c3+c4)/4;
			double d = cc-avg;
			double dev = AstroUtil.sign(d)*(d*d);
			this.total = avg+dev;
			*/
			double sum = c1 + c2 + c3 + c4 + cc * 4;
			double area = (r.x2 - r.x1) * (r.y2 - r.y1);
			this.total = sum / area;
		}
		boolean tooBig()
		{
			return (r.x2 - r.x1) < minx || (r.y2 - r.y1) < miny;
		}
		public String toString()
		{
			return "[" + r + ":" + total + "]";
		}
		public int compareTo(Object o)
		{
			return AstroUtil.sign(total - ((SearchSpace) o).total);
		}
	}

	public Optimizer(Rect4f bounds)
	{
		this.bounds = bounds;
		this.costs = new Quadtree(bounds);
		bestcost = 1e300;
		maxcost = -1e300;
		minx = miny = 1e-6f;
	}

	public Rect4f getBounds()
	{
		return bounds;
	}

	public void setMinXSize(float minx)
	{
		this.minx = minx;
	}

	public void setMinYSize(float miny)
	{
		this.miny = miny;
	}

	protected void addSpace(SearchSpace ss)
	{
		if (ss.tooBig())
			return;
		if (debug)
			System.out.println("adding " + ss);
		pqv.add(ss);
	}

	public double getCost(float x, float y)
	{
		Vector2f pos = new Vector2f(x, y);
		Double cost = (Double) costs.get(pos);
		if (cost == null)
		{
			double n = computeCost(pos.x, pos.y);
			if (n < bestcost)
			{
				bestcost = n;
				bestpoint = pos;
			}
			if (n > maxcost)
			{
				maxcost = n;
			}
			cost = new Double(n);
			//			costs.put(pos, cost);
			costs.put(cost, pos);
		}
		return cost.doubleValue();
	}

	public double getMinCost()
	{
		return bestcost;
	}

	public double getMaxCost()
	{
		return maxcost;
	}

	public double getInterpCost(float x, float y, float rad)
	{
		Rect4f r = new Rect4f(x - rad, y - rad, x + rad, y + rad);
		Vector v = costs.getEntriesInRect(r);
		Enumeration it = v.elements();
		double sum = 0;
		double denom = 0;
		float rad2 = rad * rad;
		while (it.hasMoreElements())
		{
			Map.Entry ent = (Map.Entry) it.nextElement();
			Tuple2f pt = (Tuple2f) ent.getValue();
			float r2 = (pt.x - x) * (pt.x - x) + (pt.y - y) * (pt.y - y);
			if (r2 < rad2)
			{
				float weight = rad2 - r2;
				sum += ((Double) ent.getKey()).doubleValue() * weight;
				denom += weight;
			}
		}
		if (denom == 0)
			return 0;
		return (sum / denom);
	}

	public Tuple2f getBestPoint()
	{
		return bestpoint;
	}

	public abstract double computeCost(float x, float y);

	public boolean iterate()
	{
		if (niters++ == 0)
			addSpace(new SearchSpace(bounds));
		if (pqv.isEmpty())
			return false;
		SearchSpace ss = (SearchSpace) pqv.remove();
		Vector2f cen = new Vector2f((ss.r.x1 + ss.r.x2) / 2, (ss.r.y1 + ss.r.y2) / 2);
		addSpace(new SearchSpace(new Rect4f(ss.r.x1, ss.r.y1, cen.x, cen.y)));
		addSpace(new SearchSpace(new Rect4f(cen.x, ss.r.y1, ss.r.x2, cen.y)));
		addSpace(new SearchSpace(new Rect4f(ss.r.x1, cen.y, cen.x, ss.r.y2)));
		addSpace(new SearchSpace(new Rect4f(cen.x, cen.y, ss.r.x2, ss.r.y2)));
		return true;
	}

	/**
	  * Returns a Vector of Map.Entry
	  * which contains (Double, Tuple2f)
	  */
	public Vector getPoints(Rect4f rect)
	{
		return costs.getEntriesInRect(rect);
	}

	public int getSize()
	{
		return niters;
	}

	public static void main(String[] args)
	{
		Rect4f bounds = new Rect4f(-10, -10, 10, 10);
		Optimizer opt = new Optimizer(bounds)
		{
			public double computeCost(float x, float y)
			{
				x -= 0.21;
				y -= 0.91;
				return (x * x + y * y);
			}
		};
		for (int i = 0; i < 100; i++)
			opt.iterate();
	}
}
