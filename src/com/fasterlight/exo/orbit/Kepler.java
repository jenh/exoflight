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

import com.fasterlight.game.Settings;

/**
  * Solve Kepler's equation in a variety of ways.
  * todo: handle parabolic, pass tests
  */
public class Kepler
{
	public static double THRESHOLD =
		Settings.getDouble("Kepler", "Threshold", 1e-12);
	public static int MAX_ITERS = Settings.getInt("Kepler", "MaxIters", 50);

	//

	public static double solve(double ecc, double M)
	{
		return solveHybrid(ecc, M);
	}

	/**
	  * Simple Newton-method-based solver, from Vallado pg. 232.
	  * Only handles 0 <= ecc < 1
	  */
	public static double solveNewton(double ecc, double M)
	{
		if (ecc < 0 || ecc > 1 - THRESHOLD)
			throw new IllegalArgumentException("solveNewton(): must have 0 <= ecc < 1");
		double E, E2;
		int niters = 0;
		// solve Kepler's equation
		M = AstroUtil.fixAngle2(M);
		E = (M < 0) ? M - ecc : M + ecc;
		do
		{
			E2 = E;
			E += (M - E + ecc * Math.sin(E)) / (1 - ecc * Math.cos(E));
		}
		while (Math.abs(E - E2) > THRESHOLD && niters++ <= MAX_ITERS);
		if (niters > MAX_ITERS)
			throw new ConvergenceException(
				"Kepler failed to converge (ecc="
					+ ecc
					+ ", meananom="
					+ M
					+ ")");
		return E;
	}

	/**
	  * Solve Kepler's equation given eccentricity & mean anomaly.
	  * NOTE: Doesn't handle near-parabolic cases well.
	  *
	  * Based on code from Tim Crain (tim.crain1@jsc.nasa.gov)
	  */
	public static double solveHybrid(double ecc, double mean_anom)
	{
		if (ecc < 0)
			throw new IllegalArgumentException("solveHybrid(): must have ecc >= 0");

		double curr;
		int sign = 1;

		if (mean_anom == 0)
		{
			return 0;
		}

		mean_anom = AstroUtil.fixAngle2(mean_anom);

		if (ecc < 0.2)
		{
			// low-eccentricity formula from Meeus,  p. 195
			curr = Math.atan2(Math.sin(mean_anom), Math.cos(mean_anom) - ecc);
			// one correction step,  and we're done
			double err = curr - ecc * Math.sin(curr) - mean_anom;
			curr = curr - err / (1.0 - ecc * Math.cos(curr));
			return curr;
		}
		else if (ecc > 0.999 && ecc < 1.001)
		{
			return solveNearParabolic(ecc, mean_anom);
		}

		if (mean_anom < 0)
		{
			mean_anom = -mean_anom;
			sign = -1;
		}

		curr = mean_anom;

		if (ecc > 0.8
			&& mean_anom < Math.PI / 3
			|| ecc > 1.0) // up to 60 degrees
		{
			double trial = mean_anom / Math.abs(1 - ecc);
			if (trial * trial > 6.0 * Math.abs(1 - ecc))
				// cubic term is dominant
			{
				if (mean_anom < Math.PI)
				{
					trial = Math.pow(6 * mean_anom, 1d / 3);
				}
				else
				{
					// hyperbolic w/ 5th & higher-order terms predominant
					trial = AstroUtil.asinh(mean_anom / ecc);
				}
			}
			curr = trial;
		}

		return sign * solveIterative(ecc, mean_anom, curr);
	}

	private static double solveIterative(
		double ecc,
		double mean_anom,
		double curr)
	{
		double err, trial, correction;
		int n_iter = 0;
		double thresh = THRESHOLD * Math.abs(1 - ecc);

		if (ecc < 1)
		{
			err = curr - ecc * Math.sin(curr) - mean_anom;
			while (Math.abs(err) > thresh)
			{
				n_iter++;
				correction = -err / (1 - ecc * Math.cos(curr));
				curr = curr + correction;
				err = curr - ecc * Math.sin(curr) - mean_anom;

				if (n_iter > MAX_ITERS)
					break;
			}
		}
		else
		{
			err = ecc * AstroUtil.sinh(curr) - curr - mean_anom;
			while (Math.abs(err) > thresh)
			{
				n_iter++;
				correction = -err / (ecc * AstroUtil.cosh(curr) - 1);
				curr = curr + correction;
				err = ecc * AstroUtil.sinh(curr) - curr - mean_anom;

				if (n_iter > MAX_ITERS)
					break;
			}
		}

		if (n_iter > MAX_ITERS)
		{
			throw new ConvergenceException(
				"Kepler failed to converge (ecc="
					+ ecc
					+ ", meananom="
					+ mean_anom
					+ ")");
		}

		return curr;
	}

