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

public class ConditionWaitNode
extends SequencerNode
{
	private Condition cond;
	private long interval, timeout;
	private GameEvent event;

	private long starttime;

	public ConditionWaitNode(Condition cond, long interval)
	{
		this.cond = cond;
		this.interval = interval;
	}

	public ConditionWaitNode(Condition cond, long interval, long timeout)
	{
		this(cond, interval);
		this.timeout = timeout;
	}

	public ConditionWaitNode(Condition cond, String desc, long interval)
	{
		this.cond = cond;
		this.interval = interval;
		this.desc = desc;
	}

	public ConditionWaitNode(Condition cond, String desc, long interval, long timeout)
	{
		this(cond, desc, interval);
		this.timeout = timeout;
	}

	protected void startNode()
	{
		starttime = seq.getGame().time();
		event = new ConditionWaitEvent(starttime);
		seq.getGame().postEvent(event);
	}

	protected void stopNode()
	{
		seq.getGame().cancelEvent(event);
		event = null;
	}

	public String toString()
	{
		return getDescription() + "(Condition " + cond + " every " + AstroUtil.toDuration(interval*(1d/Constants.TICKS_PER_SEC)) + ")";
	}

	//

	class ConditionWaitEvent
	extends GameEvent
	{
		ConditionWaitEvent(long time)
		{
			super(time);
		}
		public void handleEvent(Game game)
		{
			int eval = cond.evaluate(getSequencer());
			if (eval > 0)
			{
				event = null;
				notifySeq(SUCCESS);
			}
			else if (eval < 0) {
				event = null;
				notifySeq(FAIL);
			}
			else {
				eventtime += interval;
				if (timeout > 0 && eventtime >= starttime + timeout)
					notifySeq(TIMEOUT);
				else
					getSequencer().getGame().postEvent(this);
			}
		}
	}

}
