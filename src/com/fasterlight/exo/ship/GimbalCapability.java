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
import com.fasterlight.spif.*;
import com.fasterlight.util.Util;
import com.fasterlight.vecmath.*;

/**
  * A gimbal creates moments by harnessing the power of other
  * PropulsionCapabilties.
  * Not affected by module orientation
  */
public class GimbalCapability
extends PerturbationCapability
implements AttitudeControlComponent
{
	float thrustratio;
	List propcaps = new ArrayList();

	Vector3f cmd_strength = new Vector3f();
	Vector3f cur_strength = new Vector3f();

	//

	public GimbalCapability(Module module)
	{
		super(module);
		setArmed(true);
	}

	public void initialize2(Properties props)
	{
		super.initialize2(props);

		thrustratio = Util.parseFloat(props.getProperty("ratio", "0"));
		String propcapstr = props.getProperty("propcaps", "");
		setPropCapsByString(propcapstr);
	}

	public void addPropulsionCapability(PropulsionCapability propcap)
	{
		propcaps.add(propcap);
	}

	public void removeAllPropulsionCaps()
	{
		propcaps.clear();
	}

	public void setPropCapsByString(String propcapstr)
	{
		removeAllPropulsionCaps();
		StringTokenizer st = new StringTokenizer(propcapstr, ";");
		while (st.hasMoreTokens())
		{
			String propname = st.nextToken();
			PropulsionCapability propcap = (PropulsionCapability)getModule().getCapabilityByName(propname);
			if (propcap == null)
				throw new PropertyRejectedException("Can't find component `" + propname + "' in " + getModule());
			addPropulsionCapability(propcap);
		}
	}

	//

	public boolean isBlocked()
	{
		return false;
	}

	protected Perturbation getRocketPerturbation()
	{
		return new Perturbation() {
			public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
				Orientation ort, Vector3d w, long time)
			{
				Vector3f tq = new Vector3f();
				if (getTorque(tq, true))
				{
					tq.x *= cur_strength.x;
					tq.y *= cur_strength.y;
					tq.z *= cur_strength.z;
					ort.transform(tq);
					force.T.x += tq.x;
					force.T.y += tq.y;
					force.T.z += tq.z;
				} else {
					setActive(false);
				}
			}
		};
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		super.doReact(react, product);

		removePerturbation();
		cur_strength.set(cmd_strength);
		addPerturbation();
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
		strength.clamp(-1,1);
		if (strength.lengthSquared() > 1e-5) {
			cmd_strength.set(strength);
			setActive(true);
		} else
			setActive(false);
	}

	private boolean getTorque(Vector3f torque, boolean max)
	{
		torque.set(0,0,0);
		if (!isArmed())
			return false;

		Vector3f v = new Vector3f();

		// go thru all prop caps and get their positions
		// with this we can compute torque
		int numcaps = 0;
		Iterator it = propcaps.iterator();
		while (it.hasNext())
		{
			PropulsionCapability propcap = (PropulsionCapability)it.next();
			if (propcap.isRunning())
			{
				float factor = (float)(propcap.getCurrentForce()*thrustratio);
				// nextver: thrust vector
				for (int i=0; i<1; i++)
				{
					v.set(1,1,0);
					v.cross(propcap.getCMOffset(), v);
					v.scale(Constants.M_TO_KM);
					v.x = Math.abs(v.x);
					v.y = Math.abs(v.y);
					v.z = Math.abs(v.z);
if (debug)
	System.out.println(propcap + ": v=" + v);
					torque.scaleAdd(factor, v, torque);
					numcaps++;
				}
			}
		}
		if (numcaps == 0)
			return false;
		else
			return true;
	}

	private void getMoment(Vector3f moment, boolean max)
	{
		if (!getTorque(moment, max))
			return;

		double m = getThing().getMass();
	 	Vector3d inertv = getThing().getStructure().getInertiaVector();
	 	moment.x = (float)(moment.x/(inertv.x*m));
	 	moment.y = (float)(moment.y/(inertv.y*m));
	 	moment.z = (float)(moment.z/(inertv.z*m));
if (debug)
	System.out.println(this + " moment = " + moment);
	}

	//

	static boolean debug = false;

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(GimbalCapability.class);

	static {
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
