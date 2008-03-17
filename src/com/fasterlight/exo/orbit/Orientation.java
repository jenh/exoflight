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
package com.fasterlight.exo.orbit;

import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * Represents the orientation of an object using quaternions.
  */
public class Orientation extends Quat4d implements java.io.Serializable, PropertyAware
{
	/**
	  * The vector (0,0,1)
	  */
	public static final Vector3d UNIT_DIR = new Vec3d(0, 0, 1);
	/**
	  * The vector (0,1,0)
	  */
	public static final Vector3d UP_DIR = new Vec3d(0, 1, 0);

	/**
	  * Construct the unit orientation (points toward z-axis, <0,0,1>)
	  */
	public Orientation()
	{
		set(0, 0, 0, 1);
	}
	/**
	  * Clone another orientation
	  */
	public Orientation(Orientation o)
	{
		set(o);
	}
	/**
	  * Construct a quaternion that points in a given direction
	  */
	public Orientation(Vector3d dir)
	{
		this(dir, UP_DIR);
	}
	/**
	  * Construct a quaternion given an axis of rotation and an angle
	  */
	public Orientation(AxisAngle4d ax)
	{
		set(ax);
	}
	/**
	  * Constructs an orientation from two vectors
	  * that describe the plane of the new coordinate system.
	  */
	public Orientation(Vector3d lookat, Vector3d up)
	{
		set(lookat, up);
	}
	/**
	  * Construct with a quaternion
	  */
	public Orientation(Quat4d q)
	{
		set(q);
	}
	/**
	  * Set with a vector to look at and up vector is (0,1,0)
	  */
	public void set(Vector3d lookat)
	{
		set(lookat, UP_DIR);
	}

	// todo: inefficient (?) and doesn't JIT
	// todo: PROBLEMS it has large discontinuities
	public void set(Vector3d lookat, Vector3d up)
	{
		// normalize the lookat vector
		Vector3d z = new Vec3d(lookat);
		z.normalize();

		// x is the right vector
		Vector3d x = new Vec3d();
		x.cross(up, z);
		double lx = x.length();
		if (lx <= 0)
		{
			set(0, 0, 0, 1);
			return;
		}
		x.scale(1 / lx);

		// y is the up vector
		Vector3d y = new Vec3d();
		y.cross(z, x);
		double ly = y.length();
		if (ly <= 0)
		{
			set(0, 0, 0, 1);
			return;
		}
		y.scale(1 / ly);

		Matrix3d m = new Matrix3d(x.x, y.x, z.x, x.y, y.y, z.y, x.z, y.z, z.z);
		set(m);
		// todo: why is this neccessary?
		/*
		if (q.w < 0)
		{
			q.x = -q.x;
			q.y = -q.y;
			q.z = -q.z;
			q.w = -q.w;
		}
		*/
	}

	public void set(Vector3f lookat)
	{
		set(new Vec3d(lookat));
	}

	public void set(Vector3f lookat, Vector3f up)
	{
		set(new Vec3d(lookat), new Vec3d(up));
	}

	public void set(Orientation o)
	{
		super.set(o);
	}

	/**
	  * Concatenates this orienation by another one
	  * todo?? not?
	  */
	public void concat(Orientation o)
	{
		this.mul(o, this);
	}

	public void concatInverse(Orientation o)
	{
		Quat4d o2 = new Quat4d(o);
		o2.inverse();
		this.mul(o2, this);
	}

	// todo??
	public void mul(Vector3d angvel, double time)
	{
		double mag = angvel.length();
		if (mag == 0)
			return;
		double s = Math.sin(mag * time * Math.PI);
		double c = Math.cos(mag * time * Math.PI);
		Quat4d q2 = new Quat4d(angvel.x * s / mag, angvel.y * s / mag, angvel.z * s / mag, c);
		mul(q2, this);
	}

	public void mul(Vector3f angvel, float time)
	{
		mul(new Vec3d(angvel), time);
	}

	public void interpolate(Orientation o2, double amt)
	{
		interpolate(o2.getQuat(), amt);
	}

	/**
	  * Returns the direction of this orientation with respect to
	  * the positive-z axis
	  * todo: hotspot
	  */
	public Vector3d getDirection()
	{
		return new Vec3d(
			2 * x * z + 2 * w * y,
			2 * y * z - 2 * w * x,
			-x * x - y * y + z * z + w * w);
	}

	/**
	  * Returns the direction of this orientation with respect to
	  * the positive-y axis
	  */
	public Vector3d getUpVector()
	{
		return new Vec3d(
			2 * x * y - 2 * w * z,
			-x * x + y * y - z * z + w * w,
			2 * w * x + 2 * y * z);
	}

	/**
	  * Returns the direction of this orientation with respect to
	  * the positive-x axis
	  */
	public Vector3d getRightVector()
	{
		return new Vec3d(
			x * x - y * y - z * z + w * w,
			2 * x * y + 2 * w * z,
			2 * x * z - 2 * w * y);
	}

