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

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import com.fasterlight.exo.crew.CrewMember;
import com.fasterlight.exo.game.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.*;
import com.fasterlight.math.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Represents a module of a Structure, and contains a set of capabilities.
  * @see Capability
  */
public class Module
extends ModuleBase
implements java.io.Serializable, PropertyAware
{
	static final float DEFAULT_TEMP = 283; // K, nice outdoor temp ??

	public static final int NORTH = 0; // +y
	public static final int SOUTH = 1; // -y
	public static final int EAST = 2; // +x
	public static final int WEST = 3; // -x
	public static final int UP = 4; // +z
	public static final int DOWN = 5; // -z
	public static final int DOCK_X = 6; // the thing docked to
	public static final int DOCK_Y = 7; // the thing being docked
	public static final int NUM_DIRS = 8;

	public static final int ROLL_0 = 0<<4;
	public static final int ROLL_90 = 1<<4;
	public static final int ROLL_180 = 2<<4;
	public static final int ROLL_270 = 3<<4;

	// length, width, height
	protected transient Vector3f dims;
	protected transient float emptymass;			// in kg
	protected transient float volume;				// in M^3
	protected transient int crewcap;					// # of crew members
	protected transient int connect;					// mask that tells directions can connect
	protected transient String shapename;
	protected transient boolean selfsuff, hollow;

	protected Module[] links = new Module[NUM_DIRS]; // n s e w u d
	protected int doors = 0; // mask of doors closed
	protected ResourceSet atmosphere = new ResourceSet();
	protected float airtemp = 283; // todo?
	protected float walltemp = DEFAULT_TEMP; // todo?
	protected List features = new ArrayList();
	protected float mass;
	protected int stagenum;

	// aero surfaces for fore, oblique, and aft
	protected AeroSurface surf_fore, surf_obliq, surf_aft;

	// things for parachutes
	protected int chute_open_time;
	protected long chute_start_time = Game.INVALID_TICK;

	protected Vector3f maxloads;

	protected Structure structure;
	// location of this module in the structure, or null
	protected Vector3f position = new Vector3f();
	protected int rotflags = (UP  | ROLL_0);

	protected transient boolean initialized = false;

	protected Orientation ort = new Orientation();

	protected String name, type;

	protected List crew = new ArrayList();

	protected Map progmap = new HashMap();

	protected ContactPoint[] contactPoints;

	protected Vector3f damping = new Vec3f();

	//

	public Module(Game game, String type)
	{
		super(game, type);
		this.type = type;
		this.name = type;
		initialize();
	}

	public Game getGame()
	{
		return game;
	}

	public String getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Structure getStructure()
	{
		return structure;
	}

	public SpaceShip getShip()
	{
		return (SpaceShip)structure.getThing();
	}

   // overrided so we can call initailize() when deserializing
	private void readObject(ObjectInputStream stream)
   	throws IOException, ClassNotFoundException
  	{
  		stream.defaultReadObject();
  		initialize();
  	}

  	// CURVE STUFF

  	static Map curvemap = new HashMap();
  	static INIFile curveini;

  	static {
  		try {
	  		curveini = new CachedINIFile(
	  			ClassLoader.getSystemResourceAsStream("etc/curves.txt"));
	  	} catch (IOException ioe) {
	  		ioe.printStackTrace();
	  		throw new RuntimeException("Couldn't read curves.txt");
	  	}
  	}

  	static Func1d loadCurve(String categ, String name)
  	{
  		if (name.length() == 0)
  			return null;
  		if (!name.startsWith("%"))
  			return CurveParser.parseCurve1d(name);
  		// it starts with a '%', so look it up in ini file
  		name = name.substring(1);
  		try {
  			Func1d o = (Func1d)curvemap.get(name);
  			if (o == null)
  			{
		  		String tmp = curveini.getString(categ, name, null);
		  		if (tmp == null)
		  			throw new RuntimeException("Couldn't find curve " + name);
  				o = CurveParser.parseCurve1d(tmp);
  			}
  			return o;
  		} catch (IOException ioe) {
  			throw new RuntimeException(ioe.toString());
  		}
  	}

  	//

	AeroSurface loadAeroSurface(Properties props,
		String prefix, String defdragcurve, float defarea, float dragzofs)
	{
		AeroSurface as = new AeroSurface();
		as.area = Util.parseFloat(props.getProperty(prefix+"area", ""+defarea));
		as.drag_mach_curve = loadCurve("DragCurves", props.getProperty(prefix+"drag_mach", defdragcurve));
		as.lift_aa_curve = loadCurve("LiftCurves", props.getProperty(prefix+"lift_aa", ""));
		as.induced_aa_curve = loadCurve("InducedDragCurves", props.getProperty(prefix+"induced_aa", ""));
		as.drag_pos = new Vector3f(AstroUtil.parseVector(props.getProperty(prefix+"drag_pos", "0,0,"+dragzofs)));
		as.drag_pos.scale(1f/1000);
		as.lift_pos = new Vector3f(AstroUtil.parseVector(props.getProperty(prefix+"lift_pos", "0,0,0")));
		as.lift_pos.scale(1f/1000);
		return as;
	}

	void loadContactPoints()
	{
		List cpoints = new ArrayList(8);
		for (int i=0; i<8; i++)
		{
			String spec = getSetting("contact#" + i, "");
			if (spec.length() > 0)
			{
				ContactPoint cp = new ContactPoint(spec);
				cpoints.add(cp);
			}
		}
		int l = cpoints.size();
		if (l > 0)
		{
			contactPoints = new ContactPoint[l];
			cpoints.toArray(contactPoints);
		}
	}

   public void initialize()
   {
   	if (initialized)
   		return;
   	// todo: error when mispelled or module not found

   	Properties modprops = getModuleSettings();

   	String tmp;
   	dims = new Vector3f(AstroUtil.parseVector(getSetting("size", "1,1,1")));
		mass = emptymass = getSettingFloat("mass", 1.0f);
		volume = getSettingFloat("volume", 0.0f);
		crewcap = getSettingInt("crew", 0);
		stagenum = getSettingInt("stage", 0);
		connect = connectStrToMask(getSetting("connect", ""));
		selfsuff = getSetting("self", "false").equals("true");
		hollow = getSetting("hollow", "false").equals("true");
		tmp = getSetting("maxloads", "");
		if (tmp.length() > 0)
		{
			maxloads = new Vector3f(AstroUtil.parseVector(tmp));
			maxloads.scale((float)(Constants.EARTH_G));
		}
		chute_open_time = (int)(getSettingFloat("chuteopentime", 0.0f)*Constants.TICKS_PER_SEC);

		// read aerodynamic data
		float zarea = (dims.x*dims.y)*(3.141592f/4);
		float xarea = dims.z*(dims.x+dims.y)/2;
		surf_fore = loadAeroSurface(modprops, "fore.", "%disc", zarea, dims.z/2);
		surf_obliq = loadAeroSurface(modprops, "obliq.", "%cylinder", xarea, 0);
		surf_aft = loadAeroSurface(modprops, "aft.", "%disc", zarea, -dims.z/2);

		damping.set(xarea, xarea, zarea);
		damping.scale(DEFAULT_DAMPING_FACTOR/1000);
		damping.set(AstroUtil.parseVector(getSetting("damping", ""+damping)));

		// read all capabilities
		if (this.features.size() == 0)
		{
			try {
				List names = settings.getSectionNames();
				Iterator it = names.iterator();
				while (it.hasNext())
				{
					String n = (String)it.next();
					String prefix = type + " -- ";
					if (n.startsWith(prefix))
					{
						String name = n.substring(prefix.length());
						Properties props = Capability.getCapsProps(settings, this, name);
						String type = props.getProperty("type", "");
						Capability cap = newCapability(type, name);
						cap.initialize(props);
						cap.setName(name);
						features.add(cap);
					}
				}
				// do 2nd stage initialize
				it = this.features.iterator();
				while (it.hasNext())
				{
					Capability cap = (Capability)it.next();
					Properties props = Capability.getCapsProps(settings, this, cap.getName());
					cap.initialize2(props);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new RuntimeException(ioe.toString());
			}
		} else {
			// todo: initialize from settings
		}

		// read module program defs
		Properties props = getModuleSettings();
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements())
		{
			String key = (String)e.nextElement();
			if (key.startsWith("prog_"))
			{
				progmap.put(key.substring(5), props.getProperty(key));
			}
		}

		// init contact points
		loadContactPoints();

		shapename = getSetting("shape", "help");
		initialized = true;
   }

   static char[] connectdirs = { 'n','s','e','w','u','d','x','y' };

   public static int connectStrToMask(String s)
   {
   	int mask = 0;
   	for (int i=0; i<connectdirs.length; i++)
   	{
   		if (s.indexOf(connectdirs[i]) >= 0)
   			mask |= (1<<i);
   	}
   	return mask;
   }

   public static int connectCharToDir(char ch)
   {
   	int mask = 0;
   	for (int i=0; i<connectdirs.length; i++)
   	{
   		if (ch == connectdirs[i])
   			return i;
   	}
   	return mask;
   }

   public static char connectDirToChar(int dir)
   {
   	return connectdirs[dir];
   }

   protected Capability newCapability(String type, String name)
   {
		String cname = "com.fasterlight.exo.caps." + type;
	   Capability cmpt;
      try {
      	Class ass = Class.forName(cname);
      	Class[] args = new Class[1];
      	args[0] = Module.class;
      	Constructor cons = ass.getConstructor(args);
      	Object[] consargs = new Object[1];
      	consargs[0] = this;
	      cmpt = (Capability)cons.newInstance(consargs);
	      cmpt.setName(name);
	      return cmpt;
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new RuntimeException(ex.toString());
      }
   }

	/**
	  * Returns the dimensions of the module, in module units
	  * -- i.e. the +Z axis is the +Z axis of the module
	  */
	public Vector3f getDimensions()
	{
		return new Vector3f(dims);
	}

	public boolean isChuteOpening()
	{
		return (chute_start_time != Game.INVALID_TICK);
	}

	public float getChuteOpenAmount()
	{
		return (chute_open_time == 0) ? 1.0f : getAreaMultiplier(getGame().time());
	}

	protected float getAreaMultiplier(long time)
	{
		if (chute_open_time == 0)
			return 1;
		float x = Math.max(0, Math.min(1, (time-chute_start_time)*1.0f/chute_open_time));
		return x;
	}

	public float getFrontalArea(long time)
	{
		float a = surf_fore.area;
		if (true || chute_open_time == 0)
			return a;
		else {
			return a*getAreaMultiplier(time);
		}
	}

	public float getObliqueArea(long time)
	{
		float a = surf_obliq.area;
		if (true || chute_open_time == 0)
			return a;
		else {
			return a*getAreaMultiplier(time);
		}
	}

	/**
	  * Gets the orientation of the module, with respect to
	  * the structure
	  */
	public Orientation getOrientation()
	{
		return ort;
	}

	public static Orientation getModuleOrientation(int connectdir)
	{
		Orientation ort = new Orientation();
		switch (connectdir)
		{
			case Module.UP : ort.set(new Vector3d(0,0,1), new Vector3d(0,1,0)); break;
			case Module.DOWN : ort.set(new Vector3d(0,0,-1), new Vector3d(0,1,0)); break;
			case Module.NORTH : ort.set(new Vector3d(0,1,0), new Vector3d(1,0,0)); break;
			case Module.SOUTH : ort.set(new Vector3d(0,-1,0), new Vector3d(1,0,0)); break;
			case Module.EAST : ort.set(new Vector3d(1,0,0), new Vector3d(0,1,0)); break;
			case Module.WEST : ort.set(new Vector3d(-1,0,0), new Vector3d(0,1,0)); break;
			case Module.DOCK_X :
			case Module.DOCK_Y :
				break;
		}
		return ort;
	}

	/**
	  * Returns the dimensions of the module, rotated into
	  * structure units
	  */
	public Vector3f getRotatedDimensions()
	{
		Vector3f v = new Vector3f(dims);
		ort.transform(v);
		return v;
	}

	/**
	  * Returns the offset from the CM of the structure
	  */
	public Vector3f getOffset()
	{
		Vector3f v = new Vector3f(position);
		v.sub(structure.cenmass);
		return v;
	}

	public Vector3f getPosition()
	{
		return new Vector3f(position);
	}

	public void setPosition(Vector3f pos)
	{
		position.set(pos);
		if (structure != null)
			structure.adjustCenterOfMass();
	}

   public String getShapeName()
   {
   	return shapename;
   }

	public int getStageNum()
	{
		return stagenum;
	}

   public float getMass()
   {
      return mass;
   }

   public float getEmptyMass()
   {
   	return emptymass;
   }

   void adjustMass(float dm)
   {
   	mass += dm;
   	if (structure != null)
	   	structure.adjustMass(dm);
   }

   void adjustEmptyMass(float dm)
   {
   	emptymass += dm;
   	adjustMass(dm);
   }

   public ResourceSet getSupply()
   {
   	ResourceSet res = new ResourceSet();
   	Iterator it = features.iterator();
   	while (it.hasNext())
   	{
   		res.add( ((Capability)it.next()).getSupply() );
   	}
   	return res;
   }

   /*
   public void adjustSupply(ResourceSet resset, float amt)
   {
   	supply.add(resset, amt);
   	structure.adjustSupply(resset, amt);
   }
   */

   //

   public ResourceSet getAtmosphere()
   {
   	return new ImmutableResourceSet(atmosphere);
   }

   public void addAtmosphere(ResourceSet rset)
   {
   	atmosphere.add(rset);
   	adjustMass(rset.mass());
   	if (structure != null)
   	{
   		structure.equalizePressure();
   	}
   }

   public void setAtmosphere(ResourceSet rset)
   {
   	adjustMass(-atmosphere.mass());
   	atmosphere.clear();
   	addAtmosphere(rset);
   }

   void adjustAtmosphere(ResourceSet rset)
   {
   	atmosphere.add(rset);
   }

   public float consumeAtmosphere(Resource res, float amt, boolean allornone)
   {
   	return atmosphere.consume(res, amt, allornone);
   }

   // 1 atm = 101.3 kpa = 760mmHg
   static final float IDEALGAS_CONST = 8.3145f;  // L * Pa / mol * K

	/**
	  * Returns the pressure in kPa
	  */
   public float getPressure()
   {
   	if (volume == 0)
   		return 0;
   	Iterator it = atmosphere.getResources();
   	float sum = 0;
   	while (it.hasNext())
   	{
   		Resource res = (Resource)it.next();
   		float mw = res.getMolecularWeight(); // todo! molecular weight for each resource (this is O2)
   		sum += (atmosphere.getAmountOf(res)/mw);
   	}
   	return sum*airtemp*IDEALGAS_CONST/volume; // * 1000 for kg, and /1000 for L
   }

   //

   // these shuld only be called by CrewMember methods

   public void addCrewMember(CrewMember cm)
   {
   	if (crew.contains(cm))
   		return;
   	if (crewcap == 0)
   		throw new PropertyRejectedException(this + " is not habitable");
   	if (crew.size() >= crewcap)
   		throw new PropertyRejectedException(this + " already has a crew of " + crewcap);
   	crew.add(cm);
   	adjustMass(cm.getMass());
   }

   public void removeCrewMember(CrewMember cm)
   {
   	if (!crew.contains(cm))
   		return;
   	crew.remove(cm);
   	adjustMass(cm.getMass());
   }

   //

   public int getCrewCount()
   {
   	return crew.size();
   }

   public CrewMember getCrewMember(int i)
   {
   	return (CrewMember)crew.get(i);
   }

   public List getCrewList()
   {
   	return Collections.unmodifiableList(crew);
   }

   //

   public List getCapabilities()
   {
   	return Collections.unmodifiableList(features);
   }

   public Capability getCapability(int i)
   {
   	if (i<0 || i>=features.size())
   		return null;
   	else
	   	return (Capability)features.get(i);
   }

   public int getCapabilityCount()
   {
   	return features.size();
   }

   public Capability getCapabilityByName(String name)
   {
   	for (int i=0; i<getCapabilityCount(); i++)
   	{
   		Capability cap = getCapability(i);
   		if (cap.getName().equals(name))
   			return cap;
   	}
   	return null;
   }

	// todo: return multiple results
	public Capability getCapabilityOfClass(Class c)
	{
		Iterator it = features.iterator();
		while (it.hasNext())
		{
			Capability cap = (Capability)it.next();
			if (c.isAssignableFrom(cap.getClass()))
				return cap;
		}
		return null;
	}

   /**
     * Turn off all capabilities on this module
     */
   public void deactivate()
   {
   	Iterator it = features.iterator();
   	while (it.hasNext())
   	{
   		// todo: deactivate differently?
   		Capability cap = (Capability)it.next();
   		cap.shutdown();
   	}
   }

   public void shutdown(boolean b)
   {
   	if (b)
   		deactivate();
   }

   public float getVolume()
   {
   	return volume;
   }

   void setStructure(Structure structure)
   {
   	this.structure = structure;
   }

	void startParachute(float amt)
	{
   	chute_start_time = getGame().time() - (long)(chute_open_time*amt);
   }

   public Module getLink(int dir)
   {
   	return links[dir];
   }

   public boolean canConnect(int dir)
   {
		return true; //todo?
   	//return ((1<<dir)&connect) != 0;
   }

   public static int getLinkDir(Module a, Module b)
   {
   	for (int i=0; i<NUM_DIRS; i++)
   	{
   		if (a.links[i] == b)
   			return i;
   	}
   	return -1;
   }

   public static void link(Module a, Module b, int dirfroma, int dirfromb)
   {
   	if (a.structure != b.structure)
   	{
			throw new RuntimeException("Modules " + a + " and " + b + " are not in same structure!");
		}
		int dira = dirfroma;
		int dirb = dirfromb;
		if (!a.canConnect(dira))
			throw new RuntimeException("Module " + a + " cannot connect in direction " + dira);
		if (!b.canConnect(dirb))
			throw new RuntimeException("Module " + b + " cannot connect in direction " + dirb);
		if (a.getLink(dira) != null || b.getLink(dirb) != null)
		{
			throw new RuntimeException("Modules " + a + " and " + b + " are already attached to something!");
		}
		a.links[dira] = b;
		b.links[dirb] = a;
	}

	public static void unlink(Module a, Module b)
	{
		if (a.structure != b.structure)
		{
			throw new RuntimeException("Modules " + a + " and " + b + " are not in same structure!");
		}
		int dira = getLinkDir(a, b);
		int dirb = getLinkDir(b, a);
		if (dira < 0 || dirb < 0)
		{
			throw new RuntimeException("Modules " + a + " and " + b + " are not connected!");
		}
   	a.links[dira] = null;
   	b.links[dirb] = null;
   }

   /**
     * Recursively adds modules to a set, starting at a
     * single direction.  Modules already visited are not
     * revisited.
     */
   public void addAllConnected(Set set)
   {
   	if (set.contains(this))
   		return;

   	set.add(this);
  		for (int i=0; i<NUM_DIRS; i++)
  		{
  			if (getLink(i) != null)
	  			getLink(i).addAllConnected(set);
  		}
   }

   /**
     * Strange little method - returns total consumables
     * consumed by propulsion capabilities
     */
   public ResourceSet getPropulsionConsumation()
   {
   	Iterator it = getCapabilities().iterator();
   	ResourceSet total = new ResourceSet();
   	while (it.hasNext())
   	{
   		Capability cap = (Capability)it.next();
   		if (cap instanceof PropulsionCapability)
   		{
   			ResourceSet consume = ((PropulsionCapability)cap).getReactants();
   			total.add(consume);
   		}
   	}
   	return total;
   }

	/**
	  * Another strange little method
	  * Returns total propulsion time (estimated) in seconds
	  */
   public float getPropulsionDuration()
   {
   	ResourceSet total = getPropulsionConsumation();
   	ResourceSet sup = new ResourceSet(getSupply());
   	sup.div(total);
   	return sup.getMaxAmount()*3600; // hrs to sec
   }

   public AeroSurface getForeSurface()
   {
   	return surf_fore;
   }

   public AeroSurface getObliqueSurface()
   {
   	return surf_obliq;
   }

   public AeroSurface getAftSurface()
   {
   	return surf_aft;
   }

   public Vector3f getDampingVector()
   {
   	return damping;
   }

   public boolean isHollow()
   {
   	return hollow;
   }

   public SpaceShip detach()
   {
   	return getShip().detach(this);
   }

   public void detach(boolean b)
   {
   	if (b)
   		detach();
   }

   public SpaceShip jettison()
   {
   	SpaceShip ship = getShip().detach(this);
   	ship.setExpendable(true);
   	return ship;
   }

   public void jettison(boolean b)
   {
   	if (b)
   		jettison();
   }

   public String toString()
   {
   	return getName();
   }

   public String getProgramForID(String id)
   {
   	return (String)progmap.get(id);
   }

   //

   public void notifyForce(Vector3d acc)
   {
   	if (getShip().isExploded())
   		return; // it's already dead, what else can we do??

   	if (!DO_AEROSTRESS)
   		return;

		// first notify the crew
		if (crew.size() > 0)
		{
			float alen = (float)acc.length();
			Iterator it = crew.iterator();
			while (it.hasNext())
			{
				((CrewMember)it.next()).notifyAccel(alen);
			}
		}

		// now blow up the structure, if load is too high
   	if (maxloads != null)
   	{
   		Vector3f a2 = new Vector3f(acc);
   		getOrientation().invTransform(a2);
   		float a2x = Math.abs(a2.x/maxloads.x);
   		float a2y = Math.abs(a2.y/maxloads.y);
   		float a2z = Math.abs(a2.z/maxloads.z);
   		float maxf = Math.max(Math.max(a2x, a2y), a2z);
   		if (maxf > 0.8f) // todo: const
   		{
   			getShip().getShipWarningSystem().setWarning("STRESS-OVERG",
   				"Over-G warning! " + getName() );
//		a2.scale((float)(1f/Constants.EARTH_G));
//		System.out.println(a2);
   			if (maxf > 1.0f)
   			{
   				// random chance of breakup gets more probable
   				// as G approaches maxG * 2
   				float prob = 1 + game.getRandom().nextFloat();
   				if (maxf > prob)
   				{
	   				game.postEvent(new ModuleOverstressEvent(game.time()));
   					getShip().setExpendable(true);
   				}
   			}
   		}
   	}
   }

   class ModuleOverstressEvent
   extends AlertEvent
   {
   	ModuleOverstressEvent(long t)
   	{
   		super(t,
   			getShip().isExpendable() ? NotifyingEvent.INFO : NotifyingEvent.CRITICAL,
				"Aeroframe failure! " + Module.this.getName(),
				getShip().isExpendable() ? "" : "GSTRESS-AEROFAILURE");
   	}
   	public void handleEvent(Game game)
   	{
			if (getShip().getStructure().getModuleCount() > 1)
			{
  				SpaceShip newship = jettison();
  				Trajectory traj = newship.getTrajectory();
  				if (traj instanceof MutableTrajectory)
  				{
	  				Vector3d angvel = traj.getAngularVelocity();
  					float s = 0.25f;
  					angvel.add(new Vector3d(
  						s*(game.getRandom().nextFloat()-0.5f),
  						s*(game.getRandom().nextFloat()-0.5f),
  						s*(game.getRandom().nextFloat()-0.5f)));
	  				((MutableTrajectory)traj).setAngularVelocity(angvel);
	  			}
  			}
  			else
  			{
  				getShip().explode();
  			}
   	}
   }

   //

   public void notifyAdded()
   {
   	// override me
   }

   public void notifyRemoved()
   {
   	// override me
   }

   //

   public ContactPoint[] getContactPoints()
   {
   	return contactPoints;
   }

   //

   public static void main(String[] args)
   throws Exception
   {
   	Game g = new Game();
   	Enumeration e = getAllModuleNames();
   	while (e.hasMoreElements())
   	{
   		String s = (String)e.nextElement();
   		if (s.indexOf(" -- ") < 0)
   		{
	   		System.out.println("Testing module " + s);
   			Module m = new Module(g, s);
   		}
   	}
   }

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Module.class);

	static {
		prophelp.registerGet("structure", "getStructure");
		prophelp.registerGet("ship", "getShip");
		prophelp.registerGetSet("name", "Name", String.class);
		prophelp.registerGet("type", "getType");
		prophelp.registerGet("dim", "getDimensions");
		prophelp.registerGet("ort", "getOrientation");
		prophelp.registerGet("rotdim", "getRotatedDimensions");
		prophelp.registerGet("offset", "getOffset");
		prophelp.registerGet("mass", "getMass");
		prophelp.registerGet("emptymass", "getEmptyMass");
		prophelp.registerGet("supply", "getSupply");
		prophelp.registerGet("shapename", "getShapeName");
		prophelp.registerGet("stagenum", "getStageNum");
		prophelp.registerGetSet("atmosphere", "Atmosphere", ResourceSet.class);
		prophelp.registerGet("pressure", "getPressure");
		prophelp.registerGet("volume", "getVolume");
		prophelp.registerSet("shutdown", "shutdown", boolean.class);
		prophelp.registerSet("detach", "detach", boolean.class);
		prophelp.registerGet("get_detach", "detach");
		prophelp.registerSet("jettison", "jettison", boolean.class);
		prophelp.registerGet("get_jettison", "jettison");
		prophelp.registerGet("hollow", "isHollow");
		prophelp.registerGet("crewlist", "getCrewList");
		// todo
	}

	public Object getProp(String key)
	{
		if (key.startsWith("#"))
			return getCapability(PropertyUtil.toInt(key.substring(1)));
		else if (key.startsWith("$"))
			return getCapabilityByName(key.substring(1));
		else if (key.startsWith("crew#"))
			return getCrewMember(Integer.parseInt(key.substring(5)));
		else
			return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	// SETTINGS

	static boolean DO_AEROSTRESS;
	static float DEFAULT_DAMPING_FACTOR;

	static SettingsGroup _settings = new SettingsGroup(Module.class, "Ship")
	{
		public void updateSettings()
		{
			DEFAULT_DAMPING_FACTOR = getFloat("DefaultDampingFactor", 0.10f);
			DO_AEROSTRESS = getBoolean("DoAerostress", true);
		}
	};

}

