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


/**
  * A single sequence operation
  * @see Sequencer
  */
public abstract class SequencerNode
{
	protected Sequencer seq;
	protected int status;
	protected String desc;

	public static final int IDLE = 0;
	public static final int SUCCESS = 1;
	public static final int STOPPED = 2;
	public static final int FAIL = 3;
	public static final int WAIT = 4;
	public static final int MONITOR = 5;
	public static final int TIMEOUT = 6;

	public static final String STATUS_CHARS = " *SFWMT";

	void reset()
	{
		status = IDLE;
	}
	void start(Sequencer seq)
	{
		this.seq = seq;
		this.status = WAIT;
		if (seq.debug)
			System.out.println("starting " + this);
		startNode();
	}
	void stop(Sequencer seq)
	{
		if (seq.debug)
			System.out.println("stopping " + this);
		stopNode();
		this.seq = null;
		if (status == WAIT)
			this.status = STOPPED;
	}
	protected void startNode()
	{
		// override me
	}
	protected void stopNode()
	{
		// override me
	}
	public Sequencer getSequencer()
	{
		return seq;
	}
	public boolean isStarted()
	{
		return (status >= WAIT);
	}
	public int getStatus()
	{
		return status;
	}
	public void notifySeq(int mode)
	{
		this.status = mode;
		seq.notify(this);
	}

	public String getDescription()
	{
		return desc;
	}
	public void setDescription(String desc)
	{
		this.desc = desc;
	}

	public String toString()
	{
		String s = getDescription();
		return (s != null) ? s : "";
	}
}
