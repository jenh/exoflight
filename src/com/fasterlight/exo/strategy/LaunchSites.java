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
package com.fasterlight.exo.strategy;

import java.io.IOException;
import java.util.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.SpaceBase;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.Vector3d;

public class LaunchSites
{
	public static List readLaunchSites(SpaceGame game)
	{
		try {
			INIFile ini = new CachedINIFile(
				ClassLoader.getSystemResourceAsStream("etc/launchsites.txt"));
			List basenames = ini.getSectionNames();
			List v = new ArrayList();
			Iterator it = basenames.iterator();
			while (it.hasNext())
			{
				String basename = (String)it.next();
				Properties props = ini.getSection(basename);
				SpaceBase base = getLaunchSite(game, basename, props);
				v.add(base);
			}
			return v;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException(ioe.toString());
		}
	}

	public static SpaceBase getLaunchSite(SpaceGame game, String name, Properties props)
	{
		String tmp = props.getProperty("ll");
		Vector3d llr = AstroUtil.getLLR(tmp);
		tmp = props.getProperty("parent", "Earth");
		Planet planet = (Planet)game.getBody(tmp);
		if (planet == null)
			throw new RuntimeException("Body \"" + tmp + "\" not found");
		double elev = planet.getElevationAt(llr.y,llr.x);
		elev = planet.getElevationAt(llr.y,llr.x);
		llr.z = planet.getRadius() + elev + 0.005; // 5 m (todo: const)
		SpaceBase base = new SpaceBase(game, planet, llr);
		base.setName(name);

		tmp = props.getProperty("modelname");
		if (tmp != null)
			base.setModelName(tmp);

		return base;
	}

}
