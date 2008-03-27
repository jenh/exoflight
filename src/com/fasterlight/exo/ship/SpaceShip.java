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

import com.fasterlight.exo.game.AlertEvent;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.*;
import com.fasterlight.exo.seq.*;
import com.fasterlight.exo.ship.sys.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * A space ship.
  */
public class SpaceShip
extends StructureThing
{
//	protected PropulsionController propctrl;
	protected AttitudeController attctrl;
	protected Sequencer sequencer;
	protected GuidanceCapability guidance_cap;
	protected ShipAttitudeSystem attsys;
	protected ShipTargetingSystem targsys;
	protected ShipLaunchSystem launchsys;
	protected ShipManeuverSystem maneuvsys;
	protected ShipWarningSystem warnsys;
	protected ShipReentrySystem reentrysys;
	protected ShipLandingSystem landsys;
	protected int flags;

	static final int EXPLODED = 1;
	static final int EXPENDABLE = 2;

	//

	public SpaceShip(Structure struct)
	{
		super(struct);
	}

	public SpaceShip(Game game, Planet planet, Vector3d llr, Structure struct)
	{
		super(game, planet, llr, struct);
	}

	protected Telemetry makeTelemetry()
	{
		return new SpaceShipTelemetry(this);
	}

	public AttitudeController getAttitudeController()
	{
		if (attctrl == null)
			attctrl = new AttitudeController(game, this);
		return attctrl;
	}

	public ShipAttitudeSystem getShipAttitudeSystem()
	{
		if (attsys == null)
			attsys = new ShipAttitudeSystem(this);
		return attsys;
	}

	public ShipTargetingSystem getShipTargetingSystem()
	{
		if (targsys == null)
			targsys = new ShipTargetingSystem(this);
		return targsys;
	}

	public ShipLaunchSystem getShipLaunchSystem()
	{
		if (launchsys == null)
			launchsys = new ShipLaunchSystem(this);
		return launchsys;
	}

	public ShipManeuverSystem getShipManeuverSystem()
	{
		if (maneuvsys == null)
			maneuvsys = new ShipManeuverSystem(this);
		return maneuvsys;
	}

	public ShipWarningSystem getShipWarningSystem()
	{
		if (warnsys == null)
			warnsys = new ShipWarningSystem(this);
		return warnsys;
	}

	public ShipReentrySystem getShipReentrySystem()
	{
		if (reentrysys == null)
			reentrysys = new ShipReentrySystem(this);
		return reentrysys;
	}

	public ShipLandingSystem getShipLandingSystem()
	{
		if (landsys == null)
			landsys = new ShipLandingSystem(this);
		return landsys;
	}

	public GuidanceCapability getGuidanceCapability()
	{
		if (guidance_cap != null && guidance_cap.getStructure() != getStructure())
			guidance_cap = null;
		if (guidance_cap == null)
		{
			guidance_cap = (GuidanceCapability)getStructure().getCapabilityOfClass(GuidanceCapability.class);
		}
		return guidance_cap;
	}

	public void setGuidanceCapability(GuidanceCapability cap)
	{
		if (cap != this.guidance_cap)
		{
			if (guidance_cap != null)
				guidance_cap.deactivate();
			this.guidance_cap = cap;
		}
	}

	// SEQUENCER, PROGRAM STUFF

	public void setSequencer(Sequencer sequencer)
	{
		if (sequencer != this.sequencer) //avoid recursion
		{
			if (this.sequencer != null)
				this.sequencer.stop();
			this.sequencer = sequencer;
			if (sequencer != null)
				sequencer.setShip(this);
		}
	}

	public Sequencer getSequencer()
	{
		return sequencer;
	}

	public void startProgram(String progid)
	{
		Sequencer newseq = loadProgram(progid);
		if (newseq != null)
		{
			setSequencer(newseq);
			newseq.start();
		}
	}

	public void startProgramVerbatim(String progname)
	{
		Sequencer newseq = loadProgramVerbatim(progname);
		if (newseq != null)
		{
			setSequencer(newseq);
			newseq.start();
		}
	}

	// todo: error when no program exist
	public Sequencer loadProgram(String progid)
	{
		String progname = null;
		float bestmass = 0;

		// resolve the program name from the ID
		// of the module with the greatest mass
		Iterator it = getStructure().getModules().iterator();
		while (it.hasNext())
		{
			Module m = (Module)it.next();
			String tmp = m.getProgramForID(progid);
			if (tmp != null && (progname==null || m.getEmptyMass()>bestmass))
			{
				progname = tmp;
				bestmass = m.getEmptyMass();
			}
		}

		return loadProgramVerbatim(progname);
	}

	public Sequencer loadProgramVerbatim(String progname)
	{
		String path = "programs/" + progname + ".seq";
		return SequencerParser.loadSequence(game, path);
	}


	//

	/**
	  * Get acceleration for all engines operating
	  * at current throttle setting
	  */
	public double getTotalAccel()
	{
		double total = 0;
		Iterator it = structure.getAllCaps();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			if (cap instanceof PropulsionCapability)
			{
				total += ((PropulsionCapability)cap).getCurrentForce();
			}
		}
		total /= getMass();
		return total;
	}

	/**
	  * Get accel for all engines operating
	  */
	public double getMaxAccel()
	{
		return getMaxThrust()/getMass();
	}

	/**
	  * Get thrust for all engines operating
	  */
	public double getMaxThrust()
	{
		double total = 0;
		Iterator it = structure.getAllCaps();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			if (cap instanceof PropulsionCapability)
			{
				total += ((PropulsionCapability)cap).getNominalForce();
			}
		}
		return total;
	}

	/**
	  * Returns the time needed to velocity change of 'dv' km/s,
	  * given a set of engines and an initial mass
	  */
	public double getTimeForDv(double dv, PropulsionCapability cap)
	{
		// dv = c*ln(m0/m1)
		// m1 = m0*exp(-dv/c)
		// c = exhaustv
		double m0 = this.getMass();
		double c = cap.getExhaustVel();
		double m1 = m0*Math.exp(-dv/c);
		return (m0-m1)/cap.exhmass1;
	}

	static Random rnd = new Random();

	public void refreshTrajectory()
	{
		((MutableTrajectory)getTrajectory()).refresh();
	}

	/**
	  * Attach two ships together
	  * srcmod - the module on the ship being attached (ship)
	  * destmod - this module on the ship attached to (this)
	  * todo: make it straight
	  */
	public void attach(SpaceShip ship,
		Module srcmod, Vector3f srcofs, int srcdir,
		Module destmod, Vector3f destofs, int destdir)
	{
		Game game = getUniverse().getGame();
		Vector3d shipofs = this.getPosition(ship, game.time());
		shipofs.scale(-1000);
		Orientation shiport = ship.getOrientation(game.time());
		Orientation thisort = this.getOrientation(game.time());
		Structure shipstruct = ship.getStructure();
		Structure struct = this.getStructure();
		ship.refreshTrajectory();

		// record positions of all modules in 'ship'
		// because the CM changes as we remove & add modules
		int modcount = shipstruct.getModuleCount();
		Vector3f[] mposns = new Vector3f[modcount];
		for (int i=0; i<modcount; i++)
		{
			Module m = shipstruct.getModule(i);
			mposns[i] = m.getOffset();
		}

		Vector3f origcm = struct.getCenterOfMass();
		for (int i=modcount-1; i>=0; i--)
		{
			Module m = shipstruct.getModule(i);

			// get the offset, in 'this' structure coordinates
			// of the module
			Vector3d ofs = new Vector3d(mposns[i]); // cm-relative struct coords
			shiport.transform(ofs); // transform to world coord frame
			ofs.add(shipofs); // add position from ship --> this
//			ofs.scale(-1); // convert from ship-centric to this-centric
			thisort.invTransform(ofs); // transform to local frame

			// transform orientation to 'this' local frame
			Orientation ort = new Orientation(m.getOrientation());
			ort.mul(shiport);
			ort.concatInverse(thisort);

			// add module to ship, adjusting for CM bias
			Vector3f pos = new Vector3f(ofs);
			pos.add(origcm);

			shipstruct.removeModule(m);
			struct.addModule(m, ort, pos);
			if (debug) {
				System.out.println("Added " + m + " at " + pos + ", dir=" + ort.getDirection());
			}
		}

		// since we used addModule(3) we have to call this
		struct.adjustCenterOfMass();

		// alert event
		AlertEvent.postAlert(game, "Docked " + ship + " to " + this, "DOCKING-ATTACH");
	}

