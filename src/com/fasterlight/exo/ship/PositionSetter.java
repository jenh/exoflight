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
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * Subclass for the vehicle that lets you select
  * a vehicle and starting location
  */
public class PositionSetter
implements PropertyAware
{
	UniverseThing thing;
	SpaceShip ship;
	Game game;

	KeplerianElements elements;
	UniverseThing body, base, last_base;
	Vec3d llr = new Vec3d();

	static final int SEMIMAJ = 1;
	static final int ECC     = 2;
	static final int PERI    = 4;
	static final int APO     = 8;

	byte cons_flags = SEMIMAJ | ECC;
	byte flag1 = SEMIMAJ;
	byte flag2 = ECC;

	double fpa, truevel, heading;

	//

	public PositionSetter(SpaceShip ship)
	{
		this((UniverseThing)ship);
		this.ship = ship;
	}

	public PositionSetter(UniverseThing thing)
	{
		this.thing = thing;
		this.game = thing.getUniverse().getGame();

		Conic curorbit = UniverseUtil.getGeocentricConicFor(thing);
		body = thing.getParent();
		if (curorbit != null)
			elements = curorbit.getElements();
		else
			elements = new KeplerianElements(
				body.getRadius()+300, 0,
				0, 0, 0, 0,
				body.getMass()*Constants.GRAV_CONST_KM, 0);
		// set LLR
		llr.x = thing.getTelemetry().getLONG();
		llr.y = thing.getTelemetry().getLAT();
		llr.z = thing.getTelemetry().getCENDIST();
	}

	// PROPERTIES

	// we can change 4 params (peri, apo, semimajor, and ecc)
	// so we need 2 constraints
	// whenever a param is changed, it becomes one of the constraints
	// the complete set of eq's is:

	// rp = a*(1-e)
	// ra = a*(1+e)
	// ra = 2*a - rp
	// rp = 2*a - ra

	//todo

	private KeplerianElements cloneElements()
	{
		return new KeplerianElements(elements);
	}

	private void commitElements(KeplerianElements ke)
	{
		this.elements = ke;
	}
	
	private void checkAlt(double x) throws UserException
	{
		if (x < 0)
			throw new UserException("You must give a positive value (otherwise you'll be inside of the planet!)");
	}

	public double getSemiMajorAxis()
	{
		return elements.getSemiMajorAxis();
	}

	public void setSemiMajorAxis(double x)
	{
		setFlag(SEMIMAJ);
		switch (cons_flags)
		{
			case SEMIMAJ|PERI:
				elements.setAxisWRSPeriapsis(x);
				break;
			case SEMIMAJ|ECC:
				elements.setSemiMajorAxis(x);
				break;
			case SEMIMAJ|APO:
				elements.setAxisWRSApoapsis(x);
				break;
		}
	}

	public double getEccentricity()
	{
		return elements.getEccentricity();
	}

	public void setEccentricity(double x)
	{
		setFlag(ECC);
		switch (cons_flags)
		{
			case ECC|PERI:
				elements.setEccentricityWRSPeriapsis(x);
				break;
			case ECC|SEMIMAJ:
				elements.setEccentricityWRSAxis(x);
				break;
			case ECC|APO:
				elements.setEccentricityWRSApoapsis(x);
				break;
		}
	}

	public double getAltPeriapsis()
	{
		return elements.getPeriapsis()-body.getRadius();
	}

	public void setAltPeriapsis(double x)
	{
		setFlag(PERI);
		x += body.getRadius();
		switch (cons_flags)
		{
			case PERI|ECC:
				elements.setPeriapsis(x);
				break;
			case PERI|SEMIMAJ:
				elements.setPeriapsisWRSAxis(x);
				break;
			case PERI|APO:
				elements.setPeriapsisWRSApoapsis(x);
				break;
		}
	}

	public double getAltApoapsis()
	{
		return elements.getApoapsis()-body.getRadius();
	}

	public void setAltApoapsis(double x)
	{
		setFlag(APO);
		x += body.getRadius();
		switch (cons_flags)
		{
			case APO|ECC:
				elements.setApoapsis(x);
				break;
			case APO|SEMIMAJ:
				elements.setApoapsisWRSAxis(x);
				break;
			case APO|PERI:
				elements.setApoapsisWRSPeriapsis(x);
				break;
		}
	}


	//

	public KeplerianElements getElements()
	{
		return elements;
	}

	public UniverseThing getBody()
	{
		return body;
	}

	public void setBody(UniverseThing body)
	{
		if (body != this.body)
		{
			if (this.body != null && this.body.getRadius() > 0)
			{
				elements.setSemiMajorAxis(elements.getSemiMajorAxis()
						* body.getRadius() / this.body.getRadius());
			}
			this.body = body;
		}
	}

	public UniverseThing getBase()
	{
		return base;
	}

	public void setBase(UniverseThing base)
	{
		this.base = base;
		if (base != null)
			body = base.getParent();
	}

	public Vec3d getLLR()
	{
		if (base != last_base)
		{
			last_base = base;
			Telemetry telem = base.getTelemetry();
			llr.set(telem.getLONG(), telem.getLAT(), telem.getCENDIST());
			updateAltitude();
		}
		return new Vec3d(llr);
	}

	protected void updateAltitude()
	{
		if (body instanceof Planet)
		{
			llr.z = ((Planet)body).getElevationAt(llr.y, llr.x) + body.getRadius();
		}
	}

	public double getLatitude()
	{
		return getLLR().y;
	}

	public void setLatitude(double a)
	{
		llr.y = a;
		updateAltitude();
	}

	public double getLongitude()
	{
		return getLLR().x;
	}

	public void setLongitude(double a)
	{
		llr.x = a;
		updateAltitude();
	}

	public double getAltitude()
	{
		return getLLR().z - body.getRadius();
	}

	public void setAltitude(double a)
	{
		llr.z = a + body.getRadius();
	}

	public double getTrueVelocity()
	{
		return truevel;
	}

	public void setTrueVelocity(double truevel)
	{
		this.truevel = truevel;
	}

	public double getHeading()
	{
		return heading;
	}

	public void setHeading(double heading)
	{
		this.heading = heading;
	}

	public double getFlightPathAngle()
	{
		return fpa;
	}

	public void setFlightPathAngle(double fpa)
	{
		this.fpa = fpa;
	}

	//

	public void applyPositionOrbit()
	{
		UniverseUtil.setPositionInOrbit(thing, body, elements, game.time());
	}

	public void applyPositionGround()
	{
		Vector3d llr = getLLR();

		// if no velocity, add the thing's butt
		// TODO: should use ship's orientation and contact points
		if (ship != null && truevel <= 0)
		{
			llr.z += ship.getStructure().getLoExtents().z*Constants.M_TO_KM;
			// turn guidance off so we don't spin around
			ship.getGuidanceCapability().setActive(false);
		}

		Planet planet = (body instanceof Planet) ? ((Planet)body) : null;
		// if we have a >0 velocity, add a velocity
		long time = game.time();
		if (planet != null && truevel > 0)
		{
			UniverseUtil.setPositionOnGroundWithVel(thing,
				planet, llr, time, new Vec3d(heading, fpa, truevel));
		} else {
			UniverseUtil.setPositionOnGround(thing, body, llr, time);
		}
	}

	void setFlag(int flag)
	{
		if ((cons_flags | flag) != cons_flags)
		{
			flag1 = flag2;
			flag2 = (byte)flag;
			int new_flags = (flag1 | flag2);
			if (AstroUtil.countBits(new_flags) == 2)
				cons_flags = (byte)new_flags;
			else
				System.out.println("*** flags = " + cons_flags);
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(PositionSetter.class);

	static {
		prophelp.registerGetSet("body", "Body", UniverseThing.class);
		prophelp.registerGetSet("base", "Base", UniverseThing.class);
		prophelp.registerGet("elements", "getElements");
		prophelp.registerGetSet("alt_periapsis", "AltPeriapsis", double.class);
		prophelp.registerGetSet("alt_apoapsis", "AltApoapsis", double.class);
		prophelp.registerGetSet("semimajor", "SemiMajorAxis", double.class);
		prophelp.registerGetSet("eccent", "Eccentricity", double.class);
		prophelp.registerGetSet("lat", "Latitude", double.class);
		prophelp.registerGetSet("long", "Longitude", double.class);
		prophelp.registerGetSet("alt", "Altitude", double.class);
		prophelp.registerGetSet("truevel", "TrueVelocity", double.class);
		prophelp.registerGetSet("heading", "Heading", double.class);
		prophelp.registerGetSet("fpa", "FlightPathAngle", double.class);
	}

	public Object getProp(String key)
	{
		if (key.startsWith("flags#")) {
			int flagno = PropertyUtil.toInt(key.substring(6));
			return (cons_flags & (1<<flagno)) != 0 ? Boolean.TRUE : Boolean.FALSE;
		}
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		if (key.startsWith("flags#")) {
			int flagno = PropertyUtil.toInt(key.substring(6));
			setFlag(1<<flagno);
			return;
		}
		prophelp.setProp(this, key, value);
	}

}
