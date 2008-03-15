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
import com.fasterlight.game.*;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.*;

/**
  * The AttitudeController is an "attitude program" used by the
  * GuidanceCapability to command the spacecraft to point to
  * a particular direction.  It can be extended to provide
  * different guidance programs.
  *
  * INPUTS:
  * - space ship
  * - RCS capability strength
  *
  * OUTPUTS:
  * - RCS strength
  * - status
  *
  * It is an Observable which notifies its observers when
  * the status changes.
  *
  * NEW ALGORITHM:
  *
  * 1. Get total available torque for this frame.
  * 2. Compute desired ang. velocity to target.
  * 3. Iterate over RCS caps, set each one until desired change reached
  * 4. For all others, set strength = 0
  *
  * todo: make better for shuttle vernier!!!
  */
public class AttitudeController
extends Observable
implements PropertyAware
{
	protected SpaceShip ship;
	protected AttitudeControllerSettings settings = new AttitudeControllerSettings();

	protected Game game;
	protected boolean locked;

	protected double last_dev; // angle to desired orientation
	protected double last_power, last_maxrate, last_err;

	protected Orientation destort = new Orientation();

	protected GuidanceProgram guidance;

	protected boolean biasonly = false;
	protected Vector3f angvelbias = new Vector3f();

	static final float MIN_MOMENT_VALUE = 1e-10f;

	protected int trans_flags;
	protected Vector3f trans_strength = new Vector3f();

	// this magic # controls how fast the attitude controller
	// thrusts away from the edges of the deadband.
	// we use this to avoid "hanging out" right on the edge.
	static final float DEV_BIAS = Settings.getFloat(
		"ACS", "DevBias", 5e-5f);

	//

	public AttitudeController(Game game, SpaceShip thing)
	{
		this.game = game;
		this.ship = thing;
	}

	public void setTargetOrientation(Orientation ort)
	{
		// todo: check for valid ort
		this.destort.set(ort);
	}

	public Orientation getTargetOrientation()
	{
		return destort;
	}

	public AttitudeControllerSettings getSettings()
	{
		return settings;
	}

	public void setSettings(AttitudeControllerSettings settings)
	{
		if (settings == null)
			throw new IllegalArgumentException();
		this.settings = settings;
	}

	public void setAngVelBias(Vector3f angvel)
	{
		this.angvelbias.set(angvel);
		biasonly = (angvel.lengthSquared() > 0);
	}

	public Vector3f getAngVelBias()
	{
		return angvelbias;
	}

	public void setGuidanceProgram(GuidanceProgram guidance)
	{
		this.guidance = guidance;
	}

	public GuidanceProgram getGuidanceProgram()
	{
		return guidance;
	}

	// "fixed" orientation is multiplied by the planet's
	// frame of reference if planet-relative
	public Orientation getFixedOrientation()
	{
		Orientation ort = new Orientation(destort);
		Vector3d thrustvec = ship.getStructure().getThrustVector();
		if (thrustvec.lengthSquared() > 1e-10)
		{
			ort.mulInverse(new Orientation(thrustvec));
//			System.out.println("  targort = " + ort.getDirection());
		}
		return ort;
	}

	public void setTranslating(int flags, Vector3f strength)
	{
		this.trans_flags = flags;
		this.trans_strength.set(strength);
	}

	public SpaceShip getShip()
	{
		return ship;
	}

	public void doRCS()
	{
		locked = false;

		boolean gactive = ship.getShipAttitudeSystem().isGuidanceActive();
		if (guidance != null && gactive)
		{
			guidance.compute(this);
		}

		// compute power of all rcs caps
		Vector3d power = new Vector3d();
		Vector3f moment = new Vector3f();
		List rcscaps = ship.getStructure().getCapabilitiesOfClass(AttitudeControlComponent.class);

		int numarmed=0;
		Iterator it = rcscaps.iterator();
		while (it.hasNext())
		{
			AttitudeControlComponent rcscap = (AttitudeControlComponent)it.next();
			if (rcscap.isArmed())
			{
				numarmed++;
				rcscap.getMaxMoment(moment);
				power.x += moment.x;
				power.y += moment.y;
				power.z += moment.z;
			}
		}

		last_power = power.length();

		if (power.x+power.y+power.z <= 1e-20)
		{
			// if we had none that were armed, issue a warning
			ship.getShipWarningSystem().setWarning("GUID-NORCS", "No RCS capability is available!");
 			return; // no power, no RCS!
		}

  		Trajectory traj = ship.getTrajectory();
  		Vector3d velvec = traj.getAngularVelocity();
  		Orientation curort = ship.getOrientation(game.time());

		Vector3d vv;

   		double rateband = settings.rateband;

   	if (biasonly)
   	{

   		vv = new Vector3d(velvec);
   		curort.invTransform(vv);
   		vv.sub(new Vector3d(angvelbias), vv);
   		setTargetOrientation(curort);
   		locked = false;
//   		System.out.println("vv=" + vv);

   	} else {

   		Orientation targort = getFixedOrientation();
   		curort.invTransform(velvec);

   		// get axis to target orientation
   		Vector3d axis = curort.getShortestSlerpAxis(targort);
   		// transform by the inverse current orientation
   		curort.invTransform(axis);
   		vv = new Vector3d(axis);
   		double dist = vv.length()*2;
   		last_dev = dist;

   		locked = (dist < settings.devband);

   		if (!locked)
   		{
   			double maxrate;

   			rateband *= 1+dist;

   			// how fast would we go if we started at the target
	   		// and acclerate to the current position?
   			// that's the limit of our velocity
	   		// v = sqrt(2*a*s)/2
	   		//
	   		// todo: this works good with moving targets
	   		// but not good for long-term deadbands
	   		//
	   		{
	   			Vector3d vcorr = new Vector3d(vv);
   				vcorr.scale((dist-settings.devband+DEV_BIAS)/dist);
   				vcorr.x *= power.x;
   				vcorr.y *= power.y;
	   			vcorr.z *= power.z;
	   			double maxpower = vcorr.length();
		   		maxrate = Math.sqrt(2*maxpower)/2; // see above equation
    				maxrate = Math.min(settings.maxrate, maxrate);
    			}

  				last_maxrate = maxrate;
   			if (debug) {
   				System.out.println("\n" + ship.getName() + "\ndist=" + AstroUtil.toDegrees(dist)
  						+ " maxrate=" + AstroUtil.toDegrees(maxrate)
  						+ " angvel=" + AstroUtil.toDegrees(velvec.length())
  						+ " devband=" + AstroUtil.toDegrees(settings.devband)
  						+ " rateband=" + AstroUtil.toDegrees(settings.rateband)
  					);
   			}

   			// vv now contains our desired velocity
   			double vvl = vv.length();
	   		if ( /* vvl > maxrate && */vvl > 1e-15) // todo: const?
   			{
   				vv.scale(maxrate/vvl);
   			}

	   		// subtract current velocity
   			vv.sub(velvec);

				// kindler, gentler locked so the light bulb shows up right
   			locked = (dist < settings.devband*2);
	   	} else {
	   		vv.set(0,0,0);
//	   		vv.set(-velvec.x, -velvec.y, -velvec.z);
	   	}

   	}

		double l2 = vv.lengthSquared();
		if (Double.isNaN(l2))
		{
			if (debug)
				System.out.println("NaN: " + vv);
			ship.getShipWarningSystem().setWarning("GUID-NAN", "NaN: " + vv);
			return;
		}

		boolean doChange = (l2 > rateband*rateband);


		// now command the rcs component(s)

		last_err = vv.length();

		Vector3f str = new Vector3f();
		// vv is the desired moment
if (debug) {
	System.out.println("  vv = " + vv);
	System.out.println("rate = " + velvec);
}
		Vector3f thispwr = new Vector3f();

		it = rcscaps.iterator();
		while (it.hasNext())
		{
			AttitudeControlComponent rcscap = (AttitudeControlComponent)it.next();
			if (rcscap.isArmed())
			{
				if (doChange && vv.lengthSquared() > 1e-30) //todo: const
				{
					rcscap.getMaxMoment(thispwr);
					// scale by interval because we want to accelerate
					// that much before the next update
					if (rcscap instanceof PeriodicCapability) {
						float rcsint = ((PeriodicCapability)rcscap).getIntervalFloat();
						thispwr.scale((float)Math.sqrt(rcsint));
					}
					if (thispwr.lengthSquared() > 1e-30) //todo: const
					{
						thispwr.clampMin(MIN_MOMENT_VALUE); // avoid NaNs
						str.set((float)(vv.x/thispwr.x), (float)(vv.y/thispwr.y), (float)(vv.z/thispwr.z));
						rcscap.setStrength(str);
						if (debug) {
							System.out.println(rcscap + ": str=" + str);
							System.out.println("  vvrem=" + vv);
						}
						// subtract this impulse from total
						vv.x += thispwr.x*str.x;
						vv.y += thispwr.y*str.y;
						vv.z += thispwr.z*str.z;
					} else {
						str.set(0,0,0);
						rcscap.setStrength(str);
					}
				} else {
					str.set(0,0,0);
					rcscap.setStrength(str);
				}
			}
		}
		if (debug)
			System.out.println("vv rem = " + vv);

   	// if trans_flags, we can translate
   	if (trans_flags != 0)
   	{
   		doTranslate(rcscaps);
   	}
	}

	//

	/**
	  * Iterates thru all capabilities in 'rcscaps' and if they are
	  * an RCSCapability, calls setTranslateRCSParams on it.
	  */
	protected void doTranslate(List rcscaps)
	{
		Iterator it = rcscaps.iterator();
		while (it.hasNext())
		{
			AttitudeControlComponent rcscap = (AttitudeControlComponent)it.next();
			if (rcscap instanceof RCSCapability && rcscap.isArmed())
			{
				RCSCapability rcs = (RCSCapability)rcscap;
				Vector3f str = new Vector3f();
				str.x = Math.abs(trans_strength.x);
				str.y = Math.abs(trans_strength.y);
				str.z = Math.abs(trans_strength.z);
				rcs.setTranslateRCSParams(trans_flags, str);
			}
		}
	}

	//

	public double getDeviationAngle()
	{
		return last_dev;
	}

	public double getLastRCSPower()
	{
		return last_power;
	}

	public double getLastMaxRate()
	{
		return last_maxrate;
	}

	public double getLastError()
	{
		return last_err;
	}

	public boolean getLocked()
	{
		return locked;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(AttitudeController.class);

	static {
		prophelp.registerGetSet("targort", "TargetOrientation", Orientation.class);
		prophelp.registerGetSet("angvelbias", "AngVelBias", Vector3f.class);
		prophelp.registerGet("fixedort", "getFixedOrientation");
		prophelp.registerGet("ship", "getShip");
		prophelp.registerGet("devangle", "getDeviationAngle");
		prophelp.registerGet("power", "getLastRCSPower");
		prophelp.registerGet("lastmaxrate", "getLastMaxRate");
		prophelp.registerGet("error", "getLastError");
		prophelp.registerGet("locked", "getLocked");
		prophelp.registerGetSet("guidance", "GuidanceProgram", GuidanceProgram.class);
		prophelp.registerGetSet("settings", "Settings", AttitudeControllerSettings.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	//

	static boolean debug = false;


}
