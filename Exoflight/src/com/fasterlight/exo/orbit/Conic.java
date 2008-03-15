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

import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * A wrapper for either a StateVector or KeplerianElements.
  * Has some added nifty-functions.
  */
public class Conic implements PropertyAware
{
	private KeplerianElements kelem;
	private StateVector statevec;
	private double mu;
	private double t0;

	// Careful -- this object isn't mutable, neither
	// are the objects it contains

	public Conic(Conic conic)
	{
		if (conic.kelem != null)
			this.kelem = new KeplerianElements(conic.kelem);
		if (conic.statevec != null)
			this.statevec = new StateVector(conic.statevec);
		this.mu = conic.mu;
		this.t0 = conic.t0;
	}

	public Conic(StateVector sv, double mu, double t0)
	{
		this.statevec = new StateVector(sv);
		this.mu = mu;
		this.t0 = t0;
	}

	public Conic(Vector3d r0, Vector3d v0, double mu, double t0)
	{
		this.statevec = new StateVector(r0, v0);
		this.mu = mu;
		this.t0 = t0;
	}

	public Conic(KeplerianElements ke)
	{
		this.kelem = new KeplerianElements(ke);
		this.t0 = ke.T;
		this.mu = ke.U;
	}

	public StateVector getStateVectorAtEpoch()
	{
		return new StateVector(getStateVectorAtEpoch_unsafe());
	}

	public StateVector getStateVectorAtEpoch_unsafe()
	{
		if (statevec == null)
		{
			statevec = kelem.getStateVectorAtEpoch();
		}
		return statevec;
	}

	public KeplerianElements getElements()
	{
		return new KeplerianElements(getElements_unsafe());
	}

	public KeplerianElements getElementsAtTime(double t)
	{
		KeplerianElements ke = new KeplerianElements(getElements_unsafe());
		ke.setMeanAnomaly(ke.getMeanAnomalyAtTime(t));
		return ke;
	}

	public KeplerianElements getElements_unsafe()
	{
		if (kelem == null)
		{
			kelem = new KeplerianElements(statevec, mu, t0);
		}
		return new KeplerianElements(kelem);
	}

	public StateVector getStateVectorAtTime(double t)
	{
		if (t == t0)
			return getStateVectorAtEpoch();

		return getElements_unsafe().getStateVectorAtTime(t);
	}

	// todo: cache!

	public double getTimeAtMeanAnomaly(double M)
	{
		double M0 = getElements_unsafe().getMeanAnomaly();
		return t0 + AstroUtil.fixAngle(M - M0) * getPeriod() / (Math.PI * 2);
	}

	public double getTimeAtMeanAnomaly2(double M)
	{
		double M0 = getElements_unsafe().getMeanAnomaly();
		return t0 + AstroUtil.fixAngle2(M - M0) * getPeriod() / (Math.PI * 2);
	}

	public double getPeriod()
	{
		double a = getSemiMajorAxis();
		return 2 * Math.PI * Math.sqrt(a * a * a / mu);
	}

	public Vector3d getPlaneNormal()
	{
		Vector3d h = new Vec3d();
		StateVector sv = getStateVectorAtEpoch();
		h.cross(sv.v, sv.r);
		return h;
	}

	public double getSemiMajorAxis()
	{
		return getElements_unsafe().getSemiMajorAxis();
	}

	public double getSemiLatusRectum()
	{
		return getElements_unsafe().getSemiLatusRectum();
	}

	public double getEccentricity()
	{
		return getElements_unsafe().getEccentricity();
	}

	public double getVelocityAtRadius(double r)
	{
		return Math.sqrt(mu * (2 / r - 1 / getSemiMajorAxis()));
	}

	public double getMaxVelocity()
	{
		// v = sqrt(mu*(2/r-1/a))
		// max velocity occurs at periapsis
		// so v=sqrt( (u*(e+1)) / (a*(1-e)) )
		return getVelocityAtRadius(getPeriapsis());
	}

	public double getMinVelocity()
	{
		return getVelocityAtRadius(getApoapsis());
	}

	public double getApoapsis()
	{
		return getSemiMajorAxis() * (1 + getEccentricity());
	}

	public double getPeriapsis()
	{
		return getSemiMajorAxis() * (1 - getEccentricity());
	}

