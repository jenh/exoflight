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

import com.fasterlight.exo.crew.CrewMember;
import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * Helper for the structure view screen.
  * Container for module & capability list models.
  */
public class StructureViewHelper
extends GLOComponent
{
	GLOListModel moddata, capdata;
	GLOListModel crewdata;

	SpaceShip ship;
	String ship_prop;

	//

	public StructureViewHelper()
	{
		moddata = new ModuleListModel();
		capdata = new CapabilityListModel();
		crewdata = new CrewListModel();
	}

	public String getPropertyForShip()
	{
		return ship_prop;
	}

	public void setPropertyForShip(String ship_prop)
	{
		this.ship_prop = ship_prop;
	}

	public Structure getStructure()
	{
		if (ship_prop != null)
		{
			SpaceShip newship = (SpaceShip)getForPropertyKey(ship_prop);
			if (newship != null && newship != ship)
			{
				ship = newship;
			}
		}
		return (ship != null) ? ship.getStructure() : null;
	}

	public void render(GLOContext ctx)
	{
		// renders nothing
	}

	//

	static String ALL_MODULES = "(all modules)";
	static String ALL_COMPONENTS = "(all components)";

	class ModuleListModel extends GLOAbstractListModel
	{
		public Object get(int index)
		{
			if (index==0)
				return ALL_MODULES;
			else
				return getStructure().getModule(index-1);
		}
		public int size()
		{
			if (getStructure() != null) {
				return 1 + getStructure().getModuleCount();
			} else
				return 0;
		}
		public int indexOf(Object o)
		{
			if (o == ALL_MODULES)
				return 0;
			else {
				int p = getStructure().getModules().indexOf(o);
				return (p < 0) ? p : (p+1);
			}
		}
	}

	class CapabilityListModel extends GLOAbstractListModel
	{
		public Object get(int index)
		{
			if (index==0 || getSelectedModule() == null)
				return ALL_COMPONENTS;
			else
				return getSelectedModule().getCapability(index-1);
		}
		public int size()
		{
			if (getSelectedModule() != null) {
				return 1 + getSelectedModule().getCapabilityCount();
			} else
				return 0;
		}
		public int indexOf(Object o)
		{
			if (o == ALL_COMPONENTS || getSelectedModule() == null)
				return 0;
			else {
				int p = getSelectedModule().getCapabilities().indexOf(o);
				return (p < 0) ? p : (p+1);
			}
		}
	}

	class CrewListModel extends GLOAbstractListModel
	{
		List getList()
		{
			List list;
			Structure struct = getStructure();
			if (struct == null)
				list = new ArrayList(0);
			else if (true || getSelectedModule() == null)
				list = getStructure().getAllCrew();
			else
				list = getSelectedModule().getCrewList();
			return list;
		}
		public Object get(int index)
		{
			return getList().get(index);
		}
		public int size()
		{
			return getList().size();
		}
		public int indexOf(Object o)
		{
			return getList().indexOf(o);
		}
	}

	public GLOListModel getModuleListModel()
	{
		return moddata;
	}

	public GLOListModel getCapabilityListModel()
	{
		return capdata;
	}

	public Module getSelectedModule()
	{
		Object item = moddata.getSelectedItem();
		if (item instanceof Module)
			return (Module)item;
		else
			return null;
	}

	public void setSelectedModule(Module m)
	{
		moddata.setSelectedItem(m);
	}

	public Capability getSelectedCapability()
	{
		Object item = capdata.getSelectedItem();
		if (item instanceof Capability)
			return (Capability)item;
		else
			return null;
	}

	public void setSelectedCapability(Capability cap)
	{
		capdata.setSelectedItem(cap);
	}

	public ResourceSet getSelectedSupply()
	{
		Capability cap = getSelectedCapability();
		if (cap != null)
			return cap.getSupply();
		Module mod = getSelectedModule();
		if (mod != null)
			return mod.getSupply();
		Structure struct = getStructure();
		if (struct != null)
			return struct.getSupply();

		return null;
	}

	public ResourceSet getSelectedCapacity()
	{
		Capability cap = getSelectedCapability();
		if (cap != null)
			return cap.getCapacity();
		Module mod = getSelectedModule();
		if (mod != null)
			return mod.getSupply();
		Structure struct = getStructure();
		if (struct != null)
			return struct.getCapacity();

		return null;
	}

	public GLOListModel getCrewListModel()
	{
		return crewdata;
	}

	public CrewMember getSelectedCrew()
	{
		Object item = crewdata.getSelectedItem();
		if (item instanceof CrewMember)
			return (CrewMember)item;
		else
			return null;
	}

	public void moveCrewMember(boolean b)
	{
		if (!b)
			return;
		try {
			CrewMember crew = getSelectedCrew();
			if (crew == null)
				throw new GLOUserException("No crew member selected!");
			Module module = getSelectedModule();
			if (module == null)
				throw new GLOUserException("No module selected!");
			crew.setModule(module);
			System.out.println("Moving " + crew + " to " + module);
		} catch (Exception ex) {
			ex.printStackTrace();
			GLOMessageBox.showOk(ex.getMessage());
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(StructureViewHelper.class);

	static {
		prophelp.registerGetSet("ship_prop", "PropertyForShip", String.class);
		prophelp.registerGet("modlistmodel", "getModuleListModel");
		prophelp.registerGet("caplistmodel", "getCapabilityListModel");
		prophelp.registerGet("crewlistmodel", "getCrewListModel");
		prophelp.registerGet("supply", "getSelectedSupply");
		prophelp.registerGet("capacity", "getSelectedCapacity");
		prophelp.registerGetSet("selmodule", "SelectedModule", Module.class);
		prophelp.registerGetSet("selcap", "SelectedCapability", Capability.class);
		prophelp.registerSet("movecrew", "moveCrewMember", boolean.class);
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
