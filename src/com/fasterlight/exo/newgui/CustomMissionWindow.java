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
package com.fasterlight.exo.newgui;

import java.util.Date;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.exo.ship.SpaceBase;
import com.fasterlight.exo.strategy.*;
import com.fasterlight.glout.GLOActionEvent;
import com.fasterlight.spif.*;

/**
  * Lets you select a mission start time, vehicle, and
  * starting base.
  */
public class CustomMissionWindow
extends NewVehicleWindow
{
	long mission_start_time;
	long mission_elapsed_time;
	boolean starts_now;

	//

	public CustomMissionWindow()
	{
		super();

		if (game.getCurrentMission() != null)
		{
			Mission miss = game.getCurrentMission();
			setMissionStartTime(miss.getStartTime());
			setMissionElapsedTime(miss.getElapsedTime());
		}
	}

	public long getMissionStartTime()
	{
		return starts_now ? AstroUtil.javaDateToGameTick(new Date()) : mission_start_time;
	}

	public void setMissionStartTime(long mission_start_time)
	{
		this.mission_start_time = mission_start_time;
	}

	public long getMissionElapsedTime()
	{
		return mission_elapsed_time;
	}

	public void setMissionElapsedTime(long mission_elapsed_time)
	{
		this.mission_elapsed_time = mission_elapsed_time;
	}

	public String getMissionStartTimeString()
	{
		return AstroUtil.formatDate(AstroUtil.gameTickToJavaDate(getMissionStartTime()));
	}

	public void setMissionStartTimeString(String mission_start_time)
	{
		Date d = AstroUtil.parseDate(mission_start_time);
		this.mission_start_time = AstroUtil.javaDateToGameTick(d);
	}

	public String getMissionElapsedHMSString()
	{
		return AstroUtil.toTimeHMS(mission_elapsed_time);
	}

	public void setMissionElapsedHMSString(String mission_elapsed_time)
	{
		this.mission_elapsed_time = AstroUtil.dbl2tick(
			AstroUtil.parseTimeHMS(mission_elapsed_time));
	}

	public boolean getStartsNow()
	{
		return starts_now;
	}

	public void setStartsNow(boolean starts_now)
	{
		this.starts_now = starts_now;
	}


	public void dialogApply(String s)
	{
		Vehicle v = getCurrentVehicle();
		SpaceBase b = getCurrentBase();
		long t1 = getMissionStartTime();
		long t2 = t1 + getMissionElapsedTime();

		Mission m = new Mission(v, b, t1, t2);

		SpaceGame game = ((GUIContext)ctx).createNewGame(m);
		ctx.deliverEvent(new GLOActionEvent(ctx, game), this);
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(CustomMissionWindow.class);

	static {
		prophelp.registerGetSet("mission_start_time", "MissionStartTime", long.class);
		prophelp.registerGetSet("mission_elapsed_time", "MissionElapsedTime", long.class);
		prophelp.registerGetSet("mission_start_time_str", "MissionStartTimeString", String.class);
		prophelp.registerGetSet("mission_elapsed_time_str", "MissionElapsedHMSString", String.class);
		prophelp.registerGetSet("starts_now", "StartsNow", boolean.class);
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
