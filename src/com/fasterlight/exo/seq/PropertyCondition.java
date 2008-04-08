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

import com.fasterlight.spif.*;

public class PropertyCondition
implements Condition
{
	private Object obj;
	private String prop1, prop2;
	private int op;

	public PropertyCondition(String prop1, Object obj, int op)
	{
		this.prop1 = prop1;
		this.obj = obj;
		this.op = op;
	}

	public PropertyCondition(String prop1, String prop2, int op, boolean b)
	{
		this.prop1 = prop1;
		this.prop2 = prop2;
		this.op = op;
	}

	private Object getArg1(Sequencer seq)
	{
		return PropertyEvaluator.get(seq, prop1);
	}

	private Object getArg2(Sequencer seq)
	{
		if (prop2 != null)
			return PropertyEvaluator.get(seq, prop2);
		else
			return obj;
	}

	public String toString()
	{
		if (prop2 != null)
			return "('" + prop1 + "' " + opStrings[op] + " '" + prop2 + "')";
		else
			return "(" + prop1 + " " + opStrings[op] + " " + obj + ")";
	}

	public int evaluate(Sequencer seq)
	{
		Object o1,o2;
		try {
			o1 = getArg1(seq);
			o2 = getArg2(seq);
		} catch (PropertyException pre) {
			return -1;
		}

		if (o1 == null)
			return (o2 == null)?1:0;
		if (o2 == null)
			return 0;

		int c;
		if (o1 instanceof Number && o2 instanceof Number)
		{
			double x = ((Number)o1).doubleValue() - ((Number)o2).doubleValue();
			c = (x == 0) ? 0 : (x<0) ? -1 : 1;
		}
		else if (o1 instanceof Comparable)
		{
			c = ((Comparable)o1).compareTo( o2 );
		}
		else {
			c = o1.equals(o2) ? 0 : 1;
		}

		boolean b;
		switch (op) {
			case OP_EQ : b = (c==0); break;
			case OP_NE : b = (c!=0); break;
			case OP_GT : b = (c>0); break;
			case OP_LE : b = (c<=0); break;
			case OP_LT : b = (c<0); break;
			case OP_GE : b = (c>=0); break;
			default : throw new RuntimeException("Invalid compare op " + op);
		}

		return (b) ? 1 : 0;
	}
}
