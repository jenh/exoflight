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
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Uses RCS to align the ship with a target and dock.
  */
public class DockingProgram
implements GuidanceProgram, Constants, PropertyAware
{
	SpaceShip ship;
	UniverseThing target,ref;

	protected float rcs_thresh = 0.0001f; // 0.1 m/s
	protected float av_bias = 0.0005f; // 0.5 m/s at time of dock
	protected float av_scale = 0.005f;
	protected float av_maxvel = 0.003f;  // max spd 3 m/s
	protected float cyl_rad = 0.001f;
	protected float att_dev = (float)Util.toDegrees(2); // x deg or less before translation takes place

	Orientation revort = new Orientation(new Vec3d(0,0,-1), new Vec3d(0,1,0));

	public void compute(AttitudeController attctrl)
	{
		ship = attctrl.getShip();
		if (ship == null)
			return;

		// make sure there is a target
		target = ship.getShipTargetingSystem().getTarget();
		if (target == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for docking program");
			return;
		}

		Game game = ship.getUniverse().getGame();
		ref = ship.getParent();
		Telemetry telem = ship.getTelemetry();

		Vector3d dockvec = ship.getShipTargetingSystem().getTargetDockingVec();
		if (dockvec != null)
		{
			Vector3d posvec = ship.getShipTargetingSystem().getTargetPosVec();
			Vector3d velvec = ship.getShipTargetingSystem().getTargetVelVec();
//			double linedist = AstroUtil.distPointToLine(dockvec, new Vector3d(), new Vector3d(0,0,1));

			Vector3d v = new Vector3d(dockvec);
			double xylen = v.x*v.x + v.y*v.y;
			if (xylen > cyl_rad*cyl_rad)
			{
				v.z -= target.getRadius()*2;
			} else {
				v.x *= 2;
				v.y *= 2;
			}
			double dist = v.length();
			double approachv = Math.min(av_maxvel, dist*av_scale + av_bias);
//	System.out.println(v + " " +dist);
			v.scale(approachv/dist);
			v.add(velvec);

			boolean doTranslation = ship.getAttitudeController().getDeviationAngle() < att_dev;

			// set THC (translation controller)
			if (doTranslation)
				v.scale(1000); // todo: const?
			else
				v.set(0,0,0);

			ship.getShipAttitudeSystem().setTranslationController(new Vector3f(v));

//			System.out.println(Integer.toString(flags,16) + " approachv=" + approachv + ", v=" + v);

			Orientation dockort = ship.getShipTargetingSystem().getTargetDockingOrt();
			if (dockort != null)
			{
				attctrl.setTargetOrientation(dockort);
			}
		} else {
//			ship.getShipWarningSystem().setWarning("GUID-NOPROBE", "Docking target is not active");
		}
	}

	public String toString()
	{
		return "Docking trajectory";
	}

	boolean debug=false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(DockingProgram.class);

	static {
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
