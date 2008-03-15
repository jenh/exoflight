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


public class NotCondition
implements Condition
{
	private Condition c1;

	public NotCondition(Condition c1)
	{
		this.c1 = c1;
	}

	public int evaluate(Sequencer seq)
	{
		int x1 = c1.evaluate(seq);
		if (x1 < 0)
			return -1;
		else
			return (x1 ^ 1);
	}

	public String toString()
	{
		return "not " + c1;
	}
}