	public static double solveNearParabolic(double e, double M)
	{
		// series solution
		// Vallado pg 240
		double e2 = e * e;
		double e3 = e2 * e;
		double e4 = e3 * e;
		double e5 = e4 * e;
		double B =
			M
				+ (e - e3 / 8d + e5 / 192d) * Math.sin(M)
				+ (e2 / 2d - e4 / 6d) * Math.sin(2 * M)
				+ (3d / 8d * e3 - 27d / 128d * e5) * Math.sin(3 * M)
				+ (e4 / 3d) * Math.sin(4 * M);
		// refine series solution
		try
		{
			return solveIterative(e, M, B);
		}
		catch (ConvergenceException ce)
		{
			return solveVeryNearParabolic(e, M);
		}
	}

	public static double solveVeryNearParabolic(double ecc, double mean_anom)
	{
		// Vallado p 236
		// solve cubic for parabolic ecc.
		double k1 = 0.7937005259;
		double term1 = Math.sqrt(9 * mean_anom * mean_anom + 4);
		double term2 = 3 * mean_anom;
		double B =
			k1
				* (Math.pow(term1 + term2, 1d / 3)
					- Math.pow(term1 - term2, 1d / 3));
		// apply Simpson's correction
		// Peter Colwell, "Solving Kepler's Equation", pg 60
		double u = B;
		double x =
			((1 - ecc) / 10)
				* Math.tan(u / 2)
				* (4 - 3 * Math.cos(u / 2) - 6 * Math.pow(u / 2, 4));
		return B + x;
	}

	// for universal Kepler solver

	private static double getC(double x)
	{
		double term = 1 / 2d;
		double sum = term;
		int i = 2;
		do
		{
			//			System.out.println("C" + i + "\t" + sum + "\t" + term + "\t" + (i-1)*i);
			i += 2;
			term *= -x / ((i - 1) * i);
			if (Math.abs(term) < 1e-12)
				return sum;
			sum += term;
		}
		while (i < MAX_ITERS);
		System.out.println("C overflow: " + sum);
		return sum;
	}

	private static double getS(double x)
	{
		double term = 1 / 6d;
		double sum = term;
		int i = 3;
		do
		{
			//			System.out.println("S:" + i + "\t" + sum + "\t" + term + "\t" + (i-1)*i);
			i += 2;
			term *= -x / ((i - 1) * i);
			if (Math.abs(term) < 1e-12)
				return sum;
			sum += term;
		}
		while (i < MAX_ITERS);
		System.out.println("S overflow: " + sum);
		return sum;
	}

	public static double computeC(double z)
	{
		if (Math.abs(z) < THRESHOLD)
		{
			return getC(z);
		}
		else if (z > 0)
		{ // z is pos
			double sqrtz = Math.sqrt(z);
			return (1 - Math.cos(sqrtz)) / z;
		}
		else
		{ // z is neg
			double sqrtnz = Math.sqrt(-z);
			return (-Math.exp(sqrtnz) / 2 - Math.exp(-sqrtnz) / 2 + 1) / z;
		}
	}

	public static double computeS(double z)
	{
		if (Math.abs(z) < THRESHOLD)
		{
			return getS(z);
		}
		else if (z > 0)
		{ // z is pos
			double sqrtz = Math.sqrt(z);
			return (sqrtz - Math.sin(sqrtz)) / (z * sqrtz);
		}
		else
		{ // z is neg
			double sqrtnz = Math.sqrt(-z);
			double zz3 = -z * sqrtnz;
			return (Math.exp(sqrtnz) / 2 - Math.exp(-sqrtnz) / 2 - sqrtnz)
				/ zz3;
		}
	}

}
