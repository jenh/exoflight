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
package com.fasterlight.exo.orbit.nav;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.Settings;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Implements the Lambert algorithm -- returns the conic
  * orbit that passes through two points and has a given
  * transfer time between the points.
  */
public class Lambert implements Constants
{
	int MAX_ITERS = Settings.getInt("Lambert", "MaxIters", 200);
	int MAX_ADJ = 25;
	double THRESHOLD = Settings.getDouble("Lambert", "Threshold", 1e-12);
	boolean debug = false;

	public Vector3d r1, r2, v1, v2;
	public double U;

	//

	public void setThreshold(double thresh)
	{
		this.THRESHOLD = thresh;
	}

	// todo: check if y < 0 (pg. 240)

	public void solve(Vector3d r1, Vector3d r2, double U, double desiredt, boolean longway)
		throws LambertException
	{
		solveBattan(r1, r2, U, desiredt, longway);
	}

	double NEWTON_FACTOR = 0.5;

	public void solveold(Vector3d r1, Vector3d r2, double U, double desiredt, boolean longway)
		throws LambertException
	{
		double dv = r1.angle(r2);
		int DM;
		if (longway)
		{
			dv = Math.PI * 2 - dv;
			DM = -1;
			/*
			solveBisect(r1, r2, U, desiredt, longway);
			return;
			*/
		} else
			DM = 1;
		double sqrtU = Math.sqrt(U);
		double udesiredt = desiredt * sqrtU;
		double sindv = Math.sin(dv);
		double cosdv = Math.cos(dv);
		double r1l = r1.length();
		double r2l = r2.length();
		double C, S, x, y;
		// evaluate A
		double A = Math.sqrt(r1l * r2l * (1 + cosdv)) * DM;
		if (debug)
			System.out.println("A=" + A + "\tdv=" + dv + "\tr1=" + r1l + "\tr2=" + r2l);
		// pick trial value for z
		double z = 0; // todo
		double ut = 0;
		int iter = 0;
		do
		{
			int adj = 0;
			do
			{
				// pg 230
				C = Kepler.computeC(z); // or "c2"
				S = Kepler.computeS(z); // or "c3"
				// determine aux x,y
				y = r1l + r2l + A * (z * S - 1) / Math.sqrt(C);
				if (A > 0 && y < 0)
				{
					System.out.println("A=" + A + " y=" + y + " lw=" + longway);
					z += (Math.PI * 4 - z) / 2;
				} else
					break;
			} while (++adj < MAX_ADJ);
			/*
				C = Kepler.computeC(z);
				S = Kepler.computeS(z);
				// determine aux x,y
				y = r1l + r2l - A*(1-z*S)/Math.sqrt(C);
				// check for y<0 -- if so, "fix" z and continue
				// todo: bad
				if (y<0)
				{
					//z = (Math.sqrt(C)*0.1+A-Math.sqrt(C)*(r1l+r2l))/(A*S);
					z = 3.99*Math.PI*Math.PI;
					if (debug)
						System.out.println("y="+y+", so z=" + z);
					continue;
				}
			*/
			x = Math.sqrt(y / C);
			if (Double.isNaN(x) || debug)
				System.out.println("y=" + y + "\tx=" + x);
			// determine t
			ut = x * x * x * S + A * Math.sqrt(y);
			if (Double.isNaN(ut) || debug)
				System.out.println(udesiredt + "\t" + ut);
			if (Math.abs((ut - udesiredt) / sqrtU) < THRESHOLD)
				break;
			// determine dt/dz
			double Cp, Sp;
			try
			{
				Cp = powerSeries(z, 24, 3);
				Sp = powerSeries(z, 120, 4);
			} catch (IllegalArgumentException iae)
			{ //todo: remove
				System.out.println(iae.getMessage());
				System.out.println(
					r1 + ", " + r2 + ", " + AstroUtil.toDuration(desiredt) + ", " + longway);
				break;
			}
			double udtdz =
				x * x * x * (Sp - 3 * S * Cp / 2 * C) + A * (3 * S * Math.sqrt(y) / C + A / x) / 8;
			// apply newton's method
			z += (udesiredt - ut) * NEWTON_FACTOR / udtdz;
		}
		while (iter++ < MAX_ITERS);
		if (iter >= MAX_ITERS)
		{
			System.out.println(
				"Lambert had "
					+ iter
					+ " iters! z="
					+ z
					+ " udt="
					+ udesiredt
					+ " ut="
					+ ut
					+ " long="
					+ longway);
			throw new LambertException("Convergence failure");
		}
		// now we're done ... compute the goodness
		double f = 1 - y / r1l;
		double g = A * Math.sqrt(y / U);
		double dg = 1 - y / r2l;
		this.r1 = r1;
		this.r2 = r2;
		this.U = U;
		v1 = new Vec3d(r2.x - f * r1.x, r2.y - f * r1.y, r2.z - f * r1.z);
		v1.scale(1 / g);
		v2 = new Vec3d(dg * r2.x - r1.x, dg * r2.y - r1.y, dg * r2.z - r1.z);
		v2.scale(1 / g);
	}

