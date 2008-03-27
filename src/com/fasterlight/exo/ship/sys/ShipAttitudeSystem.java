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

import java.util.*;

import com.fasterlight.exo.orbit.Orientation;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.*;
import com.fasterlight.spif.*;
import com.fasterlight.vecmath.*;

/**
  * Encapsulates the attitude control system for a ship.
  * Handles setting reference modes, getting euler angles
  * and handling the RHC and THC.
  */
public class ShipAttitudeSystem
extends ShipSystem
implements PropertyAware
{
	public static final int REF_FIXED = 0;
	public static final int REF_LVLH  = 1;
	public static final int REF_PATH  = 2;
	public static final int REF_DOCK  = 3;

	public static final int RCS_AUTO       = 0;
	public static final int RCS_ATTHOLD    = 1;
	public static final int RCS_MANINHIBIT = 2;

	public static final int THROT_AUTO       = 0;
	public static final int THROT_MANUAL     = 1;

	// todo: consts
	public static final float DETENT_THRESHOLD = 0.02f;
	public static final float DETENT_THRESHOLD_SQR = DETENT_THRESHOLD*DETENT_THRESHOLD;

	private int refmode = REF_LVLH;
	private int rcsmanual = RCS_AUTO;
	private int throtmanual = THROT_AUTO;
	private boolean rcsdirect = false;

	private Vector3f rhc = new Vector3f(); // rotational hand controller
	private Vector3f thc = new Vector3f(); // translation hand controller

	private float RHC_SCALE = 1/32f;

	private String guidmode = null;

	//

	public ShipAttitudeSystem(SpaceShip ship)
	{
		super(ship);
	}

	public int getRCSManual()
	{
		return rcsmanual;
	}

	public void setRCSManual(int rcsmanual)
	{
		if (rcsmanual != this.rcsmanual)
		{
			this.rcsmanual = rcsmanual;
			rhc.set(1,1,1);
			setRotationController(new Vector3f());
			// if manual, set the target orientation to the current
			if (rcsmanual == RCS_ATTHOLD)
			{
				ship.getAttitudeController().setTargetOrientation(
					ship.getOrientation(getGame().time()));
			}
		}
	}

	public int getThrottleManual()
	{
		return throtmanual;
	}

	public void setThrottleManual(int throtmanual)
	{
		if (throtmanual != this.throtmanual)
		{
			this.throtmanual = throtmanual;
		}
	}

	public boolean getRCSDirect()
	{
		return rcsdirect;
	}

	public void setRCSDirect(boolean rcsdirect)
	{
		this.rcsdirect = rcsdirect;
		GuidanceCapability gcap = ship.getGuidanceCapability();
		if (gcap != null)
			gcap.setActive(!rcsdirect);
	}

	public int getReferenceMode()
	{
		return refmode;
	}

	public void setReferenceMode(int refmode)
	{
		this.refmode = refmode;
	}

	public Vector3d getEulerAngles()
	{
		switch (refmode)
		{
			case REF_FIXED:
				return getShipTelemetry().getEulerFixed();
			case REF_LVLH:
				return getShipTelemetry().getEulerPlanet();
			case REF_PATH:
				return getShipTelemetry().getEulerAirVel();
			default:
			case REF_DOCK:
				System.out.println("refmode " + refmode + " NYI");
				return new Vector3d();
		}
	}

	public Orientation getReferenceOrt()
	{
		switch (refmode)
		{
			case REF_FIXED:
				return new Orientation();
			case REF_LVLH:
				return getShipTelemetry().getPlanetRefOrientation();
			case REF_PATH:
				return getShipTelemetry().getAirRefOrientation();
			default:
			case REF_DOCK:
				System.out.println("refmode " + refmode + " NYI");
				return new Orientation();
		}
	}

	public double getPitch()
	{
		return getEulerAngles().x;
	}

	public double getYaw()
	{
		return getEulerAngles().y;
	}

	public double getRoll()
	{
		return getEulerAngles().z;
	}

	public boolean isGuidanceActive()
	{
		switch (rcsmanual)
		{
			case RCS_ATTHOLD:
				return false;
			case RCS_MANINHIBIT:
				return true;
			case RCS_AUTO:
			default:
				return (rhc.lengthSquared() < DETENT_THRESHOLD_SQR);
		}
	}

	static float maprange(float x, float smin, float dmin, float smax, float dmax)
	{
		boolean neg = (x < 0);
		if (neg)
			x = -x;
		if (x < smin)
			return 0;
		x = (x-smin)*(dmax-dmin)/(smax-smin) + dmin;
		x = Math.max(dmin, Math.min(dmax, x));
		return neg ? -x : x;
	}

	public void setRotationController(Vector3f rhc)
	{
		AttitudeController attctrl = ship.getAttitudeController();

		if (rcsdirect)
		{
			// set all armed RCS components
			List attctrlcaps = ship.getStructure().getCapabilitiesOfClass(AttitudeControlComponent.class);
			Iterator it = attctrlcaps.iterator();
	      while (it.hasNext())
   	   {
      	   AttitudeControlComponent attctrlcap = (AttitudeControlComponent)it.next();
      	   attctrlcap.setStrength(rhc);
			}
			return;
		}

		// if stick is currently at rest, don't do anything
		// this is required for attitude hold
		if (this.rhc.lengthSquared() < DETENT_THRESHOLD_SQR &&
			rhc.lengthSquared() < DETENT_THRESHOLD_SQR)
			return;

		this.rhc.set(rhc);
		this.rhc.clamp(-1,1);
		rhc = this.rhc;
		if (rcsmanual != RCS_MANINHIBIT)
		{
			AttitudeControllerSettings attset = attctrl.getSettings();
			Vector3f bias = new Vector3f(rhc);
			float dmin = attset.getRateDeadband();
			float dmax = attset.getMaxRate();
			bias.x = maprange(bias.x, DETENT_THRESHOLD, dmin, 1, dmax);
			bias.y = maprange(bias.y, DETENT_THRESHOLD, dmin, 1, dmax);
			bias.z = maprange(bias.z, DETENT_THRESHOLD, dmin, 1, dmax);
			attctrl.setAngVelBias(bias);
//			System.out.println(dmin + " " + dmax + " " + bias);
		}
	}

	public Vector3f getRotationController()
	{
		return new Vector3f(rhc);
	}

	public void setRotX(float x)
	{
		Vector3f v = getRotationController();
		v.x = x;
		setRotationController(v);
	}

	public void setRotY(float y)
	{
		Vector3f v = getRotationController();
		v.y = y;
		setRotationController(v);
	}

	public void setRotZ(float z)
	{
		Vector3f v = getRotationController();
		v.z = z;
		setRotationController(v);
	}

	//

	public Vector3f getTranslationController()
	{
		return new Vector3f(thc);
	}

	public void setTranslationController(Vector3f thc)
	{
		this.thc.set(thc);
		thc = this.thc;
		thc.clamp(-1,1);

		boolean nulled = (thc.lengthSquared() < DETENT_THRESHOLD_SQR);
		int flags=0;
		if (!nulled)
		{
			if (thc.x > DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_POS_X;
			else if (thc.x < -DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_NEG_X;
			if (thc.y > DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_POS_Y;
			else if (thc.y < -DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_NEG_Y;
			if (thc.z > DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_POS_Z_FAST;
			else if (thc.z < -DETENT_THRESHOLD)
				flags |= RCSCapability.TRANS_NEG_Z_FAST;
		}
		AttitudeController attctrl = ship.getAttitudeController();
		if (attctrl != null)
		{
			attctrl.setTranslating(flags, thc);
		}
	}

	public void setTransX(float x)
	{
		Vector3f v = getTranslationController();
		v.x = x;
		setTranslationController(v);
	}

	public void setTransY(float y)
	{
		Vector3f v = getTranslationController();
		v.y = y;
		setTranslationController(v);
	}

	public void setTransZ(float z)
	{
		Vector3f v = getTranslationController();
		v.z = z;
		setTranslationController(v);
	}


	public void setAutoThrottle(float t)
	{
		setThrottle(t, false);
	}

	public void setManualThrottle(float t)
	{
		setThrottle(t, true);
	}

	public void setThrottle(float t, boolean isManual)
	{
		if ( (throtmanual==THROT_MANUAL) == isManual)
		{
			java.util.List engcaps = ship.getStructure().getCapabilitiesOfClass(
				RocketEngineCapability.class);
			Iterator it = engcaps.iterator();
			while (it.hasNext())
			{
				RocketEngineCapability pcap = (RocketEngineCapability)it.next();
				if (pcap.isArmed())
					pcap.setThrottle(t);
			}
		}
	}

	protected Sequencer loadSequencer()
	{
		return null;
	}

	//

	public String getGuidanceMode()
	{
		return guidmode;
	}

	public void setGuidanceMode(String gmode)
	{
		// TODO: warning, guidance programs do not get executed immediately!
		// this may screw up some sequencer programs

//		if (gmode != guidmode && (gmode == null || !gmode.equals(guidmode)))
		{
			this.guidmode = gmode;
			Sequencer seq = ship.loadProgramVerbatim("guidance/" + gmode);
			seq.setTop(ship);
			seq.start();
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipAttitudeSystem.class);

	static {
		prophelp.registerGetSet("guidmode", "GuidanceMode", String.class);
		prophelp.registerGetSet("refmode", "ReferenceMode", int.class);
		prophelp.registerGet("eulerort", "getEulerAngles");
		prophelp.registerGet("refort", "getReferenceOrt");
		prophelp.registerGetSet("rcsmanual", "RCSManual", int.class);
		prophelp.registerGetSet("rcsdirect", "RCSDirect", boolean.class);
		prophelp.registerGet("pitch", "getPitch");
		prophelp.registerGet("yaw", "getYaw");
		prophelp.registerGet("roll", "getRoll");
		prophelp.registerSet("throttle", "setAutoThrottle", float.class);
		prophelp.registerSet("manthrottle", "setManualThrottle", float.class);
		prophelp.registerGetSet("throtmanual", "ThrottleManual", int.class);
		prophelp.registerSet("transx", "setTransX", float.class);
		prophelp.registerSet("transy", "setTransY", float.class);
		prophelp.registerSet("transz", "setTransZ", float.class);
		prophelp.registerSet("rotx", "setRotX", float.class);
		prophelp.registerSet("roty", "setRotY", float.class);
		prophelp.registerSet("rotz", "setRotZ", float.class);
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
