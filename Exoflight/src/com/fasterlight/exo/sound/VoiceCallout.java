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

import com.fasterlight.game.Game;
import com.fasterlight.spif.*;

public abstract class VoiceCallout
implements PropertyAware
{
	protected long last_update_time = Game.INVALID_TICK;
	protected PropertyAware top;

	//

	public PropertyAware getTop()
	{
		return top;
	}

	public void setTop(PropertyAware top)
	{
		this.top = top;
	}

	public abstract float getPriority(long time);

	public abstract String getMessage(long time);

	public void reset(long time)
	{
		this.last_update_time = time;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(VoiceCallout.class);

	static {
		prophelp.registerGetSet("top", "Top", PropertyAware.class);
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
