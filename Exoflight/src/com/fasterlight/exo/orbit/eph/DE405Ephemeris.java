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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.util.Util;

/***
<pre>
	  This class contains the methods necessary to parse the JPL DE405
	  ephemeris files (text versions), and compute the position and velocity
	  of the planets, Moon, and Sun.

	  IMPORTANT: In order to use these methods, the user should:

		- save this class in a directory of his/her choosing;

		- save to the same directory the text versions of the DE405 ephemeris
		- files, which must be named "ASCPxxxx.txt", where xxxx represents the
		- start-year of the 20-year block;

		- have at least Java 1.1.8 installed.

 The input is the julian date (jultime) for which the ephemeris is needed.
 Note that only julian dates from 2414992.5 to 2524624.5 are supported.
 This input must be specified in the "main" method, which contains the call
 to "planetary_ephemeris".

	  GENERAL IDEA:  The "get_ephemeris_coefficients" method reads the
	  ephemeris file corresponding to the input julian day, and stores the
	  ephemeris coefficients needed to calculate planetary positions and
	  velocities in the array "ephemeris_coefficients".

	  The "get_planet_posvel" method calls "get_ephemeris_coefficients" if
	  needed, then calculates the position and velocity of the specified
	  planet.

	  The "planetary_ephemeris" method calls "get_planet_posvel" for each
	  planet, and resolves the position and velocity of the Earth/Moon
	  barycenter and geocentric Moon into the position and velocity of the
	  Earth and Moon.

	  Since the "ephemeris_coefficients" array is declared as an instance
	  variable, its contents will remain intact, should this code be modified
	  to call "planetary_ephemeris" more than once.  As a result, assuming
	  the julian date of the subsequent call fell within the same 20-year
	  file as the initial call, there would be no need to reread the
	  ephemeris file; this would save on i/o time.

	  The outputs are the arrays "planet_r" and "planet_rprime", also
	  declared as instance variables.

	  Several key constants and variables follow.  As noted, they are
	  configured for DE405; however, they could be adjusted to use the DE200
	  ephemeris, whose format is quite similar.
</pre>
***/

