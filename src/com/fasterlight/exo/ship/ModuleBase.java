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
package com.fasterlight.exo.ship;

import java.io.IOException;
import java.util.*;

import com.fasterlight.game.Game;
import com.fasterlight.io.IOUtil;
import com.fasterlight.util.*;

/**
  * Base class for Module.
  * @see Module.
  */
public abstract class ModuleBase
implements java.io.Serializable
{
	private static Map index = new HashMap();

	static {
		try {
			List filelist = IOUtil.getFilesInClassPath("modules", ".ini");
			Iterator it = filelist.iterator();
			while (it.hasNext())
			{
				String inifile = (String)it.next();
				INIFile ini = new INIFile(IOUtil.getBinaryResource(
					"modules/" + inifile));
				List v = ini.getSectionNames();
				Iterator vit = v.iterator();
				while (vit.hasNext())
				{
					String sect = (String)vit.next();
					if (sect.indexOf(" -- ") < 0)
					{
						index.put(sect, inifile);
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Couldn't read module index: " + ioe);
		}
	}

	protected Game game;
	protected String type;

	protected INIFile settings;

   public ModuleBase(Game game, String type)
   {
   	this.game = game;
   	this.type = type;
   	settings = lookupModuleIndex(type);
   }

   private static INIFile lookupModuleIndex(String type)
   {
   	try {
	   	String filename = (String)index.get(type);
	   	if (filename == null)
	   		filename = "Default.ini";
   		return new CachedINIFile(IOUtil.getBinaryResource("modules/" + filename));
   	} catch (IOException ioe) {
   		throw new RuntimeException(ioe.toString());
   	}
   }

   public String getType()
   {
   	return type;
   }

	// RESOURCE-GETTING

	public static String trimPeriod(String s)
	{
		int pos = s.lastIndexOf('.');
		if (pos >= 0)
			s = s.substring(pos+1);
      return s;
	}

   public String getIdentifier()
   {
   	//return trimPeriod(this.getClass().getName());
   	return type;
   }

	protected String getSetting(String key, String dflt)
	{
		String section = this.getIdentifier();
		try {
			//System.err.println("Looking for [" + section + "] = " + key);
			return settings.getString(section, key, dflt);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Key " + key + " not found");
		}
	}

	protected Properties getModuleSettings()
	{
		String section = this.getIdentifier();
		try {
			return settings.getSection(section);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Section " + section + " not found");
		}
	}

	protected int getSettingInt(String key, int dflt)
	{
		return Integer.parseInt(getSetting(key, Integer.toString(dflt)));
	}

	protected float getSettingFloat(String key, float dflt)
	{
		return Util.parseFloat(getSetting(key, Float.toString(dflt)));
	}

/***
	public static String[] getCategoryNames()
	{
		String s;
		try {
			s = index.getString("SETUP", "Categories", "");
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
		StringTokenizer st = new StringTokenizer(s, ",");
		Vector v = new Vector();
		while (st.hasMoreTokens())
		{
			String cat = st.nextToken();
			v.addElement(cat);
		}
		String[] arr = new String[v.size()];
		v.copyInto(arr);
		return arr;
	}

	// todo: don't make 1-1 between classes & components
	public static String[] getComponentNames(String category)
	{
		try {
			Properties props = index.getSection("cat_" + category);
			Enumeration e = props.propertyNames();
			Vector v = new Vector();
			while (e.hasMoreElements())
			{
				String name = (String)e.nextElement();
				v.addElement(name);
			}
			String[] arr = new String[v.size()];
			v.copyInto(arr);
			return arr;
		} catch (Exception ioe) {
			ioe.printStackTrace();
			throw new RuntimeException(ioe.toString());
		}
	}

	public static Class getComponentClass(String name)
	{
		try {
			String classname = index.getString(name, "class", name);
			if (classname.indexOf('.') < 0)
				classname = "com.fasterlight.exo.cmpts." + classname;
			return Class.forName(classname);
		} catch (Exception ioe) {
			ioe.printStackTrace();
			throw new RuntimeException(ioe.toString());
		}
	}
***/

	public static Enumeration getAllModuleNames()
	{
		return Collections.enumeration(index.keySet());
	}

}

