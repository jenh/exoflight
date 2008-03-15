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

import java.io.IOException;
import java.text.*;
import java.util.*;

import com.fasterlight.game.Settings;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Describes the 6-element Keplerian orbit.
  * Also has 2 additional fields for mu (gravity) and epoch time.
  * todo: check ranges of ecc, etc.
  * todo: enforce ordering that vars are set (bitfield?)
  * DONT forget to clear cached_matrix!!!!
  * todo: make mutable version
  */
public class KeplerianElements implements Constants, PropertyAware
{
	// todo: these letters suck!
	protected double p; // semi-latus rectum
	protected double e; // eccentricity
	protected double i; // inclination
	protected double O; // longitude of ascending node (N)
	protected double W; // argument of periapsis (w)
	protected double v; // true anomaly at epoch (v)

	protected double U; // G*mass (mu)
	protected double T; // epoch time

	private Matrix3d cached_matrix;

	//

	public KeplerianElements()
	{
	}

	public KeplerianElements(
		double semilatus,
		double eccent,
		double incl,
		double raan,
		double argperi,
		double trueanom,
		double mu,
		double epochtime)
	{
		setSemiLatusRectum(semilatus);
		setEccentricity(eccent);
		setInclination(incl);
		setRAAN(raan);
		setArgPeriapsis(argperi);
		setTrueAnomaly(trueanom);
		setMu(mu);
		setEpoch(epochtime);
	}

	public KeplerianElements(KeplerianElements ke)
	{
		this.p = ke.p;
		this.e = ke.e;
		this.i = ke.i;
		this.O = ke.O;
		this.W = ke.W;
		this.v = ke.v;
		this.U = ke.U;
		this.T = ke.T;
	}

	/**
	  * Parses distance, default is AU
	  * returns km
	  * todo: move into units parser (wherever it is!)
	  */
	public static double parseDistance(String s)
	{
		int p;
		double factor;
		if ((p = s.indexOf("km")) > 0)
		{
			s = s.substring(0, p).trim();
			factor = 1;
		} else
			factor = AU_TO_KM;
		return factor * Util.parseDouble(s);
	}

	private static DateFormat epochdfmt = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss zzz");

	/**
	  * Read elements from an INI file and a section name
	  * @deprecated
	  */
	public KeplerianElements(INIFile ini, String name) throws IOException
	{
		e = getDouble(ini, name, "e");
		String tmp = ini.getString(name, "q", "");
		if (tmp.length() > 0)
		{
			setPeriapsis(parseDistance(tmp));
		} else
		{
			setSemiMajorAxis(parseDistance(ini.getString(name, "a", "0")));
		}
		i = Util.toRadians(getDouble(ini, name, "i"));
		O = Util.toRadians(getDouble(ini, name, "N"));
		W = Util.toRadians(getDouble(ini, name, "w"));
		double longper = getDouble(ini, name, "p");
		if (longper != 0)
		{
			// todo???
			W = longper - O;
		}
		v = Util.toRadians(getDouble(ini, name, "M"));
		String parent = ini.getString(name, "parent", "");
		if (parent.length() > 0)
		{
			U = getDouble(ini, parent, "mass") * GRAV_CONST_KM;
			//			System.out.println("U of " + name + " is " + U);
		}
		String epochstr = ini.getString(name, "epoch", "01/01/2000 00:00:00 GMT");
		setEpochDateString(epochstr);
	}

	private static double getDouble(INIFile ini, String section, String name) throws IOException
	{
		return Util.parseDouble(ini.getString(section, name, "0"));
	}

	/**
	  * Calculate elements from a TLE + U param
	  * todo: semimajor axis isn't right (MIR)
	  */
	public KeplerianElements(String tle1, String tle2, double U)
	{
		setTLEs(tle1, tle2, U);
	}

