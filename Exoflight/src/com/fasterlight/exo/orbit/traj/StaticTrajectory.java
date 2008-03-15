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
  * A StaticTrajectory is one that can't be altered.
  * The default implementation just stays in once place,
  * but you can extend it to make trajectories that do anything
  * else, as long as they can't be modified during the game.
  */
public class StaticTrajectory
implements Trajectory, PropertyAware
{
	protected UniverseThing ref;
	protected Vector3d r0 = new Vec3d();
	protected Orientation ort0 = new Orientation();
	protected Game game;
	protected UniverseThing thing;

	//

	public StaticTrajectory()
	{
		this(new Vec3d());
	}
	public StaticTrajectory(Vector3d r0)
	{
		set(null, r0, null, 0, null);
	}
	public StaticTrajectory(UniverseThing ref, Vector3d r0)
	{
		set(ref, r0, null, 0, null);
	}
	public void set(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort)
	{
		this.ref = ref;
		this.r0.set(r0);
		this.ort0.set(ort0);
	}
	public void setParent(UniverseThing ref)
	{
		this.ref = ref;
	}
	public UniverseThing getParent()
	{
		return ref;
	}
	public Game getGame()
	{
		if (game != null)
			return game;
		if (ref != null && ref.getUniverse() != null)
			return ref.getUniverse().getGame();
		throw new RuntimeException("Could not get game");
	}
	public void activate(UniverseThing subject)
	{
		this.thing = subject;
		this.game = subject.getUniverse().getGame();
	}
	public void deactivate()
	{
		this.game = null;
		this.thing = null;
	}
	public UniverseThing getThing()
	{
		return thing;
	}
	public Vector3d getPos(long time)
	{
		return new Vec3d(r0);
	}
	public Vector3d getVel(long time)
	{
		return new Vec3d();
	}
	public Orientation getOrt(long time)
	{
		return new Orientation(ort0);
	}
	public String getType()
	{
		return "static";
	}
	public Vector3d getAngularVelocity()
	{
		throw new RuntimeException("Not supported");
	}

	// PROPERTIES

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	static PropertyHelper prophelp = new PropertyHelper(StaticTrajectory.class);

	static {
		prophelp.registerGetSet("parent", "Parent", UniverseThing.class);
		prophelp.registerGet("thing", "getThing");
		prophelp.registerGet("type", "getType");
	}

}
