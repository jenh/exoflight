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

import java.text.*;
import java.util.*;

import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * A collection of utility methods for formatting,
  * parsing, and conversion.
  */
public class AstroUtil implements Constants
{
	/**
	 * Don't allow to be instantiated!
	 */
	private AstroUtil()
	{
	}

	static final NumberFormat DIST_M_FMT = new DecimalFormat("####.00 m");
	static final NumberFormat DIST_KM_FMT = new DecimalFormat("####.00 km");
	static final NumberFormat DIST_MKM_FMT = new DecimalFormat("######## km");
	static final NumberFormat DIST_AU_FMT = new DecimalFormat("####.000 AU");
	static final NumberFormat DIST_LY_FMT = new DecimalFormat("####.000 LY");

	static final NumberFormat MASS_KG_FMT = new DecimalFormat("###### kg");
	static final NumberFormat MASS_MKG_FMT = new DecimalFormat("###.000 Mkg");
	static final NumberFormat MASS_EU_FMT = new DecimalFormat("###.000 EU");

	static final NumberFormat DUR_S_FMT = new DecimalFormat("####.00 s");
	static final NumberFormat DUR_MIN_FMT = new DecimalFormat("####:00 m");
	static final NumberFormat DUR_HR_FMT = new DecimalFormat("####:00 h");
	static final NumberFormat DUR_DAY_FMT = new DecimalFormat("####.000 d");
	static final NumberFormat DUR_YR_FMT = new DecimalFormat("####.000 y");

	static final NumberFormat GENERIC_FMT = new DecimalFormat("###.000");
	static final NumberFormat GENERIC_FMT2 = new DecimalFormat("#######");

	public static final char DEGREES_SYM = (char) 176;
	public static final char MINUTES_SYM = '\'';
	public static final char SECONDS_SYM = '"';

	static final DateFormat DATE_FMT =
		new SimpleDateFormat("yyyy MMM dd HH:mm:ss z");
	static final DateFormat DATE_FMT2 =
		new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");

	//

	public static long dbl2tick(double x)
	{
		return Double.isNaN(x) ? INVALID_TICK : (long) (x * TICKS_PER_SEC);
	}

	public static double tick2dbl(long t)
	{
		return (t == INVALID_TICK) ? Double.NaN : t * (1d / TICKS_PER_SEC);
	}

	public static String format(double r)
	{
		if (Math.abs(r) < 1e4)
			return GENERIC_FMT.format(r);
		else
			return GENERIC_FMT2.format(r);
	}

	public static String format(Vector3d v)
	{
		return '(' + format(v.x) + ',' + format(v.y) + ',' + format(v.z) + ')';
	}

	public static String format(Vector3f v)
	{
		return '(' + format(v.x) + ',' + format(v.y) + ',' + format(v.z) + ')';
	}

	public static String formatDate(Date d)
	{
		return DATE_FMT.format(d);
	}

	public static java.util.Date parseDate(String s)
	{
		try
		{
			return DATE_FMT2.parse(s);
		}
		catch (Exception e)
		{
			try
			{
				return DATE_FMT.parse(s);
			}
			catch (Exception e2)
			{
				return null;
			}
		}
	}

	public static String toDegrees(double d)
	{
		return format(Util.toDegrees(d));
	}

	public static String formatDegrees(double d)
	{
		return format(d);
	}

	public static String toDistance(double r)
	{
		double ar = Math.abs(r);
		if (ar < 1)
			return DIST_M_FMT.format(r * 1000);
		else if (ar < 1000)
			return DIST_KM_FMT.format(r);
		else if (ar < 1e7)
			return DIST_MKM_FMT.format(r);
		else if (ar < 1e12)
			return DIST_AU_FMT.format(r / AU_TO_KM);
		else
			return DIST_LY_FMT.format(r / LIGHT_YEAR_KM);
	}

	public static String toMass(double m)
	{
		double am = Math.abs(m);
		if (am < 1e6)
			return MASS_KG_FMT.format(m);
		else if (am < 1e9)
			return MASS_MKG_FMT.format(m / 1e6);
		else
			return MASS_EU_FMT.format(m / EARTH_MASS);
	}

