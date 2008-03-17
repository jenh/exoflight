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

import java.util.*;

import junit.framework.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.Game;
import com.fasterlight.testing.*;

public class UtilTests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 20000;

	//

	public UtilTests(String name)
	{
		super(name);
	}

	//

	public void testDMS()
	{
		THRESHOLD = 1e-5;
		for (int i=0; i<NUM_ITERS; i++)
		{
			double rad1 = AstroUtil.fixAngle(rnd.rndangle());
			String dmsstr = AstroUtil.formatDMS(Math.toDegrees(rad1), (i>NUM_ITERS/2));
			double rad2 = AstroUtil.fixAngle(AstroUtil.DMSToRadians(dmsstr));
			compareAssert(rad1, rad2);
		}
	}

	public void testTime1()
	{
		THRESHOLD = 1e-3;
		compareAssert(AstroUtil.dbl2tick(Double.NaN), Game.INVALID_TICK);
		compareAssert(AstroUtil.tick2dbl(Game.INVALID_TICK), Double.NaN);
		for (int i=0; i<NUM_ITERS; i++)
		{
			double dt = rnd.rnd(-1e10, 1e10);
			compareAssert(AstroUtil.dbl2tick(dt)/(dt*Constants.TICKS_PER_SEC), 1);
			long tick = (long)dt;
			compareAssert(AstroUtil.tick2dbl(tick)/(tick/Constants.TICKS_PER_SEC), 1);
		}
	}

	public void testDates()
	{
		THRESHOLD = 1e-8;
		TimeZone tz = TimeZone.getTimeZone("GMT");
		GregorianCalendar cal = new GregorianCalendar(tz);
		cal.set(2000, 0, 1, 12, 0, 0);
		cal.set(cal.MILLISECOND, 0);
//		System.out.println(cal.getTime());
		java.util.Date date = AstroUtil.parseDate("01/01/2000 12:00:00 GMT");
//		System.out.println(date.getTime());
//		System.out.println(date.getTime() - Constants.JAVA_MSEC_J2000);
		compareAssert(AstroUtil.javaDateToGameTick(date), Constants.EPOCH_2000);
		compareAssert(AstroUtil.javaDateToSeconds(date), Constants.EPOCH_2000);
		assertEquals(AstroUtil.gameTickToJavaDate(Constants.EPOCH_2000), date);
	}

	public void testSinCosH()
	{
		THRESHOLD = 1e-8;
		for (int i=0; i<NUM_ITERS; i++)
		{
			double x = rnd.rnd(0,1);
			compareAssert(x, AstroUtil.asinh(AstroUtil.sinh(x)));
			compareAssert(x, AstroUtil.acosh(AstroUtil.cosh(x)));
		}
	}

	public void testLog2()
	{
		compareAssert(AstroUtil.log2(0), -1);
		compareAssert(AstroUtil.log2(1), 0);
		compareAssert(AstroUtil.log2(2), 1);
		compareAssert(AstroUtil.log2(3), 1);
		compareAssert(AstroUtil.log2(0x10000), 16);
		compareAssert(AstroUtil.log2(-1), 31);
		compareAssert(AstroUtil.log2(-1l), 63);
	}

	public void testLog2b()
	{
		for (int i=0; i<NUM_ITERS; i++)
		{
			int x = rnd.rnd(0,1<<30);
			int y1 = AstroUtil.log2(x);
			int y2 = AstroUtil.log2fast(x);
			compareAssert(y1,y2);
		}
	}

	public void testCountBits()
	{
		compareAssert(AstroUtil.countBits(0), 0);
		compareAssert(AstroUtil.countBits(1), 1);
		compareAssert(AstroUtil.countBits(0x10101), 3);
		compareAssert(AstroUtil.countBits(-1), 32);
		compareAssert(AstroUtil.countBits(-1l), 64);
	}

	public void testPad()
	{
		assertEquals("00", AstroUtil.pad("0", 2, '0'));
		assertEquals("00", AstroUtil.pad("00", 2, '*'));
		assertEquals("**", AstroUtil.pad("", 2, '*'));
		assertEquals("!!!123", AstroUtil.pad("123", 6, '!'));
		assertEquals("******", AstroUtil.pad("1234567890", 6, '?'));
	}

	public void testTimeHMS()
	{
		assertEquals("+000:00:00", AstroUtil.toTimeHMS(0));
		assertEquals("+000:00:00", AstroUtil.toTimeHMS(Constants.TICKS_PER_SEC-1));
		assertEquals("-000:00:01", AstroUtil.toTimeHMS(-1));
	}

	public void testParseTimeHMS()
	{
		THRESHOLD = 1e-8;
		long limit = Constants.TICKS_PER_SEC*3600l*999;
		for (int i=0; i<NUM_ITERS; i++)
		{
			long t = rnd.rnd(-limit, limit);
			String hms = AstroUtil.toTimeHMS(t);
			double t2 = AstroUtil.parseTimeHMS(hms);
			String hms2 = AstroUtil.toTimeHMS(AstroUtil.dbl2tick(t2));
			assertEquals(hms, hms2);
		}
	}

	public void testHM()
	{
		assertEquals("00:00", AstroUtil.formatHM(0, ":", ""));
	}

	public void testParseDuration()
	{
		THRESHOLD = 1e-3;
		double limit = -1e10;
		for (int i=0; i<NUM_ITERS; i++)
		{
			double t = rnd.rnd(-limit, limit);
			String s1 = AstroUtil.toDuration(t);
			double t2 = AstroUtil.parseDuration(s1);
			compareAssert(t/t2, 1);
		}
	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(UtilTests.class);
		return suite;
	}

}
