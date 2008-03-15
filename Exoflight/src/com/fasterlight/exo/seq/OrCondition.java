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
package com.fasterlight.exo.seq;


public class OrCondition
implements Condition
{
	private Condition c1,c2;

	public OrCondition(Condition c1, Condition c2)
	{
		this.c1 = c1;
		this.c2 = c2;
	}

	public int evaluate(Sequencer seq)
	{
		int x1 = c1.evaluate(seq);
		int x2 = c2.evaluate(seq);
		return (x1 | x2);
	}

	public String toString()
	{
		return c1 + " or " + c2;
	}
}