	public static String toDuration(double t)
	{
		double at = Math.abs(t);
		if (at < 300)
			return DUR_S_FMT.format(t); // sec.frac
		else if (at < 3600)
			return formatHM(t, "m ", "s"); // min:sec
		else if (at < SECS_PER_DAY)
			return formatHM(t / 60, "h ", "m"); // hr:min
		else if (at < DAYS_PER_YEAR * SECS_PER_DAY)
			return DUR_DAY_FMT.format(t / SECS_PER_DAY); // days.frac
		else
			return DUR_YR_FMT.format(t / (DAYS_PER_YEAR * SECS_PER_DAY));
		// yrs.frac
	}

	public static double acos(double x)
	{
		return Math.acos(x);
	}

	public static double asin(double x)
	{
		return Math.asin(x);
	}

	public static double sinh(double x)
	{
		return (Math.exp(x) - Math.exp(-x)) / 2;
	}

	public static double cosh(double x)
	{
		return (Math.exp(x) + Math.exp(-x)) / 2;
	}

	public static double tanh(double x)
	{
		return (Math.exp(2 * x) - 1) / (Math.exp(2 * x) + 1);
	}

	public static double asinh(double x)
	{
		return Math.log(Math.sqrt(x * x + 1) + x);
	}

	public static double acosh(double x)
	{
		return Math.log(x + Math.sqrt(x * x - 1));
	}

	/**
	  * Wraps the given angle to the range 0..PI*2
	  * Negative input angles are supported
	  */
	public static double fixAngle(double ang)
	{
		if (ang >= 0 && ang < Math.PI * 2)
			return ang;
		double x = ang / (Math.PI * 2);
		return (x - Math.floor(x)) * Math.PI * 2;
	}

	/**
	  * Wraps the given angle to the range -PI..PI
	  * Negative input angles are supported
	  */
	public static double fixAngle2(double ang)
	{
		if (ang >= -Math.PI && ang < Math.PI)
			return ang;
		return fixAngle(ang + Math.PI) - Math.PI;
	}

	public static double sqr(double x)
	{
		return x * x;
	}

	public static int sign2(double x)
	{
		return (x < 0) ? -1 : 1;
	}

	public static int sign(double x)
	{
		return (x < 0) ? -1 : (x > 0) ? 1 : 0;
	}

	public static int sign2(float x)
	{
		return (x < 0) ? -1 : 1;
	}

	public static int sign(float x)
	{
		return (x < 0) ? -1 : (x > 0) ? 1 : 0;
	}

	/**
	  * Returns the smallest number n such that (1<<n) >= x
	  * Treats x as an unsigned #
	  */
	public static int log2(int x)
	{
		int n = -1;
		while (x != 0)
		{
			x >>>= 1;
			n++;
		}
		return n;
	}

	/**
	  * Returns the smallest number n such that (1<<n) >= x
	  * Treats x as an unsigned #
	  */
	public static int log2(long x)
	{
		int n = -1;
		while (x != 0)
		{
			x >>>= 1;
			n++;
		}
		return n;
	}

	/**
	  * Returns the smallest number n such that (1<<n) >= x
	  * x must be > 0, or returns negative
	  */
	public static int log2fast(int v)
	{
		int fv = Float.floatToIntBits(v);
		return (fv >> 23) - 127;
	}

	/**
	  * Count the bits in an int
	  */
	public static int countBits(int x)
	{
		int n = 0;
		while (x != 0)
		{
			if ((x & 1) != 0)
				n++;
			x >>>= 1;
		}
		return n;
	}

	/**
	  * Count the bits in an long
	  */
	public static long countBits(long x)
	{
		int n = 0;
		while (x != 0)
		{
			if ((x & 1) != 0)
				n++;
			x >>>= 1;
		}
		return n;
	}

	/**
	  * 5th order Taylor series approx of sin(x)
	  * only valid from about -pi < x < pi
	  */
	public static float sin5(float x)
	{
		float x2 = x * x;
		return x * (x2 * x2 - 20 * x2 + 120) / 120;
	}

	/**
	  * 5th order Taylor series approx of cos(x)
	  * only valid for about -pi < x < pi
	  */
	public static float cos5(float x)
	{
		float x2 = x * x;
		return (x2 * x2 - 12 * x2 + 24) / 24;
	}

	//

	public static Date gameTickToJavaDate(long t)
	{
		return new Date(t * 1000 / TICKS_PER_SEC + Constants.JAVA_MSEC_2000);
	}

	public static long javaDateToGameTick(Date d)
	{
		return TICKS_PER_SEC * (d.getTime() - Constants.JAVA_MSEC_2000) / 1000;
	}

