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

import com.fasterlight.exo.orbit.Trajectory;
import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * Subclass for the vehicle that lets you select
  * a vehicle and starting location
  */
public class SetPositionWindow
extends GLODialog
{
	GUIContext guictx;
	SpaceShip ship;
	PositionSetter pos_setter;

	//

	public SetPositionWindow()
	{
		super();

		guictx = (GUIContext)getContext();
		ship = guictx.getShip();
		if (ship != null)
		{
			pos_setter = new PositionSetter(ship);
		}
		else
		{
			setVisible(false);
			GLOMessageBox.showOk("Cannot set position: Assign an object as a reference first.");
		}
	}

	//

	public void dialogApply(String s)
	{
		Trajectory curtraj = ship.getTrajectory();

		// todo: other forms of trajectories
		// todo: what happen to perturbs?
		if (s.equals("-orbit"))
		{
			pos_setter.applyPositionOrbit();
		}
		else if (s.equals("-ground"))
		{
			pos_setter.applyPositionGround();
		}
		else
			throw new IllegalArgumentException(s);
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SetPositionWindow.class);

	static {
	}

	public Object getProp(String key)
	{
		Object o;
		if (pos_setter != null)
		{
			o = pos_setter.getProp(key);
			if (o != null)
				return o;
		}
		o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		if (pos_setter != null)
		{
			try {
				pos_setter.setProp(key, value);
				return;
			} catch (PropertyRejectedException pre) {
			}
		}
		try {
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}

}
