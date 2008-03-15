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

import java.util.List;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.strategy.Mission;
import com.fasterlight.glout.*;

/**
  * Subclass for the dialog that lets you select a mission
  */
public class NewMissionWindow
extends GLODialog
{
	GLOListModel category_lm;
	GLOListModel missions_lm;
	List missions;
	String last_cat;

	public NewMissionWindow()
	{
		super();
		category_lm = new GLODefaultListModel(Mission.getCategories());
	}

	public void setup()
	{
		getPageStack().setPageNum(0);
		last_cat = null;
	}

	public GLOPageStack getPageStack()
	{
		return (GLOPageStack)this.getContent();
	}

	public String getSelectedCategory()
	{
		return (String)category_lm.getSelectedItem();
	}

	public GLOListModel getMissionListModel()
	{
		String this_cat = getSelectedCategory();
		if (this_cat == null)
		{
			missions_lm = null;
		}
		else if (!this_cat.equals(last_cat))
		{
			missions = Mission.getMissions(getSelectedCategory());
			if (missions != null)
				missions_lm = new GLODefaultListModel(missions);
			else
				missions_lm = null;
		}
		last_cat = this_cat;
		return missions_lm;
	}

	public Mission getCurrentMission()
	{
		return (getMissionListModel() != null) ?
			(Mission)missions_lm.getSelectedItem() : null;
	}

	public String getMissionSlideshow()
	{
		Mission m = getCurrentMission();
		// TODO: why do we need 'data'...
		return (m != null) ? "data/uitexs/illust/missions/" + getSelectedCategory() +
			"/" + m.getName() + ".txt" : null;
	}

	public SpaceGame makeMission()
	{
		return ((GUIContext)ctx).createNewGame(getCurrentMission());
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		if (key.equals("mission_lm"))
			return getMissionListModel();
		else if (key.equals("mission_sshow"))
			return getMissionSlideshow();
		else if (key.equals("category_lm"))
			return category_lm;
		else
			return super.getProp(key);
	}

	//

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
		SpaceGame game = makeMission();
      // when a new game is commanded, we send it in an
      // Action event.  It gets intercepted by the main pgm
      // which resets the UI.  Kinda cheezy, but ... <shrug>
      ctx.deliverEvent(new GLOActionEvent(ctx, game), this);
	}

}