	public void solvenew(Vector3d r1, Vector3d r2, double U, double desiredt, boolean longway)
	{
		int DM = longway ? -1 : 1;
		double sqrtU = Math.sqrt(U);
		double udesiredt = desiredt * sqrtU;
		double r1l = r1.length();
		double r2l = r2.length();
		Vector3d nml1 = new Vec3d();
		nml1.cross(r1, r2);
		double cosdv = nml1.length() / (r1l * r2l);
		double sindv = DM * Math.sqrt(1 - cosdv * cosdv);

		double C, S, x, y;
		// evaluate A
		double A = Math.sqrt(r1l * r2l * (1 + cosdv)) * DM;
		if (debug)
			System.out.println("A=" + A + "\tr1=" + r1l + "\tr2=" + r2l);
		// pick trial value for z
		double z = 0; // todo

		int iter = 0;
		do
		{
			C = Kepler.computeC(z);
			S = Kepler.computeS(z);
			// determine aux x,y
			y = r1l + r2l - A * (1 - z * S) / Math.sqrt(C);
			// check for y<0 -- if so, "fix" z and continue
			// todo: bad
			if (y < 0)
			{
				//z = (Math.sqrt(C)*0.1+A-Math.sqrt(C)*(r1l+r2l))/(A*S);
				z = 3.99 * Math.PI * Math.PI;
				if (debug)
					System.out.println("y=" + y + ", so z=" + z);
				continue;
			}
			x = Math.sqrt(y / C);
			if (debug)
				System.out.println("y=" + y + "\tx=" + x);
			// determine t
			double ut = x * x * x * S + A * Math.sqrt(y);
			if (debug)
				System.out.println(udesiredt + "\t" + ut);
			if (Math.abs(ut - udesiredt) < THRESHOLD)
				break;
			// determine dt/dz
			double Cp, Sp;
			try
			{
				Cp = powerSeries(z, 24, 3);
				Sp = powerSeries(z, 120, 4);
			} catch (IllegalArgumentException iae)
			{ //todo: remove
				System.out.println(iae.getMessage());
				System.out.println(
					r1 + ", " + r2 + ", " + AstroUtil.toDuration(desiredt) + ", " + longway);
				break;
			}
			double udtdz =
				x * x * x * (Sp - 3 * S * Cp / 2 * C) + A * (3 * S * Math.sqrt(y) / C + A / x) / 8;
			// apply newton's method
			z += (udesiredt - ut) / udtdz;
		} while (iter++ < MAX_ITERS);

		if (iter >= MAX_ITERS)
		{
			System.out.println("Lambert had " + iter + " iters! z=" + z);
		}

		// now we're done ... compute the goodness
		double f = 1 - y / r1l;
		double g = A * Math.sqrt(y / U);
		double dg = 1 - y / r2l;
		this.r1 = r1;
		this.r2 = r2;
		this.U = U;
		v1 = new Vec3d(r2.x - f * r1.x, r2.y - f * r1.y, r2.z - f * r1.z);
		v1.scale(1 / g);
		v2 = new Vec3d(dg * r2.x - r1.x, dg * r2.y - r1.y, dg * r2.z - r1.z);
		v2.scale(1 / g);
	}

