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

public class PropertySetNode
extends SequencerNode
{
	String key;
	Object value;
	boolean valueprop = false;
	boolean optional = false;

	public PropertySetNode(String key, String value, String desc, boolean valueprop)
	{
		this(key, value, desc, valueprop, false);
	}

	public PropertySetNode(String key, Object value, String desc, boolean valueprop, boolean opt)
	{
		this.key = key;
		this.value = value;
		this.desc = desc;
		this.valueprop = valueprop;
		this.optional = opt;
	}

	protected void startNode()
	{
		try {
			if (valueprop) {
				Object v = PropertyEvaluator.get(seq, (String)value);
				if (v == null)
				{
					notifySeq(optional ? SUCCESS : FAIL);
					return;
				}
				PropertyEvaluator.set(seq, key, v);
			} else {
				PropertyEvaluator.set(seq, key, value);
			}
			notifySeq(SUCCESS);
		} catch (PropertyException pre) {
			System.out.println(pre);
			notifySeq(optional ? SUCCESS : FAIL);
		}
	}

	protected void stopNode()
	{
	}

	public String toString()
	{
		return getDescription() + (valueprop?" (set prop ":" (set ") + key + " to " + value + ")";
	}

}
