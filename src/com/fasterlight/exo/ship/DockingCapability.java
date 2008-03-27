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

import java.util.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.Vector3d;

/**
  * A docking node capability -- subclasses are 'probe' and 'drogue'.
  * It has a radius, and an orientation (the direction it points).
  * When activated, it seeks out the mate of the targeted spaceship,
  * and when the probe is inside the drogue's radius (and one is active)
  * the two ships dock.
  */
public abstract class DockingCapability
extends PeriodicCapability
{
	protected transient float radius;
	protected transient Orientation dockort = new Orientation();
	protected int conndir;
	protected float FORCE_MULT = 64;
	protected float CAPTURE_SCALE = 4;

	protected DockingPerturbation dockperturb;

	public DockingCapability(Module module)
	{
		super(module);
	}

	public abstract boolean isProbe();

	public float getRadius()
	{
		return radius;
	}

	public int getConnectDir()
	{
		return conndir;
	}

	public Orientation getDockingOrientation()
	{
		return dockort;
	}

	protected boolean notifyActivated()
	{
		// don't allow activation if something is already docked here
		return getModule().getLink(getConnectDir()) == null;
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		radius = Util.parseFloat(props.getProperty("radius", "0"))/1000;
		conndir = Module.connectCharToDir(props.getProperty("dir", "u").charAt(0));
		dockort.set(Module.getModuleOrientation(conndir));
	}

	//

	boolean setDockingInfo(Vector3d shippos, long t)
	{
		SpaceShip ship = (SpaceShip)getThing();
		UniverseThing target = ship.getShipTargetingSystem().getTarget();
		if (target == null || !(target instanceof SpaceShip) || target == ship)
		{
			return false;
		}
		SpaceShip targetship = (SpaceShip)target;

		// compute distance from this probe to the target's CM
		Vector3d probeofs = new Vector3d(getCMOffset());
		ship.getOrientation(t).transform(probeofs);
		probeofs.scale(0.001); // m to km

		Vector3d targpos = target.getTrajectory().getPos(t);
		targpos.sub(shippos);

		probeofs.sub(targpos, probeofs);


		// iterator over all target's stuff
		// todo: what if multiple target drogues/probes?
		Iterator it = targetship.getStructure().getCapabilitiesOfClass(DockingCapability.class).iterator();
		while (it.hasNext())
		{
			DockingCapability dcap = (DockingCapability)it.next();
			if (dcap.isProbe() != isProbe())
			{
				// compute distance between probes
				Vector3d drogueofs = new Vector3d(dcap.getCMOffset());
				targetship.getOrientation(t).transform(drogueofs);
				drogueofs.scale(0.001); // m to km
				drogueofs.add(probeofs);
				ship.getOrientation(t).invTransform(drogueofs);

				// find target orientation
				Vector3d dockdir = dcap.dockort.getDirection(); // targ module frame
				dcap.getModule().getOrientation().transform(dockdir); // targ struct frame
				this.getModule().getOrientation().invTransform(dockdir); // ship struct frame
				this.dockort.invTransform(dockdir); // ship docking cap frame
				dockdir.scale(-1); // reverse it
				targetship.getOrientation(t).transform(dockdir); // intertial frame

				this.dockdir = dockdir;
				this.drogueofs = drogueofs;
				this.dcap = dcap;
				return true;
			}
		}
		return false;
	}

	/**
	  * the "other" docking capability-- set by setDockingInfo()
	  */
	DockingCapability dcap;
	Vector3d drogueofs, dockdir;

	public void doReact(ResourceSet react, ResourceSet product)
	{
		long t = getGame().time();
		Vector3d shippos = getThing().getTrajectory().getPos(t);
		if (!setDockingInfo(shippos, t))
			return;

		SpaceShip ship = (SpaceShip)getThing();
		ship.getShipTargetingSystem().setLastDockingOffset(drogueofs);
		ship.getShipTargetingSystem().setLastDockingDirection(dockdir);

		// if radius below certain level, dock!
		// (todo: which one retains its identity?)
		float rad = isProbe() ? dcap.getRadius() : getRadius();
		if (drogueofs.length() < rad)
		{
/*
			addDockingPerturb();
		} else {
			removeDockingPerturb();
		}
*/
			SpaceShip targetship = (SpaceShip)ship.getShipTargetingSystem().getTarget();
			this.deactivate();
			dcap.deactivate();

			// find docked position & ort before attaching
			Vector3d r0 = new Vector3d(shippos);
			Vector3d r1 = new Vector3d(drogueofs);
			ship.getOrientation(t).transform(r1);
			r0.add(r1);

			Vector3d v0 = targetship.getTrajectory().getVel(t);

			Orientation ort0 = new Orientation(dockdir,
				ship.getOrientation(t).getUpVector());

			// todo: this ain't so great!
			((MutableTrajectory)ship.getTrajectory()).set(ship.getParent(), r0, v0, t, ort0);

			// attach ship in its current position
			ship.attach(targetship,
				dcap.getModule(), dcap.getCMOffset(), dcap.getConnectDir(),
				getModule(), getCMOffset(), getConnectDir());
			targetship.setTrajectory(null); // gone from universe
			// todo: removing objects is sketchy, fix problems
		}

	}

	// todo: dont' use

	class DockingPerturbation
	implements Perturbation
	{
		boolean capture=true;

		/**
		  * Computes the mass at time 'time' and scales the thrust vector accordingly
		  */
		public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
			Orientation ort, Vector3d w, long time)
		{
			if (!setDockingInfo(r, time))
				return;

			// if rad < probe radius, apply suction force
			float rad = isProbe() ? dcap.getRadius() : getRadius();
			if (capture)
				rad *= CAPTURE_SCALE;

//			System.out.println("dist=" + drogueofs.length() + ", rad=" + rad);
			if (drogueofs.length() < rad)
			{
				Vector3d dv = new Vector3d(drogueofs);
				ort.transform(drogueofs);
				dv.scale(FORCE_MULT);
				System.out.println("dv=" + dv);
				force.f.add(dv);
			}
		}
	}

	//

	protected void addDockingPerturb()
	{
		if (dockperturb == null)
		{
			dockperturb = new DockingPerturbation();
			((MutableTrajectory)getThing().getTrajectory()).addPerturbation(dockperturb);
		}
	}

	protected void removeDockingPerturb()
	{
		if (dockperturb != null)
		{
			((MutableTrajectory)getThing().getTrajectory()).removePerturbation(dockperturb);
			dockperturb = null;
		}
	}

	protected boolean notifyDeactivated()
	{
		removeDockingPerturb();
		return true;
	}



}