	// Vallado, pgs 441-444
	/***
		public void solveBattan(Vector3d r1, Vector3d r2, double U, double desiredt, boolean longway)
		{
			// normalize
			U1d3 = Math.pow(U,1d/3);
	      U2d3 = Math.pow(U,2d/3);
	      U1d6 = Math.pow(U,1d/6);
	      r1 = new Vector3d(r1);
	      r1.scale(1/U1d3);
	      r2 = new Vector3d(r2);
	      r2.scale(1/U1d3);

			int DM = longway ? -1 : 1;
			double r1l = r1.length();
			double r2l = r2.length();
			double cosdv = r1.dot(r2)/(r1l*r2l);
			double sindv = DM * Math.sqrt(1-cosdv*cosdv);
			double A = Math.sqrt(r1l*r2l*(1+cosdv))*DM;

			double c = Math.sqrt(r1l*r1l + r2l*r2l - 2*r1l*r2l*cosdv);
			double s = (r1l+r2l+c)/2;
			double eps = (r2l-r1l)/r1l;

			double tmp1 = Math.sqrt(r2l/r1l);
			double tmp2 = tmp1 + r2l*(2+tmp1)/r1l;
			double tan2_2w = eps*eps/(4*tmp2);

			double cosdvd4 = Math.cos(dv/4);
			double rop = Math.sqrt(r1l*r2l)*(cosdvd4*cosdvd4 + tan2_2w);

			double l;
			if (sindv >= 0)
			{
				double cosdvd2 = Math.cos(dv/2);
				double sindvd4 = Math.sin(dv/4);
				l = (sindvd4*sindvd4 + tan2_2w)/(sindvd4*sindvd4 + tan2_2w + cosdvd2);
			} else {
				double cosdvd2 = Math.cos(dv/2);
				l = (cosdvd4*cosdvd4 + tan2_dw - cosdvd2)/(cosdvd4*cosdvd4 + tan2_2w);
			}

			// todo: parabolic mean point radius?!?!?!
		}
	***/

	/***
	% * This subroutine is a Lambert Algorithm which given two radius
	% * vectors and the time to get from one to the other, it finds the
	% * orbit connecting the two. It solves the problem using a new
	% * algorithm developed by R. Battin. It solves the Lambert problem
	% * for all possible types of orbits (circles, ellipses, parabolas,
	% * and hyperbolas). The only singularity is for the case of a
	% * transfer angle of 360 degrees, which is a rather obscure case.
	% * It computes the velocity vectors corresponding to the given radius
	% * vectors except for the case when the transfer angle is 180 degrees
	% * in which case the orbit plane is ambiguous (an infinite number of
	% * transfer orbits exist).
	% *
	% * The algorithm computes the semi-major axis, the parameter (semi-
	% * latus rectum), the eccentricity, and the velocity vectors.
	% *
	% * NOTE: Open file 6 prior to calling LAMBERT. If an error occurs
	% * or the 360 or 180 degree transfer case is encountered,
	% * LAMBERT writes to unit 6.
	% *
	% * INPUTS TO THE SUBROUTINE
	% * RI(3) = A three element array containing the initial
	% * position vector (distance unit)
	% * RF(3) = A three element array containing the final
	% * position vector (distance unit)
	% * TOF = The transfer time, time of flight (time unit)
	% * MU = Gravitational parameter of primary
	% * (distance unit)**3/(time unit)**2
	% * LONGWAY = Logical variable defining whether transfer is
	% * greater or less than pi radians.
	% * .TRUE. Transfer is greater than pi radians
	% * .FALSE. Transfer is less than pi radians
	% *
	% * OUTPUTS FROM THE SUBROUTINE
	% * A = Semi-major axis of the transfer orbit
	% * (distance unit)
	% * P = Semi-latus rectum of the transfer orbit
	% * (distance unit)
	% * E = Eccentricity of the transfer orbit
	% * ERROR = Error flag
	% * .FALSE. No error
	% * .TRUE. Error, routine failed to converge
	% * VI(3) = A three element array containing the initial
	% * velocity vector (distance unit/time unit)
	% * VT(3) = A three element array containing the final
	% * velocity vector (distance unit/time unit)
	% * TPAR = Parabolic flight time between RI and RF (time unit)
	% * THETA = The transfer angle (radians)
	% *
	% * NOTE: The semi-major axis, positions, times, & gravitational
	% * parameter must be in compatible units.
	% *
	% * MISSION PLANNING SUBROUTINES AND FUNCTIONS CALLED
	% * ABV, CROSSP, DOTP, PI, QCK(PI)
	% *
	% * PROGRAMMER: Chris D'Souza
	% *
	% * DATE: January 20, 1989
	% *
	% * VERIFIED BY: Darrel Monroe, 10/25/90
	% *
	% ************************************************************************
	***/

	private static double sqr(double x)
	{
		return x * x;
	}

