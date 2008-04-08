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

import com.fasterlight.spif.PropertyHelper;

/**
  * A capability that can deploy another module, connecting it
  * to the rest of the structure via one of the module's link points.
  * Mainly for use with parachutes.
  */
public class DeployableCapability
extends Capability
{
	String depmodulename;
	Module depmodule;
	byte fromdir,todir,pointdir,updir;

	public DeployableCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		depmodulename = props.getProperty("module");
		fromdir = (byte)Module.connectCharToDir(props.getProperty("fromdir", "d").charAt(0));
		todir = (byte)Module.connectCharToDir(props.getProperty("dir", "u").charAt(0));
		pointdir = (byte)Module.connectCharToDir(props.getProperty("pointdir", "u").charAt(0));
		updir = (byte)Module.connectCharToDir(props.getProperty("updir", "n").charAt(0));
	}

	public void deploy()
	{
		SpaceShip ship = getShip();
		if (isDeployed())
		{
			ship.getShipWarningSystem().setWarning("MISC-DEPLOY",
				this + " already deployed");
			return;
		}

		if (depmodulename == null)
		{
			ship.getShipWarningSystem().setWarning("MISC-DEPLOY",
				"Could not deploy " + this + ", module not found: " + depmodulename);
			return;
		}

		if (getShip().isExploded())
		{
			ship.getShipWarningSystem().setWarning("MISC-DEPLOY",
				"Could not deploy " + this + ", vehicle destroyed");
			return;
		}

		if (getModule().getLink(todir) != null)
		{
			ship.getShipWarningSystem().setWarning("MISC-DEPLOY",
				"Could not deploy " + this + ", module blocked");
			return;
		}

		// do this so that Cowell won't wig out
		getShip().refreshTrajectory();

		// create the new module
		depmodule = new Module(getGame(), depmodulename);
		depmodule.setName(this.getName());

		// start the parachute opening
		depmodule.startParachute(0.0f);

		// mass has changed on module that deployed this thing
		getModule().adjustEmptyMass(-depmodule.getMass());

		// we have a new module, add it!
		getStructure().addModule(depmodule, this.getModule(),
			fromdir, todir, pointdir, updir); //todo: const dirs
	}

	public boolean isDeployed()
	{
		return getDeployed();
	}

	public boolean getDeployed()
	{
		return (depmodule != null);
	}

	public void setDeployed(boolean b)
	{
		if (b)
		{
			deploy();
		}
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(DeployableCapability.class);

	static {
		prophelp.registerGetSet("deployed", "Deployed", boolean.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	public static boolean debug = false;

}
