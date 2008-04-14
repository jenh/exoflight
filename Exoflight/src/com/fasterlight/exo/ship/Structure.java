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
import com.fasterlight.exo.strategy.Agency;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * A Structure contains the set of modules for a
  * spaceship or spacebase.  A StructureThing points
  * to the Structure, and vice versa.
  */
public class Structure
implements java.io.Serializable, PropertyAware, Constants
{
	protected List modules = new ArrayList();
//	protected ResourceSet supply = new ResourceSet();
	protected List allcaps = new ArrayList();
	protected Game game;
	protected float mass;
	protected float radius;
	protected StructureThing thing;
	protected Agency agency;

	protected Vector3f cenmass = new Vector3f();
	protected Vector3f lextents = new Vector3f(); // low extents
	protected Vector3f hextents = new Vector3f(); // high extents

	protected AeroForces lastforces;

	protected double massflow; // mass flow rate from rocket exhaust
	protected double thrust; // mass flow rate from rocket exhaust
	protected Vector3d thrustVector = new Vector3d();

	protected float temp = 283; // in K
	protected float specheat = 972.7f; // specific heat (J/kg-K) (for Al2024)
	protected long lastheat = INVALID_TICK;

	protected ContactPoint[] contactPoints;

	protected Vector3f totalDamping = new Vector3f();

	//

	public Structure(Game game)
	{
		this.game = game;
		last_massadj_time = game.time();
	}

	public void setOwner(Agency owner)
	{
		this.agency = owner;
	}

	public Agency getOwner()
	{
		return agency;
	}

	public Iterator getAllCaps()
	{
		return allcaps.iterator();
	}

	public List getAllCrew()
	{
		ArrayList list = new ArrayList();
		Iterator it = getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			list.addAll(m.getCrewList());
		}
		Collections.sort(list);
		return list;
	}

	// todo: figure out this module-adding thing

	public void addModule(Module m)
	{
		if (modules.size() != 0)
		{
			throw new IllegalArgumentException("Can only addModule(Module) when empty");
		}
		addModule2(m);
		m.notifyAdded();
	}

	/**
	  * Add the first module to a structure
	  */
	protected void addModule2(Module m)
	{
		modules.add(m);
		m.setStructure(this);
		adjustMass(m.getMass());
//		adjustSupply(m.getSupply(), 1);
		allcaps.addAll(m.getCapabilities());
		// if only 1 module, adjust structure's center of mass
		if (modules.size() == 1)
			adjustCenterOfMass();
	}

	/**
	  * Adds a module to the structure.
	  * from_m - the module being added (from)
	  * to_m - the existing module in the structure to link to, or null
	  * fromdir - the direction on the 'from' module to connect
	  * todir - the direction on the 'to' module to connect to
	  */
	public void addModule(Module from_m, Module to_m, int fromdir, int todir,
		int pointdir, int updir)
	{
		if (to_m == null)
		{
			addModule(from_m);
			return;
		}

		// figure out orientation of from module
		Orientation ort;
		if (pointdir >= 0)
		{
			ort = new Orientation(
				Module.getModuleOrientation(pointdir).getDirection(),
				Module.getModuleOrientation(updir).getDirection());
		} else {
			ort = Module.getModuleOrientation(fromdir ^ 1);
			ort.concat(Module.getModuleOrientation(todir));
			ort = new Orientation(
				ort.getDirection(),
				Module.getModuleOrientation(updir).getDirection());
		}

		// rotate by target module's orientation
		ort.concat(to_m.getOrientation());
		/*
		System.out.println("to_dir=" + todir +", from_dir=" + fromdir);
		System.out.println("to_ort=" + to_ort + ", from_ort=" + from_ort + ", ort=" + ort);
		*/

		// compute new position relative to local coord frame
		// of target module
		Vector3f ofs = new Vector3f();
		// todo: must rotate from_m dimensions
		Vector3f dims = from_m.getDimensions();
		Vector3f cdims = to_m.getDimensions();
		float axisofs = 0;
		// get offset of from module along axis
		switch (fromdir)
		{
			case Module.UP : axisofs = (dims.z); break;
			case Module.DOWN : axisofs = (dims.z); break;
			case Module.NORTH : axisofs = (dims.y); break;
			case Module.SOUTH : axisofs = (dims.y); break;
			case Module.EAST : axisofs = (dims.x); break;
			case Module.WEST : axisofs = (dims.x); break;
		}
		// add offset of from and to module to axis
		switch (todir)
		{
			case Module.UP : ofs.z += (axisofs+cdims.z)/2; break;
			case Module.DOWN : ofs.z -= (axisofs+cdims.z)/2; break;
			case Module.NORTH : ofs.y += (axisofs+cdims.y)/2; break;
			case Module.SOUTH : ofs.y -= (axisofs+cdims.y)/2; break;
			case Module.EAST : ofs.x += (axisofs+cdims.x)/2; break;
			case Module.WEST : ofs.x -= (axisofs+cdims.x)/2; break;
		}
		// now rotate out of target module's frame
		to_m.getOrientation().transform(ofs);

		addModule(from_m, to_m, fromdir, todir, ort, ofs);
	}

	protected void addModule(Module from_m, Module to_m, int fromdir, int todir,
		Orientation ort, Vector3f ofs)
	{
		addModule2(from_m);
		if (to_m != null)
		{
			// link the two modules
			Module.link(from_m, to_m, fromdir, todir);

			from_m.ort.set(ort);
			Vector3f pos = new Vector3f(ofs);
			pos.add(to_m.getPosition());
			from_m.setPosition(pos);

			if (debug) {
				System.out.println("addModule(): " + from_m + "(" + fromdir + ") to "
					 + to_m + "( " + todir + "), pos = " + pos);
				System.out.println("   module points " + from_m.getOrientation().getDirection() +
					", up is " + from_m.getOrientation().getUpVector());
			}
		}

		from_m.notifyAdded();
	}

	void addModule(Module m, Orientation ort, Vector3f ofs)
	{
		addModule2(m);
		m.ort.set(ort);
		m.setPosition(ofs);
		m.notifyAdded();
	}

	public void removeModule(Module m)
	{
		if (!modules.contains(m))
			throw new RuntimeException(this + " doesn't contain " + m);
		// todo: don't allow dangling
		for (int i=0; i<Module.NUM_DIRS; i++)
		{
			if (m.getLink(i) != null)
				Module.unlink(m, m.getLink(i));
		}
		// todo: only deactivate leases aren't in this module
		m.deactivate(); // remove all leases
		removeDependencies(m);
		modules.remove(m);
		m.setStructure(null);
		adjustMass(-m.getMass());
//		adjustSupply(m.getSupply(), -1);
		// todo: doesn't seem to work?
		allcaps.removeAll(m.getCapabilities());
		adjustCenterOfMass();
		if (debug) {
			System.out.println("Cap count = " + allcaps.size());
			System.out.println("Supply = " + getSupply());
		}
		m.notifyRemoved();
	}

	/**
	  * Remove all dependencies on module 'm' in the rest of structure
	  * todo: remove source from m to rest of structure
	  */
	protected void removeDependencies(Module m)
	{
		Iterator it = allcaps.iterator();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			cap.removeDependencies(m);
		}
	}

	/**
	  * Get the maximum component of a vector.
	  */
	float getDimRad(Vector3f d)
	{
		return Math.max(Math.max(d.x, d.y), d.z);
	}

	/**
	  * Compute center of mass, relative
	  * to the current CM -- also recomputes
	  * 'extents' and 'radius'
	  */
	Vector3f computeCenterOfMass()
	{
		Vector3f cm = new Vector3f();
		Iterator it = getModules().iterator();
		float M = Float.MAX_VALUE;
		lextents.set(M,M,M);
		hextents.set(-M,-M,-M);
		int numCustomContactPoints = 0;
		totalDamping.set(0,0,0);
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			Vector3f ofs = m.getPosition();
			Vector3f dims = m.getDimensions();
//			System.out.println(m + "\t" + ofs + "\t" + m.getMass());
			lextents.x = Math.min(lextents.x, ofs.x-dims.x/2);
			lextents.y = Math.min(lextents.y, ofs.y-dims.y/2);
			lextents.z = Math.min(lextents.z, ofs.z-dims.z/2);
			hextents.x = Math.max(hextents.x, ofs.x+dims.x/2);
			hextents.y = Math.max(hextents.y, ofs.y+dims.y/2);
			hextents.z = Math.max(hextents.z, ofs.z+dims.z/2);
			ofs.scale(m.getMass());
			cm.add(ofs);
			// compute contact points
			ContactPoint[] cpts = m.getContactPoints();
			if (cpts != null)
				numCustomContactPoints += cpts.length;
			// while we're here, compute total damping too!
			Vector3f mdamp = m.getDampingVector();
			totalDamping.x = Math.max(totalDamping.x, mdamp.x);
			totalDamping.y = Math.max(totalDamping.y, mdamp.y);
			totalDamping.z = Math.max(totalDamping.z, mdamp.z);
		}
