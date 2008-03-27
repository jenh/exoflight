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
package com.fasterlight.exo.ship;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.LandedTrajectory;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * A stationary ground structure.
  */
public class SpaceBase
extends DefaultUniverseThing
{
	String modelname = "tower1";

	//

	public SpaceBase()
	{
	}

	public SpaceBase(Game game, Planet planet, Vector3d llr)
	{
		setTrajectory(new LandedTrajectory(game, planet, llr));
	}

	public double getRadius()
	{
		// todo
		return 0.1;
	}

	public String getModelName()
	{
		return modelname;
	}

	public void setModelName(String s)
	{
		this.modelname = s;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SpaceBase.class);

	static {
		prophelp.registerGetSet("modelname", "ModelName", String.class);
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
