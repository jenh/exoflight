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

import java.io.IOException;

import com.fasterlight.exo.ship.Module;
import com.fasterlight.glout.*;
import com.fasterlight.spif.PropertyAware;

/**
  * This is a frame around a module panel.
  * It just supplies a nice frame, and the name of the
  * module, then sticks the module panel innards inside.
  */
public class ModulePanel
extends GLOContainer
{
	private Module module;
	private GUIContext guictx;
	private GLOMultiEventCounter evcounter;

	public ModulePanel(GUIContext guictx, Module module)
	{
		this.module = module;
		this.guictx = guictx;
		reload();
	}

	public void reload()
	{
//		System.gc(); //???
		this.removeAllChildren();
		GLOLoader loader = new GUILoader(this, guictx);
		String fn = "panels/modules/" + module.getType() + ".txt";
		System.out.println("Loading panel " + fn);
		GLOComponent top;
		try {
			top = loader.load(fn);
			this.setSize(top.getSize());
		} catch (IOException ioe) {
			System.out.println("Couldn't load module panel: " + ioe);
		}
	}

	public Module getModule()
	{
		return module;
	}

	public PropertyAware getPropertyTop()
	{
		return module;
	}

	public void render(GLOContext ctx)
	{
		super.render(ctx);

		if (evcounter != null && evcounter.getCount() == 2)
		{
			ctx.setShader("redout");
			this.drawBox();
		}
	}

	//

/***
	private static final GLOMenu mpmenu = new GLOMenu("panels/module.mnu");

	public void openMenu(int x, int y)
	{
		GLOMenuPopup popup = new GLOMenuPopup();
		popup.setMenu(mpmenu);
		popup.open(ctx,x,y);
	}
***/

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOMouseButtonEvent)
		{
			GLOMouseButtonEvent mbe = (GLOMouseButtonEvent)event;
			if (mbe.isPressed(2))
			{
				if (evcounter == null)
					evcounter = new GLOMultiEventCounter(event.getContext());
				if (module != null && evcounter.notifyEvent() == 3)
				{
					module.detach().setExpendable(true);
				}
//				openMenu(mbe.x, mbe.y);
				return true;
			}
		}

		return super.handleEvent(event);
	}

}