public class DE405Ephemeris
implements Ephemeris, java.io.Serializable, Comparable
{

	static final long serialVersionUID = 1550631246544033296L;


	/*  DECLARE CLASS CONSTANTS  */


	/*
	  Length of an A.U., in km
	*/
	static final double au = Constants.AU_TO_KM;


	/*  Declare Class Variables  */

	/*
	  Ratio of mass of Earth to mass of Moon
	*/
	static final double emrat = 81.30056;

	/*
	  Chebyshev coefficients for the DE405 ephemeris are contained in the
	  files "ASCPxxxx.txt".  These files are broken into intervals of length
	  "interval_duration", in days.
	*/
	static int interval_duration = 32;

	/*
	  Each interval contains an interval number, length, start and end
	  jultimes, and Chebyshev coefficients.  We keep only the coefficients.
	*/
	static int numbers_per_interval = 816;

	/*
	  For each planet (and the Moon makes 10, and the Sun makes 11), each
	  interval contains several complete sets of coefficients, each covering
	  a fraction of the interval duration
	*/
	static final int[] number_of_coef_sets = { 0, // FORTRAN legacy!
		4, 2, 2, 1, 1, 1, 1, 1, 1, 8, 2 };

	/*
	  Each planet (and the Moon makes 10, and the Sun makes 11) has a
	  different number of Chebyshev coefficients used to calculate each
	  component of position and velocity.
	*/
	static final int[] number_of_coefs = { 0, // FORTRAN legacy!
		14, 10, 13, 11, 8, 7, 6, 6, 6, 13, 11 };



	/*  DEFINE INSTANCE VARIABLES  */

	/*  Define ephemeris dates and coefficients as instance variables  */

	double[] ephemeris_coefficients = new double[187681];

	double start_time, end_time;

	/*
	  Define the positions and velocities of the major planets as instance
	  variables.  Note that the first subscript is the planet number, while
	  the second subscript specifies x, y, or z component.
	*/

	transient double[][] planet_r;
	transient double[][] planet_rprime;

	// temporary stuff

	transient double[] position_poly;
	transient double[] velocity_poly;

	// the Julian date which we have cached
	transient double cached_jed;

	// bits define which bodies are cached for 'last_jed'
	transient int cached_flags;

	public static final int UNITS_AU = 1;
	public static final int UNITS_KM = 0;

	boolean units_au = false;
	boolean xform_moon = true;
	boolean xform_earth = true;



	/**********************/

	/**
	  * Initialize the ephemeris.
	  * The coefficients are initialized to 0.
	  */
	public DE405Ephemeris()
	{
		init(); // to set up the arrays
	}

	private void init()
	{
		planet_r = new double[12][4];
		planet_rprime = new double[12][4];
		position_poly = new double[20];
		velocity_poly = new double[20];
		cached_jed = Double.MIN_VALUE;
	}

	private void readObject(java.io.ObjectInputStream stream)
	throws IOException, ClassNotFoundException, NotActiveException
	{
		init();
		stream.defaultReadObject();
	}

	/**
	  * Gets the minimum Julian time for this ephemeris.
	  * All dates passed in must be >= this value.
	  */
	public double getStartTime()
	{
		return start_time;
	}

	/**
	  * Gets the maximum Julian time for this ephemeris.
	  * All dates passed in must be < this value.
	  */
	public double getEndTime()
	{
		return end_time;
	}

	// which indexes are supported?
	public int getBodiesSupported()
	{
		// we support 1-11
		return ((1<<11)-1) << 1;
	}

	/**
	  * We can do units in UNITS_KM (default) or UNITS_AU.
	  */
	public void setUnits(int units)
	{
		units_au = (units == UNITS_AU);
		cached_jed = Double.MIN_VALUE;
	}

	/**
	  * Should we transform Earth by the Earth-Moon barycenter?
	  * (default = true)
	  */
	public void setTransformEarth(boolean b)
	{
		this.xform_earth = b;
	}

	/**
	  * Should we transform the Moon by the Earth-Moon barycenter?
	  * (default = true)
	  */
	public void setTransformMoon(boolean b)
	{
		this.xform_moon = b;
	}

	public void getBodyStateVector(StateVector sv, int bodyIndex, double julianTime)
	{
		// if time different, reset flags
		if (julianTime != this.cached_jed)
		{
			this.cached_jed = julianTime;
			this.cached_flags = 0;
		}
		// now compute what we need
		internalCompute(bodyIndex, julianTime);

		// set the result state vector
		sv.r.set(planet_r[bodyIndex][1], planet_r[bodyIndex][2], planet_r[bodyIndex][3]);
		sv.v.set(planet_rprime[bodyIndex][1], planet_rprime[bodyIndex][2], planet_rprime[bodyIndex][3]);
	}

	/**
	  * Computes the state vector for a body
	  * and sticks it into 'sv'.
	  */
	void internalCompute(int bodyIndex, double julianTime)
	{
		// if cached, return -- it is computed!
		if ( ((1<<bodyIndex) & cached_flags) != 0 )
			return;

		cached_flags |= (1<<bodyIndex);

		// run the gnarly algorithm
		get_planet_posvel(julianTime, bodyIndex,
			planet_r[bodyIndex], planet_rprime[bodyIndex]);


		/*  The positions and velocities of the Earth and Moon are found
		/*  indirectly.  We already have the pos/vel of the Earth-Moon
		/*  barycenter (i = 3).  We have also calculated planet_r(10,j), a
		/*  geocentric vector from the Earth to the Moon.  Using the ratio of
		/*  masses, we get vectors from the Earth-Moon barycenter to the Moon
		/*  and to the Earth.  */

		// note: this won't recurse b/c of the flags

//		System.out.println(xform_moon + " " + xform_earth);
	if (xform_moon || xform_earth)
	{
		if (bodyIndex == 10)
			internalCompute(3, julianTime);
		else if (bodyIndex == 3)
		{
			internalCompute(10, julianTime);
			for (int j=1;j<=3;j++) {
				double new3;
				new3 = planet_r[3][j] - planet_r[10][j]/(1 + emrat);
				if (xform_moon)
					planet_r[10][j] = new3 + planet_r[10][j];
				if (xform_earth)
					planet_r[3][j] = new3;
				new3 = planet_rprime[3][j] - planet_rprime[10][j]/(1 + emrat);
				if (xform_moon)
					planet_rprime[10][j] = new3 + planet_rprime[10][j];
				if (xform_earth)
					planet_rprime[3][j] = new3;
			}
		}
	}

	}


	/**********************/


	void get_planet_posvel(double jultime,int i,double ephemeris_r[],double ephemeris_rprime[])
	{

		/*
		  Procedure to calculate the position and velocity of planet i,
		  subject to the JPL DE405 ephemeris.  The positions and velocities
		  are calculated using Chebyshev polynomials, the coefficients of
		  which are stored in the files "ASCPxxxx.txt".

		  The general idea is as follows:  First, check to be sure the proper
		  ephemeris coefficients (corresponding to jultime) are available.
		  Then read the coefficients corresponding to jultime, and calculate
		  the positions and velocities of the planet.
		*/

		int interval = 0, numbers_to_skip = 0, pointer = 0, j = 0, k = 0,
			subinterval = 0, light_pointer = 0;

		double interval_start_time = 0, subinterval_duration = 0,
			chebyshev_time = 0;

		/*
		  Begin by determining whether the current ephemeris coefficients are
		  appropriate for jultime, or if we need to load a new set.
		*/
		if ((jultime < start_time) || (jultime > end_time))
			throw new IllegalArgumentException("jultime is outside bounds: " + jultime);

		interval = (int)(Math.floor((jultime - start_time)/interval_duration) + 1);
		interval_start_time = (interval - 1)*interval_duration + start_time;
		subinterval_duration = interval_duration/number_of_coef_sets[i];
		subinterval = (int)(Math.floor((jultime - interval_start_time)/subinterval_duration) + 1);
		numbers_to_skip = (interval - 1)*numbers_per_interval;

		/*
	  	  Starting at the beginning of the coefficient array, skip the first
	  	  "numbers_to_skip" coefficients.  This puts the pointer on the first
	  	  piece of data in the correct interval.
		*/
		pointer = numbers_to_skip + 1;

		/*  Skip the coefficients for the first (i-1) planets  */
		for (j=1;j<=(i-1);j++)
			pointer = pointer + 3*number_of_coef_sets[j]*number_of_coefs[j];

		/*  Skip the next (subinterval - 1)*3*number_of_coefs(i) coefficients  */
		pointer = pointer + (subinterval - 1)*3*number_of_coefs[i];

		int ncoeff = number_of_coefs[i];
		int p;

/***
		for (j=1;j<=3;j++) {
			for (k=1;k<=ncoeff;k++) {
				//  Read the pointer'th coefficient as the array entry coef[j][k]
				coef[j][k] = ephemeris_coefficients[pointer];
				pointer = pointer + 1;
				}
			}
***/

		/*
		Calculate the chebyshev time within the subinterval, between -1 and +1
		*/

		chebyshev_time = 2*(jultime - ((subinterval - 1)*subinterval_duration + interval_start_time))/subinterval_duration - 1;

		/*  Calculate the Chebyshev position polynomials   */
		position_poly[1] = 1;
		position_poly[2] = chebyshev_time;
		for (j=3;j<=number_of_coefs[i];j++)
			position_poly[j] = 2*chebyshev_time* position_poly[j-1] - position_poly[j-2];

		/*  Calculate the position of the i'th planet at jultime  */
		p = pointer;
		for (j=1;j<=3;j++) {
			ephemeris_r[j] = 0;
			for (k=1;k<=ncoeff;k++)
				ephemeris_r[j] += ephemeris_coefficients[p++]*position_poly[k];

			/*  Convert from km to A.U.  */
			if (units_au)
				ephemeris_r[j] /= au;
		}

		/*  Calculate the Chebyshev velocity polynomials  */
		velocity_poly[1] = 0;
		velocity_poly[2] = 1;
		velocity_poly[3] = 4*chebyshev_time;
		for (j=4;j<=number_of_coefs[i];j++)
			velocity_poly[j] = 2*chebyshev_time*velocity_poly[j-1] + 2*position_poly[j-1] - velocity_poly[j-2];

		/*  Calculate the velocity of the i'th planet  */
		p = pointer;
		for (j=1;j<=3;j++) {
			ephemeris_rprime[j] = 0;
			for (k=1;k<=ncoeff;k++)
				ephemeris_rprime[j] += ephemeris_coefficients[p++]*velocity_poly[k];

			/*  The next line accounts for differentiation of the iterative
			/*  formula with respect to chebyshev time.  Essentially, if dx/dt
			/*  = (dx/dct) times (dct/dt), the next line includes the factor
			/*  (dct/dt) so that the units are km/day */

			ephemeris_rprime[j] *= (2.0*number_of_coef_sets[i]/interval_duration);

			/*  Convert from km to A.U.  */
			if (units_au)
				ephemeris_rprime[j] = ephemeris_rprime[j]/au;

		}
	}


	/**
	  * Read the coefficients from an ASCII DE405 file.
	  */
	public int loadCoefficients(BufferedReader in, int records)
	throws IOException
	{

		/*
		  Procedure to read the DE405 ephemeris file corresponding to jultime.
		  The start and end dates of the ephemeris file are returned, as are
		  the Chebyshev coefficients for Mercury, Venus, Earth-Moon, Mars,
		  Jupiter, Saturn, Uranus, Neptune, Pluto, Geocentric Moon, and Sun.
		*/

		int mantissa, mantissa1, mantissa2, exponent, i, j;
		String s, line;

		this.cached_jed = Double.MIN_VALUE;

		// reset start, end dates
		start_time = Double.MAX_VALUE;
		end_time = Double.MIN_VALUE;

			/* Read each record in the file */
			for (j = 1; j <= records; j++)
			{

				/*  read line 1 and ignore  */
				line = in.readLine();
				if (line == null)
					break;

				/* read lines 2 through 274 and parse as appropriate */
				for (i=2;i<=274;i++)
				{
					line = in.readLine();
					if (line == null)
						break;
					if (i==2)
					{
						// update start & end times
						line = line.replace('D','e');
						double n1 = Util.parseDouble(line.substring(1,26).trim());
						double n2 = Util.parseDouble(line.substring(27,52).trim());
						start_time = Math.min(start_time, n1);
						end_time = Math.max(end_time, n2);
					}
  					if (i > 2)
  					{
  						/*  parse first entry  */
  						mantissa1 = Integer.parseInt(line.substring(4,13));
  						mantissa2 = Integer.parseInt(line.substring(13,22));
  						exponent = Integer.parseInt(line.substring(24,26));
  						if (line.substring(23,24).equals("+"))
  							ephemeris_coefficients[(j-1)*816 + (3*(i-2) - 1)] = mantissa1*Math.pow(10,(exponent-9)) + mantissa2*Math.pow(10,(exponent-18)) ;
  						else
  							ephemeris_coefficients[(j-1)*816 + (3*(i-2) - 1)] = mantissa1*Math.pow(10,-(exponent+9)) + mantissa2*Math.pow(10,-(exponent+18)) ;
  						if (line.substring(1,2).equals("-")) ephemeris_coefficients[(j-1)*816 + (3*(i-2) - 1)] = -ephemeris_coefficients[(j-1)*816 + (3*(i-2) - 1)];
  					}
  					if (i > 2)
  					{
  						/*  parse second entry  */
  						mantissa1 = Integer.parseInt(line.substring(30,39));
  						mantissa2 = Integer.parseInt(line.substring(39,48));
  						exponent = Integer.parseInt(line.substring(50,52));
  						if (line.substring(49,50).equals("+"))
  							ephemeris_coefficients[(j-1)*816 + 3*(i-2)] = mantissa1*Math.pow(10,(exponent-9)) + mantissa2*Math.pow(10,(exponent-18)) ;
  						else
  							ephemeris_coefficients[(j-1)*816 + 3*(i-2)] = mantissa1*Math.pow(10,-(exponent+9)) + mantissa2*Math.pow(10,-(exponent+18)) ;
  						if (line.substring(27,28).equals("-")) ephemeris_coefficients[(j-1)*816 + 3*(i-2)] = -ephemeris_coefficients[(j-1)*816 + 3*(i-2)];
  					}
  					if (i < 274)
  					{
  						/*  parse third entry  */
  						mantissa1 = Integer.parseInt(line.substring(56,65));
  						mantissa2 = Integer.parseInt(line.substring(65,74));
  						exponent = Integer.parseInt(line.substring(76,78));
  						if (line.substring(75,76).equals("+"))
  							ephemeris_coefficients[(j-1)*816 + (3*(i-2) + 1)] = mantissa1*Math.pow(10,(exponent-9)) + mantissa2*Math.pow(10,(exponent-18)) ;
  						else
  							ephemeris_coefficients[(j-1)*816 + (3*(i-2) + 1)] = mantissa1*Math.pow(10,-(exponent+9)) + mantissa2*Math.pow(10,-(exponent+18)) ;
  						if (line.substring(53,54).equals("-")) ephemeris_coefficients[(j-1)*816 + (3*(i-2) + 1)] = -ephemeris_coefficients[(j-1)*816 + (3*(i-2) + 1)];
  					}
				}

				/* read lines 275 through 341 and ignore */
				for (i=275;i<=341;i++)
					line = in.readLine();


			}

			in.close();
			return j; // return # of lines read

	}

	//

	public String toString()
	{
		return "[DE405Ephemeris, JD:" + start_time + " to " + end_time +
			", flags=" + Integer.toString(getBodiesSupported(),16) + "]";
	}

	public int compareTo(Object o)
	{
		Ephemeris eph = (Ephemeris)o;
		return AstroUtil.sign(getStartTime() - eph.getStartTime());
	}

}
