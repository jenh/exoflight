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

import java.util.Iterator;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.spif.*;

/**
  */
public class SpaceShipTelemetry
extends DefaultTelemetry
{
	SpaceShip ship;

	//

	public SpaceShipTelemetry(SpaceShip ship)
	{
		super(ship);
		this.ship = ship;
	}

	//

	public double getDYNPRES()
	{
		Trajectory traj = ship.getTrajectory();
		if (traj instanceof MutableTrajectory)
		{
			Iterator it = ((MutableTrajectory)traj).getPerturbations();
			while (it.hasNext())
			{
				Perturbation pb = (Perturbation)it.next();
				if (pb instanceof StructureThing.DragPerturbation)
				{
					return ((StructureThing.DragPerturbation)pb).getLastQ()/1000;
				}
			}
		}
		return 0;
	}

	public double getDRAGCOEFF()
	{
		Structure struct = ship.getStructure();
		if (struct != null)
		{
			float mach = (float)getMACH();
			if (mach > 0)
			{
				return struct.getLastDragCoeff();
			}
		}
		return Double.NaN;
	}


	// PROPERTY AWARE

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

	static PropertyHelper prophelp = new PropertyHelper(SpaceShipTelemetry.class);

	static {
		prophelp.registerGet("dynpres", "getDYNPRES");
		prophelp.registerGet("dragcoeff", "getDRAGCOEFF");
	}

}
