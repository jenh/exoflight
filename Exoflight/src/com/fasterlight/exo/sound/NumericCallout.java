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
package com.fasterlight.exo.sound;

import java.text.MessageFormat;

import com.fasterlight.exo.orbit.Constants;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;

public class NumericCallout
extends VoiceCallout
{
	protected static MessageFormat defaultFormat = new MessageFormat("{0,number,#.#}");

	protected float rangelo = -1e30f;
	protected float rangehi =  1e30f;
	protected MessageFormat format = defaultFormat;
	protected String value_prop;
	protected float value_scale = 1;
	protected float value_step = 1;
	protected float factor = 1;

	protected float last_update_value;

	//

	public float getRangeLo()
	{
		return rangelo;
	}

	public void setRangeLo(float rangelo)
	{
		this.rangelo = rangelo;
	}

	public float getRangeHi()
	{
		return rangehi;
	}

	public void setRangeHi(float rangehi)
	{
		this.rangehi = rangehi;
	}

	public float getValueStep()
	{
		return value_step;
	}

	public void setValueStep(float value_step)
	{
		this.value_step = value_step;
	}

	public String getMessageFormat()
	{
		return format.toPattern();
	}

	public void setMessageFormat(String format)
	{
		this.format = new MessageFormat(format);
	}

	public String getPropertyForValue()
	{
		return value_prop;
	}

	public void setPropertyForValue(String value_prop)
	{
		this.value_prop = value_prop;
	}

	public float getValueScale()
	{
		return value_scale;
	}

	public void setValueScale(float value_scale)
	{
		this.value_scale = value_scale;
	}

	public float getFactor()
	{
		return factor;
	}

	public void setFactor(float factor)
	{
		this.factor = factor;
	}

	//

	public float getValue()
	{
		if (value_prop == null || top == null)
			return 0;
		Object o = PropertyEvaluator.get(top, value_prop);
		if (o == null)
			return 0;
		float v = PropertyUtil.toFloat(o);
		// todo: doesn't work when decreasing
		v = (float)(Math.floor(v/value_step)*value_step);
		return v;
	}

	public float getPriority(long time)
	{
		float value = getValue();
		if (value < rangelo || value >= rangehi)
			return 0;
		float prio = Math.abs(value - last_update_value);
		if (prio == 0)
			return 0;
		if (last_update_time == Game.INVALID_TICK)
			last_update_time = time;
		prio *= (time - last_update_time)*(1f/Constants.TICKS_PER_SEC);
		prio *= factor;
//		System.out.println(this + " = " + value + ", " + prio);
		return prio;
	}

	public String getMessage(long time)
	{
		float value = getValue();
		last_update_value = value;
		Object[] arr = { new Float(value*value_scale) };
		return format.format(arr);
	}

	public String toString()
	{
		return "[NumericCallout:" + value_prop + '*' + value_scale + ", " + rangelo + "<=x<" + rangehi + " -> " + format + ']';
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(NumericCallout.class);

	static {
		prophelp.registerGetSet("rangelo", "RangeLo", float.class);
		prophelp.registerGetSet("rangehi", "RangeHi", float.class);
		prophelp.registerGetSet("value_step", "ValueStep", float.class);
		prophelp.registerGetSet("format", "MessageFormat", String.class);
		prophelp.registerGetSet("value_prop", "PropertyForValue", String.class);
		prophelp.registerGetSet("value_scale", "ValueScale", float.class);
		prophelp.registerGetSet("factor", "Factor", float.class);
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try {
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}

}