	public static double javaDateToSeconds(Date d)
	{
		return (d.getTime() - Constants.JAVA_MSEC_2000) / 1000d;
	}

	public static Date secondsToJavaDate(double t)
	{
		return new Date((long) (t * 1000 + Constants.JAVA_MSEC_2000));
	}

	/**
	  * Convert duration, in ticks, to -hhh:mm:ss (10 char string)
	  */
	public static String toTimeHMS(long t)
	{
		char plusminus;
		if (t < 0)
		{
			t = -t + TICKS_PER_SEC - 1;
			plusminus = '-';
		}
		else
		{
			plusminus = '+';
		}
		t /= TICKS_PER_SEC;
		int hr = (int) (t / 3600);
		int min = (int) ((t / 60) % 60);
		int s = (int) (t % 60);
		return plusminus
			+ pad(hr, 3, '0')
			+ ':'
			+ pad(min, 2, '0')
			+ ':'
			+ pad(s, 2, '0');
	}

	/**
	  * Convert string of format -hhh:mm:ss to floating point value
	  */
	public static double parseTimeHMS(String str)
	{
		boolean minus = str.startsWith("-");
		if (minus)
			str = str.substring(1);
		StringTokenizer st = new StringTokenizer(str, ": \t");
		double hr = 0;
		double min = 0;
		double s = 0;
		try
		{
			hr = Util.parseDouble(st.nextToken());
			min = Util.parseDouble(st.nextToken());
			s = Util.parseDouble(st.nextToken());
		}
		catch (Exception e)
		{
		}
		return (s + min * 60 + hr * 3600) * (minus ? -1 : 1);
	}

	private static final MessageFormat DUR_HM_FORMAT =
		new MessageFormat("{0,number,#}h {1,number,#}m");
	private static final MessageFormat DUR_MS_FORMAT =
		new MessageFormat("{0,number,#}m {1,number,#}s");

	/**
	  * Parses strings in form ###m ###s or ###h ###m to seconds.
	  */
	public static double parseDuration(String s)
	{
		s = s.trim().toLowerCase();
		try
		{
			Object[] objs = DUR_HM_FORMAT.parse(s);
			return ((Number) objs[0]).doubleValue() * 3600
				+ ((Number) objs[1]).doubleValue() * 60;
		}
		catch (Exception e)
		{
		}
		try
		{
			Object[] objs = DUR_MS_FORMAT.parse(s);
			return ((Number) objs[0]).doubleValue() * 60
				+ ((Number) objs[1]).doubleValue();
		}
		catch (Exception e)
		{
		}
		return UnitConverter.parse(s);
	}

	// converts secs since epoch -> Julian
	public static double dbl2julian(double t)
	{
		return Constants.J2000 + t * (1d / SECS_PER_DAY);
	}

	// converts Julian -> secs since epoch
	public static double julian2dbl(double jd)
	{
		return (jd - Constants.J2000) * SECS_PER_DAY;
	}

	// converts secs since epoch -> Julian
	public static double tick2julian(long t)
	{
		return Constants.J2000 + t * (1d / TICKS_PER_DAY);
	}

	// converts Julian -> secs since epoch
	public static long julian2tick(double jd)
	{
		return (long) ((jd - Constants.J2000) * TICKS_PER_DAY);
	}

	/**
	  * Formats an hour or minute value into HH:MM or MM:SS,
	  * with a user-given delimiter between the HH and MM.
	  * 'x' is the number of hours or minutes.
	  */
	public static String formatHM(double x, String delim1, String delim2)
	{
		boolean minus;
		if (x < 0)
		{
			x = -x;
			minus = true;
		}
		else
		{
			minus = false;
		}
		int y = ((int) x / 60);
		int z = ((int) x % 60);
		return (minus ? "-" : "")
			+ (y < 10 ? "0" : "")
			+ y
			+ delim1
			+ (z < 10 ? "0" : "")
			+ z
			+ delim2;
	}

	/**
	  * Pads a string at the beginningto a given length.
	  * If string is longer than 'nchars', returns
	  * string of '*' of length 'nchars'.
	  */
	public static String pad(String s, int nchars, char padchar)
	{
		int sl = s.length();
		if (sl > nchars)
			return pad("", nchars, '*');
		else if (sl == nchars)
			return s;
		else
		{
			char[] arr = new char[nchars];
			for (int i = 0; i < sl; i++)
				arr[i + nchars - sl] = s.charAt(i);
			for (int i = sl; i < nchars; i++)
				arr[i - sl] = padchar;
			return new String(arr);
		}
	}