	public void setTLEs(String tle1, String tle2, double U)
	{
		this.U = U;
		int eyear = Integer.parseInt(tle1.substring(18, 20).trim());
		if (eyear > 57)
			eyear -= 100; // 1957-2057
		double eday = Util.parseDouble(tle1.substring(20, 32).trim());
		double tsec = eyear * Constants.DAYS_PER_YEAR * 86400;
		setEpoch(tsec + (eday - 1) * 86400);
		//todo: check?
		this.i = Util.toRadians(Util.parseDouble(tle2.substring(8, 16).trim()));
		this.O = Util.toRadians(Util.parseDouble(tle2.substring(17, 25).trim()));
		this.e = Util.parseDouble("0." + tle2.substring(26, 33).trim());
		this.W = Util.toRadians(Util.parseDouble(tle2.substring(34, 42).trim()));
		setMeanAnomaly(Util.toRadians(Util.parseDouble(tle2.substring(43, 51).trim())));
		// set semi-major axis from mean motion (todo??)
		double revsday = Util.parseDouble(tle2.substring(52, 63).trim());
		double periodday = 1 / revsday;
		// compute from n=(UA)^(3/2)
		double a = Math.pow(periodday, 2d / 3);
		setSemiMajorAxis(a * GEOSTAT_RAD);
	}

	public void setTLEString(String tlestring)
	{
		StringTokenizer st = new StringTokenizer(tlestring, "/");
		setTLEs(st.nextToken(), st.nextToken(), Double.parseDouble(st.nextToken()));
	}

	public KeplerianElements(StateVector sv, double mu, double t0)
	{
		this(sv.r, sv.v, mu, t0);
	}

	public KeplerianElements(Vector3d r0, Vector3d v0, double mu, double t0)
	{
		this(r0, v0, mu);
		this.T = t0;
	}

	/**
	  * Calculate classic elements from r0, v0, mu
	  * in IJK frame
	  */
	public KeplerianElements(Vector3d r0, Vector3d v0, double mu)
	{
		this.setMu(mu);
		//	this(r0, v0, mu, new Vector3d(0,0,1));

		Vector3d rhat = new Vec3d(r0);
		rhat.normalize();
		double rmag = r0.length();

		Vector3d vhat = new Vec3d(v0);
		vhat.normalize();
		double vmag2 = v0.lengthSquared();
		double vmag = Math.sqrt(vmag2);

		// angular momentum vectors
		Vector3d hv = new Vec3d();
		hv.cross(r0, v0);

		Vector3d hhat = new Vec3d(hv);
		hhat.normalize();

		// eccentricity vector
		Vector3d ebar = new Vector3d(v0);
		ebar.scale(1 / mu);
		ebar.cross(ebar, hv);
		ebar.sub(rhat);

		double p, q, const1;
		if (hhat.z > -1)
		{
			p = hhat.x / (1 + hhat.z);
			q = -hhat.y / (1 + hhat.z);
			const1 = 1 / (1 + p * p + q * q);
		} else
		{
			p = 0;
			q = 0;
			const1 = 1;
		}

		Vector3d fhat =
			new Vec3d(const1 * (1 - p * p + q * q), const1 * 2 * p * q, -const1 * 2 * p);
		Vector3d ghat = new Vec3d(const1 * 2 * p * q, const1 * (1 + p * p - q * q), const1 * 2 * q);

		double h = ebar.dot(ghat);
		double xk = ebar.dot(fhat);
		double x1 = r0.dot(fhat);
		double y1 = r0.dot(ghat);

		// orbital ecc
		double eccm2 = h * h + xk * xk;
		double eccm = Math.sqrt(eccm2);

		// semilatus
		double slr = mu * rmag * (eccm2 - 1) / (rmag * vmag2 - 2 * mu);

		// orbital incl
		double xinc = 2 * Math.atan(Math.sqrt(p * p + q * q));

		// true long
		double xlambdat = atan3(y1, x1);

		// check for equitorial orbit
		double raan;
		if (xinc > 1e-12)
			raan = atan3(p, q);
		else
			raan = 0;

		// check for circular orbit
		double argper;
		if (eccm > 1e-12)
			argper = Math.atan2(h, xk) - raan;
		else
			argper = 0;

		// true anom
		double tanom = xlambdat - raan - argper;

		// set orbital elems
		this.e = eccm;
		this.p = slr;
		this.i = xinc;
		this.W = AstroUtil.fixAngle(argper);
		this.O = raan;
		this.v = AstroUtil.fixAngle(tanom);
	}


	private static double atan3(double x, double y)
	{
		x = Math.atan2(x, y);
		if (x < 0)
			x += Math.PI * 2;
		return x;
	}

	/**
	  * Calculate classic elements from r0, v0, mu
	  * as represented in a Conic object
	  */
	public KeplerianElements(Conic conic)
	{
		this(conic.getElements_unsafe());
	}

	/**
	  * Constructs a new orbit based on these params
	  */
	public Conic getConic()
	{
		return new Conic(this);
	}

	public StateVector getStateVectorAtEpoch()
	{
		return getStateVectorAtTrueAnom(this.v);
	}

