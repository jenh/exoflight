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
package com.fasterlight.exo.strategy;

import java.io.*;
import java.util.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.*;
import com.fasterlight.io.IOUtil;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;

/**
  * A Mission is a "scenario" that starts at a specific time,
  * involves one or more vehicles, and is scriptable.
  */
public class Mission
implements PropertyAware
{
	protected static Map allmissions = new HashMap();
	protected static List missionlist = new ArrayList();

	String name, desc, seqname, basename;
	long curtime, starttime;
	Vehicle vehicle;

	Sequencer seq;

	//

	/**
	  * Returns a list of all categories (type String).
	  */
	public static List getCategories()
	{
     	List categlist = new ArrayList();
		try {
			List fileList = IOUtil.getFilesInClassPath("missions", ".ini");
			Iterator it = fileList.iterator();
			while (it.hasNext())
			{
				Properties props = getCategoriesFromINI(it);
				categlist.addAll(props.keySet());
			}
			Collections.sort(categlist);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		categlist = Collections.unmodifiableList(categlist);
      return categlist;
	}

	private static Properties getCategoriesFromINI(Iterator it) throws IOException
	{
		String iniPath = "missions/" + it.next();
		InputStream in = IOUtil.getBinaryResource(iniPath);
		INIFile ini = new INIFile(in);
		Properties props = ini.getSection("Categories");
		return props;
	}

	/**
	  * Returns a list of all missions in a specific category.
	  * Category must be equal to one returned by getCategories().
	  */
	public static List getMissions(String categname)
	{
		try {
			List misslist = new ArrayList();

			List missfiles = IOUtil.getFilesInClassPath(
				"missions/" + categname, ".ini");
			Iterator miit = missfiles.iterator();
			while (miit.hasNext())
			{
				String missinipath = (String)miit.next();
				INIFile ini = new CachedINIFile(IOUtil.getBinaryResource(
					"missions/" + categname + "/" + missinipath));

				List missnames = ini.getSectionNames();
				Iterator it = missnames.iterator();
				while (it.hasNext())
				{
					String missname = (String)it.next();
					Mission miss = new Mission(ini, categname, missname);
					misslist.add(miss);
				}
			}
			misslist = Collections.unmodifiableList(misslist);
			return misslist;
		} catch (IOException ioe) {
			System.out.println("Error reading category " + categname);
			ioe.printStackTrace();
			return null;
		}
	}

	public static Mission getMission(String categname, String missname)
	{
      List l = getMissions(categname);
      if (l == null)
         return null;
		Iterator it = l.iterator();
		while (it.hasNext())
		{
			Mission m = (Mission)it.next();
         if (m.getName().equals(missname))
            return m;
		}
      return null;
	}

	///

	public Mission(Vehicle vehicle, SpaceBase base, long starttime, long curtime)
	{
		this.vehicle = vehicle;
		this.basename = base.getName();
		this.starttime = starttime;
		this.curtime = curtime;
	}

	protected Mission(INIFile ini, String categname, String id)
	{
		this.name = id;
		try {
			String tmp;
			this.desc = ini.getString(id, "desc", "");
			tmp = ini.getString(id, "date", "");
			if (tmp.length() > 0)
			{
				Date d = AstroUtil.parseDate(tmp);
				curtime = AstroUtil.javaDateToGameTick(d);
				starttime = curtime;
			}
			tmp = ini.getString(id, "start", "");
			if (tmp.length() > 0)
			{
				Date d = AstroUtil.parseDate(tmp);
				starttime = AstroUtil.javaDateToGameTick(d);
			}
			tmp = ini.getString(id, "get", "");
			if (tmp.length() > 0)
			{
				curtime = starttime + (long)(AstroUtil.parseTimeHMS(tmp)*Constants.TICKS_PER_SEC);
			}
			tmp = ini.getString(id, "tick", "");
			if (tmp.length() > 0)
			{
				curtime = Long.parseLong(tmp);
			}
			tmp = ini.getString(id, "vehicle", "");
			if (tmp.length() > 0)
			{
				vehicle = Vehicle.getVehicle(tmp);
            if (vehicle == null)
               System.out.println("Mission " + name + ": Could not find vehicle " + tmp);
			}
			basename = ini.getString(id, "base", "");
			seqname = ini.getString(id, "sequence",
				"missions/" + categname + "/" + name);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public String getID()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}

	public String getDescription()
	{
		return desc;
	}

	public String toString()
	{
		return name;
	}

	public long getElapsedTime()
	{
		return curtime-starttime;
	}

	public long getStartTime()
	{
		return starttime;
	}

	public java.util.Date getStartDate()
	{
		return AstroUtil.gameTickToJavaDate(getElapsedTime()+getStartTime());
	}

	public java.util.Date getMissionStartDate()
	{
		return AstroUtil.gameTickToJavaDate(getStartTime());
	}

	public Vehicle getVehicle()
	{
		return vehicle;
	}

	public String getBaseName()
	{
		return basename;
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof Mission))
			return false;
		return ( ((Mission)o).name.equals(name) );
	}

	public int hashCode()
	{
		return name.hashCode();
	}

	/**
	  * Starts the mission going
	  * Must be given a virgin SpaceGame (not started)
	  */
	public void prepare(SpaceGame game)
	{
		game.setTime(curtime);
		// now we gotta setup the solar system
		game.start();
		game.setCurrentMission(this);
		SpaceShip ship;
		if (basename != null && basename.length() > 0)
		{
			UniverseThing ut = game.getBody(basename);
			if (!(ut instanceof SpaceBase))
				throw new RuntimeException("Could not find base " + basename);
			ship = game.getAgency().prepareVehicle(vehicle, (SpaceBase)ut);
		} else {
			// put spaceship around Earth at 300 km circular orbit
			// the mission designer can move it later
			ship = game.getAgency().prepareVehicle(vehicle);
		}
		// now we run the script
		if (seqname != null)
		{
			seq = game.loadSequence(seqname + ".seq");
			seq.setZeroTime(starttime);
			seq.setShip(ship);
		}
	}

   /**
     *  Call after prepare() to get the sequencer for this
     * mission
     */
   public Sequencer getSequencer()
   {
      return seq;
   }


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Mission.class);

	static {
		prophelp.registerGet("id", "getName");
		prophelp.registerGet("name", "getName");
		prophelp.registerGet("description", "getDescription");
		prophelp.registerGet("startdate", "getStartDate");
		prophelp.registerGet("missionstartdate", "getMissionStartDate");
		prophelp.registerGet("vehicle", "getVehicle");
		prophelp.registerGet("basename", "getBaseName");
		prophelp.registerGet("sequencer", "getSequencer");
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	//

	public static void main(String[] args)
	throws Exception
	{
		List l = getCategories();
		for (int i=0; i<l.size(); i++)
		{
			String categ = l.get(i).toString();
			System.out.println("CATEGORY " + categ);
			List m = getMissions(categ);
			for (int j=0; j<m.size(); j++)
			{
				Mission mm = (Mission)m.get(j);
				System.out.println(mm);
			}
		}
	}
}