	/**
	  * Transforms a point by this orientation
	  */
	public void transform(Vector3d p)
	{
		Quat4d pt = new Quat4d(p.x, p.y, p.z, 0);
		pt.mul(this, pt);
		pt.mulInverse(this);
		p.set(pt.x, pt.y, pt.z);
	}

	/**
	  * Transforms a point by the inverse of this orientation
	  */
	public void invTransform(Vector3d p)
	{
		// todo: more efficient way?
		Matrix3d mat = this.getMatrix();
		mat.invert();
		mat.transform(p);
	}

	public void transform(Vector3f p)
	{
		Vector3d v = new Vec3d(p);
		transform(v);
		p.set(v);
	}

	public void invTransform(Vector3f p)
	{
		Vector3d v = new Vec3d(p);
		invTransform(v);
		p.set(v);
	}

	/**
	  * Returns the quaternion used by this orientation.
	  * WARNING modifying this modifies the orientation
	  */
	public Quat4d getQuat()
	{
		return this;
	}

	/**
	  * Returns the rotation matrix for this orientation.
	  * Inversion may be needed, depending on your application.
	  */
	public Matrix3d getMatrix()
	{
		Matrix3d m1 = new Matrix3d();
		m1.set(this);
		return m1;
	}

	/**
	  * Returns the rotation matrix for this orientation.
	  * Inversion may be needed, depending on your application.
	  */
	public Matrix3f getMatrixf()
	{
		Matrix3f m1 = new Matrix3f();
		m1.set(this);
		return m1;
	}

	/**
	  * Returns the inverted rotation matrix for this orientation.
	  */
	public Matrix3d getInvertedMatrix()
	{
		Matrix3d m1 = new Matrix3d();
		w = -w;
		m1.set(this);
		w = -w;
		return m1;
	}

	/**
	  * Returns the inverted rotation matrix for this orientation.
	  */
	public Matrix3f getInvertedMatrixf()
	{
		Matrix3f m1 = new Matrix3f();
		w = -w;
		m1.set(this);
		w = -w;
		return m1;
	}

	public void invert()
	{
		w = -w;
	}

	// we want YPR (yaw-pitch roll) which is YXZ in our system

	// these algorithms taken from NASA STI #77N26175,
	// "Euler Angles, Quaternions, and Transformations
	// Matrices for Space Shuttle Analysis"
	// But unfortunately, there is an error in page 17,
	// which contains the YXZ transformation that I use.
	// (instead of atan(m31/m33), should be (m13/m33))

	public Vector3d getEulerPYR()
	{
		double q1 = w;
		double q2 = x;
		double q3 = y;
		double q4 = z;
		//		double m12 = 2*(q2*q3-q1*q4);
		double m13 = 2 * (q1 * q3 + q2 * q4);
		double m21 = 2 * (q2 * q3 + q1 * q4);
		double m22 = (q1 * q1 - q2 * q2 + q3 * q3 - q4 * q4);
		double m23 = 2 * (q3 * q4 - q1 * q2);
		//		double m32 = 2*(q3*q4+q1*q2);
		//		double m31 = 2*(q2*q4-q1*q3);
		double m33 = (q1 * q1 - q2 * q2 - q3 * q3 + q4 * q4);

		// YPR (YXZ)
		double Y = -Math.atan2(m13, m33);
		double P = -Math.asin(-m23);
		double R = -Math.atan2(m21, m22);

		return new Vec3d(P, Y, R);
	}

	public void setEulerPYR(Vector3d pyr)
	{
		// YPR (YXZ)
		double sin2 = Math.sin(-pyr.x / 2);
		double sin1 = Math.sin(-pyr.y / 2);
		double sin3 = Math.sin(-pyr.z / 2);
		double cos2 = Math.cos(-pyr.x / 2);
		double cos1 = Math.cos(-pyr.y / 2);
		double cos3 = Math.cos(-pyr.z / 2);
		double q1 = sin1 * sin2 * sin3 + cos1 * cos2 * cos3;
		double q2 = sin1 * sin3 * cos2 + sin2 * cos1 * cos3;
		double q3 = sin1 * cos2 * cos3 - sin2 * sin3 * cos1;
		double q4 = -sin1 * sin2 * cos3 + sin3 * cos1 * cos2;

		set(q2, q3, q4, q1);
	}

	// PYR stuff

	public double getPitch()
	{
		return getEulerPYR().x;
	}

	public double getYaw()
	{
		return getEulerPYR().y;
	}

	public double getRoll()
	{
		return getEulerPYR().z;
	}

	public void setPitch(double a)
	{
		Vector3d v = getEulerPYR();
		v.x = a;
		setEulerPYR(v);
	}

	public void setYaw(double a)
	{
		Vector3d v = getEulerPYR();
		v.y = a;
		setEulerPYR(v);
	}

	public void setRoll(double a)
	{
		Vector3d v = getEulerPYR();
		v.z = a;
		setEulerPYR(v);
	}

