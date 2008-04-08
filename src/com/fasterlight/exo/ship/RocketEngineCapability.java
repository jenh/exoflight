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
import com.fasterlight.math.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * A type of propulsion system that thrusts in a single direction,
  * and is optionally throttlable.
  */
public abstract class RocketEngineCapability
extends PropulsionCapability
{
	private transient float exitpres;
	private transient float minthrot, maxthrot, limthrot;
	private transient int throtsteps;
	private transient float tattack, tdecay;

	private float cmd_throttle; // throttle commanded by user
	private float act_throttle; // actual thrust percentage (pctthrust)
	private float cur_throttle; // last throttle setting when perturbation added

	private Func1d thrustprof;

	private Vector3f thrustdir = new Vec3f(0,0,1);
	private Vector3f adjthrustdir = new Vec3f(); // translate into structure coords

	//

	public RocketEngineCapability(Module module)
	{
		super(module);
	}

	protected void setInitialOffset()
	{
		// for engines, the default offset is the -Z axis
		setModuleOffset(new Vector3f(0,0,-getModule().getDimensions().z/2));
	}

	public void initialize(Properties props)
	{
		setInitialOffset();

		super.initialize(props);

		exitpres = Util.parseFloat(props.getProperty("exitpres", "0"));
		setThrustProfile( props.getProperty("profile", "") );
		minthrot = Util.parseFloat(props.getProperty("minthrot", "0"));
		maxthrot = Util.parseFloat(props.getProperty("maxthrot", "1"));
		limthrot = Util.parseFloat(props.getProperty("limthrot", "999"));
		throtsteps = Integer.parseInt(props.getProperty("throtsteps", "100"));
		thrustdir.set(AstroUtil.parseVector(props.getProperty("thrustdir", "0,0,1")));
		thrustdir.normalize();
		tattack = (1.0f/Util.parseFloat(props.getProperty("tattack", "0.001")))*getIntervalFloat();
		tdecay = (1.0f/Util.parseFloat(props.getProperty("tdecay", "0.001")))*getIntervalFloat();
	}

   public void setThrustProfile(String tmp)
   {
		tmp = tmp.trim();
		if (tmp.length() > 0)
			thrustprof = CurveParser.parseCurve1d(tmp);
	}

	public ResourceSet getReactants()
	{
		return new ResourceSet(super.getReactants(), act_throttle);
	}

	public float getExitPressure() // in kPa
	{
		return exitpres;
	}

	public float getThrottle()
	{
		return cmd_throttle;
	}

	public void setThrottle(float throttle)
	{
		if (!isArmed())
			throttle = 0;
		// we can always set throttle to 0
		if (throttle != 0)
		{
			if (throttle > limthrot)
				throttle = maxthrot;
			else
				throttle = ((float)Math.round(Math.max(Math.min(maxthrot,throttle),minthrot)*throtsteps))/throtsteps;
		}
		cmd_throttle = throttle;
		setActive(cmd_throttle != 0);
	}

	private void adjustThrust()
	{
		if (cmd_throttle > act_throttle)
			act_throttle = Math.min(cmd_throttle, act_throttle+tattack);
		else
			act_throttle = Math.max(cmd_throttle, act_throttle-tdecay);
	}

	public boolean notifyActivated()
	{
		cur_throttle = 0;
		act_throttle = 0;
		adjustThrust();
		adjthrustdir.set(thrustdir);
		getModule().getOrientation().transform(adjthrustdir);

		return super.notifyActivated();
	}

	public boolean notifyDeactivated()
	{
		if (super.notifyDeactivated())
		{
			cur_throttle = 0;
			cmd_throttle = 0; // todo: is this a good idea
			act_throttle = 0;
			return true;
		} else
			return false;
	}

	// hotspot
	public float getThrustAdjustment(Vector3d r)
	{
		// static thrust loss, or other factor
		// returned in K-newtons
		// todo: can make more efficient
		// todo: check units
		// todo: store somewhere for property to grab
		if (exitarea > 0)
		{
			StructureThing thing = getThing();
			if (getThing() != null &&
				getThing().getParent() != null && getThing().getParent() instanceof Planet)
			{
				Planet p = (Planet)(getThing().getParent());
				Atmosphere atmo = p.getAtmosphere();
				if (atmo != null)
				{
					float alt = (float)(r.length() - p.getRadius());
					double ambientpres = atmo.getParamsAt(alt).pressure;
					if (ambientpres > 0)
					{
						if (alt < 0)
							alt = 0;
						//float N = (float)(exitarea*(exitpres-ambientpres));
						// gotta convert pressure from kPa to kg/cm^2
						// and exitarea from m^2 to cm^2 (*100*100)
						// so our units are in kg, we want kilonewtons (*9.80665/1000)
						// so we multiply by (0.01019716*100*100*9.80665/1000), which,
						// amazingly enough, == 1
						float N = (float)(exitarea*ambientpres);
						return -N;
					}
				}
			}
		}
		return 0.0f;
	}

	public boolean isBlocked()
	{
		// can't activate if -Z direction is blocked by another module
		Module linked = getModule().getLink(Module.DOWN);
		return (linked != null && !linked.isHollow());
	}

	public Vector3d getThrustVector()
	{
		return new Vec3d(adjthrustdir);
	}

	public Vector3f getThrustDirection()
	{
		return new Vec3f(thrustdir);
	}

	//

	protected Perturbation getRocketPerturbation()
	{
		return new RocketThrustPerturbation();
	}

	class RocketThrustPerturbation
	implements Perturbation
	{
		/**
		  * Computes the mass at time 'time' and scales the thrust vector accordingly
		  */
		public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
			Orientation ort, Vector3d w, long time)
		{
			// compute thrust vector & magnitude
			Vector3d f = getThrustVector();
			ort.transform(f);
			float adjust = getThrustAdjustment(r);

			float thrust = (getExhaustVel()*exhmass1+adjust);
			f.scale(thrust);

			// compute thrust position
			Vector3d cmofs = new Vector3d(getCMOffset());
			ort.transform(cmofs);
			cmofs.scale(1d/1000);
			force.addOffsetForce(f, cmofs);
		}
	}

	public boolean doPeriodic()
	{
		// setup throttle params
		if (thrustprof != null)
		{
			long at = getActivateTime();
			float x;
			if (at == INVALID_TICK)
				x = 0;
			else
				x = (getGame().time() - at)*(1f/TICKS_PER_SEC);
			float throt = (float)thrustprof.f(x);
			setThrottle(throt);
//			System.out.println(this + " " + x + " " + throt);
		}

		return isActive() && super.doPeriodic();
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		super.doReact(react, product);

		// todo: we have problems here?
		if (act_throttle != cur_throttle)
		{
			removePerturbation();
			cur_throttle = act_throttle;
			addPerturbation();
		}
		adjustThrust();
	}

	public float getPctThrust()
	{
		return cur_throttle;
	}

	public boolean getFired()
	{
		return isActive();
	}

	public void setFired(boolean b)
	{
		setArmed(b);
		setThrottle(1);
	}

	protected void addStringAttrs(List l)
	{
		super.addStringAttrs(l);
		if (isRunning())
		{
			l.add(Integer.toString((int)(getThrottle()*100)) + '%');
		}
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(RocketEngineCapability.class);

	static {
		prophelp.registerGetSet("throttle", "Throttle", float.class);
		prophelp.registerGetSet("fired", "Fired", boolean.class);
		prophelp.registerGet("exitpressure", "getExitPressure");
		prophelp.registerGet("pctthrust", "getPctThrust");
		prophelp.registerSet("profile", "setThrustProfile", String.class);
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
