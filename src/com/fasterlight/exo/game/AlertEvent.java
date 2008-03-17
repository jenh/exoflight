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

import com.fasterlight.game.Game;

/**
  * This event allows to send an "alert", like a dialog box that
  * pops up an important message.
  **/
public class AlertEvent
extends NotifyingEvent
{
	/**
	  * A text message describing the event
	  */
	protected String msg;
	/**
	  * A string code identifying the event for lookup of
	  * sound, logging, or other info.
	  */
	protected String code;

	//

	public AlertEvent(long time)
	{
		super(time);
	}

	public AlertEvent(long time, int priority)
	{
		super(time, priority);
	}

	public AlertEvent(long time, int priority, String msg)
	{
		super(time, priority);
		this.msg = msg;
	}

	public AlertEvent(long time, int priority, String msg, String code)
	{
		this(time, priority, msg);
		this.code = code;
	}

	public void handleEvent(Game game)
	{
		// default behavior: nothing, just alerts
	}

	public String getMessage()
	{
		return msg;
	}

	public String getCode()
	{
		return code;
	}

	public static void postAlert(Game game, String msg)
	{
		postAlert(game, msg, IMPORTANT);
	}

	public static void postAlert(Game game, String msg, String code)
	{
		postAlert(game, msg, IMPORTANT, code);
	}

	public static void postAlert(Game game, String msg, int prio)
	{
		game.postEvent(new AlertEvent(game.time(), prio, msg));
	}

	public static void postAlert(Game game, String msg, int prio, String code)
	{
		game.postEvent(new AlertEvent(game.time(), prio, msg, code));
	}

}
