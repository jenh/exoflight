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
package com.fasterlight.exo.newgui;

import com.fasterlight.exo.ship.*;
import com.fasterlight.glout.GLOWindow;

/**
  * Subclass of GLOWindow that implements handy functions
  * for getting the current ship, etc.
  */
public abstract class SpaceShipWindow
extends GLOWindow
implements ShipSelectable
{
	ShipSelectable shipsel;

	//

	public SpaceShip getSpaceShip()
	{
		if (shipsel != null)
			return shipsel.getSelectedShip();
		else
			return null;
	}

	public ShipSelectable getShipSelectable()
	{
		return shipsel;
	}

	public void setShipSelectable(ShipSelectable shipsel)
	{
		this.shipsel = shipsel;
	}

	public Structure getStructure()
	{
		if (getSpaceShip() != null)
			return getSpaceShip().getStructure();
		else
			return null;
	}

	public SpaceShip getSelectedShip()
	{
		return getSpaceShip();
	}

	public void setSelectedShip(SpaceShip s)
	{
		if (shipsel != null)
			shipsel.setSelectedShip(s);
	}
}
