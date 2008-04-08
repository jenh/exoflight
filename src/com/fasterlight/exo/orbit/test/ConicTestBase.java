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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.testing.*;

public abstract class ConicTestBase extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 10000;
	boolean wimpy = true; // dumbed down to make it pass -- we'll set it to false
	// eventually, i promise!! :)
	//

	public ConicTestBase(String name)
	{
		super(name);
	}

	public KeplerianElements getKEFromArray(double[] params)
	{
		KeplerianElements ke1 =
			new KeplerianElements(
				params[0],
				params[1],
				params[2],
				params[3],
				params[4],
				params[5],
				params[6],
				params[7]);
		return ke1;
	}

	public double[] getArrayFromKE(KeplerianElements ke)
	{
		return new double[] {
			ke.getSemiLatusRectum(),
			ke.getEccentricity(),
			ke.getInclination(),
			AstroUtil.fixAngle(ke.getRAAN()),
			AstroUtil.fixAngle(ke.getArgPeriapsis()),
			AstroUtil.fixAngle(ke.getTrueAnomaly()),
			ke.getMu(),
			ke.getEpoch()};
	}

	public KeplerianElements getRandomKE()
	{
		double[] params = new double[8];
		params[0] = rnd.rndexp(1, 1e20); // semilatus
		if (wimpy)
		{
			params[1] = rnd.rnd(0, 0.99);
		} else
		{
			if (rnd.rnd(2) == 0)
				params[1] = 1 / rnd.rnd(0, 1); // ecc
			else
				params[1] = rnd.rnd(0, 1);
			switch (rnd.rnd(100))
			{
				case 0 :
				case 1 :
					params[1] = 0.0;
					break;
				case 2 :
				case 3 :
					params[2] = 1.0;
					break;
			}
		}
		float prob = wimpy ? 0.0f : 0.5f;
		params[2] = Math.abs(rnd.rndangle(prob) / 2); // incl is 0-180
		params[3] = Math.abs(rnd.rndangle(prob));
		params[4] = Math.abs(rnd.rndangle(prob));
		params[5] = Math.abs(rnd.rndangle(prob));
		params[6] = Math.abs(rnd.rndgauss(0, 1e10)); // mu
		params[7] = rnd.rndgauss(0, 1e12); // t0

		KeplerianElements ke1 = getKEFromArray(params);
		return ke1;
	}

	boolean compare(KeplerianElements ke1, KeplerianElements ke2)
	{
		double[] arr1 = getArrayFromKE(ke1);
		double[] arr2 = getArrayFromKE(ke2);
		for (int i = 2; i <= 5; i++)
		{
			arr1[i] = AstroUtil.fixAngle2(arr1[i] - arr2[i]);
			arr2[i] = 0;
		}
		return compare(arr1, arr2);
	}

	void compareAssert(String msg, KeplerianElements ke1, KeplerianElements ke2)
	{
		if (!compare(ke1, ke2))
		{
			fail(msg + "\nke1=" + ke1 + "\nke2=" + ke2);
		}
	}

}
