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

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.ship.*;
import com.fasterlight.exo.strategy.*;
import com.fasterlight.glout.*;

/**
  * Subclass for the vehicle that lets you select
  * a vehicle and starting location
  */
public class NewVehicleWindow
extends GLODialog
{
	GLOListModel vehicles_lm;
	GLOListModel bases_lm;
	SpaceGame game;
	Vehicle last_vehicle;
	Structure last_struct;

	public NewVehicleWindow()
	{
		super();

		game = ((GUIContext)getContext()).game;
		vehicles_lm = new GLODefaultListModel(game.getVehicles());
		bases_lm = new GLODefaultListModel(game.getBases());
		// if there is a mission, set default parameters
		if (game.getCurrentMission() != null)
		{
			Mission miss = game.getCurrentMission();
			vehicles_lm.setSelectedItem(miss.getVehicle());
			if (miss.getBaseName() != null)
				bases_lm.setSelectedItem(game.getBody(miss.getBaseName()));
		}
	}

	public void setup()
	{
		getPageStack().setPageNum(0);
	}

	public GLOPageStack getPageStack()
	{
		return (GLOPageStack)this.getContent();
	}

	public GLOListModel getVehicleListModel()
	{
		return vehicles_lm;
	}

	public GLOListModel getBaseListModel()
	{
		return bases_lm;
	}

	public Vehicle getCurrentVehicle()
	{
		return (Vehicle)vehicles_lm.getSelectedItem();
	}

	public Structure getCurrentStructure()
	{
		Vehicle v = getCurrentVehicle();
		if (v != null && v != last_vehicle)
		{
			last_vehicle = v;
			last_struct = v.toStructure(game.getAgency());
		}
		return last_struct;
	}

	public SpaceBase getCurrentBase()
	{
		return (SpaceBase)bases_lm.getSelectedItem();
	}

	public String getVehicleSlideshow()
	{
		Vehicle v = getCurrentVehicle();
		return (v != null) ? "data/uitexs/illust/vehicles/" + v.getName() + ".txt" : null;
	}

	public String getBaseSlideshow()
	{
		SpaceBase b = getCurrentBase();
		return (b != null) ? "data/uitexs/illust/bases/" + b.getName() + ".txt" : null;
	}

	public void makeVehicle()
	{
		Vehicle v = getCurrentVehicle();
		SpaceBase b = getCurrentBase();
		Agency a = game.getAgency();
		if (v != null && b != null && a != null)
		{
			a.prepareVehicle(v,b);
		} else {
			System.out.println("Couldn't prepare vehicle " + v + " at " + b);
		}
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		if (key.equals("vehicle_lm"))
			return vehicles_lm;
		else if (key.equals("bases_lm"))
			return bases_lm;
		else if (key.equals("structure"))
			return getCurrentStructure();
		else if (key.equals("vehicle_sshow"))
			return getVehicleSlideshow();
		else if (key.equals("bases_sshow"))
			return getBaseSlideshow();
		else
			return super.getProp(key);
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOActionEvent)
		{
			Object action = ((GLOActionEvent)event).getAction();
			if ("Next".equals(action))
			{
				getPageStack().nextPage();
				return true;
			}
			else if ("Back".equals(action))
			{
				getPageStack().prevPage();
				return true;
			}
		}
		return super.handleEvent(event);
	}

	//

	public void dialogApply(String s)
	{
		makeVehicle();
	}
}