	public static String pad(int i, int nchars, char padchar)
	{
		return pad(Integer.toString(i), nchars, padchar);
	}

	/**
	  * Adds 'nchars' zeroes to 'st'.
	  * If nchars <= 0, does nothing.
	  */
	public static void addzeros(StringBuffer st, int nchars)
	{
		while (nchars-- > 0)
			st.append('0');
	}

	public static double dms2rad(double d, double m, double s)
	{
		return Util.toRadians(sign(d) * (s / 3600 + m / 60 + Math.abs(d)));
	}

	public static float[][] to2dArray(Matrix3d m1)
	{
		float[][] m = new float[4][4];
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
			{
				m[y][x] = (float) (m1.getElement(y, x));
			}
		m[3][3] = 1f;
		return m;
	}

	public static float[] toArray(Matrix3f m1)
	{
		float[] m = new float[4 * 4];
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
			{
				m[y * 4 + x] = m1.getElement(y, x);
			}
		m[15] = 1f;
		return m;
	}

	public static float[] toArray(Matrix3d m1)
	{
		float[] m = new float[4 * 4];
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
			{
				m[y * 4 + x] = (float) m1.getElement(y, x);
			}
		m[15] = 1f;
		return m;
	}

	public static float[][] to2dArray(Matrix3f m1)
	{
		float[][] m = new float[4][4];
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
			{
				m[y][x] = m1.getElement(y, x);
			}
		m[3][3] = 1f;
		return m;
	}

