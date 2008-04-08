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

import java.io.IOException;
import java.util.*;

import com.fasterlight.exo.game.NotifyingEvent;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.strategy.Agency;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.INIFile;
import com.fasterlight.vecmath.Vector3f;

/**
  * A Capability is a component of a Module.
  * It can contain a given capacity of resources, and
  * can draw from a set of sources (which are other capabilities,
  * need not be in this Module)
  * It should be subclasses to create specialized components like
  * power generators, propulsion etc.
  */
public class Capability
implements Constants, PropertyAware
{
	private Module module;

	private String name;

	// failure properties
	private int rating;
	private List failuremodes = new ArrayList();

	private ResourceSet capacity = new ResourceSet();
	private ResourceSet supply = new ResourceSet();

	private List sources = new ArrayList();
	private int source_inhibit = 0;

	private Vector3f offset;

	public static final int CHECK_ONLY  = 1;
	public static final int ALL_OR_NONE = 2;

	static final Resource RES_E = Resource.getResourceByName("E");

	private PropertyMap attrs_map;

	//

	public Capability(Module module)
	{
		this.module = module;
	}

	public Module getModule()
	{
		return module;
	}

	public Structure getStructure()
	{
		return module.getStructure();
	}

	public Agency getOwner()
	{
		return module.getStructure().getOwner();
	}

	public void initialize(Properties props)
	{
		// get failure modes
		String failstr = props.getProperty("fail");
		if (failstr != null)
		{
			StringTokenizer st = new StringTokenizer(failstr, ",");
			while (st.hasMoreTokens())
			{
				String tok = st.nextToken();
				int p = tok.indexOf(":");
				if (p > 0)
				{
					String ftype = tok.substring(0,p);
					String rstr = tok.substring(p+1);
					int r = Integer.parseInt(rstr);
					try {
						Class clazz = Class.forName("com.fasterlight.exo.ship.fail." + ftype);
						FailureMode fm = (FailureMode)clazz.newInstance();
						fm.set(ftype, r);
						failuremodes.add(fm);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		String tmp;
		// supply, capacity
		tmp = props.getProperty("capacity", "-");
		capacity.set(tmp);

		tmp = props.getProperty("supply");
		if (tmp != null)
			setSupply(new ResourceSet(tmp));
		else
			setSupply(capacity);

		// component pos
		tmp = props.getProperty("offset");
		if (tmp != null)
		{
			offset = parseVector3f(tmp);
		}

		// attributes
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements())
		{
			String pname = (String)e.nextElement();
			if (pname.startsWith("attrs."))
			{
				String pval = props.getProperty(pname);
				getAttributes().put(pname.substring(6), pval);
			}
		}
	}

	protected static Vector3f parseVector3f(String tmp)
	{
		return new Vector3f(AstroUtil.parseVector(tmp));
	}

	// initialize links to other capabilities (in this module)
	public void initialize2(Properties props)
	{
		String tmp;
		tmp = props.getProperty("sources", getName());
		StringTokenizer st = new StringTokenizer(tmp, ";");
		while (st.hasMoreTokens())
		{
			String capname = st.nextToken();
			Capability cap = module.getCapabilityByName(capname);
			if (cap == null)
			{
				System.out.println("Unknown capability: " + capname + " in " + module);
			} else {
				addSource(cap);
			}
		}
	}

	public boolean deactivate()
	{
		return false;
	}

	/**
	  * An extreme version of deactivate that is used to
	  * shutdown before removing a module for detachment
	  */
	public void shutdown()
	{
	}

	public ResourceSet getCapacity()
	{
		return new ImmutableResourceSet(capacity);
	}

	public ResourceSet getSupply()
	{
		return new ImmutableResourceSet(supply);
	}

	public void setSupply(ResourceSet newsupply)
	{
		float mass1 = supply.mass();
		supply.set(newsupply);
		fixSupply();
		float mass2 = supply.mass();
		if (mass2 != mass1)
			module.adjustMass(mass2-mass1);
	}

	public void addSupply(ResourceSet newsupply, float scale)
	{
		float mass1 = supply.mass();
		supply.add(newsupply, scale);
		fixSupply();
		float mass2 = supply.mass();
		if (mass2 != mass1)
			module.adjustMass(mass2-mass1);
	}

	public void addSupply(ResourceSet newsupply)
	{
		addSupply(newsupply, 1);
	}

	private void fixSupply()
	{
		supply.intersect(getCapacity());
		supply.union(ResourceSet.EMPTY);
	}

	// CONSUMATION

	public void setSources(Object o)
	{
		removeAllSources();
		if (o instanceof Capability)
		{
			addSource( (Capability)o );
		}
		else if (o == null)
		{
			// do nothing
		}
		else
			throw new PropertyRejectedException("Unknown type for setsource: " + o.getClass());
	}

	public void removeAllSources()
	{
		sources.clear();
	}

	public void addSource(Capability src)
	{
		if (sources.contains(src))
			sources.remove(src);
		sources.add(src);
	}

	public void removeSource(Capability src)
	{
		sources.remove(src);
	}

	public int getSourceInhibitFlags()
	{
		return source_inhibit;
	}

	public void setSourceInhibitFlags(int x)
	{
		this.source_inhibit = x;
	}

	public boolean dependsOnModule(Module m)
	{
		Iterator it = getSources().iterator();
		while (it.hasNext())
		{
			if ( ((Capability)it.next()).getModule() == m )
				return true;
		}
		return false;
	}

	public List getSources()
	{
		return Collections.unmodifiableList(sources);
	}

	public ResourceSet consume(ResourceSet desired, int flags)
	{
		if (debug)
			System.out.println("consume(" + this + ":" + desired + ", " + flags + ")");
		ResourceSet sup = new ResourceSet(getSupply());
		sup.sub(desired);
		if (debug)
			System.out.println("consume(" + this + ") : diff=" + sup);
		// if min amount >= 0, we got all we needed
		float min = sup.getMinAmount();
		if (min >= 0)
		{
			if ((flags & CHECK_ONLY) == 0)
				setSupply(sup);
			if (debug)
				System.out.println("consume(" + this + ") : got all");
			return desired;
		}
		// if all-or-none, we don't have enuff, return null
		if ((flags & ALL_OR_NONE) != 0)
		{
			if (debug)
				System.out.println("consume(" + this + ") : got none");
			return ResourceSet.EMPTY;
		}

		// otherwise, return what we got
		// sup = supply - desired
		// want: intersect(desired, supply)
		ResourceSet diff = new ResourceSet(desired);
		diff.intersect(getSupply());
		if ((flags & CHECK_ONLY) == 0)
		{
			addSupply(diff, -1);
			// if we're dealing with E, update the heat too
			float amountE = diff.getAmountOf(RES_E);
			if (amountE > 0)
			{
				getStructure().addHeat(Constants.KWH_TO_J*amountE, Constants.COBE_T0);
			}
		}
		if (debug)
			System.out.println("consume(" + this + ") : got " + diff);
		return diff;
	}

	public ResourceSet request(ResourceSet desired, int flags)
	{
		if (debug)
			System.out.println("request(" + desired + ", " + flags + ")");
		// if ALL_OR_NONE is specified, we must first do a complete check
		if ( (flags&(ALL_OR_NONE|CHECK_ONLY)) == ALL_OR_NONE )
		{
			ResourceSet req = request(desired, ALL_OR_NONE | CHECK_ONLY);
			// if we got nothin, it wasn't successful
			if (req.mag() == 0)
			{
				if (debug)
					System.out.println("request(): all or none, return none");
				return ResourceSet.EMPTY;
			}
		}
		// go thru all the capabilities
		ResourceSet rem = new ResourceSet(desired);
		int si = 0;
		Iterator it = getSources().iterator();
		while (it.hasNext())
		{
			if ( (source_inhibit & (1<<si)) == 0 )
			{
				Capability cap = (Capability)it.next();
				rem.sub(cap.consume(rem, flags & CHECK_ONLY));
				if (rem.mag() <= 0)
				{
					if (debug)
						System.out.println("request(): got all");
					return desired;
				}
			}
			si++;
		}
		// if we got here, it means we didn't get all we wanted
		if ((flags & ALL_OR_NONE) != 0) {
			if (debug)
				System.out.println("request(): all or none, return none");
			return ResourceSet.EMPTY;
		} else {
			ResourceSet diff = new ResourceSet(desired);
			diff.sub(rem);
			if (debug)
				System.out.println("request(): got " + diff);
			return diff;
		}
	}

	/**
	  * Remove all dependencies from this capability to module 'm'
	  */
	public void removeDependencies(Module m)
	{
		Module m2 = this.getModule();
		Iterator srcit = getSources().iterator();
		while (srcit.hasNext())
		{
			Capability src = (Capability)srcit.next();
			if (m != m2 && src.getModule() == m)
			{
				removeSource(src);
				srcit = getSources().iterator();
			}
		}
	}

	//

	public float getMass()
	{
		return getSupply().mass();
	}

	public float getPercentRemaining()
	{
		return getSupply().mag()*100.0f/getCapacity().mag();
	}

	public float getPercentRemaining(String resname)
	{
		Resource res = Resource.getResourceByName(resname);
		return getSupply().getAmountOf(res)*100.0f/getCapacity().getAmountOf(res);
	}

	public Game getGame()
	{
		return module.getGame();
	}

	public StructureThing getThing()
	{
		return module.getStructure().getThing();
	}

	public SpaceShip getShip()
	{
		return (SpaceShip)module.getStructure().getThing();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name) // todo: public?
	{
		this.name = name;
	}

	protected void addStringAttrs(List l)
	{
		// override me
	}

	public String toString()
	{
		ArrayList l = new ArrayList(4);
		addStringAttrs(l);
		if (l.size() == 0) {
			return getName();
		} else {
			StringBuffer st = new StringBuffer();
			st.append(getName());
			st.append(" (");
			for (int i=0; i<l.size(); i++)
			{
				if (i>0)
					st.append(", ");
				st.append(l.get(i).toString());
			}
			st.append(')');
			return st.toString();
		}
	}

	// POSITION STUFF

	/**
	  * Get offset from CM, in structure coords, meters
	  * hotspot
	  */
	public Vector3f getCMOffset()
	{
		Vector3f mofs = getModule().getOffset();
		if (offset != null)
		{
			Vector3f ofs2 = new Vector3f(offset);
			getModule().getOrientation().transform(ofs2);
			mofs.add(ofs2);
		}
		return mofs;
	}

	void setModuleOffset(Vector3f cm)
	{
		offset = cm;
	}

	Vector3f getModuleOffset()
	{
		return offset;
	}

	// FAILURE STUFF

	protected void activateFailures()
	{
		for (int i=0; i<failuremodes.size(); i++)
		{
			FailureMode fm = (FailureMode)failuremodes.get(i);
			// get the failure rating of this capability
			int r = fm.r;
			// create the failure event
			long t = getFailTimeTicks(getGame().getRandom(), r);
			fm.setEvent(new FailureEvent(getGame().time() + t, fm), this);
			getGame().postEvent(fm.failevent);//todo?
		}
	}

	protected void deactivateFailures()
	{
		for (int i=0; i<failuremodes.size(); i++)
		{
			FailureMode fm = (FailureMode)failuremodes.get(i);
			fm.clearEvent();
		}
	}

	class FailureEvent extends NotifyingEvent
	{
		FailureMode fm;
		FailureEvent(long t, FailureMode fm)
		{
			super(t, CRITICAL);
			this.fm = fm;
		}
		public String getMessage()
		{
			return Capability.this + " failed!";
		}
		public void handleEvent(Game g)
		{
			fm.fail();
		}
	}

	public static long getFailTimeTicks(Random rnd, int r)
	{
		double x = rnd.nextDouble();
		// TTF=-(1/MTBF)*ln(U)
		// This is discussed in Law and Kelton, Simulation Modeling and Analysis.
		double ttf = -Math.exp(r)*Math.log(x);
		if (debug)
			System.out.println("r=" + r + ", x=" + x + ", ttf=" + ttf);
		return (long)(ttf*Constants.TICKS_PER_SEC);
	}

	// INI FILE READING

	public static Properties getCapsProps(INIFile ini, Module m, String capname)
	throws IOException
	{
		String n = m.getType() + " -- " + capname;
		Properties p = ini.getSection(n);
		String inherit = p.getProperty("inherits");
		if (inherit != null)
		{
			n = inherit;
			if (n.indexOf(" -- ") < 0)
				n = m.getType() + " -- " + n;
			Properties p2 = ini.getSection(n);

			// make new properties, add child's properties
			p = new Properties(p);
			Enumeration e = p2.propertyNames();
			while (e.hasMoreElements())
			{
				String pname = (String)e.nextElement();
				if (p.getProperty(pname) == null)
					p.put(pname, p2.getProperty(pname));
			}
		}
		return p;
	}

	// ATTRIBUTES

	public PropertyMap getAttributes()
	{
		if (attrs_map == null)
			attrs_map = new PropertyMap();
		return attrs_map;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Capability.class);

	static {
		prophelp.registerGet("module", "getModule");
		prophelp.registerGet("structure", "getStructure");
		prophelp.registerGet("owner", "getOwner");
		prophelp.registerGet("mass", "getMass");
		prophelp.registerGetSet("supply", "Supply", ResourceSet.class);
		prophelp.registerGet("capacity", "getCapacity");
		prophelp.registerGet("thing", "getThing");
		prophelp.registerGet("ship", "getThing");
		prophelp.registerGet("name", "getName");
		prophelp.registerGet("sources", "getSources");
		prophelp.registerSet("sources", "setSources", Object.class);
		prophelp.registerGetSet("source_inhibit", "SourceInhibitFlags", int.class);
		prophelp.registerSet("addsource", "addSource", Capability.class);
		prophelp.registerSet("removesource", "removeSource", Capability.class);
		prophelp.registerGet("percent", "getPercentRemaining");
		prophelp.registerGet("attrs", "getAttributes");
	}

	public Object getProp(String key)
	{
		if (key.startsWith("%"))
			return new Float(getPercentRemaining(key.substring(1)));
		else
			return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	public static boolean debug = false;

}