	public StateVector getStateVectorAtMean(double M) throws ConvergenceException
	{
		// get eccentric anomaly
		double E = Kepler.solve(e, M);
		// todo: what if hyperbolic
		double trueAnom = getTrueAnomalyForEcc(E);
		return getStateVectorAtTrueAnom(trueAnom);
	}

	public StateVector getStateVectorAtTime(double t1)
	{
		double M = getMeanAnomalyAtTime(t1);
		return getStateVectorAtMean(M);
	}

	public StateVector getStateVectorAtTrueAnom(double v)
	{
		// Vallado pg 149
		if (e == 0)
			v = (i == 0) ? 0 : O;

		// find perifocal elements
		double rmag = p / (1 + e * Math.cos(v));
		Vector3d R = new Vector3d(Math.cos(v) * rmag, Math.sin(v) * rmag, 0);
		// now get derivative, that is velocity
		double vmag = Math.sqrt(U / p);
		Vector3d V = new Vector3d(-vmag * Math.sin(v), vmag * (e + Math.cos(v)), 0);

		// now transform to coordinate system
		Matrix3d xform = getMatrix();
		xform.transform(R);
		xform.transform(V);

		return new StateVector(R, V);
	}

	public Vector3d getVelAtTrueAnom(double v)
	{
		// Vallado pg 149
		if (e == 0)
			v = (i == 0) ? 0 : O;

		double vmag = Math.sqrt(U / p);
		Vector3d V = new Vector3d(-vmag * Math.sin(v), vmag * (e + Math.cos(v)), 0);

		// now transform to coordinate system
		Matrix3d xform = getMatrix();
		xform.transform(V);

		return V;
	}

	public Vector3d getPosAtTrueAnom(double v)
	{
		// Vallado pg 149
		if (e == 0)
			v = (i == 0) ? 0 : O;

		double rmag = p / (1 + e * Math.cos(v));
		Vector3d R = new Vector3d(Math.cos(v) * rmag, Math.sin(v) * rmag, 0);

		// now transform to coordinate system
		Matrix3d xform = getMatrix();
		xform.transform(R);

		return R;
	}

	/**
	  * Returns matrix that transforms perifocal coordinates
	  * into IJK coordinates.
	  * NOTE: don't modify the returned matrix, it might be cached.
	  */
	public Matrix3d getMatrix()
	{
		if (cached_matrix != null)
			return cached_matrix;

		double co = Math.cos(O);
		double cw = Math.cos(W);
		double ci = Math.cos(i);
		double so = Math.sin(O);
		double sw = Math.sin(W);
		double si = Math.sin(i);
		Matrix3d xform =
			new Matrix3d(
				co * cw - so * sw * ci,
				-co * sw - so * cw * ci,
				so * si,
				so * cw + co * sw * ci,
				-so * sw + co * cw * ci,
				-co * si,
				sw * si,
				cw * si,
				ci);
		return (cached_matrix = xform);
	}

	// get/set

	/**
	  * a is in km
	  * e must have already been set
	  */
	public void setSemiMajorAxis(double a)
	{
		this.p = checkNonNeg(a * (1 - e * e));
	}

	public double getSemiMajorAxis()
	{
		return p / (1 - e * e);
	}

	/**
	  * a is in km
	  * e must have already been set
	  */
	public void setPeriapsis(double peri)
	{
		this.p = checkNonNeg(peri * (e + 1));
	}

	public double getPeriapsis()
	{
		return p / (e + 1);
	}

	/**
	  * Set p and e wrs. peri and apo.
	  */
	public void setPeriapsisAndApoapsis(double peri, double apo)
	{
		setSemiLatusRectum(2 * apo * peri / (apo + peri));
		setEccentricity((apo - peri) / (apo + peri));
	}

	public void setPeriapsisWRSApoapsis(double peri)
	{
		setPeriapsisAndApoapsis(peri, getApoapsis());
	}

	public void setApoapsisWRSPeriapsis(double apo)
	{
		setPeriapsisAndApoapsis(getPeriapsis(), apo);
	}

	/**
	  * a is in km
	  * e must have been set
	  */
	public void setApoapsis(double apo)
	{
		setSemiLatusRectum(apo * (1 - e));
	}

	public double getApoapsis()
	{
		return p / (1 - e);
	}

	public double getSemiLatusRectum()
	{
		return p;
	}