//		System.out.print(cm + "\t" + getMass() + "\t");
		cm.scale(1/getMass());
//		System.out.println(cm);
		lextents.sub(cm);
		hextents.sub(cm);
		radius = (float)(Math.max(getDimRad(lextents), getDimRad(hextents)) * 0.001); // m to km
		computeContactPoints(numCustomContactPoints);
		return cm;
	}

	public Vector3f getTotalDamping()
	{
		return totalDamping;
	}

	public float getRadius()
	{
		return radius; // units are km
	}

	public Vector3f getCenterOfMass()
	{
		return new Vector3f(cenmass);
	}

	public Vector3f getLoExtents()
	{
		return new Vector3f(lextents);
	}

	public Vector3f getHiExtents()
	{
		return new Vector3f(hextents);
	}

	/**
	  * Recomputes the new center of mass.
	  * Called internally when a module is added or deleted.
	  */
	void adjustCenterOfMass()
	{
		if (debug2)
			System.out.println("old CM = " + cenmass);
		Vector3f cm = computeCenterOfMass();
		if (debug2)
			System.out.println("CM delta = " + cm);
		cenmass.set(cm);
		if (debug2)
			System.out.println("new CM " + cenmass);
		/*
		Iterator it = getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			m.position.sub(cm);
		}
		*/
	}

	// MASS & MASS-FLOW ROUTINES

	private long last_massadj_time = INVALID_TICK;
	private long last_cmadj_time = INVALID_TICK;

	void adjustMass(float amt)
	{
		mass += amt;
		long t = game.time();
		last_massadj_time = t;
		if (t > last_cmadj_time+TICKS_PER_SEC)
		{
			adjustCenterOfMass(); // todo??
			last_cmadj_time = t;
		}
	}

	long getLastMassAdjustTime()
	{
		return last_massadj_time;
	}

	// maps Capabilities to mass flow amts (Vector2d)
	private Map massflowmap = new HashMap();

	// maps Capabilities to thrust vector (Vector3d)
	private Map thrustvecmap = new HashMap();

	private void recalcMassFlow()
	{
		massflow = thrust = 0;
		Iterator it = massflowmap.values().iterator();
		while (it.hasNext())
		{
			Vector2d v = (Vector2d)it.next();
			massflow += v.x;
			thrust += v.y;
		}
		thrustVector.set(0,0,0);
		it = thrustvecmap.values().iterator();
		while (it.hasNext())
		{
			Vector3d v = (Vector3d)it.next();
			thrustVector.add(v);
		}
//		System.out.println("thrustVector = " + thrustVector);
	}

	// sets mass flow for a capability
	void setMassFlowRate(Capability cap, double dflow, double exhaustvel, Vector3d thrustvec)
	{
		if (dflow == 0) {
			massflowmap.remove(cap);
			thrustvecmap.remove(cap);
		} else {
			double thrust = dflow*exhaustvel;
			massflowmap.put(cap, new Vector2d(dflow, thrust));
			thrustvec.scale(thrust);
			thrustvecmap.put(cap, thrustvec);
		}
		recalcMassFlow();
	}

	public double getMassFlowRate()
	{
		return massflow;
	}

	public double getCurrentThrust()
	{
		return thrust;
	}

	public Vector3d getThrustVector()
	{
		return new Vec3d(thrustVector);
	}

	public double getMassAtTime(long time)
	{
		return getMass() - (time-last_massadj_time)*massflow/TICKS_PER_SEC;
	}

