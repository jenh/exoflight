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

import java.util.*;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.Vector3d;

/**
  * The base class of several Trajectory objects.
  * todo: better semantics for set() methods
  */
public abstract class DefaultMutableTrajectory
	implements MutableTrajectory, Constants, Cloneable, PropertyAware
{
	private Game game;

	UniverseThing ref, thing;
	double refinfrad;

	private Set default_perturbs = new HashSet();
	private Set user_perturbs = new HashSet();
	protected Perturbation dragperturb;
	private boolean activated;

	protected Vector3d r0 = new Vec3d();
	protected Vector3d v0 = new Vec3d();
	protected long t0, angt0;
	protected Orientation ort0 = new Orientation();
	protected Vector3d angvel = new Vec3d();

	public static int PF_DRAG = 1;
	public static int PF_3RDBODY = 2;
	public static int PF_J2 = 4;

	public static int PF_ORBITABLE = 0; // don't use DynamicOrbitTrajectory

	public static int perturbFlags;
	//

	/**
	  * The # of perturbations has changed, so see if we need
	  * to change our trajectory or whatever.
	  */
	public abstract boolean checkPerturbs();

	/**
	  * Returns a string that designates the type of trajectory.
	  */
	public abstract String getType();

	/**
	  * Default Orientation handler.
	  */
	public Orientation getOrt(long time)
	{
		Orientation o = new Orientation(ort0);
		double scale = 1d / (Math.PI * 2);
		o.mul(angvel, (time - angt0) * (scale / TICKS_PER_SEC)); //todo: verify
		return o;
	}

	//todo: what if game == null?

	public void setOrientation(Orientation ort)
	{
		ort0.set(ort);
		ort0.normalize();
		angt0 = getGame().time();
	}

	// todo: very large values of time cause pain?
	public Orientation getOrientation()
	{
		return getOrt(getGame().time());
	}

	public Orientation getPlanetRefOrientation()
	{
		Orientation ort = getOrientation();
		if (ref instanceof Planet)
		{
			Vector3d pos = getPos(getGame().time());
			ort.concatInverse(((Planet) ref).getOrientation(pos));
		}
		return ort;
	}

	public void setPlanetRefOrientation(Orientation newort)
	{
		Orientation ort = new Orientation(newort);
		if (ref instanceof Planet)
		{
			Vector3d pos = getPos(getGame().time());
			ort.mul(((Planet) ref).getOrientation(pos), ort);
		}
		setOrientation(ort);
		//System.out.println("set planet ref = " + newort);
		//System.out.println("get planet ref = " + getPlanetRefOrientation());
	}

	public Orientation getVelRefOrientation()
	{
		Orientation ort = getOrientation();
		Vector3d pos = getPos(getGame().time());
		Vector3d vel = getVel(getGame().time());
		ort.concatInverse(new Orientation(vel, pos));
		return ort;
	}

	public void setVelRefOrientation(Orientation newort)
	{
		Orientation ort = new Orientation(newort);
		Vector3d pos = getPos(getGame().time());
		Vector3d vel = getVel(getGame().time());
		ort.concat(new Orientation(vel, pos));
		setOrientation(ort);
		//System.out.println("set vel ref = " + newort);
		//System.out.println("cur ort     = " + getOrientation());
		//System.out.println("get vel ref = " + getVelRefOrientation());
	}

	/**
	  * Sets the angular velocity.
	  */
	public void setAngularVelocity(Vector3d angvel)
	{
		if (isActive())
		{
			refresh();
			// todo: resolve angt0 when cloning trajectories
			// todo: reactivate?
		}
		this.angvel.set(angvel);
	}

	protected void fixCurrentOrt()
	{
		setOrientation(getOrientation());
	}

	public Vector3d getAngularVelocity()
	{
		return new Vec3d(angvel);
	}

	/**
	  * Set the initial conditions for this trajectory.
	  */
	public void set(UniverseThing ref, Vector3d r0, Vector3d v0, long t0, Orientation ort)
	{
		this.t0 = this.angt0 = t0;
		if (r0 != null)
			this.r0.set(r0);
		if (v0 != null)
			this.v0.set(v0);
		if (ort != null)
		{
			this.ort0.set(ort);
			this.ort0.normalize();
		}
		// set the parent (ref)
		setParent(ref);
		reactivate();
	}
	public void set(
		UniverseThing ref,
		Vector3d r0,
		Vector3d v0,
		long t0,
		Orientation ort,
		Vector3d angvel)
	{
		setAngularVelocity(angvel);
		set(ref, r0, v0, t0, ort);
	}
	/**
	  * Set the reference object (parent)
	  */
	public void setParent(UniverseThing ref)
	{
		assertActive(false);
		if (ref != this.ref)
		{
			this.ref = ref;
			this.refinfrad =
				ref.getInfluenceRadius(ref.getUniverse().getGame().time()) * INFRAD_SLOP;
		}
	}
	/**
	  * Get the reference object (parent)
	  */
	public UniverseThing getParent()
	{
		return ref;
	}
	/**
	  * Returns the UniverseThing that this Trajectory is activated
	  * upon (passed in the activate method)
	  */
	public UniverseThing getThing()
	{
		return thing;
	}

	public Game getGame()
	{
		if (game != null)
			return game;
		if (ref != null)
			return ref.getUniverse().getGame();
		else
			throw new RuntimeException("Need to set 'parent' first!");
	}

	/**
	  * Activate the trajectory for use with a UniverseThing at the
	  * current game time.
	  */
	public void activate(UniverseThing subject)
	{
		if (ref == null)
			throw new RuntimeException(this +" has no reference object!");
		if (activated)
			throw new RuntimeException(this +" already activated");
		this.thing = subject;
		this.game = subject.getUniverse().getGame();
		if (t0 == INVALID_TICK)
			t0 = game.time();
		if (angt0 == INVALID_TICK)
			angt0 = t0;
		addDefaultPerturbs();
		activated = true;
		activateTrajectory();
	}
	/**
	  * Returns true if the trajectory was changed
	  */
	protected boolean activateTrajectory()
	{
		return false;
	}
	/**
	  * End use of this trajectory with a UniverseThing.
	  * Must be called after activate().
	  */
	public void deactivate()
	{
		if (!activated)
			throw new RuntimeException(this +" not activated");
		activated = false;
	}

	protected void reactivate()
	{
		if (!activated)
			return;
		UniverseThing t = thing;
		deactivate();
		activate(t);
	}

	protected boolean isActive()
	{
		return activated;
	}

	protected void assertActive(boolean a)
	{
		if (isActive() != a)
			throw new RuntimeException("Assertion failed: " + this +" active == " + (!a));
	}

	/**
	  * Adds a perturbation. This may cause the trajectory to
	  * be changed if it is activated.
	  */
	public void addPerturbation(Perturbation p)
	{
		refresh();
		user_perturbs.add(p);
		checkPerturbs();
	}
	/**
	  * Removes a perturbation. This may cause the trajectory to
	  * be changed if it is activated.
	  */
	public void removePerturbation(Perturbation p)
	{
		refresh();
		user_perturbs.remove(p);
		checkPerturbs();
	}

	/**
	 * @deprecated
	  */
	public Iterator getPerturbs()
	{
		return getPerturbations();
	}

	public Iterator getPerturbations()
	{
		return new JoinedIterator(default_perturbs.iterator(), user_perturbs.iterator());
	}

	public Iterator getDefaultPerturbations()
	{
		return default_perturbs.iterator();
	}

	public Iterator getUserPerturbations()
	{
		return user_perturbs.iterator();
	}

	public int countPerturbations()
	{
		return countUserPerturbations() + countDefaultPerturbations();
	}

	public int countUserPerturbations()
	{
		return user_perturbs.size();
	}

	public int countDefaultPerturbations()
	{
		return default_perturbs.size();
	}

	/**
	  * Called before we call remove or addPerturbation
	  */
	public void refresh()
	{
		fixCurrentOrt(); //todo: dont need this?
	}

	/**
	  * See if our trajectory can be converted into an OrbitTrajectory.
	  * (if only force is drag and force is below given threshold)
	  * (todo?)
	  */
	protected boolean isOrbitable(boolean goout)
	{
		// if we have a non-drag perturbation, return false
		if ((perturbFlags & ~PF_ORBITABLE) != 0 || countUserPerturbations() > 0)
			return false;
		boolean gotdrag = (dragperturb != null);
		// check force is below threshold
		long t = getGame().time();
		Vector3d r = getPos(t);
		// if we are underneath the planet lo radius, return...
		double loradius;
		if (ref instanceof Planet)
			loradius = ((Planet) ref).getMaxRadius();
		else
			loradius = ref.getRadius();
		if (r.length() <= loradius)
			return false;
		// at this point, if we have no drag, return true -- is orbitable
		if (!gotdrag)
			return true;
		Vector3d v = getVel(t);
		PerturbForce pf = getPerturbForces(r, v, ort0, angvel, t);
		double acc = pf.a.length() + pf.f.length() / thing.getMass();
		if (debug)
			System.out.println(thing + ": acc=" + acc);
		return (acc < (goout ? ORBIT_OUT_THRESHOLD : ORBIT_IN_THRESHOLD));
	}

	PerturbForce getPerturbForces(Vector3d r, Vector3d v, Orientation ort, Vector3d w, long time)
	{
		PerturbForce pf = new PerturbForce();
		// now go thru list of perturbs
		Iterator it = getPerturbations();
		while (it.hasNext())
		{
			Perturbation pert = (Perturbation) it.next();
			pert.addPerturbForce(pf, r, v, ort, w, time);
		}
		return pf;
	}

	protected void addDefaultPerturbs()
	{
		default_perturbs.clear();
		dragperturb = null;
		// add drag perturbation
		if ((perturbFlags & PF_DRAG) != 0)
			addDragPerturbation();
		// add 3rd body perturbations from ref's parents and descendants
		if ((perturbFlags & PF_3RDBODY) != 0)
			add3rdBodyPerturbations();
		// add J2 perturbations
		if ((perturbFlags & PF_J2) != 0 && ref instanceof Planet)
		{
			Planet planet = (Planet) ref;
			if (planet.getJ2() > 0)
			{
				default_perturbs.add(new J2Perturbation(planet, planet.getJ2()));
			}
		}
	}

	void addDragPerturbation()
	{
		if (ref instanceof Planet && ((Planet) ref).getAtmosphere() != null)
		{
			if (debug)
				System.out.println("added drag to " + this);
			dragperturb = thing.getDragPerturbation(this);
			if (dragperturb != null)
				default_perturbs.add(dragperturb);
		}
	}

	void add3rdBodyPerturbations()
	{
		// iterate thru parents
		UniverseThing ut = ref.getParent();
		while (ut instanceof Planet)
		{
			Perturbation pert = new ThirdBodyPerturbation(ref, ut, true);
			default_perturbs.add(pert);
			ut = ut.getParent();
		}
		// now do children
		Iterator it = ref.getChildren();
		while (it.hasNext())
		{
			ut = (UniverseThing) it.next();
			if (ut instanceof Planet)
			{
				Perturbation pert = new ThirdBodyPerturbation(ref, ut);
				default_perturbs.add(pert);
			}
		}
	}

	// todo: how to decide what perturbs to add?
	// what if some depend on the thing?
	public void addUserPerturbations(MutableTrajectory traj)
	{
		if (debug)
			System.out.println("addPerturbs: " + this +" for " + getThing());
		Iterator it = user_perturbs.iterator();
		while (it.hasNext())
		{
			Perturbation pert = (Perturbation) it.next();
			traj.addPerturbation(pert);
		}
		traj.setAngularVelocity(getAngularVelocity());
	}

	/**
	  * Used when smashie-smashie
	  */
	protected void crash()
	{
		assertActive(true);

		long t = getGame().time();
		Vector3d r0 = getPos(t);
		Vector3d v0 = getVel(t);
		Orientation ort = getOrt(t);

		LandedTrajectory traj = new LandedTrajectory();
		traj.set(getParent(), r0, v0, t, ort);
		getThing().setTrajectory(traj);
	}

	/**
	  * Used when entering/escaping influence of another body
	  */
	protected void changeParent(UniverseThing newparent)
	{
		assertActive(true);

		long t = getGame().time();
		this.set(
			newparent,
			thing.getPosition(newparent, t),
			thing.getVelocity(newparent, t),
			t,
			getOrt(t),
			getAngularVelocity());
	}

	// ORBIT FNS
	// todo: state transitions for these

	public Conic getConic()
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		return new Conic(r0, v0, ref.getMass() * GRAV_CONST_KM, t0 * (1d / TICKS_PER_SEC));
	}

	public void setStateVector(StateVector sv)
	{
		long time = getGame().time();
		set(ref, sv.r, sv.v, time, ort0);
	}

	public void setConic(Conic orbit)
	{
		long time = getGame().time();
		StateVector sv = orbit.getStateVectorAtTime(time * (1d / TICKS_PER_SEC));
		setStateVector(sv);
	}

	public KeplerianElements getInertialOrbitElements()
	{
		return getConic().getElements();
	}

	public void setInertialOrbitElements(KeplerianElements elem)
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		elem.setMu(ref.getMass() * GRAV_CONST_KM);
		setConic(elem.getConic());
	}

	// really should be "equitorial", not geocentric
	public KeplerianElements getGeocentricOrbitElements()
	{
		if (ref instanceof Planet)
			return ((Planet) ref).xyz2ijk(getConic()).getElements();
		else
			return getConic().getElements();
	}

	public void setGeocentricOrbitElements(KeplerianElements elem)
	{
		if (ref == null)
			throw new IllegalArgumentException("Set parent first");
		elem.setMu(ref.getMass() * GRAV_CONST_KM);
		if (ref instanceof Planet)
			setConic(((Planet) ref).ijk2xyz(elem.getConic()));
		else
			setConic(elem.getConic());
	}

	public void setB1950OrbitElements(KeplerianElements elem)
	{
		elem.setMu(ref.getMass() * GRAV_CONST_KM);
		Conic conic = new Conic(elem);
		StateVector sv = conic.getStateVectorAtEpoch();
		Constants.FROM_B1950.transform(sv.r);
		Constants.FROM_B1950.transform(sv.v);
		setConic(new Conic(sv, elem.getMu(), elem.getEpoch()));
	}

	//

	public String toString()
	{
		return "["
			+ getClass().getName()
			+ ": "
			+ default_perturbs
			+ ", user="
			+ user_perturbs
			+ "]";
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

	static PropertyHelper prophelp = new PropertyHelper(DefaultMutableTrajectory.class);

	static {
		prophelp.registerGetSet("angvel", "AngularVelocity", Vector3d.class);
		prophelp.registerGetSet("orientation", "Orientation", Orientation.class);
		prophelp.registerGetSet("planetrefort", "PlanetRefOrientation", Orientation.class);
		prophelp.registerGetSet("velrefort", "VelRefOrientation", Orientation.class);
		prophelp.registerGetSet("parent", "Parent", UniverseThing.class);
		prophelp.registerGet("thing", "getThing");
		prophelp.registerGet("type", "getType");

		prophelp.registerGetSet("conic", "Conic", Conic.class);
		prophelp.registerGetSet("orbelemgeo", "GeocentricOrbitElements", KeplerianElements.class);
		prophelp.registerGetSet("orbelemintrl", "InertialOrbitElements", KeplerianElements.class);
		prophelp.registerSet("orbelemb1950", "setB1950OrbitElements", KeplerianElements.class);
		prophelp.registerSet("statevector", "setStateVector", StateVector.class);
	}

	static boolean debug = !true;

	// SETTINGS

	// _IN_ is the threshold of drag which the ship must attain
	// in order to enter an OrbitTrajectory
	// therefore it must be greater than _OUT_ to avoid hysterisis
	static double ORBIT_IN_THRESHOLD;

	static double ORBIT_OUT_THRESHOLD;

	// the factor to alter the influence radius for purposes of detecting
	// the exit criteria
	static double INFRAD_SLOP;

	static SettingsGroup settings = new SettingsGroup(DefaultMutableTrajectory.class, "Trajectory")
	{
		public void updateSettings()
		{
			ORBIT_IN_THRESHOLD = getDouble("ToOrbitThreshold", 4e-11);
			ORBIT_OUT_THRESHOLD = getDouble("ToOrbitThreshold", 1e-11);
			INFRAD_SLOP = getDouble("InfluenceSlop", 5e-4) + 1;
			perturbFlags = 0;
			if (getBoolean("Drag", true))
				perturbFlags |= PF_DRAG;
			if (getBoolean("ThirdBody", true))
				perturbFlags |= PF_3RDBODY;
			if (getBoolean("J2", false))
				perturbFlags |= PF_J2;
		}
	};

}
