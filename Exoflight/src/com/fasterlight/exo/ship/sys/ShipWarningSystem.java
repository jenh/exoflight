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
package com.fasterlight.exo.ship.sys;

import java.util.*;

import com.fasterlight.exo.game.AlertEvent;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.spif.*;

/**
  * Returns a variety of warning & caution booleans about the ship.
  * Warnings are in the form CLASS-ID where CLASS is "GUID", "RCS", etc.
  * Id can be any string.  Recommend uppercase.
  */
public class ShipWarningSystem
extends ShipSystem
implements PropertyAware
{
	private PropertySet warnings = new PropertySet(); // set of strings
	private Set prefixes = new HashSet(); // ditto, but just prefixes

	//

	public ShipWarningSystem(SpaceShip ship)
	{
		super(ship);
	}

	public boolean hasWarning(String name)
	{
		return warnings.contains(name);
	}

	public boolean hasWarningPrefix(String clazz)
	{
		return prefixes.contains(clazz);
	}

	// returns everything before the '-', if there is one
	public static String getWarningPrefix(String name)
	{
		int p = name.indexOf('-');
		if (p > 0)
		{
			return name.substring(0,p);
		} else
			return name;
	}

	public PropertySet getWarnings()
	{
		return warnings;
	}

	public void setWarning(String name, boolean b)
	{
		if (b) {
			if (!warnings.contains(name))
			{
				warnings.add(name);
				prefixes.add(getWarningPrefix(name));
			}
		} else {
			if (warnings.contains(name))
			{
				warnings.remove(name);
				// make sure nothing else with that prefix before removing
				String prefix = getWarningPrefix(name);
				Iterator it = warnings.iterator();
				int count=0;
				while (it.hasNext())
				{
					String warn = (String)it.next();
					if (warn.startsWith(prefix))
						count++;
				}
				if (count == 0)
					prefixes.remove(getWarningPrefix(name));
			}
		}
	}

	public void clearWarning(String name)
	{
		setWarning(name, false);
	}

	public void clearWarningPrefix(String prefix)
	{
		Iterator it = warnings.iterator();
		while (it.hasNext())
		{
			String warn = (String)it.next();
			if (warn.startsWith(prefix))
				it.remove();
		}
		prefixes.remove(prefix);
	}

	public void setWarning(String name, String msg)
	{
		if (!ship.isExpendable() && !hasWarning(name))
		{
			String code = "WARNING-" + name;
			AlertEvent.postAlert(getGame(), msg, AlertEvent.IMPORTANT, code);
		}
		setWarning(name, true);
	}

	public Sequencer loadSequencer()
	{
		return null;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipWarningSystem.class);

	static {
		prophelp.registerGet("warnings", "getWarnings");
	}

	public Object getProp(String key)
	{
		// if key begins with uppercase letter,
		// we want the warning class
		if (key.length() > 0)
		{
			char ch = key.charAt(0);
			if (ch >= 'A' && ch <= 'Z')
				return hasWarningPrefix(key) ? Boolean.TRUE : Boolean.FALSE;
		}
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try {
			// if key begins with uppercase letter,
			// we want the warning class
			if (key.length() > 0)
			{
				char ch = key.charAt(0);
				if (ch >= 'A' && ch <= 'Z')
				{
					clearWarningPrefix(key);
					return;
				}
			}
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}
}
