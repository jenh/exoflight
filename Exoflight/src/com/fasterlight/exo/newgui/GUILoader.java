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
import com.fasterlight.exo.orbit.Planet;
import com.fasterlight.glout.*;

/**
  * Subclass of GLOLoader that loads game-specific widgets.
  */
public class GUILoader
extends GLOLoader
{
	SpaceGame game;
	GUIContext guictx;

	static {
		registerType("numlabel", NumericLabel.class);
		registerType("8ball", AttitudeIndicator.class);
		registerType("xpointer", CrosspointerIndicator.class);
		registerType("sequencerlist", SequencerList.class);
		registerType("structureview", StructureView.class);
		registerType("structureviewhelper", StructureViewHelper.class);
		registerType("resourcesbox", ResourcesTable.class);
		registerType("settingswindow", SettingsWindow.class);
		registerType("arrowind", ArrowIndicator.class);
	}

	public GUILoader(GLOComponent top, SpaceGame game, GUIContext guictx)
	{
		super(top);
		this.game = game;
		this.guictx = guictx;
	}

	public GUILoader(GLOComponent top, GUIContext guictx)
	{
		this(top, guictx.getGame(), guictx);
	}

	public Object makeObject(String type)
	{
		// nextver: get rid of (game) contrsuctor
		if (type.equals("groundtrack"))
		{
			GroundtrackView gtv = new GroundtrackView(game);
			gtv.setRefPlanet((Planet)game.getBody("Earth"));
			return gtv;
		}
		else if (type.equals("visual"))
		{
			VisualView vv = new VisualView(game);
			return vv;
		}
		else if (type.equals("tactical"))
		{
			TacticalView vv = new TacticalView(game);
			return vv;
		}
		else if (type.equals("modulepanel"))
		{
			ModulePanelContainer mpc = new ModulePanelContainer(guictx);
			return mpc;
		}
		else
			return super.makeObject(type);
	}
}
