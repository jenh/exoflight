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

import java.util.*;

import com.fasterlight.exo.game.NotifyingEvent;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;

/**
  * A class for sequencing operations for launch & maneuvers.
  * todo: what is diff. between "top" and "ship"
  */
public class Sequencer implements PropertyAware
{
	private Game game;
	private String name;
	private int cur;
	private int failseq = -1;
	private List nodes;
	private long t0;
	private Set monitornodes = new HashSet();
	private Map aborts = new TreeMap();
	private List abortlist = new ArrayList();
	private String abortmode;
	private PropertyAware top;
	private PropertyMap vars = new PropertyMap();

	private boolean started;
	private byte laststatcode;

	//

	public Sequencer()
	{
		this.nodes = new ArrayList();
	}
	public Sequencer(Game game)
	{
		this();
		this.game = game;
	}
	public Sequencer(Game game, List nodes)
	{
		this(game);
		this.nodes = Collections.unmodifiableList(nodes);
	}
	public void setGame(Game game)
	{
		this.game = game;
	}
	public Game getGame()
	{
		return game;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
	public List getNodes()
	{
		return Collections.unmodifiableList(nodes);
	}
	public PropertyMap getVars()
	{
		return vars;
	}
	public Object getVar(String key)
	{
		return vars.get(key);
	}
	public void setVar(String key, Object val)
	{
		vars.put(key, val);
	}
	public void addNode(SequencerNode node)
	{
		nodes.add(node);
	}

	public void setAbortOption(String abortid, int nodeidx)
	{
		abortlist.remove(abortid);
		if (nodeidx >= 0)
		{
			aborts.put(abortid, new Integer(nodeidx));
			abortlist.add(abortid);
		}
		else
		{
			aborts.remove(abortid);
		}
	}
	public List getAbortOptions()
	{
		return Collections.unmodifiableList(abortlist);
	}

	public String getAbortMode()
	{
		return abortmode;
	}

	public void setAbortMode(String abortmode)
	{
		this.abortmode = abortmode;
	}

	public void abort()
	{
		Integer ii = (Integer) aborts.get(abortmode);
		if (ii == null && aborts.size() > 0)
			ii = (Integer) aborts.get(abortlist.get(0));
		if (ii != null)
		{
			aborts.clear();
			abortlist.clear();
			setIndex(ii.intValue());
			start();
		}
	}

	public void abort(String abortid)
	{
		if (!"true".equals(abortid))
			setAbortMode(abortid);
		abort();
	}

	public int getIndex()
	{
		return cur;
	}
	public void setIndex(int index)
	{
		boolean wasstart = isStarted();
		if (wasstart)
			stop();
		if (cur != index)
			this.cur = index;
		if (wasstart)
			start();
	}
	public void goToPrev(boolean b)
	{
		if (b)
		{
			int i = getIndex();
			while (--i >= 0)
			{
				if (getNode(i) != null && getNode(i).getDescription() != null)
					setIndex(i);
			}
		}
	}
	public void goToNext(boolean b)
	{
		if (b)
			next();
	}

	public void reset()
	{
		stop();
		setIndex(0);
		failseq = -1;
		vars.clear();
		monitornodes.clear();
		int l = getNodeCount();
		for (int i = 0; i < l; i++)
		{
			SequencerNode node = getNode(i);
			if (node != null)
				node.reset();
		}
	}
	public void stop()
	{
		if (started)
		{
			SequencerNode curnode = getCurrentNode();
			if (curnode != null && curnode.isStarted())
				curnode.stop(this);
			started = false;
		}
	}
	public void start()
	{
		if (!started)
		{
			SequencerNode curnode = getCurrentNode();
			if (curnode != null && !curnode.isStarted())
				curnode.start(this);
			if (curnode != null)
				started = true;
		}
	}
	public boolean isStarted()
	{
		return started;
	}
	public void setStarted(boolean b)
	{
		if (b)
			start();
		else
			stop();
	}
	public void next()
	{
		setIndex(cur + 1);
	}
	public int getNodeCount()
	{
		return nodes.size();
	}
	public SequencerNode getNode(int index)
	{
		if (index < 0 || index >= nodes.size())
			return null;
		else
			return (SequencerNode) nodes.get(index);
	}

	public int getNodeIndex(SequencerNode node)
	{
		for (int i = 0; i < getNodeCount(); i++)
		{
			if (getNode(i) == node)
				return i;
		}
		return -1;
	}

	public void setCurrentNode(SequencerNode node)
	{
		if (node == null)
			throw new RuntimeException("Sequencer node is null");
		int i = getNodeIndex(node);
		if (i >= 0)
			setIndex(i);
		else
			throw new RuntimeException("Could not set node " + node);
	}

	public SequencerNode getCurrentNode()
	{
		return getNode(cur);
	}

	public SequencerNode getPreviousNode()
	{
		return getNode(cur - 1);
	}

	public SequencerNode getNextNode()
	{
		return getNode(cur + 1);
	}

	public String getCurrentDesc()
	{
		int i = cur;
		while (i >= 0 && i < nodes.size())
		{
			String s = getNode(i).getDescription();
			if (s != null)
				return s;
			i--;
		}
		return null;
	}

	public String getPreviousDesc()
	{
		int i = cur;
		while (i >= 0 && i < nodes.size())
		{
			String s = getNode(i).getDescription();
			i--;
			if (s != null)
				break;
		}
		while (i >= 0 && i < nodes.size())
		{
			String s = getNode(i).getDescription();
			if (s != null)
				return s;
			i--;
		}
		return null;
	}

	public String getNextDesc()
	{
		int i = cur + 1;
		while (i >= 0 && i < nodes.size())
		{
			String s = getNode(i).getDescription();
			if (s != null)
				return s;
			i++;
		}
		return null;
	}

	public void setZeroTime(long t0)
	{
		this.t0 = t0;
	}

	public long getZeroTime()
	{
		return t0;
	}

	public void setShip(SpaceShip ship)
	{
		setTop(ship);
		/*
		if (ship != top)
		{
			setTop(ship);
			if (ship != null)
				ship.setSequencer(this);
		}
		*/
	}
	public SpaceShip getShip()
	{
		return (top instanceof SpaceShip) ? (SpaceShip) top : null;
	}

	public void setTop(PropertyAware top)
	{
		if (top != this.top)
		{
			this.top = top;
		}
	}
	public PropertyAware getTop()
	{
		return top;
	}

	public double getMissionTimeSeconds()
	{
		long t = (game.time() - getZeroTime());
		return t * (1d / Constants.TICKS_PER_SEC);
	}

	public String getMissionTimeHMS()
	{
		long t = (game.time() - getZeroTime());
		return AstroUtil.toTimeHMS(t);
	}

	//

	void notify(SequencerNode seqnode)
	{
		game.postEvent(new NodeNotifyEvent(game.time(), seqnode));
	}

	void handleNotify(SequencerNode seqnode)
	{
		if (debug)
			System.out.println(cur + ": notify " + seqnode.getStatus());
		if (seqnode == getCurrentNode())
		{
			int stat = seqnode.getStatus();
			laststatcode = (byte) stat;
			switch (stat)
			{
				case SequencerNode.SUCCESS :
					next();
					break;
				case SequencerNode.FAIL :
					if (failseq >= 0)
						setIndex(failseq);
					else
					{
						notifyFail(seqnode);
						started = false;
					}
					break;
				case SequencerNode.TIMEOUT :
					if (failseq >= 0)
						setIndex(failseq);
					else
					{
						started = false;
						next();
					}
					break;
				case SequencerNode.STOPPED :
					started = false;
					break;
				case SequencerNode.WAIT :
					break;
				case SequencerNode.MONITOR :
					monitornodes.add(seqnode);
					next();
					break;
			}
		}
		else
		{
			if (monitornodes.contains(seqnode))
			{
				switch (seqnode.getStatus())
				{
					case SequencerNode.SUCCESS :
						// todo
						monitornodes.remove(seqnode);
						break;
				}
			}
		}
	}

	public boolean hasFailed()
	{
		return (laststatcode == SequencerNode.FAIL) || (laststatcode == SequencerNode.TIMEOUT);
	}

	//

	public class NodeNotifyEvent extends NotifyingEvent
	{
		SequencerNode seqnode;
		NodeNotifyEvent(long time, SequencerNode seqnode)
		{
			super(time, INFO);
			this.seqnode = seqnode;
		}
		public void handleEvent(Game game)
		{
			handleNotify(seqnode);
		}
		public String getMessage()
		{
			// todo
			return seqnode.getDescription();
		}
	}

	void notifyFail(SequencerNode seqnode)
	{
		game.postEvent(new NodeFailedEvent(game.time(), seqnode));
		if (getShip() != null)
		{
			getShip().getShipWarningSystem().setWarning("SEQFAIL", true);
		}
	}

	public class NodeFailedEvent extends NotifyingEvent
	{
		SequencerNode seqnode;
		NodeFailedEvent(long time, SequencerNode seqnode)
		{
			super(time, WARNING);
			this.seqnode = seqnode;
		}
		public void handleEvent(Game game)
		{
		}
		public String getMessage()
		{
			return "Sequence \""
				+ getName()
				+ "\" failed during "
				+ (seqnode.getDescription() != null ? seqnode.getDescription() : seqnode.toString());
		}
	}

	boolean debug = false;

	public void setDebug(boolean b)
	{
		this.debug = b;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Sequencer.class);

	static {
		prophelp.registerGetSet("ship", "Ship", SpaceShip.class);
		prophelp.registerGetSet("top", "Top", PropertyAware.class);
		prophelp.registerGet("game", "getGame");
		prophelp.registerGetSet("zerotime", "ZeroTime", long.class);
		prophelp.registerGetSet("curnode", "CurrentNode", SequencerNode.class);
		prophelp.registerGet("prevnode", "getPreviousNode");
		prophelp.registerGet("nextnode", "getNextNode");
		prophelp.registerGet("curdesc", "getCurrentDesc");
		prophelp.registerGet("prevdesc", "getPreviousDesc");
		prophelp.registerGet("nextdesc", "getNextDesc");
		prophelp.registerGet("started", "isStarted");
		prophelp.registerSet("started", "setStarted", boolean.class);
		prophelp.registerGetSet("index", "Index", int.class);
		prophelp.registerGet("vars", "getVars");
		prophelp.registerGet("abortoptions", "getAbortOptions");
		prophelp.registerGet("missiontime_secs", "getMissionTimeSeconds");
		prophelp.registerGet("missiontime_hms", "getMissionTimeHMS");
		prophelp.registerSet("abort", "abort", String.class);
		prophelp.registerGetSet("abortmode", "AbortMode", String.class);
		prophelp.registerSet("next", "goToNext", boolean.class);
		prophelp.registerSet("prev", "goToPrev", boolean.class);
		prophelp.registerGet("failed", "hasFailed");
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
