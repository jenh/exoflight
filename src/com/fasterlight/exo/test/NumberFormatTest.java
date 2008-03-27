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

import java.text.*;

import junit.framework.TestCase;

public class NumberFormatTest
extends TestCase
{
	public NumberFormatTest(String s)
	{
		super(s);
	}

	public void test()
	{
		NumberFormat nf = new DecimalFormat("###.#");
		for (int i=0; i<100000; i++)
		{
			double x = (i*1.99999);
//			String s = Double.toString(x);
			String s = nf.format(x);
		}
	}
}
