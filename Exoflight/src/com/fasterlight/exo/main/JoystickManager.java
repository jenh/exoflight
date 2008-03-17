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
package com.fasterlight.exo.main;

import java.util.*;

import sdljava.*;
import sdljava.joystick.SDLJoystick;

import com.fasterlight.spif.*;

/**
 * Handles joystick input for the main program.
 * Maps each joystick axis, button, and hat to a property
 */
public class JoystickManager implements PropertyAware
{
	SDLJoystick[] sticks;
	PropertyAware top;
	Map mappings = new TreeMap();

	//

	public JoystickManager() throws SDLException
	{
		SDLMain.init(SDLMain.SDL_INIT_JOYSTICK);
	}

	public void setPropertyTop(PropertyAware top)
	{
		this.top = top;
	}

	public int getNumJoysticks()
	{
		return SDLJoystick.numJoysticks();
	}

	public void open() throws SDLException
	{
		close();
			
		int n = getNumJoysticks();
		sticks = new SDLJoystick[n];
		for (int i = 0; i < n; i++)
		{
			sticks[i] = SDLJoystick.joystickOpen(i);
		}
	}

	public void close()
	{
		if (sticks != null)
		{
			for (int i = 0; i < sticks.length; i++)
				sticks[i].joystickClose();
			sticks = null;
		}
	}

	/**
	 * Reads all controls and sets properties.
	 */
	public void updateDevices()
	{
		// update all sticks input values
		SDLJoystick.joystickUpdate();
	}

	public float getAxis(int axis)
	{
		return sticks[0].joystickGetAxis(axis);
	}
	
	/**
	 * Runs through all mappings and sets properties to appropriate values.
	 */
	public void processMappings()
	{
		Iterator it = mappings.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry entry = (Map.Entry) it.next();
			Mapping map = (Mapping) entry.getKey();
			PropertyEvaluator eval = (PropertyEvaluator) entry.getValue();

			if (eval != null)
			{
				if (map.isAxis())
				{
					float val = map.getAxisValue();
					eval.set(top, new Float(val));
				}
			}
		}
	}

	//

	/**
	 * Maps a joystick axis or button to a property.
	 */
	class Mapping implements Comparable
	{
		short stick;
		short axis;
		short button;

		float deadzone;

		//

		public boolean isAxis()
		{
			return (axis >= 0);
		}

		public float getAxisValue()
		{
			if (axis < 0)
				throw new RuntimeException("Could not get axis: " + this);
			int ival = sticks[stick].joystickGetAxis(axis);
			float fv = ival / 32768.0f;
			if (fv < deadzone && fv > -deadzone)
				return 0;
			else if (fv > 0)
				return (fv - deadzone) / (1 - deadzone);
			else
				return (deadzone - fv) / (1 - deadzone);
		}

		public boolean getButtonValue()
		{
			if (button < 0)
				throw new RuntimeException("Could not get button: " + this);
			return sticks[stick].joystickGetButton(button);
		}

		public int hashCode()
		{
			return stick ^ ((axis) << 8) ^ ((button) << 16);
		}

		public int compareTo(Object o)
		{
			Mapping map = (Mapping) o;
			if (map.axis != axis)
				return map.axis - axis;
			if (map.stick != stick)
				return map.stick - stick;
			return map.button - button;
		}

		public boolean equals(Object o)
		{
			if (!(o instanceof Mapping))
				return false;
			return compareTo(o) == 0;
		}
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		return null;
	}

	public void setProp(String key, Object value)
	{
		throw new PropertyNotFoundException(key);
	}

}
