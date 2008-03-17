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

import com.fasterlight.exo.orbit.AstroUtil;
import com.fasterlight.util.Util;

/**
  * Simulates a life-support system (ECS).
  * Consumes resources (usually O2 or O2 + N2) and puts it in the
  * atmosphere of the spacecraft.
  * Also can scrub CO2.
  */
public class ECSCapability
extends PeriodicCapability
{
	protected ResourceSet supplyrate, emitrate;
	protected float scrubrate;
	protected float target_pressure;
	protected float pressure_deadband;

	static final Resource CO2_RES = Resource.getResourceByName("CO2");

	public ECSCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		supplyrate = new ResourceSet(props.getProperty("supplyrate", ""), getIntervalFloat()/3600f);
		emitrate = new ResourceSet(props.getProperty("emitrate", ""), getIntervalFloat()/3600f);
		scrubrate = Util.parseFloat(props.getProperty("co2scrub", "0"))*getIntervalFloat()/3600f;
		target_pressure = Util.parseFloat(props.getProperty("pressure", "34.45"));
		pressure_deadband = Util.parseFloat(props.getProperty("deadband", "0.10"));
	}

	public void doReact(ResourceSet react, ResourceSet prods)
	{
		Module m = getModule();

		// what do we take out of the atmosphere (CO2!)
		// make sure we have enuff capacity to store it
		ResourceSet atmo = m.getAtmosphere();
		float co2ratio = atmo.getAmountOf(CO2_RES)/atmo.mag();
		float scrubamt = co2ratio*scrubrate;

		if (scrubamt > 0 &&
			getSupply().getAmountOf(CO2_RES) + scrubamt < getCapacity().getAmountOf(CO2_RES))
		{
			float consume = m.consumeAtmosphere(CO2_RES, scrubamt, false);
			addSupply(new ResourceSet(CO2_RES, consume));
//			System.out.println("scrubamt = " + scrubamt + ", ratio=" + co2ratio);
		}

		float pressure = m.getPressure();
		float delta = target_pressure - pressure;
		if (Math.abs(delta) < pressure_deadband)
			return;

		// emit into the atmosphere
		ResourceSet emit = new ResourceSet(emitrate, AstroUtil.sign(delta));
		if (delta > 0)
		{
			if (request(supplyrate, ALL_OR_NONE).mag() == 0)
			{
				getShip().getShipWarningSystem().setWarning("LIFE-NOSUPPLY", "No supply of gases for life support!");
				return;
			}
		}
		m.addAtmosphere(emit);

	}

	protected void notifyNoSupply()
	{
		getShip().getShipWarningSystem().setWarning("LIFE-NOPOWER", "No power for life support");
	}

}
