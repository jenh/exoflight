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

/**
  * Stumpff equations .. not yet used.
  */
public class Stumpff
{
	public double c0,c1,c2,c3;

	/**
	  * FEC, pg 78
	  */
	public Stumpff(double x)
	{
		int n;
		double a,b,c0,c1,c2,c3;

		n = 0;
		do
		{
			n++;
			x /= 4;
		} while (Math.abs(x) > 0.1);

  a = (1.0 - x * (1.0 - x / 182.0) / 132.0);
  b = (1.0 - x * (1.0 - x * a / 90.0) / 56.0);
  c2 = (1.0 - x * (1.0 - x * b / 30.0) / 12.0) / 2.0;
  a = (1.0 - x * (1.0 - x / 210.0) / 156.0);
  b = (1.0 - x * (1.0 - x * a / 110.0) / 72.0);
  c3 = (1.0 - x * (1.0  - x * b / 42.0)/ 20.0) / 6.0;

  c1 = 1.0 - x * c3;
  c0 = 1.0 - x * c2;

  do {
    n--;
    c3 = (c2 + c0 * c3) / 4.0;
    c2 = c1 * c1 / 2.0;
    c1 = c0 * c1;
    c0 = 2.0 * c0 * c0 - 1.0;
    x = x * 4.0;
  } while (n > 0);

		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
	}
}
