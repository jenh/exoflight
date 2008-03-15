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
package com.fasterlight.exo.test;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.vecmath.Vector3d;

public class Benchmark
implements Constants
{
	boolean debug = false;

	long t1,t2;
	int niters = 0;

	void startTest(long msec)
	{
		niters = 0;
		t1 = System.currentTimeMillis();
		t2 = t1+msec;
	}

	boolean testDone()
	{
		long t = System.currentTimeMillis();
		return (t >= t2);
	}

	void printResults()
	{
		// adjust for loop time
		long t3 = System.currentTimeMillis();
		for (int i=0; i<niters; i++)
			testDone();
		long t4 = System.currentTimeMillis();
		t2 -= (t4-t3);

		float rate = (niters*1000f)/(t2-t1);
		System.out.println( "Test ran " + niters + " iterations in " + (t2-t1) +
			" msec, " + AstroUtil.format(rate) + " iters/sec");
	}

	void doTest(long msec)
	{
		System.out.println("Kepler Test");
		double Uearth = 398753.97;
		Conic o = new Conic(new Vector3d(10000,500,300), new Vector3d(0,8.96,0), Uearth, 0);
		startTest(msec);
		while (!testDone())
		{
			o.getStateVectorAtTime(niters*100);
			niters++;
		}
		printResults();
	}

   public static void main(String[] args)
   throws Exception
   {
   	Benchmark os = new Benchmark();
   	for (int i=0; i<args.length; i++)
   	{
   		String s = args[i];
	   	if (args[i].equals("-d"))
   			os.debug = true;
   		else {
   		}
   	}
   	// fluff it
   	os.doTest(250);
   	// real test
   	os.doTest(5000);
   }
}