	/**
	  * Returns the skew-symmetric matrix for this orientation.
	  */
	public Matrix3d getSkewSymMatrix()
	{
		Matrix3d m = new Matrix3d();
		double theta = Math.acos(w) * 2;
		double s = Math.sin(theta / 2);
		double w0, w1, w2;
		if (s != 0)
		{
			w0 = x / s;
			w1 = y / s;
			w2 = z / s;
		}
		else
		{
			w0 = x;
			w1 = y;
			w2 = z;
		}
		m.m01 = w2;
		m.m10 = -w2;
		m.m02 = w1;
		m.m20 = -w1;
		m.m12 = w0;
		m.m21 = -w0;
		return m;
	}

	/**
	  * Returns the axis that rotates to q1
	  * @see getAngVec
	  */
	public Vector3d getSlerpAxis(Orientation q1)
	{
		return getAngVec(this, q1);
	}

	/**
	  * like getSlerpAxis, but returns the *shortest* vector,
	  * since there are 2 quaternions for every rotation
	  */
	public Vector3d getShortestSlerpAxis(Orientation q1)
	{
		Vector3d a1 = getAngVec(this, q1);
		Quat4d q2 = new Quat4d(-q1.x, -q1.y, -q1.z, -q1.w);
		Vector3d a2 = getAngVec(this, q2);
		double l1 = a1.lengthSquared();
		double l2 = a2.lengthSquared();
		if (l1 < l2)
			return a1;
		else if (!Double.isNaN(l2))
			return a2;
		else
		{
			// todo: return NaN?
			System.out.println("getShortestSlerpAxis() NaN: " + a1 + " " + a2);
			return new Vec3d();
		}
	}

	/**
	  * Given 2 quaternions, q0 and q1, this returns the axis that rotates
	  * from q0 to q1 in 2*r time, where r is the length of the axis vector.
	  * Or if you like, half the velocity vector.
	  * (See Shoemake's "Quaternions" paper)
	  */
	public static Vector3d getAngVec(Quat4d q0, Quat4d q1)
	{
		Quat4d q3 = new Quat4d(q1);
		q3.mulInverse(q0);
		return AstroUtil.quatLog(q3);
	}

	// TRACKBALL

	public void rotateTrackball(float tbsize, float p1x, float p1y, float p2x, float p2y)
	{
		if (p1x == p2x && p1y == p2y)
			return;

		Vector3f p1 = new Vector3f(p1x, p1y, projectSphere(tbsize, p1x, p1y));
		Vector3f p2 = new Vector3f(p2x, p2y, projectSphere(tbsize, p2x, p2y));
		Vector3f a = new Vector3f();
		a.cross(p2, p1);
		p1.sub(p1, p2);
		double t = p1.length() / (2 * tbsize);
		if (t > 1.0)
			t = 1.0;
		if (t < -1.0)
			t = -1.0;
		float phi = (float) (2.0 * Math.asin(t));

		a.normalize();
		AxisAngle4d aa = new AxisAngle4d(a.x, a.y, a.z, phi);
		Quat4d q2 = new Quat4d();
		q2.set(aa);
		concat(q2, this, this);
	}

	/*
	 * Project an x,y pair onto a sphere of radius r OR a hyperbolic sheet
	 * if we are away from the center of the sphere.
	 */
	private static float projectSphere(float r, float x, float y)
	{
		double d, t, z;

		d = Math.sqrt(x * x + y * y);
		if (d < r * 0.70710678118654752440)
		{ /* Inside sphere */
			z = Math.sqrt(r * r - d * d);
		}
		else
		{ /* On hyperbola */
			t = r / 1.41421356237309504880;
			z = t * t / d;
		}
		return (float) z;
	}

	private static void concat(Quat4d q1, Quat4d q2, Quat4d dest)
	{
		Vector3d t1 = new Vector3d(q1.x, q1.y, q1.z);
		Vector3d t2 = new Vector3d(q2.x, q2.y, q2.z);
		double dot = t1.dot(t2);
		Vector3d t3 = new Vector3d();
		t3.cross(t2, t1);
		//		System.out.println("t1=" + t1 + ", t2=" + t2 + ", t3=" + t3 + ", dot=" + dot + ", dest=" + dest);
		Vector3d tf = new Vector3d();
		t1.scale(q2.w);
		t2.scale(q1.w);
		tf.add(t1, t2);
		tf.add(t3);
		//		System.out.println("t3=" + t3);
		dest.set(tf.x, tf.y, tf.z, q1.w * q2.w - dot);
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	static PropertyHelper prophelp = new PropertyHelper(Orientation.class);

	static {
		prophelp.registerGetSet("pitch", "Pitch", double.class);
		prophelp.registerGetSet("yaw", "Yaw", double.class);
		prophelp.registerGetSet("roll", "Roll", double.class);
		prophelp.registerGetSet("eulerpyr", "EulerPYR", Vector3d.class);
		prophelp.registerGet("dir", "getDirection");
		prophelp.registerGet("up", "getUpVector");
		prophelp.registerGet("right", "getRightVector");
	}

}
