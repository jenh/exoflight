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

public interface Condition
{
	public static final int OP_EQ = 0;
	public static final int OP_NE = 1;
	public static final int OP_GT = 2;
	public static final int OP_LE = 3;
	public static final int OP_LT = 4;
	public static final int OP_GE = 5;

	public static final String[] opStrings = {
		"==", "!=", ">", "<=", "<=", ">="
	};

	/**
	  * Returns 0 for false, 1 for true, -1 for "could not evaluate"
	  */
	public int evaluate(Sequencer seq);
}
