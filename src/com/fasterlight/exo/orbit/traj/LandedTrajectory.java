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
package com.fasterlight.exo.orbit.traj;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * A LandedTrajectory describes a position on a Planet.
  */
public class LandedTrajectory
extends DefaultMutableTrajectory
{
	private Planet landed;
	private Vector3d landpos;
	private boolean lockdown = false;

	// todo: these are FOUL

	public LandedTrajectory()
	{
		// our default orientation is [90,0,0]
		Orientation o = new Orientation();
		o.setPitch(Math.PI/2);
		ort0.set(o);
	}
	public void set(Vector3d llr)
	{
		setLandPos(llr);
	}
	public void setLandPos(Vector3d llr)
	{
		set(ref.getUniverse().getGame(), ref, llr);
	}
	public void setLandOrt(Orientation ort)
	{
		ort0.set(ort);
	}
	public LandedTrajectory(Game game, UniverseThing ref, Vector3d llr)
	{
		this();
		set(game, ref, llr);
	}
	public LandedTrajectory(UniverseThing ref, Vector3d llr, long t0)
	{
		this();
		set(ref, llr, t0);
	}
	public LandedTrajectory(UniverseThing ref, Game game, Vector3d r0)
	{
		this();
		set(ref, r0, null, game.time(), null);
	}
	public void set(Game game, UniverseThing ref, Vector3d llr)
	{
		set(ref, llr, game.time());
	}
	public void set(UniverseThing ref, Vector3d llr, long t0)
	{
		super.set(ref, null, null, t0, null);
		this.landed = (Planet)ref;
		this.landpos = new Vec3d(llr);
		if (landpos.z == 0)
			landpos.z = landed.getRadius();
	}
	public void set(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort)
	{
		super.set(ref, r0, v0, t0, ort);
		this.landed = (Planet)ref;
		this.landpos = landed.geo2llr(r0, t0*(1d/TICKS_PER_SEC));
		if (ort != null) {
			setOrientation(ort);
		}
	}
	public Vector3d getLandPos()
	{
		return landpos;
	}
	/**
	  * Get the reference object (parent)
	  */
	public UniverseThing getParent()
	{
		return landed;
	}

	public Vector3d getPos(long time)
	{
		return landed.llr2geo(landpos, time*(1d/TICKS_PER_SEC));
	}

	public Vector3d getVel(long time)
	{
		return landed.llr2vel(landpos, time*(1d/TICKS_PER_SEC));
	}

	public void setOrientation(Orientation ort)
	{
		long time = getGame().time();
		Vector3d r1 = getPos(time);
		// ort0 = inv(sez)*ort
		Orientation o = landed.getOrientation(r1);
		o.invert();
		o.mul(ort);
		super.setOrientation(o);
	}

	public Orientation getOrt(long time)
	{
		Vector3d r1 = getPos(time);
		// ort = sez*ort0
		Orientation o = landed.getOrientation(r1);
		o.mul(ort0);
		return o;
	}

	/**
	  * If we have any perturbs, go to Cowell
	  */
	public boolean checkPerturbs()
	{
		if (isActive() && countUserPerturbations() > 0 && !isLockedDown())
		{
			free();
			return true;
		}
		return false;
	}

	protected void addDefaultPerturbs()
	{
		// none, because we don't move!
	}

	public boolean isLockedDown()
	{
		return lockdown;
	}

	public void setLockedDown(boolean b)
	{
		this.lockdown = b;
		checkPerturbs();
	}

	public void refresh()
	{
	}

	public String getType()
	{
		return "landed";
	}

	public void free(Vector3d r, Vector3d v)
	{
		long t = getGame().time();
		CowellTrajectory traj = new CowellTrajectory(ref, r, v, t, getOrt(t));
		addUserPerturbations(traj);
		thing.setTrajectory(traj);
	}

	public void free()
	{
		long t = getGame().time();
		Vector3d r = getPos(t);
		Vector3d v = getVel(t);
		free(r, v);
	}

	public void free(boolean b)
	{
		if (b)
			free();
	}

	// PROPERTIES

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
		} catch (PropertyRejectedException pre) {
			super.setProp(key, value);
		}
	}

	static PropertyHelper prophelp = new PropertyHelper(LandedTrajectory.class);

	static {
		prophelp.registerGet("locked", "isLockedDown");
		prophelp.registerSet("locked", "setLockedDown", boolean.class);
		prophelp.registerSet("landpos", "set", Vector3d.class);
		prophelp.registerSet("free", "free", boolean.class);
	}

}
