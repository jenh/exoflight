package com.fasterlight.exo.orbit.integ;

import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Stores info about a 6-element (position/velocity) integration step.
  */
public class RKState2
{
	public Vector3d a,b; // pos/vel, or vel/accel
	public RKState2()
	{
		this.a = new Vec3d();
		this.b = new Vec3d();
	}
	public RKState2(Vector3d a)	{
		this.a = new Vec3d(a);
		this.b = new Vec3d();
	}
	public RKState2(Vector3d a, Vector3d b)	{
		this.a = new Vec3d(a);
		this.b = new Vec3d(b);
	}
	public RKState2(RKState2 s) { this(s.a, s.b); }
	public void set(RKState2 s)
	{
		a.set(s.a);
		b.set(s.b);
	}
	public void add(RKState2 s) { a.add(s.a); b.add(s.b); }
	public void scale(double x) { a.scale(x); b.scale(x); }
	public String toString() { return "[" + a + ", " + b + "]"; }
	public void copyTo2(double[] arr)
	{
		arr[0] = a.x;
		arr[1] = a.y;
		arr[2] = a.z;
		arr[3] = b.x;
		arr[4] = b.y;
		arr[5] = b.z;
	}
	public void setFrom2(double[] x)
	{
		a.set(x[0], x[1], x[2]);
		b.set(x[3], x[4], x[5]);
	}
}
