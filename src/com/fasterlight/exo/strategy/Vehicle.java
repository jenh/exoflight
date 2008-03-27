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

import java.io.IOException;
import java.util.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.seq.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.io.IOUtil;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.Vector3f;

public class Vehicle
implements PropertyAware
{
	protected static INIFile ini;
	protected static Map allvehicles;
	protected static List vehiclelist;

	protected static Orientation PITCH90 = new Orientation();

	static {
		reloadVehicles();
		PITCH90.setPitch(Math.PI/2);
	}

	//

	String id, name, desc;
	List modlinks;
	List initpgms;
	Orientation takeoff_ort = PITCH90;

	//

	public static void reloadVehicles()
	{
		allvehicles = new HashMap();
		vehiclelist = new ArrayList();
		try {
			List inifilenames = IOUtil.getFilesInClassPath("vehicles", ".ini");
			Collections.sort(inifilenames);
			Iterator nit = inifilenames.iterator();
			while (nit.hasNext())
			{
				String ininame = "vehicles/" + (String)nit.next();
				ini = new CachedINIFile(IOUtil.getBinaryResource(ininame));
				List vehiclenames = ini.getSectionNames();
				Iterator it = vehiclenames.iterator();
				while (it.hasNext())
				{
					String vehiclename = (String)it.next();
					Vehicle vehicle = new Vehicle(vehiclename);
					allvehicles.put(vehiclename, vehicle);
					vehiclelist.add(vehicle);
				}
			}
			vehiclelist = Collections.unmodifiableList(vehiclelist);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}


	class ModuleLinkage
	{
		String mname, mrealname;
		int fromdir, todir, pointdir, updir;
		int connidx;
		Vector3f mpos;
		ModuleLinkage(String m, String m2, int fromdir, int todir, int pointdir, int updir, int connidx, Vector3f mpos)
		{
			this.mname=m;
			this.mrealname=m2;
			this.fromdir = fromdir;
			this.todir = todir;
			this.pointdir = (pointdir >= 0) ? pointdir : -1;
			this.updir = (updir >= 0) ? updir : Module.NORTH;
			this.connidx = connidx;
			if (fromdir >= 0 && todir < 0)
				todir = fromdir^1;
			this.mpos = mpos;
		}
	}

	//

	protected Vehicle(String id)
	{
		this.id = id;
		this.modlinks = new ArrayList();
		try {
			this.name = ini.getString(id, "name", id);
			this.desc = ini.getString(id, "desc", "");
			// read module descriptions
			int mi = 0;
			do {
				String tmp = ini.getString(id, "module"+mi, "");
				if (tmp.length() == 0)
					break;

				StringTokenizer st = new StringTokenizer(tmp, ";");
				String mname = st.nextToken();
				int fromdir = -1;
				int todir = -1;
				int pointdir = -1;
				int updir = -1;
				int modindex = -1;

				// decode module linkage
				int nchars = 0;
				if (st.hasMoreTokens())
				{
					String rest = st.nextToken();
					StringBuffer numsb = new StringBuffer();
					for (int i=0; i<rest.length(); i++)
					{
						char ch = rest.charAt(i);
						if (ch >= '0' && ch <= '9')
							numsb.append(ch);
						else {
							int dir = Module.connectCharToDir(ch);
							switch (nchars++)
							{
								case 0 : fromdir = dir; break;
								case 1 : todir = dir; break;
								case 2 : pointdir = dir; break;
								case 3 : updir = dir; break;
							}
						}
					}
					if (numsb.length() > 0)
						modindex = Integer.parseInt(numsb.toString());
				}

				// decode position vector
				Vector3f mpos = null;
				tmp = ini.getString(id, "mpos"+mi, "");
				if (tmp.length() > 0)
				{
					mpos = new Vector3f(AstroUtil.parseVector(tmp));
				}

				// get real name
				tmp = ini.getString(id, "mname"+mi, mname);

				modlinks.add(new ModuleLinkage(mname, tmp, fromdir, todir, pointdir, updir, modindex, mpos));

				tmp = ini.getString(id, "horiz", "false");
				if ("true".equals(tmp))
				{
					// PYR = 0,0,0
					takeoff_ort = new Orientation();
				}

				tmp = ini.getString(id, "takeoff_ort", null);
				if (tmp != null) {
					takeoff_ort = new Orientation();
					takeoff_ort.setEulerPYR(AstroUtil.parseVector(tmp));
				}

			} while (++mi > 0);

			// get list of init sequences
			initpgms = new ArrayList();
			int pi = 0;
			do {
				String pgmname = ini.getString(id, "init"+pi, "");
				if (pgmname.length() == 0)
					break;
				initpgms.add(pgmname);
			} while (++pi > 0);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public String getID()
	{
		return id;
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

	public Orientation getTakeoffOrientation()
	{
		return new Orientation(takeoff_ort);
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof Vehicle))
			return false;
		return ( ((Vehicle)o).id.equals(id) );
	}

	public static Vehicle getVehicle(String id)
	{
		return (Vehicle)allvehicles.get(id);
	}

	public static Iterator getAllVehicles()
	{
		return allvehicles.values().iterator();
	}

	public static List getVehicleList()
	{
		return vehiclelist;
	}

	public Structure toStructure(Agency agency)
	{
		Structure struct = new Structure(agency.game);
		struct.setOwner(agency);

		// link modules together
		Module connectm = null;
		Vector addedmodules = new Vector();
		for (int i=0; i<modlinks.size(); i++)
		{
			ModuleLinkage mlink = (ModuleLinkage)modlinks.get(i);
			Module m = new Module(agency.game, mlink.mname);
			if (!mlink.mname.equals(mlink.mrealname))
				m.setName(mlink.mrealname);
			if (mlink.connidx >= 0)
				connectm = (Module)addedmodules.elementAt(mlink.connidx);
			struct.addModule(m, connectm, mlink.fromdir, mlink.todir,
				mlink.pointdir, mlink.updir);
			// do we set the position manually?
			if (mlink.mpos != null)
			{
				m.setPosition(mlink.mpos);
			}
			// next time, we connect to this module
			connectm = m;
			addedmodules.addElement(m);
		}


		return struct;
	}

	public void initializeShip(SpaceShip ship)
	{
		Game game = ship.getUniverse().getGame();
		// now run the init pgm (sequences)
		for (int i=0; i<initpgms.size(); i++)
		{
			String progname = (String)initpgms.get(i);

				Sequencer seq = SequencerParser.loadSequence(game, progname);
   	      seq.setTop(ship);
   	      seq.start();
   	      // run the game until seq stops
   	      while (seq.isStarted())
   	      	game.update(0, 0);
   	      // if failed, throw an error
   	      if (seq.hasFailed()) {
   	      	throw new RuntimeException("Sequence " + seq.getName() +
   	      		" failed while initializing vehicle " + getName() + ": " +
   	      		seq.getCurrentNode());
   	      }
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Vehicle.class);

	static {
		prophelp.registerGet("id", "getID");
		prophelp.registerGet("name", "getName");
		prophelp.registerGet("description", "getDescription");
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