/*** ATTEMPT #1

		Structure struct2 = ship.getStructure();
		int num2 = struct2.getModuleCount();

		// record links to other modules in attaching ship

		Module[][] linkarr = new Module[struct2.getModuleCount()][];

		for (int i=0; i<num2; i++)
		{
			Module m = struct2.getModule(i);
			Module[] links = new Module[Module.NUM_DIRS];
			for (int j=0; j<Module.NUM_DIRS; j++)
			{
				if (m.canConnect(j))
					links[i] = m.getLink(j);
			}
			linkarr[i] = links;
		}

		// keep set of modules that are in the dest ship

		while (struct2.getModuleCount() > 0)
		{
			Module m = struct2.getModule(0);
			struct2.removeModule(m);
			if (m == srcmod)
			{
				getStructure().addModule(m, destmod, srcdir, destdir,
					Module.UP, Module.NORTH); // todo
			} else {
				// todo: find module in ship that connects to
				getStructure().addModule(m);
			}
		}
*/

/*** ATTEMPT #2

		Vector3f srcbias = new Vector3f();
		Vector3f destbias = new Vector3f();
		srcbias.sub(srcofs);
		destbias.add(destofs);

		Orientation srcort = new Orientation(new Vector3d(0,0,-1)); //todo

		while (struct2.getModuleCount() > 0)
		{
			Module m = struct2.getModule(0);

			Vector3f v = new Vector3f(m.getOffset());
			v.add(srcbias);
			srcort.transform(v);
			v.add(destbias);

			struct2.removeModule(m);
			getStructure().addModule(m);
		}
*/

	/**
	  * Detach a single module, and create a new spaceship.
	  * The spaceship's name is the same as the module's name.
	  */
	public SpaceShip detachSingle(Module m)
	{
		Module[] marr = { m };
		SpaceShip ss = detach(marr);
		ss.setName(m.getName());
if (debug) {
	System.out.println("DETACH: " + ss);
	System.out.println("lext=" + ss.getStructure().getLoExtents());
	System.out.println("hext=" + ss.getStructure().getHiExtents());
	System.out.println("  cm=" + ss.getStructure().getCenterOfMass());
}
		return ss;
	}

	/**
	  * Detach a single module, and create a new spaceship.
	  * The spaceship's name is the same as the module's name.
	  */
	public SpaceShip detach(Module m)
	{
		return smartDetach(m);
	}

	public SpaceShip jettison(Module m)
	{
		SpaceShip ship = smartDetach(m);
		ship.setExpendable(true);
		return ship;
	}

	/**
	  * The algorithm for "smart detach":
	  *
	  * 1. Visit each link of module 'm', starting with the last.
	  * 2. Make a set consisting of 'm' and all the modules connecting
	  *    to that direction.
	  * 3. If that set has the same number of crew member as in 'm',
	  *    then detach that set (or its inverse, if there is crew in it).
	  */
	public SpaceShip smartDetach(Module m)
	{
		int nmods = getStructure().getModuleCount();

		for (int i=Module.NUM_DIRS-1; i>=0; i--)
		{
			if (m.getLink(i) != null)
			{
				Set set = new HashSet();
				set.add(m);
				// add modules in every direction EXCEPT i
				for (int j=0; j<Module.NUM_DIRS; j++)
					if (i!=j)
						m.getLink(i).addAllConnected(set);
				// if it added all of the modules, we can't use this val of i
				if (set.size() < nmods)
				{
					// now see if there's any crew in this
					int numcrew = countCrew(set);
					Set invset = getInverseModuleSet(set);
					if (numcrew == m.getCrewCount())
					{
						if (numcrew > 0) {
							return detach(invset);
						} else {
							return detach(set);
						}
					}
				}
			}
		}

		// we must be connected at only 1 point, so detach this
		if (m.getCrewCount() > 0)
			return detachAllBut(m);
		else
			return detachSingle(m);
	}

	private Set getInverseModuleSet(Set a)
	{
		Set b = new HashSet(getStructure().getModules());
		b.removeAll(a);
		return b;
	}

	// counts the # of crew present in a set of modules
	private int countCrew(Set mset)
	{
		int total=0;
		Iterator it = mset.iterator();
		while (it.hasNext())
		{
			total += ((Module)it.next()).getCrewCount();
		}
		return total;
	}

	/**
	  * Detach multiple modules.
	  * The module list is given by a string of pipe-separated
	  * module names.
	  */
	public SpaceShip detachMultiple(String mstr)
	{
		boolean inverse=false;
		if (mstr.startsWith("~"))
		{
			mstr = mstr.substring(1);
			inverse = true;
		}

		Set set = new HashSet();
		StringTokenizer st = new StringTokenizer(mstr, "|");
		while (st.hasMoreTokens())
		{
			Module m = getStructure().getModuleByName(st.nextToken());
			if (m != null)
				set.add(m);
		}
		SpaceShip ss = inverse ? detachAllBut(set) : detach(set);
		return ss;
	}

	public SpaceShip jettisonMultiple(String mstr)
	{
		SpaceShip ship = detachMultiple(mstr);
		ship.setExpendable(true);
		return ship;
	}

	public SpaceShip detach(Set modules)
	{
		Module[] mods = new Module[modules.size()];
		int i=0;
		Iterator it = modules.iterator();
		while (it.hasNext())
		{
			mods[i++] = (Module)it.next();
		}
		return detach(mods);
	}

	public SpaceShip detachAllBut(Set modules)
	{
		return detach(getInverseModuleSet(modules));
	}

	public SpaceShip detachAllBut(Module m)
	{
		Set set = new HashSet();
		set.add(m);
		return detach(getInverseModuleSet(set));
	}

   /**
     * Detach the modules in 'modules' and create a SpaceShip object
     * out of them, and return it.
     * Its name is the name of the former spaceship the modules belonged
     * to, plus a "-2" moniker.
     * todo: remove dependencies, on ship.guidancecap and other things
     * todo: reorient things when detached
     * todo: more than 1 detached doesn't have right offs
     */
	public SpaceShip detach(Module[] modules)
	{
		if (modules == null || modules.length == 0)
		{
			throw new PropertyRejectedException("Hey... no modules to detach!");
		}
		if (!(getTrajectory() instanceof MutableTrajectory))
		{
			throw new PropertyRejectedException("Ship does not have mutable trajectory");
		}

		refreshTrajectory();

		long time = game.time();
		double m1 = structure.getMass();
		double m2 = 0;
		for (int i=0; i<modules.length; i++)
		{
			m2 += modules[i].getMass();
		}
		m1 -= m2;

		MutableTrajectory origtraj = (MutableTrajectory)this.getTrajectory();
		Orientation ort = origtraj.getOrt(time);

		if (modules.length >= structure.getModuleCount())
		{
			throw new PropertyRejectedException("ummm.. can't detach " + modules.length + " modules when only " +
				structure.getModuleCount() + " modules...");
		}

		// get old center of mass
		Vector3f oldcm = structure.getCenterOfMass();

		// find center of mass of modules that are detached using
		// coordinates of the old structure
		Vector3d modofs = new Vector3d();
		for (int i=0; i<modules.length; i++)
		{
			Module m = modules[i];
			modofs.scaleAdd(m.getMass(), new Vector3d(m.getOffset()), modofs);
		}
		modofs.scale(0.001/m2); // scale from m to km

		// make a new sturcture and add the modules to it
		Structure newstruct = new Structure(game);
		newstruct.setOwner(this.getStructure().getOwner());
		for (int i=0; i<modules.length; i++)
		{
			Module m = modules[i];
			structure.removeModule(m);
			newstruct.addModule2(m);
		}
		newstruct.adjustCenterOfMass();

		// compute delta of center-of-mass (oldcm-newcm)
		oldcm.sub(structure.getCenterOfMass());
		oldcm.scale(0.001f); // m to km
		Vector3d oldcm2 = new Vector3d(oldcm);
		ort.transform(oldcm2);
		ort.transform(modofs); // transform by orientation

		UniverseThing parent = origtraj.getParent();
		MutableTrajectory traj2 = new CowellTrajectory();

		// now add delta-v to move them away from each other
		Vector3d dir = new Vector3d(modofs);
		dir.sub(oldcm2);
		dir.normalize();
		if (dir.dot(modofs) >= 0)
			dir.scale(-1);
		Vector3d pos = origtraj.getPos(time);
		Vector3d vel = origtraj.getVel(time);

		double sepvel = 0.0005; // .5 m/s separation difference (todo: const, maybe per-module)

		Vector3d d1 = new Vector3d(dir);
		d1.scale(m2*sepvel/(m1+m2));
		d1.add(vel);
		Vector3d p1 = new Vector3d(pos);
		p1.sub(oldcm2);

		CowellTrajectory traj1 = new CowellTrajectory();
		origtraj.addUserPerturbations(traj1);
		traj1.set(parent, p1, d1, time, ort);
		this.setTrajectory(traj1);

		Vector3d d2 = new Vector3d(dir);
		d2.scale(-m1*sepvel/(m1+m2));
		d2.add(vel);
		Vector3d p2 = new Vector3d(pos);
		p2.add(modofs);
		traj2.set(parent, p2, d2, time, ort);

		if (debug) {
			System.out.println("m1=" + m1 + "\tm2=" + m2);
			System.out.println("p1=" + p1 + "\tp2=" + p2);
		}
		Vector3d pd = new Vector3d(p1);
		pd.sub(p2);
		if (debug)
			System.out.println("p1-p2=" + pd + "\t|p1-p2|=" + pd.length());

		SpaceShip ss = new SpaceShip(newstruct);
		ss.setTrajectory(traj2);
		ss.setName(getName() + "-2");
		ss.flags = this.flags;

		if (debug)
			System.out.println("traj2=" + traj2);

		// alert event
		String code = "DOCKING-SEPARATION";
		for (int i=0; i<modules.length; i++)
		{
			AlertEvent.postAlert(game, "Detached " + modules[i], code);
			code = null; // only 1 code needed
		}

		return ss;
	}

	//

	// called from CowellTrajectory when the force on the
	// object changes
	public void notifyForce(Vector3d a)
	{
		List mods = structure.getModules();
		for (int k=0; k<2; k++)
		{
			Iterator it = mods.iterator();
			while (it.hasNext())
			{
				Module m = (Module)it.next();
				if ( (m.getCrewCount()==0) ^ (k==0) )
					m.notifyForce(a);
			}
		}
	}

	public void explode()
	{
		if (!isExploded())
		{
			setExpendable(true);
			// shut down all capabilities
			getStructure().shutdown();
			// stop sequencer!
			if (getSequencer() != null)
				getSequencer().stop();
			// empty all capabilities
			ResourceSet empty = new ResourceSet();
			Iterator it = getStructure().getAllCaps();
			while (it.hasNext())
			{
				Capability cap = (Capability)it.next();
				cap.setSupply(empty);
			}
			flags |= (EXPLODED | EXPENDABLE);
		}
	}

	public boolean isExploded()
	{
		return (flags & EXPLODED) != 0;
	}

	public boolean isExpendable()
	{
		// todo: when expendable, less accurate traj.
		return (flags & EXPENDABLE) != 0;
	}

	public void setExpendable(boolean b)
	{
		if (b)
			flags |= EXPENDABLE;
		else
			flags &= ~EXPENDABLE;
	}

	public float getVisibleRadius()
	{
		float vr = super.getVisibleRadius();
		if (isExploded())
			vr *= 10; //todo: const
		return vr;
	}

	public void updateHeat()
	{
		// todo: solar heating
		structure.addHeat(0, Constants.COBE_T0);
	}

	public void setPositionInOrbit(UniverseThing body, KeplerianElements elements, long time)
	{
		CowellTrajectory traj = new CowellTrajectory();
		traj.setParent(body);
		elements.setEpoch( AstroUtil.tick2dbl(time) );
		traj.setGeocentricOrbitElements(elements);
		Trajectory curtraj = getTrajectory();
		if (curtraj instanceof MutableTrajectory)
			traj.addUserPerturbations((MutableTrajectory)curtraj);
		setTrajectory(traj);
	}

	public void setPositionOnGround(UniverseThing body, Vector3d llr, long time)
	{
		llr = new Vec3d(llr);
		LandedTrajectory traj = new LandedTrajectory(body, llr, time);
		Trajectory curtraj = getTrajectory();
		if (curtraj instanceof MutableTrajectory)
			traj.addUserPerturbations((MutableTrajectory)curtraj);
		setTrajectory(traj);
	}

	public void setPositionOnGroundWithVel(Planet planet, Vector3d llr, long time,
		Vector3d aed)
	{
		setPositionOnGround(planet, llr, time);

		// now add the velocity
		Vector3d vel = planet.aed2sez(aed);
		planet.rotateVecByLL(vel, llr, AstroUtil.tick2dbl(time));
		// free to cowell (we know it's a landed)
		LandedTrajectory traj = (LandedTrajectory)getTrajectory();
		traj.free(traj.getPos(time), vel);
	}


	static boolean debug = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SpaceShip.class);

	static {
		prophelp.registerGet("attctrl", "getAttitudeController");
		prophelp.registerGet("attsys", "getShipAttitudeSystem");
		prophelp.registerGet("targsys", "getShipTargetingSystem");
		prophelp.registerGet("launchsys", "getShipLaunchSystem");
		prophelp.registerGet("maneuvsys", "getShipManeuverSystem");
		prophelp.registerGet("warnsys", "getShipWarningSystem");
		prophelp.registerGet("reentrysys", "getShipReentrySystem");
		prophelp.registerGet("landingsys", "getShipLandingSystem");
		prophelp.registerGetSet("sequencer", "Sequencer", Sequencer.class);
		prophelp.registerGet("guidancecap", "getGuidanceCapability");
		prophelp.registerSet("detach", "detach", Module.class);
		prophelp.registerSet("detachmult", "detachMultiple", String.class);
		prophelp.registerSet("jettison", "jettison", Module.class);
		prophelp.registerSet("jettisonmult", "jettisonMultiple", String.class);
		prophelp.registerSet("expendable", "setExpendable", boolean.class);
		prophelp.registerGet("expendable", "isExpendable");
		prophelp.registerGet("exploded", "isExploded");
		prophelp.registerGet("totalaccel", "getTotalAccel");
		prophelp.registerGet("maxaccel", "getMaxAccel");
	}

	public Object getProp(String key)
	{
		if (key.startsWith("detachmult$"))
		{
			return detachMultiple(key.substring(11));
		}
		else if (key.startsWith("jettisonmult$"))
		{
			return jettisonMultiple(key.substring(11));
		}
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