	public double[] getTimesAtRadius(double r2)
	{
		KeplerianElements cop = getElements_unsafe();
		double a = cop.getSemiMajorAxis();
		double e = cop.getEccentricity();
		if (a <= 0)
			return getTimesAtRadiusHyp(r2);

		// now we find E (ecc. anom) for our target radius r2
		// r = a*(1-e*cos(E))
		// so cos(E) = (a-r)/(a*e);
		// Vallado pgs. 212-213

		double ce2 = (a - r2) / (a * e);
		// handle boundary conditions at -1 and 1
		if (ce2 < -1.0000001)
			return new double[0];
		if (ce2 > 1.0000001)
			return new double[0];
		if (ce2 < -1)
			ce2 = -1;
		else if (ce2 > 1)
			ce2 = 1;

		// now get the initial ecc. anom
		double E0 = cop.getEccentricAnomaly();
		double E0sinE0 = E0 - e * Math.sin(E0);

		double aaaU = Math.sqrt(a * a * a / mu);
		double[] arr = new double[2];
		double E = Math.PI / 2 - Math.asin(ce2);

		for (int i = 0; i < 2; i++)
		{
			double EsinE = E - e * Math.sin(E);
			double t2t = aaaU * AstroUtil.fixAngle(EsinE - E0sinE0);
			if (debug)
				System.out.println(
					"E sin E = "
						+ EsinE
						+ "\tE = "
						+ E
						+ "\te = "
						+ e
						+ "\tce2="
						+ ce2
						+ "\tE0 sin E0="
						+ E0sinE0
						+ "\tt2t="
						+ t2t);
			arr[i] = t2t + t0;
			E = -E;
		}
		return arr;
	}

	public double[] getTimesAtRadiusHyp(double r2)
	{
		KeplerianElements cop = getElements_unsafe();
		double a = cop.getSemiMajorAxis();
		double e = cop.e;

		// now get the initial ecc. anom
		double H0 = cop.getHyperbolicAnomaly();
		double H0sinH0 = e * AstroUtil.sinh(H0) - H0;

		// now we find H (ecc. anom) for our target radius r2
		// r = a*(1-e*cosh(H))
		// so cosh(H) = (a-r)/(a*e);
		// Vallado pgs. 212-213

		double ce2 = (a - r2) / (a * e);
		// todo: handle boundary conditions

		double aaaU = Math.sqrt(-a * a * a / mu);
		double[] arr = new double[2];
		double sqce2m1 = Math.sqrt(ce2 * ce2 - 1);

		for (int i = 0; i < 2; i++)
		{
			double H;
			switch (i)
			{
				case 0 :
					H = Math.log(sqce2m1 + ce2);
					break;
				case 1 :
					H = Math.log(ce2 - sqce2m1);
					break;
				default :
					throw new RuntimeException("?");
			}
			double HsinH = e * AstroUtil.sinh(H) - H;
			double t2t = aaaU * (HsinH - H0sinH0);
			if (debug)
				System.out.println(
					"H sin H = "
						+ HsinH
						+ "\tH = "
						+ H
						+ "\te = "
						+ e
						+ "\tce2="
						+ ce2
						+ "\tH0 sin H0="
						+ H0sinH0);
			arr[i] = t2t + t0;
		}
		return arr;
	}

	/**
	  * Get the acceleration for a given position
	  */
	public Vector3d getAccelAtPos(Vector3d r)
	{
		double rl = r.length();
		Vector3d accel = new Vec3d(r);
		accel.scale(-mu / (rl * rl * rl));
		return accel;
	}

	public double getMu()
	{
		return mu;
	}

	public double getInitialTime()
	{
		return t0;
	}

	public void setInitialTime(double t0)
	{
		this.t0 = t0;
		if (kelem != null)
			kelem.T = t0;
	}

	//

	public String toString()
	{
		return "[Conic:" + kelem + ", " + statevec + "]";
	}

	boolean debug = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Conic.class);

	static {
		prophelp.registerGet("elements", "getElements");
		prophelp.registerGet("mu", "getMu");
		prophelp.registerGet("t0", "getInitialTime");
		prophelp.registerGet("apoapsis", "getApoapsis");
		prophelp.registerGet("periapsis", "getPeriapsis");
		prophelp.registerGet("period", "getPeriod");
		prophelp.registerGet("normal", "getPlaneNormal");
		prophelp.registerGet("semimajor", "getSemiMajorAxis");
		prophelp.registerGet("semilatus", "getSemiLatusRectum");
		prophelp.registerGet("eccent", "getEccentricity");
		prophelp.registerGet("maxvel", "getMaxVelocity");
		prophelp.registerGet("minvel", "getMinVelocity");
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