	public void setSemiLatusRectum(double semilatus)
	{
		this.p = checkNaN(semilatus);
	}

	public double getEccentricity()
	{
		return e;
	}

	public void setEccentricity(double eccent)
	{
		this.e = checkNonNeg(eccent);
	}

	public void setEccentricityWRSPeriapsis(double eccent)
	{
		double peri = getPeriapsis();
		setEccentricity(eccent);
		setPeriapsis(peri);
	}

	public void setEccentricityWRSApoapsis(double eccent)
	{
		double apo = getApoapsis();
		setEccentricity(eccent);
		setApoapsis(apo);
	}

	public void setEccentricityWRSAxis(double eccent)
	{
		double axis = getSemiMajorAxis();
		setEccentricity(eccent);
		setSemiMajorAxis(axis);
	}

	public void setAxisWRSPeriapsis(double axis)
	{
		// we need to set both p & e
		double peri = getPeriapsis();
		setSemiLatusRectum(peri * (2 * axis - peri) / axis);
		setEccentricity((axis - peri) / axis);
	}

	public void setAxisWRSApoapsis(double axis)
	{
		// we need to set both p & e
		double apo = getApoapsis();
		setSemiLatusRectum(apo * (2 * axis - apo) / axis);
		setEccentricity((apo - axis) / axis);
	}

	/**
	  * Set eccent, axisor is constant.
	  */
	public void setPeriapsisWRSAxis(double rp)
	{
		double axis = getSemiMajorAxis();
		setEccentricity((axis - rp) / axis);
		setSemiMajorAxis(axis);
	}

	/**
	  * Set eccent, semimajor is constant.
	  */
	public void setApoapsisWRSAxis(double ra)
	{
		double axis = getSemiMajorAxis();
		setEccentricity((ra - axis) / axis);
		setSemiMajorAxis(axis);
	}

	public double getInclination()
	{
		return i;
	}

	public void setInclination(double incl)
	{
		this.i = checkNaN(incl);
		cached_matrix = null;
	}

	public double getRAAN()
	{
		return O;
	}

	public void setRAAN(double raan)
	{
		this.O = checkNaN(raan);
		cached_matrix = null;
	}

	public double getArgPeriapsis()
	{
		return W;
	}

	public void setArgPeriapsis(double argperi)
	{
		this.W = checkNaN(argperi);
		cached_matrix = null;
	}

	public double getTrueAnomaly()
	{
		return v;
	}

	public void setTrueAnomaly(double trueanom)
	{
		this.v = checkNaN(trueanom);
		cached_matrix = null;
	}

	public double getMeanAnomalyAtTime(double t1) throws ConvergenceException
	{
		// optz: all this is very consuming!!
		double a = getSemiMajorAxis();
		return Math.sqrt(U / (a * a * a)) * (t1 - T) + getMeanAnomaly();
	}

	public double getTrueAnomalyAtTime(double t1) throws ConvergenceException
	{
		if (t1 == T)
			return this.v;
		double M = getMeanAnomalyAtTime(t1);
		// todo: if hyperbolic?
		double E = Kepler.solve(e, M);
		return getTrueAnomalyForEcc(E);
	}

	/**
	  * Get mean anomaly at epoch
	  */
	public double getMeanAnomaly()
	{
		double cosnu = Math.cos(this.v);
		double ee = Math.acos((e + cosnu) / (1 + e * cosnu));
		if (this.v >= Math.PI)
		{
			ee = Math.PI * 2 - ee;
		}
		// Calculate mean anomaly
		double m = ee - e * Math.sin(ee);
		return m;
	}

	private static final double KEPLER_THRESHOLD = Settings.getDouble("Kepler", "Threshold", 1e-12);

	/**
	  * Set mean anomaly at epoch
	  */
	public void setMeanAnomaly(double M) throws ConvergenceException
	{
		// eccentricity must be set
		// todo: if hyperbolic?
		setEccentricAnomaly(Kepler.solve(e, M));
	}

	/**
	  * Get Eccentric  anomaly at epoch
	  */
	public double getEccentricAnomaly()
	{
		// Vallado pg. 215
		return 2 * Math.atan(Math.sqrt((1 - e) / (1 + e)) * Math.tan(v / 2));
	}

	/**
	  * Set Eccentric  anomaly at epoch
	  */
	public void setEccentricAnomaly(double E)
	{
		setTrueAnomaly(checkNaN(getTrueAnomalyForEcc(E)));
	}