	public void solveBattan(Vector3d r1, Vector3d r2, double mu, double tof, boolean longway)
		throws LambertException
	{
		double eta, c1, denom, h1, h2, qr, xpll, lp2xp1, gamma;
		double rim2, rfm2, rim, rfm, cth, sth, R1, S1, b;
		double delta, u, ku, l1, m, l, lambda, x0, x, y, sigma, c, s, u0, theta, beta;
		double pmin, tmin;
		int n, n1;

		rim2 = r1.lengthSquared();
		rim = Math.sqrt(rim2);
		rfm2 = r2.lengthSquared();
		rfm = Math.sqrt(rfm2);

		cth = r1.dot(r2) / (rim * rfm);
		Vector3d cr = new Vector3d();
		cr.cross(r1, r2);
		sth = cr.length() / (rim * rfm);

		//*** choose angle for up angular momentum ***

		if (cr.z < 0)
			sth = -sth;

		theta = AstroUtil.fixAngle(Math.atan2(sth, cth));
		int b1 = AstroUtil.sign2(sth);

		//*** Compute the chord and the semi-perimeter

		c = Math.sqrt(rim2 + rfm2 - 2 * rim * rfm * cth);
		s = (rim + rfm + c) / 2;

		beta = 2 * Math.asin(Math.sqrt((s - c) / s));

		pmin = Math.PI * 2 * Math.sqrt(s * s * s / (8 * mu));
		tmin = pmin * (Math.PI - beta + Math.sin(beta)) / (Math.PI * 2);
		lambda = b1 * Math.sqrt((s - c) / s);

		//*** Compute L carefully for transfer angles less than 5 degrees

		if (theta * 180 / Math.PI <= 5)
		{
			double w = Math.atan(Math.pow(rfm / rim, 0.25)) - Math.PI / 4;
			R1 = sqr(Math.sin(theta / 4));
			S1 = sqr(Math.tan(2 * w));
			l = (R1 + S1) / (R1 + S1 + Math.cos(theta / 2));
		} else
		{
			l = sqr((1 - lambda) / (1 + lambda));
		}

		m = 8 * mu * tof * tof / (s * s * s * Math.pow(1 + lambda, 6));
		double tpar = (Math.sqrt(2 / mu) / 3) * (Math.pow(s, 1.5) - Math.pow(b1 * (s - c), 1.5));
		l1 = (1 - l) / 2;

		if (debug)
			System.out.println("FOO1: " + beta + " " + lambda + " " + tpar + " " + l1);

		//*** Initialize values of y, n, and x

		y = 1;
		n = 0;
		n1 = 0;

		x0 = (tof - tpar <= 1e-3) ? 0 : l;
		x = -1e8;

		//*** Begin iteration

		while (Math.abs(x0 - x) >= THRESHOLD && n <= MAX_ITERS)
		{
			n++;
			x = x0;
			eta = x / sqr(Math.sqrt(1 + x) + 1);

			//*** Compute x by means of an algorithm devised by
			//*** Gauticci for evaluating continued fractions by the
			//*** 'Top Down' method

			delta = 1;
			u = 1;
			sigma = 1;
			int m1 = 0;

			while (Math.abs(u) > THRESHOLD && m1++ <= MAX_ITERS)
			{
				gamma = sqr(m1 + 3) / (4 * sqr(m1 + 3) - 1);
				delta = 1 / (1 + gamma * eta * delta);
				u *= (delta - 1);
				sigma += u;
			}

			c1 = 8 * (Math.sqrt(1 + x) + 1) / (3 + 1 / (5 + eta + (9 * eta / 7) * sigma));

			if (debug)
				System.out.println("FOO3: " + u + " " + sigma + " " + c1);

			if (n == 1)
			{
				denom = (1 + 2 * x + l) * (3 * c1 + x * c1 + 4 * x);
				h1 = sqr(l + x) * (c1 + 1 + 3 * x) / denom;
				h2 = m * (c1 + x - l) / denom;
			} else
			{
				qr = Math.sqrt(l1 * l1 + m / (y * y));
				xpll = qr - l1;
				lp2xp1 = 2 * qr;
				denom = lp2xp1 * (3 * c1 + x * c1 + 4 * x);
				h1 = (xpll * xpll * (c1 + 1 + 3 * x)) / denom;
				h2 = m * (c1 + x - l) / denom;
			}

			double bb = 1 + h1;
			b = 27 * h2 / (4 * bb * bb * bb);
			u = -b / (2 * (Math.sqrt(b + 1) + 1));
			//*** Compute the continued fraction expansion K(u)
			//*** by means of the 'Top Down' method

			if (debug)
				System.out.println("FOO4: " + u + " " + b);

			delta = 1;
			u0 = 1;
			sigma = 1;
			n1 = 0;

			while (n1 < MAX_ITERS && Math.abs(u0) >= THRESHOLD)
			{
				if (n1 == 0)
				{
					gamma = 4.0 / 27;
					delta = 1 / (1 - gamma * u * delta);
					u0 *= (delta - 1);
					sigma += u0;
				} else
				{
					for (int i8 = 0; i8 < 2; i8++)
					{
						if (i8 == 0)
							gamma =
								2 * (3 * n1 + 1) * (6 * n1 - 1) / (9 * (4 * n1 - 1) * (4 * n1 + 1));
						else
							gamma =
								2 * (3 * n1 + 2) * (6 * n1 + 1) / (9 * (4 * n1 + 1) * (4 * n1 + 3));
						delta = 1 / (1 - gamma * u * delta);
						u0 *= (delta - 1);
						sigma += u0;
					}
				}
				n1++;
			}
			ku = sqr(sigma / 3);
			y = ((1 + h1) / 3) * (2 + Math.sqrt(b + 1) / (1 - 2 * u * ku));
			x0 = Math.sqrt(sqr((1 - l) / 2) + m / sqr(y)) - (1 + l) / 2;

			if (debug)
				System.out.println("FOO5: " + ku + " " + y + " " + x0);
		}

		double cons = m * s * sqr(1 + lambda);
		double a = cons / (8 * x0 * y * y);

		if (debug)
			System.out.println("FOO2: " + cons + " " + a);

		//***  Compute the velocity vectors

		double r11 = sqr(1 + lambda) / (4 * tof * lambda);
		double s11 = y * (1 + x0);
		double t11 = (m * s * sqr(1 + lambda)) / s11;

		if (debug)
			System.out.println("FOO6: " + r11 + " " + s11 + " " + t11);

		this.r1 = r1;
		this.r2 = r2;
		this.U = mu;

		v1 = new Vec3d();
		v2 = new Vec3d();
		v1.x = -r11 * (s11 * (r1.x - r2.x) - t11 * r1.x / rim);
		v2.x = -r11 * (s11 * (r1.x - r2.x) + t11 * r2.x / rfm);
		v1.y = -r11 * (s11 * (r1.y - r2.y) - t11 * r1.y / rim);
		v2.y = -r11 * (s11 * (r1.y - r2.y) + t11 * r2.y / rfm);
		v1.z = -r11 * (s11 * (r1.z - r2.z) - t11 * r1.z / rim);
		v2.z = -r11 * (s11 * (r1.z - r2.z) + t11 * r2.z / rfm);

		if (n1 == MAX_ITERS || n == MAX_ITERS || Double.isNaN(r11 + s11 + t11))
		{
			throw new LambertException("Lambert algorithm did not converge");
		}

	}

