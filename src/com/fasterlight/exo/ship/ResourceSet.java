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

import java.util.*;

import com.fasterlight.spif.*;
import com.fasterlight.util.Util;

// todo: hotspot

/**
  * A set of resources, essentially a map of Resource -> quantity.
  * Various methods for modifying and querying the set.
  */
public class ResourceSet
implements java.io.Serializable, PropertyAware
{
	public static final ResourceSet EMPTY = new ImmutableResourceSet(new ResourceSet());

	HashMap amounts = new HashMap();

	public ResourceSet()
	{
	}

	public ResourceSet(ResourceSet resset)
	{
		set(resset);
	}

	public ResourceSet(ResourceSet resset, float scale)
	{
		this.add(resset, scale);
	}

	public ResourceSet(String spec)
	{
		set(spec);
	}

	public ResourceSet(String spec, float sc)
	{
		set(spec);
		scale(sc);
	}

	public ResourceSet(Resource r, float sc)
	{
		setAmount(r, sc);
	}

	public void set(String spec)
	{
		clear();
		if (spec.equals("-"))
			return;
		try {
			StringTokenizer st = new StringTokenizer(spec, ",");
			while (st.hasMoreTokens())
			{
				String s = st.nextToken();
				int pos = s.indexOf(':');
				String resname = s.substring(0, pos);
				String resvalue = s.substring(pos+1);
				setAmount(Resource.getResourceByName(resname), Util.parseFloat(resvalue));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("invalid resource set spec: " + spec);
		}
	}

	public float getAmountOf(Resource r)
	{
		Float f = (Float)amounts.get(r);
		if (f == null)
			return 0.0f;
		else
			return (f).floatValue();
	}

	public float getAmountOf(String rname)
	{
		Resource r = Resource.getResourceByName(rname);
		if (r == null)
			return Float.NaN;
		else
			return getAmountOf(r);
	}

	public Iterator getResources()
	{
		return new HashSet(amounts.keySet()).iterator();
	}

	public Iterator getEntries()
	{
		return amounts.entrySet().iterator();
	}

	public int countResources()
	{
		return amounts.size();
	}

	public float mag()
	{
		Iterator it = amounts.values().iterator();
		float total=0;
		while (it.hasNext())
		{
			total += ((Float)it.next()).floatValue();
		}
		return total;
	}

	public float mass()
	{
		float total=0;
		Iterator it = getEntries();
		while (it.hasNext())
		{
			Map.Entry ent = (Map.Entry)it.next();
			Resource r = (Resource)ent.getKey();
			float value = ((Float)ent.getValue()).floatValue();
			total += value * r.massperunit;
		}
		return total;
	}

	public void clear()
	{
		amounts.clear();
	}

	public void setAmount(Resource r, float amt)
	{
		if (amt == 0.0f)
			amounts.remove(r);
		else
			amounts.put(r, new Float(amt));
	}

	public void addAmount(Resource r, float amt)
	{
		setAmount(r, getAmountOf(r)+amt);
	}

	public void add(ResourceSet a)
	{
		add(a, 1);
	}

	public void add(ResourceSet a, float scale)
	{
		Iterator it = a.getEntries();
		while (it.hasNext())
		{
			Map.Entry ent = (Map.Entry)it.next();
			Resource r = (Resource)ent.getKey();
			float value = ((Float)ent.getValue()).floatValue();
			if (a == this)
				ent.setValue(new Float(value+value*scale));
			else
				addAmount(r, value*scale);
		}
	}

	public void scale(float scale)
	{
		Iterator it = getEntries();
		while (it.hasNext())
		{
			Map.Entry ent = (Map.Entry)it.next();
			Resource r = (Resource)ent.getKey();
			float value = ((Float)ent.getValue()).floatValue();
			ent.setValue(new Float(value*scale));
		}
	}

	public void set(ResourceSet a)
	{
		amounts = new HashMap(a.amounts);
	}

	public void sub(ResourceSet a)
	{
		add(a, -1);
	}

	public boolean intersect(ResourceSet a)
	{
		boolean modified = false;
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt = getAmountOf(r);
			float amt2 = a.getAmountOf(r);
			if (amt2 < amt)
			{
				setAmount(r, amt2);
				modified = true;
			}
		}
		return modified;
	}

	public boolean intersect(ResourceSet a, float scale)
	{
		boolean modified = false;
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt = getAmountOf(r);
			float amt2 = a.getAmountOf(r)*scale;
			if (amt2 < amt)
			{
				setAmount(r, amt2);
				modified = true;
			}
		}
		return modified;
	}

	public boolean union(ResourceSet a)
	{
		boolean modified = false;
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt = getAmountOf(r);
			float amt2 = a.getAmountOf(r);
			if (amt2 > amt)
			{
				setAmount(r, amt2);
				modified = true;
			}
		}
		return modified;
	}

	public void div(ResourceSet res)
	{
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt2 = res.getAmountOf(r);
			if (amt2 != 0)
				setAmount(r, this.getAmountOf(r)/amt2);
		}
	}

	public float getMaxAmount()
	{
		float x = 0;
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			x = Math.max(getAmountOf(r), x);
		}
		return x;
	}

	public float getMinAmount()
	{
		float x = 1e30f;
		Iterator it = getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			x = Math.min(getAmountOf(r), x);
		}
		return x;
	}

	public float consume(Resource res, float amt, boolean allornone)
	{
   	if (amt > 0)
   	{
   		float x = getAmountOf(res);
   		float got = allornone ? ((x >= amt)?amt:0) : Math.min(x, amt);
   		if (got > 0)
				setAmount(res, x-got);
   		return got;
   	} else
   		return 0;
	}

	public ResourceSet normalize()
	{
      throw new RuntimeException("TODO");
	}

	public int hashCode()
	{
		return amounts.hashCode();
	}

	public String toString()
	{
		return amounts.toString();
	}

	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof ResourceSet))
			return false;
		return (amounts.equals( ((ResourceSet)o).amounts ));
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ResourceSet.class);

	static {
		prophelp.registerGet("mass", "mass");
		prophelp.registerGet("mag", "mag");
		prophelp.registerGet("maxamount", "getMaxAmount");
		prophelp.registerGet("minamount", "getMinAmount");
		prophelp.registerSet("contents", "set", String.class);
	}

	public Object getProp(String key)
	{
		if (key.startsWith("#"))
			return new Float(getAmountOf(key.substring(1)));
		else if (key.startsWith("%"))
			return new Float(getAmountOf(key.substring(1))*100/mag());
		else
			return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		if (key.startsWith("#"))
		{
			setAmount(Resource.getResourceByName(key.substring(1)), PropertyUtil.toFloat(value));
		} else
			prophelp.setProp(this, key, value);
	}

}

