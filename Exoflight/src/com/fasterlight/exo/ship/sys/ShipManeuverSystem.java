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
package com.fasterlight.exo.ship.sys;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.spif.*;

/**
  * Performs various orbital maneuvers for a ship.
  */
public class ShipManeuverSystem
extends ShipSystem
implements PropertyAware
{
	private int which;
	private boolean initialdir;

	private KeplerianElements elements = new KeplerianElements();
	private boolean useargperi;

	private static final int APO = 1;
	private static final int PERI = 2;

	//

	public ShipManeuverSystem(SpaceShip ship)
	{
		super(ship);
	}

	public KeplerianElements getElements()
	{
		// set mu param
		elements.setMu(ship.getParent().getMass()*Constants.GRAV_CONST_KM);
		return elements;
	}

	public Conic getConic()
	{
		return new Conic(elements);
	}

	private double getParentRadius()
	{
		return ((Planet)ship.getParent()).getRadius();
	}

	public double getPeriapsis()
	{
		return elements.getPeriapsis() - getParentRadius();
	}

	public void setPeriapsis(double periapsis)
	{
		periapsis += getParentRadius();

		if (which == APO)
			elements.setPeriapsisWRSApoapsis(periapsis);
		else
			elements.setPeriapsis(periapsis);
		which = PERI;
	}

	public double getApoapsis()
	{
		return elements.getApoapsis() - getParentRadius();
	}

	public void setApoapsis(double apoapsis)
	{
		apoapsis += getParentRadius();

		if (which == PERI)
			elements.setApoapsisWRSPeriapsis(apoapsis);
		else
			elements.setApoapsis(apoapsis);
		which = APO;
	}

	/**
	  * @return name of what part of orbit is being changed (apoapsis or periapsis)
	  */
	public String getWhich()
	{
		switch (which) {
			case APO: return "apoapsis";
			case PERI: return "periapsis";
			default: return "-";
		}
	}

	public void setWhich(String s)
	{
		if ("apoapsis".equals(s))
			which = APO;
		else if ("periapsis".equals(s))
			which = PERI;
		else
			which = 0;
	}

	/**
	  * @return true if yaw=0 (orbit raise) false if yaw=180 (orbit lower)
	  */
	public boolean getDirection()
	{
		switch (which)
		{
			case APO:
				return getApoapsis() > getShipTelemetry().getAPOAPSIS();
			case PERI:
				return getPeriapsis() > getShipTelemetry().getPERIAPSIS();
			default:
				return false; //??
		}
	}

	public double getYaw()
	{
		return getDirection() ? 0 : Math.PI; // 0 or 180 deg
	}

	public long getBurnTime()
	{
		Telemetry telem = getShipTelemetry();
		Conic o = telem.getConic();
		if (o == null)
			return INVALID_TICK;

		long t;
		switch (which)
		{
			case APO:
				t = AstroUtil.getNearestTime(o, o.getPeriapsis(), getGame().time());
				break;
			case PERI:
				t = AstroUtil.getNearestTime(o, o.getApoapsis(), getGame().time());
				break;
			default:
				return INVALID_TICK;
		}
		return t;
	}

	protected Sequencer loadSequencer()
	{
		Sequencer seq = ship.loadProgram("raiselower");
		long t0 = getBurnTime();
		if (t0 == INVALID_TICK)
			throw new PropertyRejectedException("Could not compute burn time");
		if (t0 < getGame().time())
			throw new PropertyRejectedException("Burn time is in the past");

		initialdir = getDirection();
		seq.setZeroTime(t0);
		return seq;
	}

	public boolean isDone()
	{
		Telemetry telem = getShipTelemetry();
		boolean dir = getDirection();

		switch (which)
		{
			case APO:
				return (dir != initialdir) ||
					(telem.getAPOAPSIS() - telem.getALT() < telem.getALT() - telem.getPERIAPSIS());
			case PERI:
				return (dir != initialdir) ||
					(telem.getAPOAPSIS() - telem.getALT() > telem.getALT() - telem.getPERIAPSIS());
			default:
				throw new PropertyRejectedException("Invalid mode: " + which);
		}
	}

	public double getDeltaV()
	{
		Conic orbit = getShipTelemetry().getConic();
		double prad = ship.getParent().getRadius();
		double apo,peri;
		double r1,r2,a2,k,e;

		apo = orbit.getApoapsis();
		peri = orbit.getPeriapsis();
		e = orbit.getEccentricity();

		// apo+peri = 2a

		switch (which)
		{
			case APO: // apoapsis changes
				r1 = peri;
				r2 = prad + getApoapsis();
				k = r2/apo;
				a2 = (r2+peri)/2; //(e*(k-1)+k+1);
				break;
			case PERI: // periapsis changes
				r1 = apo;
				r2 = prad + getPeriapsis();
				k = r2/peri;
				a2 = (apo+r2)/2; //(e*(k-1)-k-1);
				break;
			default:
				return Double.NaN;
		}

		double U = orbit.getMu();
		double v1 = orbit.getVelocityAtRadius(r1);
		double v2 = Math.sqrt(U*(2/r1-1/a2));

		/*
		System.out.println("r1=" + r1 + ", r2=" + r2 + ", k=" + k + ", a2=" + a2);
		System.out.println("a1=" + orbit.getSemiMajorAxis());
		System.out.println("v1=" + v1 + ", v2=" + v2);
		*/

		return Math.abs(v1-v2);
	}

	public boolean getUpdate()
	{
		return false;
	}

	public void updateCurrent(boolean b)
	{
		if (b)
		{
			elements = getShipTelemetry().getConic().getElements();
			useargperi = true;
		}
	}

	public void updateIntercept(boolean b)
	{
		if (b)
		{
			elements = ship.getShipTargetingSystem().getTransferOrbit().getElements();
			useargperi = true;
		}
	}

	public void updateTargetOrbit(boolean b)
	{
		if (b)
		{
			ShipTargetingSystem sts = ship.getShipTargetingSystem();
			double mu = ship.getParent().getMass()*Constants.GRAV_CONST_KM;
			elements = new KeplerianElements(
					ship.getTelemetry().getCenDistVec(),
					ship.getTelemetry().getVelocityVec(),
					mu);
			// TODO: what if different parents for ship & target?
			useargperi = false;
		}
	}
	
	public void updateLaunch(boolean b)
	{
		if (b)
		{
			Conic orbit = ship.getShipLaunchSystem().getTargetOrbit();
			if (orbit != null)
			{
				elements = orbit.getElements();
				useargperi = false;
			}
		}
	}

	public boolean getUseArgPeriapsis()
	{
		return useargperi;
	}

	public void setUseArgPeriapsis(boolean useargperi)
	{
		this.useargperi = useargperi;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipManeuverSystem.class);

	static {
		prophelp.registerGetSet("apoapsis", "Apoapsis", double.class);
		prophelp.registerGetSet("periapsis", "Periapsis", double.class);
		prophelp.registerGetSet("which", "Which", String.class);
		prophelp.registerGet("direction", "getDirection");
		prophelp.registerGet("yaw", "getYaw");
		prophelp.registerGet("done", "isDone");
		prophelp.registerGet("deltav", "getDeltaV");
		prophelp.registerGet("elements", "getElements");
		prophelp.registerGet("conic", "getConic");
		prophelp.registerSet("updatecurrent", "updateCurrent", boolean.class);
		prophelp.registerSet("updateintercept", "updateIntercept", boolean.class);
		prophelp.registerSet("updatelaunch", "updateLaunch", boolean.class);
		prophelp.registerSet("updatetargetorbit", "updateTargetOrbit", boolean.class);
		prophelp.registerGetSet("useargperi", "UseArgPeriapsis", boolean.class);
	}

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
}