	public void solveBisect(Vector3d r1, Vector3d r2, double U, double desiredt, boolean longway)
	{
		int DM = longway ? -1 : 1;
		double sqrtU = Math.sqrt(U);
		double udesiredt = desiredt * sqrtU;
		double r1l = r1.length();
		double r2l = r2.length();
		double cosdv = r1.dot(r2) / (r1l * r2l);
		double sindv = DM * Math.sqrt(1 - cosdv * cosdv);
		double C, S, x, y;
		// evaluate A
		double A = Math.sqrt(r1l * r2l * (1 + cosdv)) * DM;
		if (debug)
			System.out.println("A=" + A + "\tr1=" + r1l + "\tr2=" + r2l);
		// pick trial value for z
		double z = 0; // todo
		double zup = Math.PI * Math.PI * 4;
		double zlow = Math.PI * -4;
		double tdif = 0;

		int iter = 0;
		do
		{
			// todo: fix adjustment
			int adj = 0;
			do
			{
				// pg 230
				C = Kepler.computeC(z); // or "c2"
				S = Kepler.computeS(z); // or "c3"
				// determine aux x,y
				y = r1l + r2l + A * (z * S - 1) / Math.sqrt(C);
				if (A > 0 && y < 0)
				{
					zlow /= 2;
					z = (zlow + zup) / 2;
					System.out.println("adjusting " + A + " " + y);
				} else
					break;
			} while (++adj < MAX_ADJ);

			if (adj >= MAX_ADJ)
			{
				System.out.println("adjustment didn't work! " + zlow + ", " + zup);
				break;
			}

			x = Math.sqrt(y / C);
			if (debug)
				System.out.println("y=" + y + "\tx=" + x);
			// determine t
			double ut = x * x * x * S + A * Math.sqrt(y);
			if (debug)
				System.out.println(udesiredt + "\t" + ut);

			if (Double.isNaN(x) || Double.isNaN(ut))
			{
				System.out.println("NaN: " + y + " " + A + " " + x + " " + ut);
				break;
			}

			// break out when desired precision reached
			tdif = Math.abs(ut - udesiredt) / sqrtU;
			if (tdif < THRESHOLD)
				break;

			// midpoint method
			if (ut < udesiredt)
				zlow = z;
			else
				zup = z;
			z = (zup + zlow) / 2;

		}
		while (++iter < MAX_ITERS);

		if (debug || iter >= MAX_ITERS)
		{
			System.out.println("Lambert had " + iter + " iters!");
			System.out.println(zlow + " < " + z + " < " + zup);
			System.out.println("tdif = " + tdif + ", udesiredt=" + udesiredt);
		}
		// now we're done ... compute the goodness
		double f = 1 - y / r1l;
		double g = A * Math.sqrt(y / U);
		double dg = 1 - y / r2l;
		this.r1 = r1;
		this.r2 = r2;
		this.U = U;
		v1 = new Vec3d(r2.x - f * r1.x, r2.y - f * r1.y, r2.z - f * r1.z);
		v1.scale(1 / g);
		v2 = new Vec3d(dg * r2.x - r1.x, dg * r2.y - r1.y, dg * r2.z - r1.z);
		v2.scale(1 / g);
		//	System.out.println(getConic().getApoapsis() + ", " + getConic().getPeriapsis());
	}

