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

import java.io.BufferedReader;

import junit.framework.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.eph.*;
import com.fasterlight.io.IOUtil;
import com.fasterlight.testing.*;
import com.fasterlight.util.Util;

/**
  * The ephemeris library passes the DE405 tests from JPL
  * so TASTE IT.
  */
public class DE405Tests
extends NumericTestCase
{
	RandomnessProvider rnd = new RandomnessProvider();
	int NUM_ITERS = 20000;

	static SpaceGame game;

	//

	public void setUp()
	{
		if (false && game == null)
		{
			game = new SpaceGame();
			game.start();
		}
	}

	//

	public DE405Tests(String name)
	{
		super(name);
		THRESHOLD = 1e-8;
	}

	//

	private void doEphemerisTest(Ephemeris de405)
	throws Exception
	{
		// read test file
		BufferedReader br = IOUtil.getTextResource("com/fasterlight/exo/orbit/test/testpo.405");

		String line;
		// skip header
		while ( !br.readLine().startsWith("EOT") )
			;

		StateVector sv = new StateVector();
		StateVector svcen = new StateVector();
		int bodflags = de405.getBodiesSupported();


		// read each line
		while ( (line=br.readLine()) != null )
		{
			String ephid = line.substring(0, 3);
			double jed = Util.parseDouble(line.substring(16, 25).trim());
			if (ephid.equals("405") &&
				jed >= de405.getStartTime() && jed < de405.getEndTime())
			{
				int tn = Integer.parseInt(line.substring(26, 28).trim());
				int cn = Integer.parseInt(line.substring(29, 31).trim());
				int xn = Integer.parseInt(line.substring(32, 34).trim());
				double refcoord = Util.parseDouble(line.substring(36).trim());
				// get the coord set
				double testcoord;
				if ( (bodflags & (1<<tn)) != 0 &&
					( cn==0 || (bodflags & (1<<cn)) != 0 ) )
				{
					de405.getBodyStateVector(sv, tn, jed);
					if (cn != 0)
					{
						de405.getBodyStateVector(svcen, cn, jed);
						sv.r.sub(svcen.r);
						sv.v.sub(svcen.v);
					}
					// convert KM to au
					sv.r.scale(1d/Constants.AU_TO_KM);
					sv.v.scale(1d/Constants.AU_TO_KM);
					switch (xn) {
						case 1: testcoord = sv.r.x; break;
						case 2: testcoord = sv.r.y; break;
						case 3: testcoord = sv.r.z; break;
						case 4: testcoord = sv.v.x; break;
						case 5: testcoord = sv.v.y; break;
						case 6: testcoord = sv.v.z; break;
						default: throw new Exception("Test is hosed : xn=" + xn);
					}
					if (false && tn==10 && cn==3) {
						System.out.println(jed + " " + tn + " " + cn + " " + xn);
						sv.r.scale(Constants.AU_TO_KM);
						sv.v.scale(Constants.AU_TO_KM);
						System.out.println("  " + sv);
					}
					if (!compare(refcoord, testcoord))
					{
						System.out.println("test failed, line " + line);
						System.out.println("  " + refcoord + " != " + testcoord);
					}
				}
			}
		}
	}

	public void testDE405()
	throws Exception
	{
		String path = "eph/de405-1980.ser";
		DE405Ephemeris de405 = (DE405Ephemeris)IOUtil.readSerializedObject(path);
		de405.setTransformMoon(true);
		System.out.println("read " + de405);

		doEphemerisTest(de405);

	}

	public void testComposite()
	throws Exception
	{
		CompositeEphemeris composite = new CompositeEphemeris();

		for (int i=1980; i<=2020; i+=20)
		{
			String path = "eph/de405-" + i + ".ser";
			DE405Ephemeris de405 = (DE405Ephemeris)IOUtil.readSerializedObject(path);
			de405.setTransformMoon(true);
			System.out.println("read " + de405);
			composite.addEphemeris(de405);
		}
		System.out.println(composite);

		doEphemerisTest(composite);

	}

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(DE405Tests.class);
		return suite;
	}

}
