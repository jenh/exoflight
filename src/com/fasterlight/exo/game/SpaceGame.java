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

import java.io.*;
import java.util.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.OrbitTrajectory;
import com.fasterlight.exo.seq.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.exo.strategy.*;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;

//todo: can't we find  away around this?
//import com.fasterlight.glout.GLFontServer;


/**
  * Extends the base Game object to implement
  * Exoflight-specific stuff -- notifications,
  * sequences, agencies, bases, and a Universe object.
  **/
public class SpaceGame
extends Game
implements Constants, PropertyAware
{
	List neos = new ArrayList(); // notifying-event observers

	// game variables
	List agencies;
	List bases;
	long gamestarttime = 0;
	Universe u;

	GameRateGovernor grg;

	private PropertyMap vars = new PropertyMap();
	private Mission curmission;

	PropertyClassLoader pcloader = new PropertyClassLoader();

	private String last_message = "";
	private List messages = new ArrayList();

	public static final int MSG_HMS_COLOR = 0xffdddddd; // white
	public static final int MSG_INFO_COLOR = 0xffffff3f; // cyan
	public static final int MSG_WARNING_COLOR = 0xffff3fff; // cyan
	public static final int MSG_IMPORTANT_COLOR = 0xff3fffff; // yellow
	public static final int MSG_CRITICAL_COLOR = 0xff3f3fff; // red

	//

	public SpaceGame()
	{
		super();
		agencies = new ArrayList();
		u = new Universe(this);
		grg = new GameRateGovernor(this);
	}

	public void addObserver(NotifyingEventObserver neo)
	{
		if (!neos.contains(neo))
			neos.add(neo);
	}

	public void removeObserver(NotifyingEventObserver neo)
	{
		neos.remove(neo);
	}

	protected boolean dispatchEvent(GameEvent event)
	{
		super.dispatchEvent(event);
		if (event instanceof NotifyingEvent)
		{
			return notifyObservers( (NotifyingEvent)event );
		} else
			return false;
	}

	protected boolean notifyObservers(NotifyingEvent event)
	{
		postMessage(event.getMessage(), event.getPriority());
		boolean b = false; // flag means "stop processing events and return"
		Iterator it = neos.iterator();
		while (it.hasNext())
		{
			NotifyingEventObserver neo = (NotifyingEventObserver)it.next();
			if (neo.notifyObservedEvent(event))
				b = true;
		}
		return b;
	}

	public Universe getUniverse()
	{
		return u;
	}

	public Agency addAgency(String agencyname)
	{
		Agency a = new Agency(this, u);
		a.setName(agencyname);
		agencies.add(a);
		return a;
	}

	public int getAgencyCount()
	{
		return agencies.size();
	}

	public Agency getAgency(int i)
	{
		return (Agency)agencies.get(i);
	}

	public Agency getAgency()
	{
		if (getAgencyCount() == 0)
			addAgency("AGENCY");
		return getAgency(0);
	}

	public void setGameStartTime(long time)
	{
		gamestarttime = time;
	}

	public void setGameStartTime(Date date)
	{
		setGameStartTime(AstroUtil.javaDateToGameTick(date));
	}

	protected void setupSolarSystem()
	{
		runSequence("init/SolarSystem.seq");
	}

	protected void setupBases()
	{
		bases = LaunchSites.readLaunchSites(this);
		bases = Collections.unmodifiableList(bases);
	}

	public void setupEarthSats()
	{
		try {
		BufferedReader in;
		String line;
      // read some TLE's
      in = new BufferedReader(new InputStreamReader(
      	ClassLoader.getSystemResourceAsStream("orbits/earth-tles.dat")));
      while ((line=in.readLine()) != null)
      {
      	String name = line.trim();
      	String tle1 = in.readLine();
      	String tle2 = in.readLine();
      	UniverseThing parent = getBody("Earth");
      	KeplerianElements params = new KeplerianElements(tle1, tle2, parent.getMass()*Constants.GRAV_CONST_KM);
      	Conic o = new Conic(params);
      	o = ((Planet)parent).ijk2xyz(o);
      	Trajectory traj = new OrbitTrajectory(parent, o);

      	Structure struct = new Structure(this);
      	struct.addModule(new Module(this, "Comm Satellite"));

      	SpaceShip thing = new SpaceShip(struct);
      	thing.setName(name);
      	thing.setTrajectory(traj);
      }
      in.close();
      } catch (IOException ioe)
      {
      	ioe.printStackTrace();
      	throw new RuntimeException(ioe.toString());
      }
	}

	public UniverseThing getBody(String name)
	{
		return u.getThingByName(name);
	}

/*
todo
	public SortedMap getBodyMapping()
	{
		return ssystem.getBodyMapping();
	}
*/

	public List getThings()
	{
		return u.getThingList();
	}

	public List getPlanets()
	{
		return u.getThingsByClass(Planet.class);
	}

	public List getBases()
	{
		return u.getThingsByClass(SpaceBase.class);
	}

	public List getShips()
	{
		return u.getThingsByClass(SpaceShip.class);
	}

	public void setValue(String key, Object value)
	{
		vars.put(key, value);
	}

	public Object getValue(String key)
	{
		return vars.get(key);
	}

	public PropertyMap getVars()
	{
		return vars;
	}

	public List getVehicles()
	{
		return Vehicle.getVehicleList();
	}

	public void start()
	{
		this.update(gamestarttime);
		setupSolarSystem();
		setupBases();
	}

	public void start(Object o)
	{
		start();
	}

	public Sequencer loadSequence(String scriptpath)
	{
		Sequencer seq = SequencerParser.loadSequence(this, scriptpath);
		return seq;
	}

	public void runSequence(String scriptpath, PropertyAware top)
	{
		Sequencer seq = loadSequence(scriptpath);
		seq.setZeroTime(time());
		seq.setTop(top);
		seq.start();
		long t = time();
		while (seq.isStarted())
			update(0,0);
		if (seq.hasFailed())
			throw new RuntimeException("Sequencer \"" + scriptpath + "\" failed at " +
				seq.getCurrentNode());
	}

	public void runSequence(String scriptpath)
	{
		runSequence(scriptpath, this);
	}

	public String getLocalDateString()
	{
		return AstroUtil.formatDate(AstroUtil.gameTickToJavaDate(time()));
	}

	public String getDateString()
	{
		return getLocalDateString();
	}

	public void setDateString(String s)
	{
		java.util.Date d = AstroUtil.parseDate(s);
		if (d != null)
		{
			setTime(AstroUtil.javaDateToGameTick(d));
		}
	}

	public PropertyClassLoader getPropertyClassLoader()
	{
		return pcloader;
	}

	public Mission getCurrentMission()
	{
		return curmission;
	}

	public void setCurrentMission(Mission curmission)
	{
		if (this.curmission != null)
		{
			Sequencer seq = this.curmission.getSequencer();
			if (seq != null)
				seq.stop();
		}
		this.curmission = curmission;
	}

	public GameRateGovernor getGovernor()
	{
		return grg;
	}

	public void message(Object o)
	{
		if (o == null)
			return;
		String msg = o.toString();
		AlertEvent.postAlert(this, msg);
	}

	/**
	  * This message should only show up on the user's display.
	  * It should not affect the game or be saved with the game log.
	  */
	public void hintMessage(String s)
	{
		last_message = s;
	}

	/**
	  * Post a message, but don't make a big deal out of it.
	  * (don't post an AlertEvent or anything)
	  */
	public void infoMessage(String s)
	{
		postMessage(s, NotifyingEvent.INFO);
	}

	// called from notifyEvent()
	private void postMessage(String msg, int priority)
	{
		if (msg == null)
			return;

		last_message = msg;
		if (curmission != null && curmission.getSequencer() != null)
		//todo: use shaders
			msg =
				/*GLFontServer.*/escapeColor(MSG_HMS_COLOR) +
				curmission.getSequencer().getMissionTimeHMS() + "  " +
				/*GLFontServer.*/escapeColor(getColorForPriority(priority)) +
				msg;
		messages.add(msg);
	}

	// taken from GLFontServer
	public static final char ESCAPE_COLOR = 0xff01;

	public static String escapeColor(int rgba)
	{
		return "" + ESCAPE_COLOR +
			(char)(((rgba>>>0) & 0xff) | 0xfe00) +
			(char)(((rgba>>>8) & 0xff) | 0xfe00) +
			(char)(((rgba>>>16) & 0xff) | 0xfe00) +
			(char)(((rgba>>>24) & 0xff) | 0xfe00);
	}
	//

	private int getColorForPriority(int priority)
	{
		switch (priority)
		{
			case NotifyingEvent.CRITICAL : return MSG_CRITICAL_COLOR;
			case NotifyingEvent.IMPORTANT : return MSG_IMPORTANT_COLOR;
			case NotifyingEvent.WARNING : return MSG_WARNING_COLOR;
			default:
			case NotifyingEvent.INFO : return MSG_INFO_COLOR;
		}
	}

	public String getLastMessage()
	{
		return last_message;
	}

	public List getMessages()
	{
		return Collections.unmodifiableList(messages);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SpaceGame.class);

	static {
		prophelp.registerGet("vehicles", "getVehicles");
		prophelp.registerGet("agency", "getAgency");
		prophelp.registerGet("universe", "getUniverse");
		prophelp.registerGet("localdate", "getLocalDateString");
		prophelp.registerGetSet("date", "DateString", String.class);
		prophelp.registerGet("classloader", "getPropertyClassLoader");
		prophelp.registerSet("start", "start", Object.class);
		prophelp.registerGet("vars", "getVars");
		prophelp.registerGetSet("mission", "CurrentMission", Mission.class);
		prophelp.registerGet("bodies", "getThings");
		prophelp.registerGet("things", "getThings");
		prophelp.registerGet("bases", "getBases");
		prophelp.registerGet("ships", "getShips");
		prophelp.registerGet("planets", "getPlanets");
		prophelp.registerGet("governor", "getGovernor");
		prophelp.registerSet("message", "message", Object.class);
		prophelp.registerSet("runsequence", "runSequence", String.class);
		prophelp.registerGet("lastmessage", "getLastMessage");
		prophelp.registerGet("messages", "getMessages");
	}

	public Object getProp(String key)
	{
		if (key.startsWith("$"))
			return getBody(key.substring(1));

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
