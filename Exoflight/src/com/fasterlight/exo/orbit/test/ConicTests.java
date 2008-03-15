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

public class ConicTests extends ConicTestBase
{
	public ConicTests(String name)
	{
		super(name);
	}

	//

	public void setUp()
	{
		THRESHOLD = 1e-6;
	}

	public void testElements2()
	{

		double oldthresh = THRESHOLD;
		THRESHOLD = 1e-5;
		try
		{
			for (int i = 0; i < NUM_ITERS; i++)
			{
				KeplerianElements ke1 = getRandomKE();
				KeplerianElements ke2 = new KeplerianElements(ke1);

				ke2.setSemiMajorAxis(ke1.getSemiMajorAxis());
				compareAssert("setSemiMajorAxis", ke1, ke2);
				ke2.setPeriapsis(ke1.getPeriapsis());
				compareAssert("setPeriapsis", ke1, ke2);
				ke2.setApoapsis(ke1.getApoapsis());
				compareAssert("setApoapsis", ke1, ke2);
				ke2.setEccentricity(ke1.getEccentricity());
				compareAssert("setEccentricity", ke1, ke2);
				ke2.setInclination(ke1.getInclination());
				compareAssert("setIncl", ke1, ke2);
				ke2.setRAAN(ke1.getRAAN());
				compareAssert("setLA", ke1, ke2);
				ke2.setArgPeriapsis(ke1.getArgPeriapsis());
				compareAssert("setArgPeri", ke1, ke2);
				ke2.setTrueAnomaly(ke1.getTrueAnomaly());
				compareAssert("setTrueAnom", ke1, ke2);
				if (ke2.getEccentricity() < 1)
				{
					ke2.setMeanAnomaly(ke1.getMeanAnomaly());
					compareAssert("setMeanAnom", ke1, ke2);
					ke2.setEccentricAnomaly(ke1.getEccentricAnomaly());
					compareAssert("setEccAnom", ke1, ke2);
				}
				ke2.setPeriapsisWRSAxis(ke1.getPeriapsis());
				compareAssert("setPeriapsisWRSAxis", ke1, ke2);
				ke2.setPeriapsisWRSApoapsis(ke1.getPeriapsis());
				compareAssert("setPeriapsisWRSApoapsis", ke1, ke2);
				ke2.setApoapsisWRSAxis(ke1.getApoapsis());
				compareAssert("setApoapsisWRSAxis", ke1, ke2);
				ke2.setApoapsisWRSPeriapsis(ke1.getApoapsis());
				compareAssert("setApoapsisWRSPeriapsis", ke1, ke2);
				ke2.setAxisWRSPeriapsis(ke1.getSemiMajorAxis());
				compareAssert("setAxisWRSPeriapsis", ke1, ke2);
				ke2.setAxisWRSApoapsis(ke1.getSemiMajorAxis());
				compareAssert("setAxisWRSApoapsis", ke1, ke2);
				ke2.setPeriapsisAndApoapsis(ke1.getPeriapsis(), ke1.getApoapsis());
				compareAssert("setPeriapsisAndApoapsis", ke1, ke2);
				ke2.setEccentricityWRSPeriapsis(ke1.getEccentricity());
				compareAssert("setEccentricityWRSPeriapsis", ke1, ke2);
				ke2.setEccentricityWRSApoapsis(ke1.getEccentricity());
				compareAssert("setEccentricityWRSApoapsis", ke1, ke2);
				ke2.setEccentricityWRSAxis(ke1.getEccentricity());
				compareAssert("setEccentricityWRSAxis", ke1, ke2);

				boolean res = compare(ke1, ke2);
				if (!res)
				{
					System.out.println("testElements2():");
					System.out.println("  ke1 = " + ke1);
					System.out.println("  ke2 = " + ke2);
				}
				assertTrue("testElements2(): KE -> State -> KE failed, iter " + i, res);
			}
		} finally
		{
			THRESHOLD = oldthresh;
		}

	}

