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

import com.fasterlight.exo.orbit.*;

public class KeplerTests
extends ConicTestBase
{
	public KeplerTests(String name)
	{
		super(name);
		wimpy = false;
		NUM_ITERS=50000;
//		THRESHOLD = 1e-7;
	}

	//

	public void testKeplerElliptical()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			double ecc = Math.abs(rnd.rnd(0, 0.999));
			double M = rnd.rndangle(0.05f);
			try {
				double E = Kepler.solveHybrid(ecc, M);
				double E2 = Kepler.solveNewton(ecc, M);
//System.out.println(ecc + " " + M + " " + E);
				compareAssert(E, E2);
				// convert back to mean anom.
				double M2 = E - ecc*Math.sin(E);
				compareAssert(AstroUtil.fixAngle(M), AstroUtil.fixAngle(M2));
			} catch (ConvergenceException exc) {
				fail("testKeplerElliptical() failed, " + exc);
			}
		}
	}

	public void testKeplerHyperbolic()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			double ecc = 1/rnd.rnd(1e-6, 0.999);
			double M = rnd.rndangle(0.05f);
			try {
				double H = Kepler.solveHybrid(ecc, M);
//System.out.println(ecc + " " + M + " " + H);
				// convert back to mean anom.
				double M2 = ecc*AstroUtil.sinh(H) - H;
				compareAssert(AstroUtil.fixAngle(M), AstroUtil.fixAngle(M2));
			} catch (ConvergenceException exc) {
				fail("testKeplerHyperbolic() failed, " + exc);
			}
		}
	}

	public void testKeplerNearParabolic()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			double ecc = rnd.rnd(0.999, 1.001);
			double M = rnd.rndangle(0.05f);
			try {
				double E = Kepler.solve(ecc, M);
System.out.println(ecc + " " + M + " " + E);
				// convert back to mean anom.
				double M2;
				if (ecc <= 1)
					M2 = E - ecc*Math.sin(E);
				else
					M2 = ecc*AstroUtil.sinh(E) - E;
				compareAssert(AstroUtil.fixAngle(M), AstroUtil.fixAngle(M2));
			} catch (ConvergenceException exc) {
				fail("testKeplerNearParabolic() failed, " + exc);
			}
		}
	}

/***
	public void testKeplerUniversal()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			// get 1st set of elements
			KeplerianElements ke1 = null;
			KeplerianElements ke2 = null;
			KeplerianElements ke3 = null;
			try {

				ke1 = getRandomKE();
				Conic conic = new Conic(ke1);
				Orbit orbit = new Orbit(ke1);

				double t = rnd.rnd(-1e10,1e10);
				Orbit.KeplerResult res = orbit.solveKepler(t);
				StateVector sv = conic.getStateVectorAtTime(t);

				// make new elements based on state vector
				ke1 = new KeplerianElements(sv.r, sv.v, orbit.getMu(), t);
				ke2 = new KeplerianElements(res.r, res.v, orbit.getMu(), t);

			} catch (Exception exc) {
				fail("testKeplerUniversal() failed, " + exc);
			}
			if (ke1 != null && ke2 != null)
			{
				boolean res = compare(ke1, ke2);
				if (!res)
				{
					System.out.println("\nke1=" + ke1 + "\nke2=" + ke2 + "\n");
				}
				assertTrue("testKeplerUniversal() failed", res);
			}
		}
	}
***/

	//


	public static Test suite()
	{
		TestSuite suite = new TestSuite(KeplerTests.class);
		return suite;
	}

}
