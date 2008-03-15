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

import java.util.Properties;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.*;

/**
  * The RCS's primary function is to control the attitude
  * of the ship.  It can also provide low delta-v translating
  * maneuvers.
  *
  * The RCS is just a propulsion unit; it must receive commands
  * from a guidance component (or manually) to be useful.
  *
  * This component models 4 RCS quads clustered around the
  * Z-axis at a given radius and Z-coordinate.
  *
  * This *is* affected by module orientation.
  *
  * todo: should be able to use translation when direct mode
  * todo: verify that cmd_strength, cur_strength, cmd_flags etc.
  *       all work as designed
  */
public abstract class RCSCapability
extends PropulsionCapability
implements AttitudeControlComponent
{
	public static final int ROT_POS_X = 0x2080;
	public static final int ROT_NEG_X = 0x8020;
	public static final int ROT_POS_Y = 0x0802;
	public static final int ROT_NEG_Y = 0x0208;
	public static final int ROT_POS_Z = 0x4040;
	public static final int ROT_NEG_Z = 0x1010;
	public static final int ROT_POS_Z_FAST = 0x4444;
	public static final int ROT_NEG_Z_FAST = 0x1111;

	public static final int TRANS_POS_X = 0x4010;
	public static final int TRANS_NEG_X = 0x1040;
	public static final int TRANS_POS_Y = 0x0104;
	public static final int TRANS_NEG_Y = 0x0401;
	public static final int TRANS_POS_Z = 0x0808;
	public static final int TRANS_NEG_Z = 0x0202;
	public static final int TRANS_POS_Z_FAST = 0x8888;
	public static final int TRANS_NEG_Z_FAST = 0x2222;

	public static final double ACCEL_THRESHOLD_SQR = 1e-18; // (min accel required for perturbation)^2

	protected transient float qradius, qz;
	protected transient float thrust_attack;
	protected transient float minThrustAmt;

	/**
	  * this is stuff used by the Perturbation object
	  * cur_flags is valid only after doReact() has been called
	  * the rest of these are valid after notifyActivated() and doPeriodic()
	  */
	protected int cur_flags;
	/**
	  * this is set by setRCSFlags()
	  */
	protected int cmd_flags;
	protected int nthrusters;

	/**
	  * The 'commanded' strength -- this can be set any time
	  */
	protected Vector3f cmd_strength = new Vector3f(1,1,1);
	protected Vector3f cmd_trans = new Vector3f(0,0,0);
	/**
	  * The 'current' strength -- this is updated with the value in
	  * cmd_strength when doPeriodic() is called
	  */
	protected Vector3f cur_strength = new Vector3f(0,0,0);
	protected Vector3f cur_trans = new Vector3f(0,0,0);

	protected Vector3f last_cm_offset = new Vector3f(); // in km


	protected PerturbForce lastpf = new PerturbForce();
	protected float pctthrust;

	protected boolean isTranslating;

	//

	public RCSCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		qradius = Util.parseFloat(props.getProperty("qradius", ""+getModule().getDimensions().x/2))/1000;
		qz = Util.parseFloat(props.getProperty("qz", "0"))/1000;
		thrust_attack = Util.parseFloat(props.getProperty("attack", "0.125"));
		minThrustAmt = Util.parseFloat(props.getProperty("minthrust", "0.10"));
	}

	public int getRCSFlags()
	{
		return cmd_flags;
	}

	public void setRCSFlags(int flags)
	{
		cmd_flags = flags;
		setActive(flags != 0);
	}

	public void setRCSParams(int flags, Vector3f strength)
	{
		isTranslating = false;
		cmd_strength.set(strength);
		setRCSFlags(flags);
	}

	/**
	  * We need this separate so we can set isTranslating,
	  * so that getThrusterFactor() works right. Bleah.
	  */
	public void setTranslateRCSParams(int flags, Vector3f strength)
	{
		isTranslating = true;
		strength.x = Math.max(minThrustAmt, Math.abs(strength.x));
		strength.y = Math.max(minThrustAmt, Math.abs(strength.y));
		strength.z = Math.max(minThrustAmt, Math.abs(strength.z));
		cmd_trans.set(strength);
		// be sure to disable opposite flags (may have been used to rotate)
		// or we will get nasty RCS behavior
//		int oppflags = getOppositeFlags(flags);
		setRCSFlags(cmd_flags | flags);
	}

	/**
	  * Gets thruster flags for opposite thrusters
	  */
	static int getOppositeFlags(int f)
	{
		// take each nibble and rotate 2 bits
		int t=0;
		for (int i=0; i<4; i++)
		{
			int x = (f>>>(i*4))&15;
			x = ((x<<2)|(x>>>2))&15;
			t |= (x<<(i*4));
		}
		return t;
	}

	/**
	  * This method sets nthrusters and pctthrust
	  * based on the value in 'newflags'
	  */
	void setup(int newflags)
	{
		nthrusters = 0;
		pctthrust = 0;
		for (int i=0; i<16; i++)
		{
			int bit = 1<<i;
			if ( (newflags&bit) != 0 )
			{
				nthrusters++;
				pctthrust += getThrusterFactor(i);
			}
		}
/*
		if (pctthrust < minThrustAmt)
		{
			pctthrust = 0;
			nthrusters = 0;
		}
*/
		if (debug)
			System.out.println("RCS pct=" + pctthrust);
	}

	public boolean isTranslating()
	{
		return isTranslating;
	}

	protected void updateMassFlowRate()
	{
		Vector3d tvec = getThrustVector();
		getStructure().setMassFlowRate(this, maxexhrate, exhaustv, tvec);
	}

	public boolean notifyActivated()
	{
		setup(cmd_flags);
		cur_flags = 0;
		if (debug)
			System.out.println(getName() + " activated, " + nthrusters + ", " + pctthrust);
		return (nthrusters > 0) && super.notifyActivated();
	}

	public boolean notifyDeactivated()
	{
		cur_flags = 0;
		pctthrust = 0;
		if (debug)
			 System.out.println(getName() + " deactivated");
		return super.notifyDeactivated();
	}

	public ResourceSet getReactants()
	{
		return new ResourceSet(super.getReactants(), pctthrust);
	}

	// These overriden from PropulsionCapability

	public boolean isBlocked()
	{
		return false;
	}

	protected Perturbation getRocketPerturbation()
	{
		return new RCSThrustPerturbation();
	}

	//

	class RCSThrustPerturbation
	implements Perturbation
	{
		/**
		  * Computes the mass at time 'time' and scales the thrust vector accordingly
		  */
		public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
			Orientation ort, Vector3d w, long time)
		{
			PerturbForce pf = new PerturbForce(lastpf);
			pf.transform(ort);
			force.add(pf);
		}
	}

	public boolean doPeriodic()
	{
		// setup commanded flags from current ones
		if (cmd_flags != cur_flags || !cmd_strength.equals(cur_strength) ||
			(isTranslating && !cmd_trans.equals(cur_trans)))
		{
			removePerturbation();
		}
		cur_strength.set(cmd_strength);
		cur_trans.set(cmd_trans);

		setup(cmd_flags);

		if (debug)
		   System.out.println("nthrusters = " + nthrusters + " str=" + cmd_strength);

		return (nthrusters > 0) && super.doPeriodic();
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		super.doReact(react, product);

		SpaceShip ship = getShip();
	   Trajectory traj = ship.getTrajectory();

		lastpf.clear();
		if (nthrusters == 0)
			return;

		last_cm_offset.set(getCMOffset());
		last_cm_offset.scale(1f/1000);

		addRCSForce(lastpf, cmd_flags);

		// if linear acceleration is almost null,
		// apply a rotational impulse instead of convering to
		// Cowell trajectory

		cur_flags = cmd_flags;
		addPerturbation();
	}

	/**
	  * Returns thrust position, in structure coords.
	  */
	public void getThrusterPosition(int i, Vector3f pos)
	{
		switch (i>>2)
		{
			case 0: pos.set(qradius,0,qz); break;
			case 1: pos.set(0,qradius,qz); break;
			case 2: pos.set(-qradius,0,qz); break;
			case 3: pos.set(0,-qradius,qz); break;
			default: throw new IllegalArgumentException("Thruster # must be 0-15, was " + i);
		}
		pos.add(last_cm_offset);
	}

	public void getThrusterDirection(int i, float t, Vector3f dir)
	{
		float xd=0,yd=0,zd=0;
		switch (i)
		{
			case 6:
			case 12:
				xd=-t; break;
			case 4:
			case 14:
				xd=t; break;
			case 0:
			case 10:
				yd=-t; break;
			case 2:
			case 8:
				yd=t; break;
			case 1:
			case 5:
			case 9:
			case 13:
				zd=-t; break;
			case 3:
			case 7:
			case 11:
			case 15:
				zd=t; break;
			default: throw new IllegalArgumentException("Thruster # must be 0-15, was " + i);
		}
		dir.set(xd,yd,zd);
	}

	public float getThrusterFactor(int i)
	{
		float s=0;

		if (isTranslating)
		{
			if (cur_trans.x>0 && ((1<<i) & (TRANS_POS_X|TRANS_NEG_X)) != 0)
				s += cur_trans.x;
			else if (cur_trans.y>0 && ((1<<i) & (TRANS_POS_Y|TRANS_NEG_Y)) != 0)
				s += cur_trans.y;
			else if (cur_trans.z>0 && ((1<<i) & (TRANS_POS_Z_FAST|TRANS_NEG_Z_FAST)) != 0)
				s += cur_trans.z;
		}
		if (((1<<i) & (ROT_POS_X|ROT_NEG_X)) != 0)
			s += cur_strength.x;
		else if (((1<<i) & (ROT_POS_Y|ROT_NEG_Y)) != 0)
			s += cur_strength.y;
		else if (((1<<i) & (ROT_POS_Z_FAST|ROT_NEG_Z_FAST)) != 0)
			s += cur_strength.z;

		return s;
	}

	/**
	  * The numbering scheme for RCS thrusters goes like this:
	  * bits 0-3 : thrust direction (right,up,left,down)
	  * bits 4-7 : thruster position (+X,+Y,-X,-Y)
	  */
	protected void addRCSForce(PerturbForce pf, int flags)
	{
		if (flags == 0 || !isRunning())
			return;
		Vector3f pos = new Vector3f();
		Vector3f dir = new Vector3f();
		float maxf = (float)getMaxForce();

		for (int i=0; i<16; i++)
		{
			if ( (flags&(1<<i)) != 0 )
			{
				getThrusterPosition(i, pos);
				getThrusterDirection(i, maxf*getThrusterFactor(i), dir);
/*				pos.x *= cur_strength.x;
				pos.y *= cur_strength.y;
				pos.z *= cur_strength.z;*/
				pf.addOffsetForce(dir, pos);
			}
		}
/*
		getMaxMoment(pos);
		System.out.println("max moment = " + pos);
		getMinMoment(pos);
		System.out.println("min moment = " + pos);
		System.out.println("CM ofs = " + last_cm_offset);
*/
	}

	// todo: this is maximum!
	public double getMaxForce()
	{
		return getExhaustVel()*maxexhrate;
	}

	//

	public void getMaxMoment(Vector3f moment)
	{
		getMoment(moment, true);
	}

	public void getMinMoment(Vector3f moment)
	{
		getMoment(moment, false);
	}

	public void setStrength(Vector3f strength)
	{
		strength.clamp(-1, 1);
		Vector3f str = new Vector3f();
		str.x = Math.abs(strength.x);
		str.y = Math.abs(strength.y);
		str.z = Math.abs(strength.z);
		int flags = getFlags2(strength, minThrustAmt);
		if (debug)
			System.out.println(this + ": 0x" + Integer.toString(flags,16) + " " + str);
		setRCSParams(flags, str);
	}

	private void getMoment(Vector3f moment, boolean max)
	{
		moment.set(0,0,0);
		if (!isArmed())
			return;

		addTorque(moment,
			max ? (ROT_POS_X|ROT_POS_Y|ROT_POS_Z) : (ROT_NEG_X|ROT_NEG_Y|ROT_NEG_Z));
		getModule().getOrientation().transform(moment);

		double m = getThing().getMass();
	 	Vector3d inertv = getThing().getStructure().getInertiaVector();
	 	moment.x = (float)(moment.x/(inertv.x*m));
	 	moment.y = (float)(moment.y/(inertv.y*m));
	 	moment.z = (float)(moment.z/(inertv.z*m));
	}

	private void addTorque(Vector3f T, int flags)
	{
		Vector3f pos = new Vector3f();
		Vector3f dir = new Vector3f();
		float maxf = (float)getMaxForce();

		for (int i=0; i<16; i++)
		{
			if ( (flags&(1<<i)) != 0 )
			{
				getThrusterPosition(i, pos);
				getThrusterDirection(i, maxf, dir);
				dir.cross(pos,dir); // pos x dir = T
				T.add(dir);
			}
		}
	}

	public void addRCSPower(Vector3d power)
	{
		Vector3f mm = new Vector3f();
		getMaxMoment(mm);
		power.x += mm.x;
		power.y += mm.y;
		power.z += mm.z;
	}

	// pctthrust is # of engines firing
	public float getPctThrust()
	{
		return pctthrust; // todo: is right?
	}

	protected void notifyNoSupply()
	{
		getShip().getShipWarningSystem().setWarning("RCS-NOSUPPLY", "No supply for RCS");
	}

	//

	public static int getFlags(Vector3f v, float factor)
	{
		float ax = Math.abs(v.x);
		float ay = Math.abs(v.y);
		float az = Math.abs(v.z);
		float m = factor;
		int flags = 0;
		if (ax>ay && ax>az && ax>m)
			flags |= (v.x>0) ? RCSCapability.ROT_POS_X : RCSCapability.ROT_NEG_X;
		else if (ay>ax && ay>az && ay>m)
			flags |= (v.y>0) ? RCSCapability.ROT_POS_Y : RCSCapability.ROT_NEG_Y;
		else if (az>m)
			flags |= (v.z>0) ? RCSCapability.ROT_POS_Z : RCSCapability.ROT_NEG_Z;
		return flags;
	}

	public static int getFlags2(Vector3f v, float factor)
	{
		float ax = Math.abs(v.x);
		float ay = Math.abs(v.y);
		float az = Math.abs(v.z);
		float m = factor;
		int flags = 0;
		if (ax>m)
			flags |= (v.x>0) ? RCSCapability.ROT_POS_X : RCSCapability.ROT_NEG_X;
		if (ay>m)
			flags |= (v.y>0) ? RCSCapability.ROT_POS_Y : RCSCapability.ROT_NEG_Y;
		if (az>m)
			flags |= (v.z>0) ? RCSCapability.ROT_POS_Z : RCSCapability.ROT_NEG_Z;
		return flags;
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(RCSCapability.class);

	static {
		prophelp.registerGetSet("rcsflags", "RCSFlags", int.class);
		prophelp.registerGet("pctthrust", "getPctThrust");
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

	//

	static boolean debug = false;

/*
	public static void main(String[] args)
	{
		System.out.println(Integer.toString(getOppositeFlags(0x8020),16));
	}
*/
}