	public Conic getConic()
	{
		return new Conic(r1, v1, U, 0);
	}

	static final double FACTLIMIT = 1e30;

	static double powerSeries(double z, double D, int n)
	{
		int i = 1;
		double denom = D;
		double sum = 1 / denom;
		double num = -1;
		do
		{
			num *= -z * (i + 1) / i;
			denom *= (i * 2 + n) * (i * 2 + n + 1);
			//			System.out.println(i + ":\t" + sum + "\t+ " + num + "/" + denom);
			double newsum = sum + num / denom;
			if (newsum == sum)
				return sum;
			sum = newsum;
			i++;
		} while (i < 200);
		// todo: fix
		throw new IllegalArgumentException("Powerseries! " + i + ", " + z + ", " + D + ", " + n);
	}

	public static boolean useLongWay(Vector3d r1, Vector3d v1, Vector3d r2, Vector3d v2)
	{
		return false; //todo!
		/*
		// Vallado, pg 456
		Vector3d trannml = new Vec3d();
		trannml.cross(r1,r2);
		Vector3d intnml = new Vec3d();
		intnml.cross(r2,v2);
		double dot = trannml.dot(intnml);
		return (dot < 0);
		*/
	}

	public static final void main(String[] args) throws Exception
	{
		System.out.println(powerSeries(100, 24, 3));
		System.out.println(powerSeries(100, 120, 4));
		Vector3d r1 = new Vec3d(0.5, 0.6, 0.7);
		Vector3d r2 = new Vec3d(0.0, 1.0, 0.0);
		double U = 1.1;
		double t = 0.9667663;
		for (int i = 0; i < 3; i++)
		{
			System.out.println(i);
			Lambert gauss = new Lambert();
			//			gauss.debug = true;
			switch (i)
			{
				case 0 :
					gauss.solveold(r1, r2, U, t, false);
					break;
				case 1 :
					gauss.solveBisect(r1, r2, U, t, false);
					break;
				case 2 :
					gauss.solveBattan(r1, r2, U, t, false);
					break;
			}
			System.out.println("\nMETHOD " + i + ":");
			System.out.println("r1=" + gauss.r1);
			System.out.println("v1=" + gauss.v1);
			System.out.println("r2=" + gauss.r2);
			System.out.println("v2=" + gauss.v2);
			Conic orbit = gauss.getConic();
			System.out.println(orbit.getPeriapsis() + " " + orbit.getApoapsis());
		}
	}
}
