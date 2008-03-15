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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;

public class TimeWaitNode
extends SequencerNode
{
	public static final int RELATIVE = 0;
	public static final int ABSOLUTE = 1;
	public static final int DURATION = 2;

	private long time, targettime;
	private int mode;
	private GameEvent event;
	private PropertyEvaluator time_prop;

	//

	public TimeWaitNode(long time, int mode)
	{
		this.time = time;
		this.mode = mode;
	}

	public TimeWaitNode(String propkey, int mode)
	{
		this.time_prop = new PropertyEvaluator(propkey);
		this.mode = mode;
	}

	public long getTicks()
	{
		if (time_prop != null)
			return AstroUtil.dbl2tick(PropertyUtil.toDouble(time_prop.get(this)));
		else
			return time;
	}

	protected void startNode()
	{
		long tt = getTicks();
		switch (mode)
		{
			case DURATION: tt += seq.getGame().time(); break;
			case RELATIVE: tt += seq.getZeroTime(); break;
		}
		// if time is in the past,
		// reset sequence time and continue
		long curtime = seq.getGame().time();
		if (tt < curtime)
		{
			seq.setZeroTime(curtime - getTicks());
			tt = curtime;
			// todo: issue warning
		}
		event = new TimeWaitEvent(tt);
		seq.getGame().postEvent(event);
		if (seq.debug)
			System.out.println(tt + ", " + event);
	}

	protected void stopNode()
	{
		seq.getGame().cancelEvent(event);
		event = null;
	}

	//

	class TimeWaitEvent
	extends GameEvent
	{
		TimeWaitEvent(long time)
		{
			super(time);
		}
		public void handleEvent(Game game)
		{
			event = null;
			notifySeq(SUCCESS);
		}
	}

	public String toString()
	{
		String desc = super.toString();
		double dt = time*1d/Constants.TICKS_PER_SEC;
		if (time_prop != null)
		{
			return desc+" (wait for `" + time_prop + "`)";
		} else {
			switch (mode)
			{
				case RELATIVE:
					return desc+" (wait until " + AstroUtil.toTimeHMS(time) + ")";
				case DURATION:
					return desc+" (wait for " + AstroUtil.toDuration(dt) + ")";
				case ABSOLUTE:
					return desc+" (wait until " + AstroUtil.formatDate(AstroUtil.gameTickToJavaDate(time)) + ")";
				default:
					return "?";
			}
		}
	}

}
