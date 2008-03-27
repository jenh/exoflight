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
  * A capability that represents a jet engine (or scramjet).
  * It extends the rocket engine capability, and adds adjustments
  * for free stream velocity, pressure and other considerations.
  * todo: fuel flow changes based on density
  */
public abstract class JetEngineCapability
extends RocketEngineCapability
{
   /**
	 * Fuel to air ratio
	 */
	protected float fuelratio;
	/**
	 * Air flow rate at density=1
	 */
//	protected float airflowrate;

//	protected float ETR;
//	protected float EPR;
//	protected float IPR;

	/**
	 * Abiabatic efficiency of nozzle
	 */
//	protected float nozzleEff;

	protected float last_pct_thrust;

	//

	public JetEngineCapability(Module module)
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
		super.initialize(props);

		fuelratio = Util.parseFloat(props.getProperty("fuelratio", "1"));
	}

/*	public ResourceSet getReactants()
	{
		return new ResourceSet(getMaximumReactants(), act_throttle);
	}*/


	//

	protected Perturbation getRocketPerturbation()
	{
		return new JetThrustPerturbation();
	}

	class JetThrustPerturbation
	implements Perturbation
	{
		/**
		  * Computes the mass at time 'time' and scales the thrust vector accordingly
		  */
		public void addPerturbForce(PerturbForce force, Vector3d r, Vector3d v,
			Orientation ort, Vector3d w, long time)
		{
			if ( getShip().getParent() instanceof Planet )
			{
				Planet planet = (Planet)getShip().getParent();
				if (planet.getAtmosphere() != null)
				{
					// get atmo params
					Atmosphere.Params params = planet.getAtmosphereParamsAt(r);
					if (params == null)
						return;

					// get free-stream velocity
					Vector3d airvel = planet.getAirVelocity(r);
					airvel.sub(v);

			   	// compute thrust vector & magnitude
		   		Vector3d f = getThrustVector();
			   	ort.transform(f);

					double V2 = airvel.lengthSquared(); // velocity ^ 2
					double V = Math.sqrt(V2);
/*
					float specheatratio = params.specheatratio;
					float mass = airflowrate*params.density; // mass flow rate
					float A2 = params.airvel*params.airvel/(1000*1000); // speed of sound ^ 2
					// compute temp at nozzle
					float Tt0 = (float)(params.temp*(1+0.5*(specheatratio-1)*V2/A2));
					float pt0 = (float)(params.pressure*Math.pow(Tt0/params.temp, specheatratio/(specheatratio-1)));

					float Tt2 = Tt0;
					float Tt8 = Tt2*ETR;
					float pt2 = pt0*IPR;
					float pt8 = pt2*EPR;
					float NPR = pt8/params.pressure;
					float EPR = getExitPressure()/pt0;
					float ETR = exittemp/Tt0;

					float pressureRatio = 1 - Math.pow(1/NPR, (specheatratio-fuelratio)/specheatratio);
					float Ve = Math.sqrt(2*params.specheat*nozzleEff*Tt8*pressureRatio);
*/
					float mass = exhmass1*params.density/fuelratio;
					last_pct_thrust = params.density*getThrottle();
				   float Ve = getExhaustVel();
					double thrust = (Ve*(1+fuelratio) - V)*mass;
					f.scale(thrust);
//System.out.println("mass=" + mass + ", thrust=" + thrust);

					// compute thrust position
					Vector3d cmofs = new Vector3d(getCMOffset());
					ort.transform(cmofs);
					cmofs.scale(1d/1000);
					force.addOffsetForce(f, cmofs);
				}
			}
		}
	}

	// hotspot
	public float getThrustAdjustment(Vector3d r)
	{
		return 0.0f;
	}

	public float getPctThrust()
	{
		return last_pct_thrust;
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(JetEngineCapability.class);

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
