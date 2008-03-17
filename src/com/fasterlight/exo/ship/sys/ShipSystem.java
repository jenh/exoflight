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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;

/**
  * An abstract base class for all SpaceShip systems.
  */
public abstract class ShipSystem
implements PropertyAware, Constants
{
	protected SpaceShip ship;
	protected Sequencer seq;

	//

	public ShipSystem(SpaceShip ship)
	{
		this.ship = ship;
	}

	protected Game getGame()
	{
		return ship.getUniverse().getGame();
	}

	protected Telemetry getShipTelemetry()
	{
		return ship.getTelemetry();
	}

	protected abstract Sequencer loadSequencer();

	public void activate()
	{
		seq = loadSequencer();
		if (seq == null)
			return;
		ship.setSequencer(seq);
		seq.start();
	}

	public void setActive(boolean a)
	{
		if (a == getActive())
			return;

		if (a)
			activate();
		else if (seq != null)
			seq.stop();
	}

	public boolean getActive()
	{
		return (seq != null && seq.isStarted());
	}

	public Sequencer getSequencer()
	{
		return seq;
	}

	public void reset(boolean b)
	{
		setActive(false);
		setActive(true);
	}


	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ShipSystem.class);

	static {
		prophelp.registerGetSet("active", "Active", boolean.class);
		prophelp.registerSet("reset", "reset", boolean.class);
		prophelp.registerGet("sequencer", "getSequencer");
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
