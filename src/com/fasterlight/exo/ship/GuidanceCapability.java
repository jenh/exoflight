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

import com.fasterlight.game.Settings;
import com.fasterlight.spif.*;

/**
  * A GuidanceCapability controls one or more RCSCapabilities
  * and uses a AttitudeController (or subclass) to perform
  * attitude control.
  */
public abstract class GuidanceCapability
extends PeriodicCapability
{
	AttitudeControllerSettings settings[];
	int cursetting;

	static String DEFAULT_SETTING_0;
	static String DEFAULT_SETTING_1;

	static { initSettings(); }
	static void initSettings()
	{
		DEFAULT_SETTING_0 = Settings.getString("Guidance", "DefaultSetting-0", "5,0.05,0.2");
		DEFAULT_SETTING_1 = Settings.getString("Guidance", "DefaultSetting-1", "2,0.1,1.0");
	}

	//

	public GuidanceCapability(Module module)
	{
		super(module);
	}

	public void initialize2(Properties props)
	{
		super.initialize2(props);

		settings = new AttitudeControllerSettings[2];
		// maxrate, rateband, devband
		settings[0] = new AttitudeControllerSettings(props.getProperty("settings-0", DEFAULT_SETTING_0));
		settings[1] = new AttitudeControllerSettings(props.getProperty("settings-1", DEFAULT_SETTING_1));
	}

	//

	public int getNumSettings()
	{
		return settings.length;
	}

	public AttitudeControllerSettings getSettings()
	{
		return settings[cursetting];
	}

	public AttitudeControllerSettings getSetting(int i)
	{
		return settings[i];
	}

	public int getSettingIndex()
	{
		return cursetting;
	}

	public void setSettingIndex(int cursetting)
	{
		this.cursetting = cursetting;
	}

	public void setActive(boolean b)
	{
		if (b && !isActive())
		{
			SpaceShip ship = getShip();
			ship.getAttitudeController().setTargetOrientation(ship.getTelemetry().getOrientationFixed());
		}
		super.setActive(b);
	}

	public boolean notifyActivated()
	{
		SpaceShip ship = getShip();
		ship.setGuidanceCapability(this);
		return true;
	}

	public boolean notifyDeactivated()
	{
		// shut down the rcs so we don't SPIN!
		deactivateAllRCSCaps();
		return true;
	}

	public void doReact(ResourceSet react, ResourceSet product)
	{
		super.doReact(react, product);

		SpaceShip ship = getShip();
      if (ship == null)
	      return;

	   AttitudeController attctrl = ship.getAttitudeController();
	   if (attctrl != null)
	   {
	   	attctrl.setSettings(getSettings());
	   	attctrl.doRCS();
	   }
	}

	void deactivateAllRCSCaps()
	{
		Iterator it = getStructure().getCapabilitiesOfClass(RCSCapability.class).iterator();
		while (it.hasNext())
		{
			((RCSCapability)it.next()).setActive(false);
		}
	}

	protected void notifyNoSupply()
	{
		getShip().getShipWarningSystem().setWarning("GUID-NOPOWER", "No power for guidance");
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(GuidanceCapability.class);

	static {
		prophelp.registerGet("settings", "getSettings");
		prophelp.registerGetSet("settingidx", "SettingIndex", int.class);
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
