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

import com.fasterlight.exo.ship.*;

public class solidpropulsion
extends RocketEngineCapability
{
	boolean hasFired = false;
	
	public solidpropulsion(Module module)
	{
		super(module);
	}

	public void setActive(boolean b)
	{
		if (b)
		{
			super.setActive(true);
			if (isActive())
				hasFired = true;
		}
		else
			; // can't deactivate a solid!
	}

	public void shutdown()
	{
		// TODO: we can't override this because removeModule()
		// depends on this (so we can detach a solid rocket part)
		super.shutdown();
	}
	
	public void setModule(Module m)
	{
		super.setModule(m);
		// we never shutdown!
		if (hasFired)
		{
			setActive(true);
		}
	}
	
	public String getTech()
	{
		return "SOLI";
	}
}
