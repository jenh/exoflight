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
package com.fasterlight.exo.orbit;

import java.util.*;

import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * The default object to implement UniverseThing
  */
public class DefaultUniverseThing
	implements UniverseThing, java.io.Serializable, Constants, PropertyAware
{
	protected Game game;
	protected Trajectory traj;
	Set children = new HashSet();
	String name = "N/A";
	double radius, mass;
	Universe u;
	Telemetry telem;

	//

	public DefaultUniverseThing()
	{
	}

	public Universe getUniverse()
	{
		return u;
	}

	public Trajectory getTrajectory()
	{
		return traj;
	}

	private void assertTrajectory()
	{
		if (traj == null)
			throw new RuntimeException(this +" has no trajectory");
	}

	/**
	  * Sets a trajectory, deactivating the old one if there was an old one,
	  * and activating the new one.
	  * Also adds the object to the appropriate universe.
	  */
	public void setTrajectory(Trajectory newtraj)
	{
		if (newtraj == traj)
			return;
		if (traj != null)
		{
			traj.deactivate();
			if (traj.getParent() != null)
			{
				traj.getParent().removeChild(this);
			}
			u.removeThing(this);
			u = null;
			game = null;
		}
		if (newtraj != null)
		{
			if (newtraj.getParent() != null)
			{
				newtraj.getParent().addChild(this);
			}
			u = newtraj.getParent().getUniverse();
			game = u.getGame();
			u.addThing(this);
			newtraj.activate(this);
		}
		//		System.out.println("setTrajectory : " + newtraj);
		this.traj = newtraj;
	}

	public UniverseThing getParent()
	{
		assertTrajectory();
		return traj.getParent();
	}

	public void addChild(UniverseThing child)
	{
		children.add(child);
	}

	public void removeChild(UniverseThing child)
	{
		children.remove(child);
	}

	public Iterator getChildren()
	{
		return children.iterator();
	}

	public Vector3d getPosition(UniverseThing ref, long time)
	{
		if (ref == this)
			return new Vec3d();
		// if the reference frame is our parent, we don't need
		// to do anything else
		assertTrajectory();

		Vector3d pos = traj.getPos(time);
		if (getParent() != ref)
		{
			// if we have a parent, add its position with relation
			// to 'ref'
			if (getParent() != null)
			{
				pos.add(getParent().getPosition(ref, time));
			}
			else
			{
				// if we don't have a parent, we need to
				// subtract the position of the reference object
				// with respect to the origin
				// optz: use common parent instead of origin
				if (ref != null)
					pos.sub(ref.getPosition(null, time));
			}
		}
		return pos;
	}

	public Vector3d getVelocity(UniverseThing ref, long time)
	{
		if (ref == this)
			return new Vec3d();

		assertTrajectory();

		Vector3d vel = traj.getVel(time);
		if (getParent() != ref)
		{
			if (getParent() != null)
			{
				vel.add(getParent().getVelocity(ref, time));
			}
			else
			{
				if (ref != null)
					vel.sub(ref.getVelocity(null, time));
			}
		}
		return vel;
	}

	public Orientation getOrientation(long time)
	{
		assertTrajectory();
		return traj.getOrt(time);
	}

	public double getRadius()
	{
		return radius;
	}

	public float getVisibleRadius()
	{
		return (float) getRadius();
	}

	public double getMass()
	{
		return mass;
	}

	public double getMass(long time)
	{
		return getMass();
	}

	public void setRadius(float rad)
	{
		this.radius = rad;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setMass(double mass)
	{
		this.mass = mass;
	}

	// optz: make faster, precalc
	public double getInfluenceRadius(long time)
	{
		if (getParent() == null || mass < 1e8)
			return 0;
		Conic orbit = UniverseUtil.getConicFor(this);
		if (orbit == null)
			return 0;
		double D = orbit.getSemiMajorAxis();
		/*
		Vector3d thispos = this.getPosition(getParent(), time);
		double D = thispos.length();
		*/
		double infrad =
			D * Math.pow(this.getMass() / getParent().getMass(), 2d / 5);
		infrad = Math.max(infrad, getRadius() * 2);
		return infrad;
	}

	public String getName()
	{
		return name;
	}

	public String toString()
	{
		return name;
	}

	protected Telemetry makeTelemetry()
	{
		return new DefaultTelemetry(this);
	}

	public Telemetry getTelemetry()
	{
		assertTrajectory();
		if (telem == null)
			telem = makeTelemetry();
		if (telem.getTime() != game.time())
			telem.setTime(game.time());
		return telem;
	}

	public Perturbation getDragPerturbation(Trajectory traj)
	{
		return null;
	}

	// PROPERTY AWARE

	public Object getProp(String key)
	{
		switch (key.charAt(0))
		{
			case 't' :
				if ("telemetry".equals(key))
					return getTelemetry();
				else if ("trajectory".equals(key))
					return getTrajectory();
				break;
			case 'n' :
				if ("name".equals(key))
					return getName();
				break;
			case 'p' :
				if ("parent".equals(key))
					return getParent();
				break;
			case 'u' :
				if ("universe".equals(key))
					return getUniverse();
				break;
		}
		return null;
	}

	public void setProp(String key, Object value)
	{
		if ("traj".equals(key))
		{
			setTrajectory((Trajectory) value);
			return;
		}
		else if ("name".equals(key))
		{
			setName(PropertyUtil.toString(value));
			return;
		}
		else if ("mass".equals(key))
		{
			setMass(PropertyUtil.toDouble(value));
			return;
		}
		else if ("radius".equals(key))
		{
			setRadius(PropertyUtil.toFloat(value));
			return;
		}
		throw new PropertyRejectedException(key);
	}
}