	public double getTrueAnomalyForEcc(double E)
	{
		return 2 * Math.atan(Math.sqrt((1 + e) / (1 - e)) * Math.tan(E / 2));
	}

	public double getHyperbolicAnomaly()
	{
		// Vallado pg. 223
		double sqe = Math.sqrt((e - 1) / (e + 1));
		double cosv = Math.cos(v);
		double sinv = Math.sin(v);
		double num = - (cosv * sqe - sinv - sqe);
		double denom = cosv * sqe + sinv - sqe;
		double x = Math.log(num / denom);
		if (Double.isNaN(x))
			return 0; //todo?
		return x;
	}

	public long getEpochTicks()
	{
		return (long) (Constants.TICKS_PER_SEC * T);
	}

	public void setEpochTicks(long t)
	{
		T = t * (1d / TICKS_PER_SEC);
	}

	public void setEpochDateString(String epochstr)
	{
		try
		{
			Date epochd = epochdfmt.parse(epochstr);
			T = AstroUtil.javaDateToSeconds(epochd);
		} catch (ParseException pe)
		{
			throw new PropertyRejectedException("Could not set epoch to " + epochstr);
		}
	}

	public String getEpochDateString()
	{
		return epochdfmt.format(AstroUtil.secondsToJavaDate(T));
	}

	public double getEpoch()
	{
		return T;
	}

	public void setEpoch(double t)
	{
		this.T = checkNaN(t);
	}

	public double getMu()
	{
		return U;
	}

	public void setMu(double mu)
	{
		this.U = checkNonNeg(mu);
	}

	public String toString()
	{
		return "(p="
			+ p
			+ ",a="
			+ getSemiMajorAxis()
			+ ",e="
			+ e
			+ ",i="
			+ Util.toDegrees(i)
			+ ",N="
			+ Util.toDegrees(O)
			+ ",w="
			+ Util.toDegrees(W)
			+ ",v="
			+ Util.toDegrees(v)
			+ ",U="
			+ U
			+ ",t0="
			+ getEpochTicks()
			+ ")";
	}

	private double checkNonNeg(double x) throws IllegalArgumentException
	{
		if (!(x >= 0))
			throw new IllegalArgumentException(x + " must be >= 0");
		return x;
	}

	private double checkNaN(double x) throws IllegalArgumentException
	{
		if (Double.isNaN(x))
			throw new IllegalArgumentException("checkNaN failed");
		return x;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(KeplerianElements.class);

	static {
		prophelp.registerGetSet("semilatus", "SemiLatusRectum", double.class);
		prophelp.registerGetSet("semimajor", "SemiMajorAxis", double.class);
		prophelp.registerGetSet("eccent", "Eccentricity", double.class);
		prophelp.registerGetSet("incl", "Inclination", double.class);
		prophelp.registerGetSet("longascnode", "RAAN", double.class);
		prophelp.registerGetSet("raan", "RAAN", double.class);
		prophelp.registerGetSet("argperi", "ArgPeriapsis", double.class);
		prophelp.registerGetSet("trueanom", "TrueAnomaly", double.class);
		prophelp.registerGetSet("epochdate", "EpochDateString", String.class);
		prophelp.registerGetSet("epochsecs", "Epoch", double.class);
		prophelp.registerGetSet("epochticks", "EpochTicks", long.class);
		prophelp.registerGetSet("periapsis", "Periapsis", double.class);
		prophelp.registerGetSet("apoapsis", "Apoapsis", double.class);
		prophelp.registerGetSet("meananom", "MeanAnomaly", double.class);
		prophelp.registerGetSet("eccanom", "EccentricAnomaly", double.class);
		prophelp.registerGet("conic", "getConic");
		prophelp.registerSet("peri_wrs_axis", "setPeriapsisWRSAxis", double.class);
		prophelp.registerSet("apo_wrs_axis", "setApoapsisWRSAxis", double.class);
		prophelp.registerSet("peri_wrs_apo", "setPeriapsisWRSApoapsis", double.class);
		prophelp.registerSet("apo_wrs_peri", "setApoapsisWRSPeriapsis", double.class);
		prophelp.registerGet("peri_wrs_axis", "getPeriapsis");
		prophelp.registerGet("apo_wrs_axis", "getApoapsis");
		prophelp.registerGet("peri_wrs_apo", "getPeriapsis");
		prophelp.registerGet("apo_wrs_peri", "getApoapsis");
		prophelp.registerSet("tles", "setTLEString", String.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
