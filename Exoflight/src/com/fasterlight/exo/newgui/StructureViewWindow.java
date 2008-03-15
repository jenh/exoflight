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

import java.util.Iterator;

import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.*;

// todo: put colors in a file, or make Shaders

/**
  * Displays information about a vehicle (structure).
  * Allows modification of some features.
  */
public class StructureViewWindow
extends SpaceShipWindow
{
	StructureView sview;
	GLOTableContainer toptab, rightbar, structpanel, modbtns, capbtns;
	GLOTableContainer respanel;
	GLOScrollBox resscroll, modscroll, capscroll;
	GLOStringList modlist, caplist;
	GLOListModel moddata, capdata;

	public StructureViewWindow()
	{
		sview = new StructureView();
		sview.setSize(300, 400);

		int s = 10;

		rightbar = new GLOTableContainer(1, 7);
		rightbar.setPadding(0, 8);

		// name label
		rightbar.add(
			new GLOLabel(20) {
				public String computeText() {
					if (getSpaceShip() != null)
						return getSpaceShip().getName();
					else
						return "";
				}
			}
		);

		structpanel = new GLOTableContainer(2, 3);
		structpanel.setPadding(8, 8);
		structpanel.setColumnFlags(0, GLOTableContainer.HALIGN_RIGHT);
		structpanel.setColumnFlags(1, GLOTableContainer.HALIGN_RIGHT);

		// module count
		structpanel.add(new GLOLabel("# modules:"));
		structpanel.add(
			new GLOLabel(s) {
				public String computeText() {
					if (getStructure() != null)
						return ""+getStructure().getModuleCount();
					else
						return ".";
				}
			}
		);
		// module mass
		structpanel.add(new GLOLabel("Mass:"));
		structpanel.add(
			new GLOLabel(s) {
				public String computeText() {
					if (getStructure() != null)
						return AstroUtil.toMass(getStructure().getMass());
					else
						return ".";
				}
			}
		);

		rightbar.add(structpanel);

		// resource panel

		resscroll = new GLOScrollBox(false, true);
		resscroll.getBox().setSize(160,64);
		respanel = null;
		rightbar.add(new GLOGroupBox(resscroll, "Resources"));

		// module list

		modscroll = new GLOScrollBox(false, true);
		modscroll.getBox().setSize(160,64);
		modlist = new GLOStringList();
		moddata = new ModuleListModel();
		modlist.setModel(moddata);
		modscroll.getBox().add(modlist);
		rightbar.add(new GLOGroupBox(modscroll, "Modules"));

		// module buttons

		modbtns = new GLOTableContainer(1, 1);
		modbtns.setPadding(4, 4);
		modbtns.add(new GLOButton("Detach!", "Detach"));
		rightbar.add(modbtns);

		// capability list

		capscroll = new GLOScrollBox(false, true);
		capscroll.getBox().setSize(160,64);
		caplist = new GLOStringList();
		capdata = new CapabilityListModel();
		caplist.setModel(capdata);
		capscroll.getBox().add(caplist);
		rightbar.add(new GLOGroupBox(capscroll, "Capability"));

		// capability buttons

		capbtns = new GLOTableContainer(2, 1);
		capbtns.setPadding(4, 4);
		capbtns.add(new GLOButton("On", "On"));
		capbtns.add(new GLOButton("Off", "Off"));
		rightbar.add(capbtns);

		// create the topmost table

		toptab = new GLOTableContainer(2, 1);
		toptab.add(sview);
		toptab.add(rightbar);
		setContent(toptab);

		setFlags(RESIZEABLE);
		setResizeChild(sview);
	}

	public void setShipSelectable(ShipSelectable shipsel)
	{
		super.setShipSelectable(shipsel);
	}

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
			if (o == ALL_COMPONENTS)
				return 0;
			else {
				int p = getSelectedModule().getCapabilities().indexOf(o);
				return (p < 0) ? p : (p+1);
			}
		}
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
		throw new RuntimeException("setSelectedModule NYI");
	}

	public Capability getSelectedCapability()
	{
		Object item = capdata.getSelectedItem();
		if (item instanceof Capability)
			return (Capability)item;
		else
			return null;
	}

	void updateResourcePanel()
	{
		if (respanel != null)
		{
			resscroll.getBox().remove(respanel);
			respanel = null;
		}

		if (getStructure() == null)
			return;

		ResourceSet resset;
		Object item = moddata.getSelectedItem();
		if (item instanceof Module)
			resset = ((Module)item).getSupply();
		else
			resset = getStructure().getSupply();

		int rescount = resset.countResources();
		if (rescount == 0)
			return;

		respanel = new GLOTableContainer(3, rescount);
		respanel.setPadding(8,4);
		respanel.setColumnFlags(0, GLOTableContainer.HALIGN_LEFT);
		respanel.setColumnFlags(1, GLOTableContainer.HALIGN_RIGHT);
		respanel.setColumnFlags(2, GLOTableContainer.HALIGN_LEFT);
		Iterator it = resset.getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt = resset.getAmountOf(r);
			respanel.add(new GLOLabel(r + ":"));
			respanel.add(new GLOLabel(AstroUtil.format(amt)));
			String units;
			// todo: method to return units type
			if (r.toString().equals("E"))
				units = "kWh";
			else
				units = "kg";
			respanel.add(new GLOLabel(units));
		}

		resscroll.getBox().add(respanel);
		respanel.layout();
		resscroll.getBox().setVirtualSize(respanel.getSize());
	}

	public void render(GLOContext ctx)
	{
		updateResourcePanel();
		super.render(ctx);
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOKeyEvent)
		{
			GLOKeyEvent kev = (GLOKeyEvent)event;
			if (kev.isPressed())
			{
				switch (kev.getKeyCode())
				{
					case GLOKeyEvent.VK_UP:
						sview.rotate(-1,0,0);
						return true;
					case GLOKeyEvent.VK_DOWN:
						sview.rotate(1,0,0);
						return true;
					case GLOKeyEvent.VK_LEFT:
						sview.rotate(0,-1,0);
						return true;
					case GLOKeyEvent.VK_RIGHT:
						sview.rotate(0,1,0);
						return true;
					case GLOKeyEvent.VK_HOME:
						sview.rotate(0,0,-1);
						return true;
					case GLOKeyEvent.VK_END:
						sview.rotate(0,0,1);
						return true;
				}
			}
		}
		else if (event instanceof GLOActionEvent)
		{
			GLOActionEvent actev = (GLOActionEvent)event;
			if ("Detach".equals(actev.getAction()))
			{
				Module m = getSelectedModule();
				if (m != null && getSpaceShip() instanceof SpaceShip)
				{
					getSpaceShip().detach(m);
					return true;
				}
			}
			else if ("On".equals(actev.getAction()))
			{
				Capability cap = getSelectedCapability();
				if (cap instanceof PeriodicCapability)
				{
					if (cap instanceof PropulsionCapability)
						((PropulsionCapability)cap).setArmed(true);
					((PeriodicCapability)cap).activate();
					return true;
				}
			}
			else if ("Off".equals(actev.getAction()))
			{
				Capability cap = getSelectedCapability();
				if (cap instanceof Capability)
				{
					if (cap instanceof PropulsionCapability)
						((PropulsionCapability)cap).setArmed(false);
					cap.deactivate();
					return true;
				}
			}
		}
		return super.handleEvent(event);
	}

}
