/**
 * 
 */
package com.fasterlight.exo.orbit.integ;

import com.fasterlight.exo.orbit.Orientation;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

public class RKState4 extends RKState2
{
	public Orientation c; // orientation
	public Vector3d d; // angvel

	public RKState4()
	{
		super();
		c = new Orientation();
		d = new Vec3d();
	}
	public RKState4(Vector3d a, Vector3d b)
	{
		super(a, b);
		c = new Orientation();
		d = new Vec3d();
	}
	public RKState4(Vector3d a, Vector3d b, Orientation c, Vector3d d)
	{
		super(a, b);
		this.c = new Orientation(c);
		this.d = new Vec3d(d);
	}
	public RKState4(RKState2 s)
	{
		this(s.a, s.b);
	}
	public RKState4(RKState4 s)
	{
		this(s.a, s.b, s.c, s.d);
	}
	public void set(RKState4 s)
	{
		super.set(s);
		c.set(s.c);
		d.set(s.d);
	}
	public void add(RKState4 s)
	{
		super.add(s);
		c.add(s.c);
		d.add(s.d);
	}
	public void scale(double x)
	{
		super.scale(x);
		c.scale(x);
		d.scale(x);
	}
	public String toString()
	{
		return "[" + a + ", " + b + "," + c + "," + d + "]";
	}
	public void copyTo4(double[] arr)
	{
		super.copyTo2(arr);
		arr[6] = c.x;
		arr[7] = c.y;
		arr[8] = c.z;
		arr[9] = c.w;
		arr[10] = d.x;
		arr[11] = d.y;
		arr[12] = d.z;
	}
	public void setFrom4(double[] arr)
	{
		super.setFrom2(arr);
		c.x = arr[6];
		c.y = arr[7];
		c.z = arr[8];
		c.w = arr[9];
		d.x = arr[10];
		d.y = arr[11];
		d.z = arr[12];
	}
}