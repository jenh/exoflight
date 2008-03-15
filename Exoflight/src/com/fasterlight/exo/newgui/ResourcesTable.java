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
import com.fasterlight.spif.*;

/**
  * Displays information about a set of resources
  * in a table format, inside a scroll box.
  * todo: got stack trace here in render() once
  */
public class ResourcesTable
extends GLOScrollBox
{
	GLOTableContainer respanel;
	String resset_prop;
	int columns = 1;

	void updateResourcePanel()
	{
		if (respanel != null)
		{
			this.getBox().remove(respanel);
			respanel = null;
		}

		ResourceSet resset = getResourceSet();
		if (resset == null)
			return;

		int rescount = resset.countResources();
		if (rescount == 0)
			return;

		respanel = new GLOTableContainer(3*columns, (rescount+columns-1)/columns);
		respanel.setPadding(8,4);
		for (int i=0; i<columns; i++)
		{
			respanel.setColumnFlags(3*i+0, GLOTableContainer.HALIGN_LEFT);
			respanel.setColumnFlags(3*i+1, GLOTableContainer.HALIGN_RIGHT);
			respanel.setColumnFlags(3*i+2, GLOTableContainer.HALIGN_LEFT);
		}
		Iterator it = resset.getResources();
		while (it.hasNext())
		{
			Resource r = (Resource)it.next();
			float amt = resset.getAmountOf(r);
			respanel.add(new GLOLabel(r.getLongName() + ':'));
			respanel.add(new GLOLabel(AstroUtil.format(amt)));
			String units;
			// todo: method to return units type
			respanel.add(new GLOLabel(r.getUnits()));
		}

		this.getBox().add(respanel);
		respanel.layout();
		this.getBox().setVirtualSize(respanel.getSize());
	}


	public int getNumResColumns()
	{
		return columns;
	}

	public void setNumResColumns(int columns)
	{
		if (columns < 1)
			throw new IllegalArgumentException("# columns must be >= 1");
		this.columns = columns;
	}

	public String getPropertyForResourceSet()
	{
		return resset_prop;
	}

	public void setPropertyForResourceSet(String resset_prop)
	{
		this.resset_prop = resset_prop;
	}

	public ResourceSet getResourceSet()
	{
		return resset_prop != null ? (ResourceSet)getForPropertyKey(resset_prop) : null;
	}

	public void render(GLOContext ctx)
	{
		updateResourcePanel();
		super.render(ctx);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ResourcesTable.class);

	static {
		prophelp.registerGetSet("rescolumns", "NumResColumns", int.class);
		prophelp.registerGetSet("resset_prop", "PropertyForResourceSet", String.class);
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
