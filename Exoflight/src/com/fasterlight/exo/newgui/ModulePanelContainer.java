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

import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * A container for holding a collection of "module panels".
  * Basically, there is one module panel for each module in a
  * ship.  They are added and removed as modules are added
  * and removed to/from the ship.
  */
public class ModulePanelContainer
extends GLOVirtualBox
{
	GUIContext guictx;
	String prop_ship;
	List panels = new ArrayList();
	Set modules = new HashSet();

	public ModulePanelContainer(GUIContext guictx)
	{
		this.guictx = guictx;
//		layout(); // todo: BLEAHGHGHGHGHHH!!!!!
	}

	public void setPropertyForShip(String prop_ship)
	{
		this.prop_ship = prop_ship;
	}

	public String getPropertyForShip()
	{
		return prop_ship;
	}

	public SpaceShip getShip()
	{
		if (prop_ship != null)
		{
			Object o = getForPropertyKey(prop_ship);
			if (o instanceof SpaceShip)
				return (SpaceShip)o;
		}
		return null;
	}

	private void checkPanels()
	{
		SpaceShip ship = getShip();
		if (ship == null)
			return;
		Structure struct = ship.getStructure();

		// see if we should remove any panels (todo)
		Iterator it = panels.iterator();
		int xbias=0;
		while (it.hasNext())
		{
			ModulePanel mp = (ModulePanel)it.next();
			if (!struct.containsModule( mp.getModule() ))
			{
				remove(mp);
				it.remove();
				xbias -= mp.getWidth();
				modules.remove(mp.getModule());
			} else {
				if (xbias != 0)
					mp.setPosition(mp.getX()+xbias, mp.getY());
			}
		}
		if (xbias != 0)
		{
			setVirtualSize(getVirtualWidth() + xbias, getVirtualHeight());
			//setPosition(getX() - xbias, getY());
		}

		// add panels we don't have yet
		it = struct.getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			if (!this.modules.contains(m))
			{
				addPanelForModule(m);
			}
		}
	}

	public void reloadPanels()
	{
		SpaceShip ship = getShip();
		if (ship == null)
			return;
		Structure struct = ship.getStructure();

		// see if we should remove any panels (todo)
		for (int i=0; i<panels.size(); i++)
		{
			ModulePanel mp = (ModulePanel)panels.get(i);
			mp.reload();
		}
	}

	private void addPanelForModule(Module m)
	{
		ModulePanel mp = new ModulePanel(guictx, m);
		this.add(mp);
		mp.setPosition(getVirtualWidth(), 0);
		mp.setSize(mp.getWidth(), this.getHeight());
		this.setVirtualSize(getVirtualWidth() + mp.getWidth(), getVirtualHeight());
		//this.setPosition(getX() - mp.getWidth(), getY());
		modules.add(m);
		panels.add(mp);
	}

	public void render(GLOContext ctx)
	{
		checkPanels();

		super.render(ctx);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ModulePanelContainer.class);

	static {
		prophelp.registerGetSet("ship_prop", "PropertyForShip", String.class);
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
