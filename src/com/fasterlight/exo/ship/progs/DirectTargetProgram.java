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
package com.fasterlight.exo.ship.progs;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.PropertyAware;
import com.fasterlight.vecmath.Vector3d;

/**
  * DirectTarget points the spacecraft directly at a target
  */
public class DirectTargetProgram
extends PYRProgram
implements GuidanceProgram, Constants, PropertyAware
{
	public void compute(AttitudeController attctrl)
	{
		SpaceShip ship = attctrl.getShip();
		if (ship == null)
			return;

		UniverseThing target = ship.getShipTargetingSystem().getTarget();
		// make sure there is a target
		if (target == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for direct target program");
			return;
		}

		Game game = ship.getUniverse().getGame();
		Vector3d targvec = target.getPosition(ship, game.time());
		Vector3d r1 = ship.getPosition(ship.getParent(), game.time());

		// set orientation

		Orientation ort = new Orientation(targvec, r1);

		// multiply by PYR

		Orientation pyrort = new Orientation();
		pyrort.setEulerPYR(pyr);
		pyrort.concat(ort);

		// set target orientation

		attctrl.setTargetOrientation(ort);
	}

	public String toString()
	{
		return "Direct to target trajectory";
	}

}
