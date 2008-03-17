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
package com.fasterlight.exo.newgui;

import java.util.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.glout.ViewVolume;
import com.fasterlight.util.Rect4f;
import com.fasterlight.vecmath.*;

// todo: use ViewVolume class

/**
  * Computes the points necessary to draw a conic section in 3D
  * for a given viewpoint & frustum.
  */
public class OrbitPolyline
implements Constants
{
	TreeMap pts = new TreeMap();
	Conic o;
	Tuple3d origin;
	UniverseProjection oproj;
	float mindev;
	int mindivs=4;
	double ang1,ang2,period;
	boolean closed;
	Rect4f viewrect;
	int ttl = MAX_NUM_POINTS;
	double fixedrad;
	ViewVolume viewvol;

	static double BIGTIME = 1l<<28; //todo:const?
	static int MAX_NUM_POINTS = 2500;

	public OrbitPolyline(Conic o, UniverseProjection oproj, float mindeviation)
	{
		this.o = o;
		this.oproj = oproj;
		this.mindev = mindeviation;
		viewrect = oproj.getViewport();
		if (o.getSemiMajorAxis() > 0) // is ellipse?
		{
			// for ellipses
			ang1 = 0;
			ang2 = Math.PI*2;
			closed = true;
		} else {
			// for hyperbolas
			// what we do is try to figure out the endpoints of the hyperbola
			// by starting at a huge value around t0
			// and cutting it in half until both endpoints give valid points
			pts.clear();
			closed = false;
			// get maxtrueanom (Vallado 4-28)
			double maxtrueanom = Math.PI - Math.acos(1.0/o.getEccentricity()) - 1e-10;
			ang1 = -maxtrueanom;
			ang2 = maxtrueanom;
		}
	}

	public void setViewVolume(ViewVolume vvol)
	{
		this.viewvol = vvol;
	}

	public void setAngles(double ang1, double ang2)
	{
		this.ang1 = ang1;
		this.ang2 = ang2;
	}

	public void setFixedRadius(double rad)
	{
		this.fixedrad = rad;
	}

	public boolean isClosed()
	{
		return closed;
	}

	boolean isValidAngle(double ang)
	{
		Vector3d v = addPoint(ang);
		Tuple3f s1 = oproj.world2scrn(v);
		return (s1.x*s1.x+s1.y*s1.y) < BIGTIME;
	}

	public void setOrigin(Tuple3d origin)
	{
		this.origin = origin;
	}

	/**
	  * Returns the Vector3d objects in sorted-by-angle order
	  */
	public Collection getPoints()
	{
		compute();
		return pts.values();
	}

	public Vector3d addPoint(double angle)
	{
		Object key = new Double(angle);
		Object val = pts.get(key);
		if (val != null)
			return (Vector3d)pts.get(key);

		Vector3d pos = o.getElements_unsafe().getPosAtTrueAnom(angle);
		// adjust if less than cutoff rad
		if (fixedrad > 0)
		{
			pos.scale(fixedrad/pos.length());
		}
		if (origin != null)
			pos.add(origin);
//		if (!Double.isNaN(pos.lengthSquared()))
		{
			pts.put(key, pos);
		}
		return pos;
	}

	public int getPointQuad(Tuple3f p)
	{
		int n=0;
		if (p.x<viewrect.x1) n |= 1;
		else if (p.x>viewrect.x2) n |= 2;
		if (p.y<viewrect.y1) n |= 4;
		else if (p.y>viewrect.y2) n |= 8;
		if (p.z>1.0f) n |= 16;
		// todo?
		if (Float.isNaN(p.x) || Float.isNaN(p.y) || Float.isInfinite(p.x) || Float.isInfinite(p.y))
			n=-1;
		return n;
	}

	public void addSegment(double ang1, double ang2, int level)
	{
		if (ttl-- < 0)
			return;
		double t3 = (ang1+ang2)/2;
		if (debug)
			System.out.println(ang1 + " " + ang2 + " " + t3);
		Vector3d p1 = addPoint(ang1);
		Vector3d p2 = addPoint(ang2);
		boolean subdiv = (level > 0);
		if (!subdiv)
		{
			int pq1,pq2;
			Tuple3f s1,s2;
			// decide to use viewvol or universe proj
			if (viewvol != null) {
				pq1 = viewvol.getFrustumFlags(p1);
				pq2 = viewvol.getFrustumFlags(p2);
				if (pq1==pq2 && pq1!=0)
					return;
				s1 = oproj.world2scrn(p1);
				s2 = oproj.world2scrn(p2);
			} else {
				s1 = oproj.world2scrn(p1);
				s2 = oproj.world2scrn(p2);
				pq1 = getPointQuad(s1);
				pq2 = getPointQuad(s2);
   			// if both start and end of points are outside of view
   			// in same quadrant, don't continue subdividing
   			// todo: prevent lines from crossing thru screen
   			if ((pq1==pq2 && pq1!=0) || pq1<0 || pq2<0)
   				return;
			}
			// otherwise let's subdivide
			Vector3d p3 = addPoint(t3);
			Tuple3f s3 = oproj.world2scrn(p3);
			// todo: check for inside viewport
			// s1-s3,s3-s2
			Vector2f v1 = new Vector2f(s1.x, s1.y);
			Vector2f v2 = new Vector2f(s3.x, s3.y);
			v1.sub(new Vector2f(s3.x,s3.y));
			v2.sub(new Vector2f(s2.x,s2.y));
			float sdot = v1.dot(v2);
			float v1d = v1.length();
			float v2d = v2.length();
			float dev = (1-sdot/(v1d*v2d))*(v1d+v2d);
			if (dev > mindev && v1d+v2d > 1)
			{
				subdiv = true;
//				System.out.println(v1 + "\t" + v2 + "\t" + (sdot) + ", " + mindev);
			}
		}
		if (subdiv)
		{
			addSegment(ang1,t3,level-1);
			addSegment(t3,ang2,level-1);
		}
	}

	protected void compute()
	{
		pts.clear();
		addSegment(ang1, ang2, mindivs);
	}

	boolean debug = false;

	public static void main(String[] args)
	{
		UniverseProjection up = new UniverseProjection() {
			static final float s = 4;
			public Rect4f getViewport()
			{
				return new Rect4f(-100,-100,100,100);
			}
		   public Vector3f world2scrn(Tuple3d wpos)
		   {
		   	return new Vector3f((float)wpos.x*s,(float)wpos.y*s,(float)wpos.z*s);
		   }
	      public Vector3d scrn2world(Tuple3f spos)
	      {
	      	return new Vector3d(spos.x/s,spos.y/s,spos.z/s);
	      }
		};
		//todo: fix parabolic (1,0,0), (1,1,0)
		Conic o = new Conic(new Vector3d(1,0,0), new Vector3d(3,1,0), 1, 0);
		OrbitPolyline pl = new OrbitPolyline(o, up, 0.01f);
		System.out.println(pl.getPoints());
	}
}