	// logarithm of a quaternion
	// from http://www.cs.technion.ac.il/Labs/Isl/projects_done/quaternion/quat_interpol_project.html
	public static Vector3d quatLog(Quat4d q)
	{
		Vec3d v = new Vec3d(q.x, q.y, q.z);
		if (q.w == 0)
		{
			v.scale(Math.PI / 2);
			return v;
		}
		else
		{
			double norm = Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z);
			double a = Math.atan2(norm, q.w);
			v.scale(a / norm);
		}
		return v;
	}

	// exponent quaternion of a vector
	// from http://www.cs.technion.ac.il/Labs/Isl/projects_done/quaternion/quat_interpol_project.html
	public static Quat4d quatExp(Vector3d v)
	{
		if (v.x == 0 && v.y == 0 && v.z == 0)
		{
			return new Quat4d(0, 0, 0, 1);
		}
		double norm = v.length();
		double a = Math.sin(norm) / norm;
		return new Quat4d(a * v.x, a * v.y, a * v.z, Math.cos(norm));
	}

	// x is in degrees
	// maximum of 10 chars
	public static String formatDMS(double x, boolean isLat)
	{
		StringBuffer st = new StringBuffer();
		char dir;
		if (x < 0)
		{
			x = -x;
			dir = (isLat ? 'S' : 'W');
		}
		else
			dir = (isLat ? 'N' : 'E');

		st.append(pad((int) x, 3, '0'));
		st.append(dir);
		//		st.append(DEGREES_SYM);
		st.append(pad((int) ((x - Math.floor(x)) * 60), 2, '0'));
		st.append(MINUTES_SYM);
		x *= 60;
		st.append(pad((int) ((x - Math.floor(x)) * 60), 2, '0'));
		st.append(SECONDS_SYM);

		return st.toString();
	}

	public static double DMSToRadians(String s)
	{
		int sign = 1;

		int l = s.length();
		char signChar = s.charAt(l - 1);
		String seconds, minutes, degrees;
		if (Character.isLetter(signChar))
		{
			seconds = s.substring(l - 3, l - 1);
			minutes = s.substring(l - 5, l - 3);
			degrees = s.substring(0, l - 5);
		}
		else
		{
			String tmp = "";
			seconds = null;
			minutes = null;
			degrees = null;
			for (int i = 0; i < l; i++)
			{
				char ch = s.charAt(i);
				if (Character.isDigit(ch))
					tmp += ch;
				else
				{
					if (Character.isLetter(ch))
						signChar = ch;
					if (degrees == null)
						degrees = tmp;
					else if (minutes == null)
						minutes = tmp;
					else if (seconds == null)
						seconds = tmp;
					tmp = "";
				}
			}
		}

		switch (Character.toUpperCase(signChar))
		{
			case 'W' :
			case 'S' :
				sign = -1;
				break;
			case 'N' :
			case 'E' :
				sign = 1;
				break;
			default :
				return Util.toRadians(Util.parseDouble(s));
		}
		double total = 0;
		// parse seconds
		total += Util.parseFloat(seconds) / 3600;
		// parse minutes
		total += Util.parseFloat(minutes) / 60;
		// parse degrees
		total += Util.parseFloat(degrees);

		return Util.toRadians(total * sign);
	}

	public static Vector3d getLLR(String s)
	{
		StringTokenizer st = new StringTokenizer(s, " \t");
		double lat = 0, lon = 0, r = 1;
		if (st.hasMoreTokens())
			lat = DMSToRadians(st.nextToken());
		if (st.hasMoreTokens())
			lon = DMSToRadians(st.nextToken());
		if (st.hasMoreTokens())
			r = Util.parseDouble(st.nextToken());
		return new Vec3d(lon, lat, r);
	}

	//

	// todo: gotta (mod) by the orbit period
	public static long getNearestTime(double[] tarr, long tnow)
	{
		return getNearestTime(tarr, tnow, Double.NaN);
	}

	// todo: this doesn't seem to always work
	public static long getNearestTime(double[] tarr, long tnow, double period)
	{
		double tnowd = tnow * (1d / TICKS_PER_SEC);
		long bestt = INVALID_TICK;
		for (int i = 0; i < tarr.length; i++)
		{
			double td = tarr[i];
			if (!Double.isNaN(td))
			{
				// if we have a valid period, normalize it to
				// the value tnowd < x < tnowd + period
				if (!Double.isNaN(period))
				{
					td = (td - tnowd) / period;
					td = td - Math.floor(td);
					td = (td * period) + tnowd;
				}
				if (td > tnowd)
				{
					long t = (long) (td * TICKS_PER_SEC);
					if (bestt == INVALID_TICK || t < bestt)
						bestt = t;
				}
			}
			//			System.out.println("tarr[" + i + "] = " + td + ", bestt=" + bestt);
		}
		return bestt;
	}

	public static long getNearestTime(Conic orbit, double rad, long tnow)
	{
		double[] tarr = orbit.getTimesAtRadius(rad);
		double period = orbit.getPeriod();
		// this routine recognizes if period == NaN
		return getNearestTime(tarr, tnow, period);
	}

	//

	public static double distPointToLine(
		Vector3d pt,
		Vector3d l1,
		Vector3d dir)
	{
		double abl = dir.length();
		if (abl < 1e-30)
			return 0;
		Vector3d ap = new Vector3d(pt);
		ap.sub(l1);
		ap.cross(dir, ap);
		return ap.length() / abl;
	}

	public static double vecdistsqr(Vector3d a, Vector3d b)
	{
		double xx = a.x - b.x;
		double yy = a.y - b.y;
		double zz = a.z - b.z;
		return xx * xx + yy * yy + zz * zz;
	}

	public static double vecdist(Vector3d a, Vector3d b)
	{
		return Math.sqrt(vecdistsqr(a, b));
	}

	public static double vecdistsqr(Vector3f a, Vector3f b)
	{
		double xx = a.x - b.x;
		double yy = a.y - b.y;
		double zz = a.z - b.z;
		return xx * xx + yy * yy + zz * zz;
	}

	public static double vecdist(Vector3f a, Vector3f b)
	{
		return Math.sqrt(vecdistsqr(a, b));
	}

	public static Vector4d parseVector4(String s)
	{
		s = s.trim();
		while (s.startsWith("("))
			s = s.substring(1);
		while (s.endsWith(")"))
			s = s.substring(0, s.length() - 1);
		Vector4d v = new Vector4d();
		StringTokenizer st = new StringTokenizer(s, ",");
		try
		{
			v.x = Util.parseDouble(st.nextToken());
			v.y = Util.parseDouble(st.nextToken());
			v.z = Util.parseDouble(st.nextToken());
			v.w = Util.parseDouble(st.nextToken());
		}
		catch (NoSuchElementException nsee)
		{
		}
		return v;
	}

	public static Vector3d parseVector3(String s)
	{
		Vector4d v = parseVector4(s);
		return new Vector3d(v.x, v.y, v.z);
	}

	public static Vector3d parseVector(String s)
	{
		return parseVector3(s);
	}

}
