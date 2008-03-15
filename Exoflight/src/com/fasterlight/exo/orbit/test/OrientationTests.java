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
package com.fasterlight.exo.orbit.test;

import junit.framework.*;

import com.fasterlight.exo.orbit.Orientation;
import com.fasterlight.testing.*;
import com.fasterlight.vecmath.*;

public class OrientationTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 20000;

	//

	public OrientationTests(String name)
	{
		super(name);
		THRESHOLD = 1e-5;
	}

	public Orientation getRandomOrt()
	{
		Vector3d a = rnd.rndvec();
		Vector3d b = rnd.rndvec();
		return new Orientation(a,b);
	}

	//

	public void testInitialization()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Vector3d a = rnd.rndvec();
			Orientation ort = new Orientation(a);
			a.normalize();
			compareAssert(a, ort.getDirection());

			Orientation ort2 = new Orientation(ort);
			compareAssert(ort.getDirection(), ort2.getDirection());

			Vector3d b = rnd.rndvec();
			ort2.set(b);
			compareAssert(ort2.getDirection(), b);
		}
	}

	public void testMultiply()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Orientation ort1 = getRandomOrt();
			Orientation ort2 = getRandomOrt();
			Orientation ort3 = new Orientation(ort1);
			ort1.mul(ort2);
			ort1.mulInverse(ort2);
			ort1.mul(ort1, ort2);
			ort1.mulInverse(ort1, ort2);
			compareAssert(ort1.getDirection(), ort3.getDirection());
		}
	}

	public void testTransform()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Orientation ort = getRandomOrt();
			Vector3d pt = rnd.rndspecialvec(0.1f);
			pt.scale(rnd.rnd(1e10));
			Vector3d pt2 = new Vector3d(pt);
			ort.transform(pt2);
			ort.invTransform(pt2);
			compareAssert(pt, pt2);
		}
	}

	public void testPYR()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Orientation ort = getRandomOrt();
			Vector3d pyr = rnd.rndvec();
			ort.setEulerPYR(pyr);
			Vector3d pyr2 = ort.getEulerPYR();
			compareAssert(pyr, pyr2);
		}
	}

	public void testMatrices()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Orientation ort = getRandomOrt();
			Matrix3d mat = ort.getMatrix();
			mat.invert();
			ort.invert();
			Matrix3d mat2 = ort.getMatrix();
			compareAssert(toArray(mat), toArray(mat2));

			Vector3d pt = rnd.rndspecialvec(0.1f);
			pt.scale(rnd.rnd(1e10));
			Vector3d pt2 = new Vector3d(pt);
			ort.transform(pt);
			mat2.transform(pt2);
			compareAssert(pt, pt2);
		}
	}

	public void testGetAxes()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			Orientation ort = getRandomOrt();
			Vector3d dir = ort.getDirection();
			Vector3d right = ort.getRightVector();
			Vector3d up = ort.getUpVector();
			Vector3d up2 = new Vector3d();
			up2.cross(dir,right);
			compareAssert(up, up2);
			ort.set(dir,up);
			compareAssert(dir, ort.getDirection());
			compareAssert(right, ort.getRightVector());
			compareAssert(up, ort.getUpVector());
		}
	}

	private static double[] toArray(Matrix3d m)
	{
		return new double[]
			{ m.m00,m.m01,m.m02,m.m10,m.m11,m.m12,m.m20,m.m21,m.m22 };
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(OrientationTests.class);
		return suite;
	}

}
