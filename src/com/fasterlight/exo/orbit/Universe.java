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

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * A Universe object is the root of a UniverseThing tree.
  * It also acts as a registry of things in the Universe.
  */
public class Universe
implements UniverseThing, java.io.Serializable, PropertyAware
{
	SpaceGame game;
	Set things;
	List thinglist;
	List children;

	Map classlists;

	//

	public Universe(SpaceGame game)
	{
		this.game = game;
		this.things = new HashSet();
		this.thinglist = new LinkedList();
		this.children = new LinkedList();
		this.classlists = new HashMap();
	}

	public SpaceGame getGame()
	{
		return game;
	}

   public void addThing(UniverseThing thing)
   {
   	if (things.contains(thing))
   		throw new RuntimeException(thing + " already exists in " + this);
   	// automagically add parent (todo?)
   	/*
   	if (thing.getParent() != null && !things.contains(thing.getParent()))
   		addThing(thing.getParent());
   	*/
   	things.add(thing);
   	thinglist.add(thing);
   	addClassThing(thing, thing.getClass());
   }

   public void removeThing(UniverseThing thing)
   {
   	if (!things.contains(thing))
   		throw new RuntimeException(thing + " does not exist in " + this);
   	things.remove(thing);
   	thinglist.remove(thing);
   	removeClassThing(thing, thing.getClass());
   }

   private void addClassThing(UniverseThing thing, Class clazz)
   {
   	if (clazz == Object.class)
   		return;
   	List l = (List)classlists.get(clazz);
   	if (l == null) {
   		l = new ArrayList();
   		classlists.put(clazz, l);
   	}
   	l.add(thing);

   	addClassThing(thing, clazz.getSuperclass());
   }

   private void removeClassThing(UniverseThing thing, Class clazz)
   {
   	if (clazz == Object.class)
   		return;
   	List l = (List)classlists.get(clazz);
  		l.remove(thing);

  		removeClassThing(thing, clazz.getSuperclass());
   }

   public Iterator getThings()
   {
   	return things.iterator();
   }

   public List getThingList()
   {
   	return Collections.unmodifiableList(thinglist);
   }

   public List getThingsByClass(Class clazz)
   {
   	List l = (List)classlists.get(clazz);
   	if (l != null)
   		return Collections.unmodifiableList(l);
   	else
   		return Collections.EMPTY_LIST;
   }

   public UniverseThing getThingByName(String name)
   {
   	Iterator it = getThings();
   	while (it.hasNext())
   	{
   		UniverseThing thing = (UniverseThing)it.next();
   		if (thing.getName() != null && thing.getName().equals(name))
   			return thing;
   	}
   	return null;
   }

	public Trajectory getTrajectory()
	{
		throw new RuntimeException("Universe doesn't move");
	}
	public void setTrajectory(Trajectory traj)
	{
		throw new RuntimeException("Universe doesn't move");
	}

	public void addPerturbation(Perturbation p)
	{
		throw new RuntimeException("Universe doesn't move");
	}
	public void removePerturbation(Perturbation p)
	{
		throw new RuntimeException("Universe doesn't move");
	}

	public UniverseThing getParent()
	{
		return null;
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
		if (ref == null)
			return new Vec3d();
		Vector3d p = ref.getPosition(null, time);
		p.scale(-1);
		return p;
	}

	public Vector3d getVelocity(UniverseThing ref, long time)
	{
		if (ref == null)
			return new Vec3d();
		Vector3d p = ref.getVelocity(null, time);
		p.scale(-1);
		return p;
	}

	public Orientation getOrientation(long time)
	{
		return new Orientation();
	}

	public double getRadius()
	{
		return Double.POSITIVE_INFINITY;
	}

	public float getVisibleRadius()
	{
		return Float.POSITIVE_INFINITY;
	}

	public double getMass()
	{
		return Double.POSITIVE_INFINITY;
	}

	public double getMass(long time)
	{
		return Double.POSITIVE_INFINITY;
	}

	public double getInfluenceRadius(long time)
	{
		return Double.POSITIVE_INFINITY;
	}

	public String getName()
	{
		return "UNIVERSE MAN";
	}

	public Universe getUniverse()
	{
		return this;
	}

	public Telemetry getTelemetry()
	{
		return new DefaultTelemetry(this);
	}

	public Perturbation getDragPerturbation(Trajectory traj)
	{
		throw new RuntimeException("Universe doesn't move");
	}

	// PROPERTY AWARE

	public Object getProp(String key)
	{
		switch (key.charAt(0))
		{
			case '$' :
				return getThingByName(key.substring(1));
			case 't' :
				if ("things".equals(key))
					return getThingList();
				break;
		}

		return null;
	}

	public void setProp(String key, Object value)
	{
		throw new PropertyRejectedException(key);
	}
}

