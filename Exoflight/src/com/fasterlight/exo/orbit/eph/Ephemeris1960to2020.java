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
package com.fasterlight.exo.orbit.eph;


public class Ephemeris1960to2020
extends CompositeEphemeris
{
	public Ephemeris1960to2020()
	{
		addEphemeris(new ProxyEphemeris(
			"eph/de405-1960.ser", 2436912.5, 2444240.5, 0xffe));
		addEphemeris(new ProxyEphemeris(
			"eph/de405-1980.ser", 2444208.5, 2451568.5, 0xffe));
		addEphemeris(new ProxyEphemeris(
			"eph/de405-2000.ser", 2451536.5, 2458864.5, 0xffe));
		addEphemeris(new ProxyEphemeris(
			"eph/de405-2020.ser", 2458832.5, 2466160.5, 0xffe));
	}
}
