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

import java.util.ArrayList;

import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.GLODefaultListModel;

/**
  * Used to filter out certain types of capabilities
  * based on criteria.
  */
public class FilteringCapabilityListModel
extends GLODefaultListModel
{
	ShipSelectable shipsel;
	Class clazz;
	boolean exact;

	public FilteringCapabilityListModel(ShipSelectable shipsel, Class clazz)
	{
		super(new ArrayList());
		this.shipsel = shipsel;
		this.clazz = clazz;
	}

	public FilteringCapabilityListModel(ShipSelectable shipsel, Class clazz, boolean exact)
	{
		this(shipsel, clazz);
		this.exact = exact;
	}

	public int size()
	{
		buildFilteredList();
		return super.size();
	}

	public Structure getStructure()
	{
		SpaceShip ship = shipsel.getSelectedShip();
		if (ship != null)
			return ship.getStructure();
		else
			return null;
	}

	protected void buildFilteredList()
	{
		Structure struct = getStructure();
		if (struct == null)
			this.setList(new ArrayList());
		else
			this.setList(struct.getCapabilitiesOfClass(clazz, exact));
	}

	public String toString(Object o)
	{
		if (!(o instanceof Capability))
			return super.toString(o);
		else {
			Capability cap = (Capability)o;
			return cap.getModule().getName() + " - " + cap.getName();
		}
	}
}
