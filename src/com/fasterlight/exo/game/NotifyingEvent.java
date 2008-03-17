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
package com.fasterlight.exo.game;

import com.fasterlight.game.GameEvent;

/**
  * An event that is used to notify an observer of
  * important events, and respond accordingly
  **/
public abstract class NotifyingEvent
extends GameEvent
{
	public static final int INFO = 1;
	public static final int WARNING = 2;
	public static final int IMPORTANT = 4;
	public static final int CRITICAL = 8;

	protected int priority;

	//

	public NotifyingEvent(long time)
	{
		this(time, INFO);
	}

	public NotifyingEvent(long time, int priority)
	{
		super(time);
		this.priority = priority;
	}

	public int getPriority()
	{
		return priority;
	}

	public abstract String getMessage();

	public String toString()
	{
		return getMessage();
	}

}
