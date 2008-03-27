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
package com.fasterlight.exo.caps;

import java.util.Properties;

import com.fasterlight.exo.ship.*;

public class lifesup
extends PeriodicCapability
{
	protected ResourceSet target;
	protected ResourceSet emission;

	public lifesup(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);
		target = new ResourceSet(props.getProperty("target"));
	}

	public void doReact(ResourceSet react, ResourceSet prods)
	{
		Module m = getModule();
		// todo: don't make it so complex
		ResourceSet atmo = new ResourceSet(target, m.getVolume());
		atmo.sub(m.getAtmosphere());
//		System.out.println(atmo + "\t" + atmo.mag() + "\t" + target + "\t" + m.getPressure());
		if (atmo.mag() <= 0) //todo???
			return;
		m.addAtmosphere(prods);
	}

	public String getTech()
	{
		return "LSUP";
	}
}
