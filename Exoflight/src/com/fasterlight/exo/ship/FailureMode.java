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


/**
  * An abstract class that describes the way in which a
  * component can fail.  Components will subclass this
  * class to define their own failure modes.
  */
public abstract class FailureMode
{
	protected String ftype;
	protected int r;

	Capability.FailureEvent failevent;

	protected Capability cap;

	//

	public void set(String ftype, int r)
	{
		this.ftype = ftype;
		this.r = r;
	}

	public String getName()
	{
		return ftype;
	}

	public int getR()
	{
		return r;
	}

	public void setEvent(Capability.FailureEvent failevent, Capability cap)
	{
		this.failevent = failevent;
		this.cap = cap;
	}

	public void clearEvent()
	{
		if (failevent != null)
		{
			cap.getGame().cancelEvent(failevent);
			this.failevent = null;
			this.cap = null;
		}
	}

	public abstract void fail();
}

