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
package com.fasterlight.exo.orbit.eph;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
  * Utility to make binary ephemeri from DE405 files.
  */
public class MakeBinaryEphemeris
{
	static void makeBinary(String infile, String outfile)
	throws Exception
	{
		try {
		DE405Ephemeris de405 = new DE405Ephemeris();
		int nrecs = 230;
		BufferedReader inreader = new BufferedReader(
			new InputStreamReader(
			new GZIPInputStream(
			new FileInputStream(infile)
		)));
		de405.loadCoefficients(inreader, nrecs);
		de405.setUnits(de405.UNITS_KM);
		// we do this to make the moon's vector an Earth-moon vector,
		// not earth-barycenter
		de405.setTransformMoon(false);
		inreader.close();
		System.out.println(infile + " " + de405);

		OutputStream out = new BufferedOutputStream(
			new FileOutputStream(outfile));
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(de405);
		oout.close();
		out.close();
		System.out.println("Wrote " + outfile);
		} catch (FileNotFoundException ioe) {
			System.out.println(ioe);
		}
	}

	public static void main(String[] args)
	throws Exception
	{
		String dir = ".";
		if (args.length > 0)
			dir = args[0];
		for (int i=1600; i<=2200; i+=20)
		{
			String yr = ""+i;
			makeBinary(dir+"/ascp"+i+".405.gz", "de405-"+i+".ser");
		}
	}
}
