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

import com.fasterlight.util.*;

/**
  * A named resource (any item that can be consumed and represented with
  * a floating-point number).
  * @see ResourceSet
  */
public class Resource
implements java.io.Serializable
{
	static Dictionary resources = new Hashtable();

	static {
		try {
			INIFile ini = new INIFile(
				ClassLoader.getSystemResourceAsStream("etc/resources.txt"));

			Properties props = ini.getSection("Resources");
			Enumeration e = props.propertyNames();
			while (e.hasMoreElements())
			{
				String name = (String)e.nextElement();
				String value = props.getProperty(name);
				Resource res = new Resource(name, value);
				res.massperunit = Util.parseFloat(ini.getString(name, "mass", "1"));
				res.molweight = Util.parseFloat(ini.getString(name, "mol", "1"));
				res.units = ini.getString(name, "units", "kg");
				resources.put(name, res);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Couldn't read resources list: " + ioe);
		}
	}

	String shortname, longname;
	String units;
	float massperunit;
	float molweight;

	public Resource(String shortname, String longname)
	{
		this.shortname = shortname;
		this.longname = longname;
	}

	public String getShortName()
	{
		return shortname;
	}

	public String getLongName()
	{
		return longname;
	}

	public String getUnits()
	{
		return units;
	}

	public float getMolecularWeight()
	{
		return molweight;
	}

	public String toString()
	{
		return shortname;
	}

	public int hashCode()
	{
		return shortname.hashCode();
	}

	public static Resource getResourceByName(String shortname)
	{
		Resource r = (Resource)resources.get(shortname);
		if (r == null)
			throw new IllegalArgumentException(shortname);
		return r;
	}
}