	public void testElements3()
	{
		for (int i = 0; i < NUM_ITERS; i++)
		{
			KeplerianElements ke1 = getRandomKE();
			StateVector sv = ke1.getStateVectorAtEpoch();
			KeplerianElements ke2 = new KeplerianElements(sv.r, sv.v, ke1.getMu(), ke1.getEpoch());

			boolean res = compare(ke1, ke2);
			if (!res)
			{
				System.out.println("ke1 = " + ke1);
				System.out.println("ke2 = " + ke2);
			}
			assertTrue("testElements3(): KE -> State -> KE failed, iter " + i, res);
		}
	}

	public void testElements4()
	{
		for (int i = 0; i < NUM_ITERS; i++)
		{
			KeplerianElements ke1 = getRandomKE();
			double M = rnd.rndangle();
			Conic conic = new Conic(ke1);
			double t = conic.getTimeAtMeanAnomaly(M);
			double M2 = AstroUtil.fixAngle(ke1.getMeanAnomalyAtTime(t));

			//			System.out.println(M + " " + t + " " + M2);

			assertTrue("mean anomaly", compare(M, M2));
		}
	}

	//

	public void testConicTrinity()
	{
		for (int i = 0; i < NUM_ITERS; i++)
		{
			KeplerianElements ke1 = getRandomKE();
			StateVector sv1 = ke1.getStateVectorAtEpoch();
			Conic c1 = new Conic(ke1);
			Conic c2 = new Conic(sv1, ke1.getMu(), ke1.getEpoch());

			// test immutability
			KeplerianElements kemut = c2.getElements();
			kemut.setSemiMajorAxis(0.5);
			kemut.setEccentricity(0.5);
			kemut.setInclination(0.5);
			kemut.setArgPeriapsis(0.5);
			kemut.setRAAN(0.5);
			kemut.setTrueAnomaly(0.5);
			StateVector svmut = c2.getStateVectorAtEpoch();
			svmut.r.set(2, 3, 4);
			svmut.v.set(1, 2, 3);

			assertTrue("mu", compare(c1.getMu(), c2.getMu()));
			assertTrue("t0", compare(c1.getInitialTime(), c2.getInitialTime()));

			assertTrue("semimajor", compare(c1.getSemiMajorAxis(), c2.getSemiMajorAxis()));
			assertTrue("semilatus", compare(c1.getSemiLatusRectum(), c2.getSemiLatusRectum()));
			assertTrue("ecc", compare(c1.getEccentricity(), c2.getEccentricity()));
			//System.out.println(c1.getPeriod() + "  " + c2.getPeriod());
			assertTrue("period", compare(c1.getPeriod(), c2.getPeriod()));
			assertTrue("maxvel", compare(c1.getMaxVelocity(), c2.getMaxVelocity()));
			assertTrue("minvel", compare(c1.getMinVelocity(), c2.getMinVelocity()));
			assertTrue("apo", compare(c1.getApoapsis(), c2.getApoapsis()));
			assertTrue("peri", compare(c1.getPeriapsis(), c2.getPeriapsis()));
		}
	}

	//

	public void testConicGetTimesAtRadius()
	{
		// compare getTimesAtRadius() to kepler solve
		for (int i = 0; i < NUM_ITERS; i++)
		{
			KeplerianElements ke1 = getRandomKE();
			Conic c = ke1.getConic();
			double rad = rnd.rnd(ke1.getPeriapsis(), ke1.getApoapsis());

			double[] times = c.getTimesAtRadius(rad);
			for (int j = 0; i < times.length; i++)
			{
				double dt = times[j];
				StateVector sv = ke1.getStateVectorAtTime(dt);
				compareAssert(sv.r.length(), rad);
			}
		}
	}

	//

	public void testTLEs()
	{
		String tle1 = "1 27647U 03003A   03032.25000000  .00070100  74763-5  12018-3 0   634";
		String tle2 = "2 27647  39.0164 124.9099 0008781 145.1170 207.2382 16.01765594  2491";
		double U = Constants.EARTH_MASS * Constants.GRAV_CONST_KM;
		KeplerianElements kle = new KeplerianElements(tle1, tle2, U);
		KeplerianElements kle2 =
			new KeplerianElements(
				6635.528150378697,
				8.781E-4,
				Math.toRadians(39.0164),
				Math.toRadians(124.91),
				Math.toRadians(145.117),
				Math.toRadians(-152.80780924550922),
				U,
				99711435416d);
		compareAssert("testTLE failed", kle2, kle);
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(ConicTests.class);
		return suite;
	}

}
