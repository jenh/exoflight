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
package com.fasterlight.exo.newgui;

import java.util.*;

import com.fasterlight.game.*;
import com.fasterlight.glout.GLODialog;
import com.fasterlight.spif.*;

/**
  * A type of Dialog box that helps you with settings.
  * Supports Cancel, Accept actions.
  */
public class SettingsWindow
extends GLODialog
{
	// contains key-value pairs
	// key is in form "section/key"
	Settings settings = new Settings();
	PropertyMap cachedSettings = new CachedSettingsMap();

	class CachedSettingsMap
	extends PropertyMap
	{
		// if not found in map, looks for key in Settings class
		public Object get(Object key)
		{
			Object value = super.get(key);
			if (value == null && key != null)
				return settings.getProp(key.toString());
			else
				return value;
		}
	}

	//

	public SettingsWindow()
	{
		super();
	}

	public PropertyMap getCachedSettings()
	{
		return cachedSettings;
	}

	public void dialogApply(String pair)
	{
		if (cachedSettings.isEmpty())
			return;

		// get all cached settings and apply to Settings
		Iterator it = cachedSettings.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			settings.setProp(entry.getKey().toString(), entry.getValue());
			System.out.println("Setting " + entry);
		}

		// we gotta tell everyone we've changed
		SettingsGroup.updateAll();
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SettingsWindow.class);

	static {
		prophelp.registerGet("settings", "getCachedSettings");
	}

	public Object getProp(String key)
	{
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
