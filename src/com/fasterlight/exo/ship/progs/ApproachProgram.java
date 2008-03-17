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
import com.fasterlight.vecmath.Vector3d;

/**
  * Quadratic guidance, similar to P64 on the LM
  */
public class ApproachProgram
implements GuidanceProgram, Constants, PropertyAware
{
	SpaceShip ship;
	UniverseThing target,ref;
	double timescale=1, timebias, heightbias;
	boolean adj_throttle;
	boolean noroll;
	double TGO;

	public double getTimeBias()
	{
		return timebias;
	}

	public void setTimeBias(double timebias)
	{
		this.timebias = timebias;
	}

	public double getTimeScale()
	{
		return timescale;
	}

	public void setTimeScale(double timescale)
	{
		this.timescale = timescale;
	}

	public boolean getAdjustThrottle()
	{
		return adj_throttle;
	}

	public void setAdjustThrottle(boolean adj_throttle)
	{
		this.adj_throttle = adj_throttle;
	}

	public boolean getNoRoll()
	{
		return noroll;
	}

	public void setNoRoll(boolean noroll)
	{
		this.noroll = noroll;
	}

	public double getHeightBias()
	{
		return heightbias;
	}

	public void setHeightBias(double heightbias)
	{
		this.heightbias = heightbias;
	}

	public Vector3d getDestPos(long t)
	{
		Vector3d r = target.getPosition(ref, t);
		if (heightbias != 0)
		{
			double rl = r.length();
			r.scale( (rl+heightbias)/rl );
		}
		return r;
	}

	public Vector3d getDestVel(long t)
	{
		return target.getVelocity(ref, t);
	}

	public void compute(AttitudeController attctrl)
	{
		ship = attctrl.getShip();
		if (ship == null)
			return;

		target = ship.getShipTargetingSystem().getTarget();
		if (target == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for approach program");
			return;
		}

		Game game = ship.getUniverse().getGame();
		ref = ship.getParent();
		Telemetry telem = ship.getTelemetry();

		long t = game.time();
		Vector3d r1 = telem.getCenDistVec();
		Vector3d v1 = telem.getVelocityVec();
		Vector3d r2 = getDestPos(t);
		Vector3d v2 = getDestVel(t);
		Vector3d ga = telem.getGravAccelVec();
		double maxdv = ship.getMaxAccel();

		// this is from NASA TR X-58040
		// "Apollo Lunar Descent and Ascent Trajectories"
		// Ac = Ad - (6(V+Vd)/TGO) - (12*(R-Rd)/TGO^2)
		// god knows why it works... :)
		// todo: figure out what god knows

		Vector3d a1 = new Vector3d(r1);
		a1.sub(r2);

		Vector3d a2 = new Vector3d(v1);
		a2.sub(v2);

		double t1 = -2*maxdv*a1.length() + a2.lengthSquared();
		t1 = (t1 < 0) ? 0 : Math.sqrt(t1);
		TGO = t1+a2.length()/maxdv;
		TGO = TGO*timescale + timebias;
		//TGO = a1.length()*2/a2.length();

		a1.scale(-12/(TGO*TGO));

		a2.set(v1);
		a2.add(v2);
		a2.scale(-6/TGO);

		Vector3d ac = new Vector3d(a1);
		ac.add(a2);
		ac.sub(ga);

		float throt = (float)(ac.length()*2/maxdv); // todo: why * 2?

		if (debug) {
			System.out.println(
				"TGO=" + AstroUtil.format(TGO) +
				" a=" + AstroUtil.format(ac.length()) +
				" throt=" + AstroUtil.format(throt) +
				" prop=" + AstroUtil.format(ship.getMass()-ship.getStructure().getEmptyMass())
			);
		}

		boolean dir = ac.dot(a1) < 0;

		Orientation ort;
		if (noroll)
		{
			ort = new Orientation(ac, telem.getOrientationFixed().getUpVector());
		} else {
			Vector3d up = new Vector3d(r1);
			//up.scale(-1); // windows-up?
			ort = new Orientation(ac, up);
		}
		attctrl.setTargetOrientation(ort);

		if (adj_throttle)
		{
			ship.getShipAttitudeSystem().setAutoThrottle(throt);
			//ship.setThrottle(dir ? 1 : 0);
		}
	}

	public double getTimeRemaining()
	{
		return TGO;
	}

	public String toString()
	{
		return "Approach trajectory";
	}

	boolean debug=false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ApproachProgram.class);

	static {
		prophelp.registerGetSet("timebias", "TimeBias", double.class);
		prophelp.registerGetSet("timescale", "TimeScale", double.class);
		prophelp.registerGetSet("heightbias", "HeightBias", double.class);
		prophelp.registerGetSet("adjthrottle", "AdjustThrottle", boolean.class);
		prophelp.registerGetSet("noroll", "NoRoll", boolean.class);
		prophelp.registerGet("timerem", "getTimeRemaining");
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
