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
package com.fasterlight.exo.strategy;

import java.util.*;

import com.fasterlight.exo.crew.CrewMember;
import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.GameEvent;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * Represents a player (space agency, commercial org, NASA, whatever...)
  */
public class Agency
implements Constants, PropertyAware
{
	SpaceGame game;
	Universe u;

	Set personnel;
	Map vehiclecnt;
	List ships;			// set of SpaceShip
	List bases;			// set of SpaceBase

	String name;

	int BUDGET_FACTOR = 250; // $.25 mil per point per month
	int BASE_RENT = 25; // .1 mil per base per day
	static final int KRAP = 0; //todo!

	GameEvent dailytick;

	List listeners = new ArrayList();

	CrewRoster roster;

	public Agency(SpaceGame game, Universe u)
	{
		this.game = game;
		this.u = u;

		personnel = new HashSet();
		ships = new ArrayList();
		bases = new ArrayList();
		vehiclecnt = new HashMap();

		roster = new CrewRoster("etc/crewnames.txt");
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	//

	static final double MIN_HEIGHT = 0.001; // 1 meter

	public SpaceShip prepareVehicle(Vehicle vehicle, SpaceBase base)
	{
		Structure struct = vehicle.toStructure(this);
		Planet planet = (Planet)base.getParent();
		// todo: use base ll pos
		Vector3d pos = base.getPosition(planet, game.time());
		double alt = -struct.getLoExtents().z/1000;
		pos.scale( (pos.length() + alt + MIN_HEIGHT)/pos.length() );
		LandedTrajectory traj = new LandedTrajectory(planet, game, pos);
		traj.setLandOrt(vehicle.getTakeoffOrientation());
		SpaceShip ship = new SpaceShip(struct);
		ship.setTrajectory(traj);
		ship.setName(vehicle.getName());
		vehicle.initializeShip(ship);
		addShip(ship);
		return ship;
	}

	public SpaceShip prepareVehicle(Vehicle vehicle)
	{
		Structure struct = vehicle.toStructure(this);
		Planet planet = (Planet)game.getBody("Earth");
		KeplerianElements kepel = new KeplerianElements(
			planet.getRadius()+300, 0, 0, 0, 0, 0,
			planet.getMass()*GRAV_CONST_KM, 0);
		Conic orbit = kepel.getConic();
      CowellTrajectory traj = new CowellTrajectory();
      traj.setParent(planet);
      traj.setConic(orbit);
		SpaceShip ship = new SpaceShip(struct);
		ship.setTrajectory(traj);
		ship.setName(vehicle.getName());
		vehicle.initializeShip(ship);
		addShip(ship);
		return ship;
	}

	public void addShip(SpaceShip ship)
	{
		ships.add(ship);
	}

	public List getBases()
	{
		return Collections.unmodifiableList(bases);
	}

	public List getShips()
	{
		return Collections.unmodifiableList(ships);
	}

	// BASE STUFF

	public SpaceBase designateNewBase()
	{
		SpaceBase sb = new SpaceBase();
		return sb;
	}

	public SpaceBase designateNewBase(Planet planet, Vector3d llr, String basename)
	{
		SpaceBase sb = new SpaceBase(game, planet, llr);
		sb.setName(basename);
		designateBase(sb);
		return sb;
	}

	public void designateBase(SpaceBase base)
	{
		bases.add(base);
	}

	// VEHICLES

	public void addVehicle(Vehicle vehicle, int count)
	{
		vehiclecnt.put(vehicle, new Integer(getNumVehicles(vehicle)+count));
	}

	public int getNumVehicles(Vehicle vehicle)
	{
		Integer i = (Integer)vehiclecnt.get(vehicle);
		if (i == null)
			return 0;
		else
			return i.intValue();
	}

	// CHEATS

	/**
	  * @deprecated
	  */
	public void cheatAddAllTechs()
	{
	}

	/**
	  * @deprecated
	  */
	public void cheatAddAllVehicles()
	{
	}

	/**
	  * @deprecated
	  */
	public void cheatAddAllBases()
	{
	}

	public String toString()
	{
		return name;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Agency.class);

	static {
		prophelp.registerGet("name", "getName");
		prophelp.registerGet("newbase", "designateNewBase");
	}

	public Object getProp(String key)
	{
		// todo: sux
		if (key.startsWith("preparevehicle$"))
		{
			// todo: error if vehicle don't exist
			StringTokenizer st = new StringTokenizer(key.substring(15), "$");
			Vehicle vehicle = Vehicle.getVehicle( st.nextToken() );
			if (vehicle == null)
				throw new PropertyRejectedException("Cannot locate vehicle: " + key);
			if (!st.hasMoreTokens())
				return prepareVehicle( vehicle );
			else {
				SpaceBase base = (SpaceBase)game.getBody(st.nextToken());
				if (base == null)
					throw new PropertyRejectedException("Cannot locate base: " +key);
				return prepareVehicle( vehicle, base );
			}
		}
		else if (key.startsWith("crew#"))
		{
			int i = Integer.parseInt(key.substring(5));
			CrewMember cm = new CrewMember();
			cm.setName(roster.getName(i));
			return cm;
		}
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}


}