/*
	void adjustSupply(ResourceSet resset, float amt)
	{
		supply.add(resset, amt);
	}
*/

	public List getModules()
	{
		return Collections.unmodifiableList(modules);
	}

	public boolean containsModule(Module m)
	{
		return modules.contains(m);
	}

	public Module getModuleByName(String name)
	{
		for (int i=0; i<modules.size(); i++)
		{
			Module m = getModule(i);
			if (m.getName().equals(name))
				return m;
		}
		return null;
	}

	public int getModuleCount()
	{
		return modules.size();
	}

	public Module getModule(int i)
	{
		if (i<0 || i>=modules.size())
			return null;
		else
			return (Module)modules.get(i);
	}

	public float getMass()
	{
		return mass;
	}

	public float getEmptyMass()
	{
		float t = 0;
		Iterator it = getModules().iterator();
		while (it.hasNext())
		{
			t += ((Module)it.next()).getEmptyMass();
		}
		return t;
	}

	public float getMiscMass()
	{
		return getMass() - getEmptyMass() - getSupply().mass();
	}

	public ResourceSet getSupply()
	{
   	ResourceSet res = new ResourceSet();
   	Iterator it = getAllCaps();
   	while (it.hasNext())
   	{
   		res.add( ((Capability)it.next()).getSupply() );
   	}
   	return res;
	}

	public ResourceSet getCapacity()
	{
   	ResourceSet res = new ResourceSet();
   	Iterator it = getAllCaps();
   	while (it.hasNext())
   	{
   		res.add( ((Capability)it.next()).getCapacity() );
   	}
   	return res;
	}

	// hotspot
	// current inertia vector:
	// it is a hoop along Z axis, and thin rod along X & Y
	// units are km
	// not scaled by mass
	public Vector3d getInertiaVector()
	{
		Vector3f dim = new Vector3f(hextents);
		dim.sub(lextents);
		dim.scale(0.0005f);
		return new Vector3d(dim.z*dim.z/3, dim.z*dim.z/3, dim.x*dim.y);
	}

	public void setThing(StructureThing thing)
	{
		this.thing = thing;
	}

	public StructureThing getThing()
	{
		return thing;
	}

	public SpaceShip getShip()
	{
		return (thing instanceof SpaceShip) ? (SpaceShip)thing : null;
	}

	// todo: return multiple results
	public Capability getCapabilityOfClass(Class c)
	{
		Iterator it = allcaps.iterator();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			if (c.isAssignableFrom(cap.getClass()))
				return cap;
		}
		return null;
	}

	public List getCapabilitiesOfClass(Class c)
	{
		return getCapabilitiesOfClass(c, false);
	}

	public List getCapabilitiesOfClass(Class c, boolean exact)
	{
		List v = new ArrayList();
		Iterator it = allcaps.iterator();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			if ( (exact && c.equals(cap.getClass())) ||
			   (!exact && c.isAssignableFrom(cap.getClass())) )
				v.add(cap);
		}
		return v;
	}

	// REGIONS, PRESSURE

	void equalizePressure()
	{
		// equalize pressure
		List rgns = getRegions();
		Iterator it = rgns.iterator();
		while (it.hasNext())
		{
			ModuleRegion rgn = (ModuleRegion)it.next();
			if (rgn.volume > 0)
			{
				// visit all modules in this region
				Iterator mods = rgn.visited.iterator();
				while (mods.hasNext())
				{
					Module m = (Module)mods.next();
					ResourceSet target = new ResourceSet(rgn.atmos, m.getVolume()/rgn.volume);
					target.sub(m.getAtmosphere());
					m.adjustAtmosphere(target);
					// todo? slower?
				}
			}
		}
	}

	List getRegions()
	{
		Set todo = new HashSet(modules);
		List v = new ArrayList();
		while (!todo.isEmpty())
		{
			Module m = (Module)todo.iterator().next();
			ModuleRegion rgn = new ModuleRegion();
			visit(m, rgn);
			todo.removeAll(rgn.visited);
			v.add(rgn);
		}
		return v;
	}

	class ModuleRegion
	{
		Set visited = new HashSet();
		float volume = 0;
		ResourceSet atmos = new ResourceSet();
		public String toString() { return visited +"," + volume + ","+atmos; }
	}

	void visit(Module m, ModuleRegion rgn)
	{
		if (rgn.visited.contains(m))
			return;
		rgn.visited.add(m);
		rgn.volume += m.getVolume();
		rgn.atmos.add(m.getAtmosphere());
		for (int i=0; i<Module.NUM_DIRS; i++)
		{
			Module linkm = m.getLink(i);
			if (linkm != null)
			{
				visit(linkm, rgn);
			}
		}
	}

	//

	/**
	  * Calculate drag coeff given velocity vector of ship
	  * relative to airflow (in structure-coordinates,
	  * and normalized) and Mach value.
	  * hotspot
	  */
	public AeroForces calculateDragCoeff(Vector3f vel, float mach, long time)
	{
		Iterator it;
		Vector3f mdir = new Vector3f();
		AeroForces res = new AeroForces();
		float area;
		float totarea = 0.0f;

		it = getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			// TODO: don't include modules that are inside of other modules
			// get direction of module
			Orientation mort = m.getOrientation();
			mdir.set(mort.getDirection());
			// get dot product of velocity vector
			float dot = mdir.dot(vel);
			if (Double.isNaN(dot))
			{
				if (getShip() != null)
					getShip().getShipWarningSystem().setWarning("UCE",
						"calculateDragCoeff(): " + mdir + " " + dot + " " + m.getOrientation() + " " + vel);
				continue;
			}
			// first do +z (fore) direction
			// todo: use different curves for different profiles
			if (dot > 0)
			{
				AeroSurface fas = m.getForeSurface();
				float zposarea = getOccludedZArea(m, Module.UP, fas.area, time);
				if (zposarea > 0)
				{
					area = dot*zposarea;
					totarea += area;
					while (fas != null)
					{
						fas.addForces(m, vel, mort, 1, res, mach, area, time);
						fas = fas.next;
					}
				}
			}
			// oblique direction (anywhere along x,y axis)
			// don't take occlusion into account
			{
				AeroSurface fas = m.getObliqueSurface();
				area = Math.abs(1-dot)*fas.area;
				totarea += area;
				while (fas != null)
				{
					fas.addForces(m, vel, mort, 1, res, mach, area, time);
					fas = fas.next;
				}
			}
			// now do rear
			if (dot < 0)
			{
				AeroSurface fas = m.getAftSurface();
				float zposarea = getOccludedZArea(m, Module.DOWN, fas.area, time);
				if (zposarea > 0)
				{
					area = -dot*zposarea;
					totarea += area;
					while (fas != null)
					{
						fas.addForces(m, vel, mort, -1, res, mach, area, time);
						fas = fas.next;
					}
				}
			}
		}
		// set values in result object
		res.area = totarea;
		lastforces = res;

		return res;
	}

	public AeroForces getLastForces()
	{
		return lastforces;
	}

	public float getLastDragCoeff()
	{
		return (lastforces != null) ? lastforces.BC : 0;
	}

	float getOccludedZArea(Module m, int dir, float area, long time)
	{
		float total = area;
		m = m.getLink(dir);
		if (m == null)
			return total;
		total = Math.min(total, area-m.getFrontalArea(time));
		if (total < 0)
			total = 0;
		return total;

		/*
		float total = area;
		// todo: this is not right, needs to use the structure-relative
		// direction, not the module-relative direction
		Set visited = new HashSet();
		while (m.canConnect(dir) && !visited.contains(m))
		{
			visited.add(m);
			m = m.getLink(dir);
			if (m == null)
				break;
			total = Math.min(total, area-m.getFrontalArea(time));
			if (total < 0)
				return 0;
		}
		return total;
		*/
	}

	/**
	  * Returns the maximum drag coefficient this vehicle
	  * can generate at a given mach.
	  */
	public float getMaxDragCoeff(float mach)
	{
		Iterator it;
		it = getModules().iterator();
		float BCO = 0.0f; // oblique BC
		float BCA = 0.0f; // aft BC
		float BCZ = 0.0f; // fore BC
		long time = game.time();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			float zarea = m.getFrontalArea(time);
			float oarea = m.getObliqueArea(time);
			// do foreward
			float zposarea = getOccludedZArea(m, Module.UP, zarea, time);
			if (zposarea > 0)
			{
				BCZ += m.getForeSurface().drag_mach_curve.f(mach)*zposarea;
			}
			// oblique direction (anywhere along x,y axis)
			BCA += m.getObliqueSurface().drag_mach_curve.f(mach)*oarea;
			// now do rear
			zposarea = getOccludedZArea(m, Module.DOWN, zarea, time);
			if (zposarea > 0)
			{
				BCO += m.getAftSurface().drag_mach_curve.f(mach)*zposarea;
			}
		}
		// return the greater of the 3 coefficients
		return Math.max(BCZ,Math.max(BCO,BCA));
	}

	// CONTACT POINTS

	// NOTE: when these change, update trajectory -- just as if CM had changed

	// returns ContactPoint records, in structure-coordinates
	private void computeContactPoints(int numCustomContactPoints)
	{
		ContactPoint[] list = new ContactPoint[8 + numCustomContactPoints];
		int i;
		// first set up the 8 default contact points, arranged
		// in a cube around the structure -- they are all very springy
		// and will hurt if you sit on them
		for (i=0; i<8; i++)
		{
			ContactPoint cp = new ContactPoint();
			Vector3f v = new Vector3f(
				((i&1)==0) ? lextents.x : hextents.x,
				((i&4)==0) ? lextents.y : hextents.y,
				((i&2)==0) ? lextents.z : hextents.z);
			v.scale(1.0f/1000);
			cp.extpos = v;
			// cp.compdir = ??
			// spring & damping depends on total structure mass
			cp.Kspring = getMass()*COLLISION_SPRING_FACTOR;
			cp.Kdamping = cp.Kspring*COLLISION_DAMPING_FACTOR;
			cp.Kstatic = cp.Krolling = COLLISION_KSTATIC;
			cp.maxCompress = 99999f;
			list[i] = cp;
		}
		// enumeration thru modules,
		Iterator it = getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			ContactPoint[] mcpts = m.getContactPoints();
			if (mcpts != null)
			{
				for (int j=0; j<mcpts.length; j++)
				{
					ContactPoint cp = new ContactPoint(mcpts[j]);
					// transform to structure coords
					m.getOrientation().transform(cp.extpos);
					cp.extpos.add(m.getOffset());
					if (cp.compdir != null) {
						m.getOrientation().transform(cp.compdir);
					}
					// add to main list
					list[i++] = cp;
				}
			}
		}
		contactPoints = list;
	}

	public ContactPoint[] getContactPoints()
	{
		return contactPoints;
	}

	// HEAT ROUTINES

	/**
	  * Surface area is approximated by a cube (todo)
	  */
	public float getSurfaceArea()
	{
		return (hextents.x-lextents.x)*(hextents.y-lextents.y)*2 +
			(hextents.x-lextents.x)*(hextents.z-lextents.z)*2 +
			(hextents.y-lextents.y)*(hextents.z-lextents.z)*2;
	}

	/**
	  * Get the volume of this structure -- max case
	  */
	public float getSurfaceVolume()
	{
		return (hextents.x-lextents.x)*(hextents.y-lextents.y)*(hextents.z-lextents.z);
	}

	/**
	  * Add a specific # of joules to the body
	  */
	public void addHeat(double heat, double tsurround)
	{
		if (!DO_HEATING)
			return;
		if (lastheat == INVALID_TICK)
			lastheat = game.time();
		else if (game.time() > lastheat+TICKS_PER_SEC)
		{
			double dt = (game.time()-lastheat)*(1d/TICKS_PER_SEC);
			// do blackbody radiation (todo)
			// q = emiss*area*SB*(T-Te)
			double emiss = 0.8;
			double dtemp = (tsurround-temp);
			double q = emiss*getSurfaceArea()*SB_CONST*dtemp*dtemp*dtemp*dtemp*AstroUtil.sign(dtemp);
			heat += q*dt;
//System.out.println("Let off " + (q*dt) + " J");
			lastheat = game.time();
		}
		//System.out.println("heat="+heat+", spec="+(specheat*getMass()));
		temp += (float)(heat/(specheat*getMass()));
	}

	public void addHeat(double heat)
	{
		addHeat(heat, Constants.COBE_T0);
	}

	public float getTemperature() // K
	{
		return temp;
	}

	public float getWallEnthalpy() // J/kg
	{
		return temp*specheat;
	}

	public float getWallMass()
	{
		return getEmptyMass()/100; // todo
	}

	//

	/**
	  * Get total delta-v for all modules.
	  * We do this by iterating over the modules in descending
	  * order of mass, and computing the dv for each module.
	  * If a module has a stage # of zero, it is considered a
	  * "booster" and it gets lumped in with the first (heaviest)
	  * stage.
	  */
	public double getTotalDeltaV()
	{
		// build sorted list of modules
		List modlist = getModules();
		SortedSet entryset = new TreeSet();
		Iterator it = modlist.iterator();

		// compute total empty mass, full mass
		double emptymass=0;
		double totalmass=0;
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			entryset.add(new ModEntry(m, m.getEmptyMass(), m.getStageNum()));
			emptymass += m.getEmptyMass();
			totalmass += m.getMass();
		}

		// iterator over sorted list
		Set modset = new HashSet();
		double totdv=0;
		it = entryset.iterator();
		while (it.hasNext())
		{
			ModEntry me = (ModEntry)it.next();
			double mmass = me.m.getMass();
			double m1=0;
			double ev=0;
			double totrate=0;

			if (debug)
				System.out.println("\nChecking " + me.m + ", mass=" + mmass);

			// find all propulsion capabilities that use this module for fuel
			// modsup = total resources for current module
			ResourceSet modsup = new ResourceSet(me.m.getSupply());
			Iterator capit = getCapabilitiesOfClass(PropulsionCapability.class).iterator();
			while (capit.hasNext())
			{
				PropulsionCapability pcap = (PropulsionCapability)capit.next();

				// if the module has not been ejected, and this propulsion capability
				// depends on the module, compute how much dv it contributes
				// todo: set iterates in random order!
				if (!modset.contains(pcap.getModule()) && pcap.dependsOnModule(me.m))
				{
					ResourceSet rate = new ResourceSet(pcap.getAverageReactRate());
					totrate += rate.mass();
					ev += pcap.getExhaustVel()*rate.mass(); // todo??

					if (debug)
						System.out.println(pcap + ", rate=" + rate + ", totrate=" + totrate);

					// find out for how long it burns
					float minburntime = Float.POSITIVE_INFINITY;
					Iterator resit = rate.getResources();
					while (resit.hasNext())
					{
						Resource res = (Resource)resit.next();
						float burntime = modsup.getAmountOf(res)/rate.getAmountOf(res);
						if (!Float.isNaN(burntime) && burntime>=0)
						{
							minburntime = Math.min(burntime, minburntime);
						}
					}

					// compute amt of resources consumed based on burntime
					if (minburntime > 0 && !Float.isInfinite(minburntime))
					{
						rate.scale(minburntime);
						m1 += rate.mass();
						modsup.sub(rate);
						if (debug) {
							System.out.println("minburntime=" + minburntime + ", modsup=" + modsup);
							System.out.println(me.m + " burns for " + AstroUtil.toDuration(minburntime) + ", " + rate);
						}
					}
				}
			}
			double dv = ev*Math.log(totalmass/(totalmass-m1))/totrate;
			totdv += dv;
			totalmass -= me.m.getMass();

			if (debug)
				System.out.println("dv=" + dv + ", totdv=" + totdv + ", totmass=" + totalmass);
			modset.add(me.m); // add to modset, b/c it is now gone
		}

		return totdv;
	}

	class ModEntry implements Comparable
	{
		Module m;
		float mass;
		int stage;
		ModEntry(Module m, float mass, int stage)
		{
			this.m=m;
			this.mass=mass;
			this.stage=stage;
		}
		public int compareTo(Object o)
		{
			ModEntry a = (ModEntry)o;
			if (a.stage > stage) return -1;
			if (a.stage < stage) return 1;
			if (a.mass > mass) return -1;
			if (a.mass < mass) return 1;
			return ModEntry.this.hashCode() - a.hashCode();
		}
	}

	//

	public void shutdown()
	{
		Iterator it = getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			m.deactivate();
		}
	}

	//

	static boolean debug = false;
	static boolean debug2 = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Structure.class);

	static {
		prophelp.registerGet("mass", "getMass");
		prophelp.registerGet("emptymass", "getEmptyMass");
		prophelp.registerGet("miscmass", "getMiscMass");
		prophelp.registerGet("owner", "getOwner");
		prophelp.registerGet("modulecount", "getModuleCount");
		prophelp.registerGet("radius", "getRadius");
		prophelp.registerGet("supply", "getSupply");
		prophelp.registerGet("thing", "getThing");
		prophelp.registerGet("ship", "getThing");
		prophelp.registerGet("temperature", "getTemperature");
		prophelp.registerGet("totaldeltav", "getTotalDeltaV");
		prophelp.registerGet("massflowrate", "getMassFlowRate");
		prophelp.registerGet("thrust", "getCurrentThrust");
		prophelp.registerGet("thrustvec", "getThrustVector");
	}

	public Object getProp(String key)
	{
		if (key.startsWith("#"))
			return getModule(PropertyUtil.toInt(key.substring(1)));
		else if (key.startsWith("$"))
			return getModuleByName(key.substring(1));
		else
			return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	// SETTINGS

	static float COLLISION_SPRING_FACTOR;
	static float COLLISION_DAMPING_FACTOR;
	static float COLLISION_KSTATIC;

	static boolean DO_HEATING;

	static SettingsGroup settings = new SettingsGroup(Structure.class, "Ship")
	{
		public void updateSettings()
		{
			DO_HEATING = getBoolean("DoHeating", true);
			COLLISION_SPRING_FACTOR = getFloat("DefaultSpringFactor", 10.0f);
			COLLISION_DAMPING_FACTOR = getFloat("DefaultDampingFactor", 0.10f);
			COLLISION_KSTATIC = getFloat("DefaultKStatic", 0.25f);
		}
	};

}

