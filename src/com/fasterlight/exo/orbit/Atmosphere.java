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
package com.fasterlight.exo.orbit;

/**
  * Describes the atmosphere of a planet (or whatever?)
  */
public interface Atmosphere
{
	/**
	  * @return Upper limit of atmosphere, in km
	  */
	public float getCeiling();
	/**
	  * @return Parameters at a given altitude above the surface (in km)
	  */
	public Params getParamsAt(float alt);

	/**
	  * Result class of Atmosphere.getParamsAt()
	  * May be cached! Do not tamper!
	  */
	public class Params
	{
		public float pressure; /* air pressure (kPa) */
		public float density; /* air density (kg/m^3) */
		public float temp; /* temperature (K) */
		public float airvel; /* air propogation velocity (m/s) */
		public float specheatratio;
//		public float viscos; /* kinematic coefficient of viscocity (m^2/s) */

		public String toString()
		{
			return "[d=" + density + ",p=" + pressure + ",T=" + temp + ",v=" + airvel + "]";
		}
	}

}
