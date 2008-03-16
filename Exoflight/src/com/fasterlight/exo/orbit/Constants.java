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

import com.fasterlight.vecmath.Matrix3d;

/**
  * Constants
  * JGM-2
  * Vallado, pg. 94-96, appendix, various
  */
public interface Constants
{
	// fundamental constants
	public static final double EARTH_RAD = 6378.1363; // km
	public static final double mu_EARTH = 398600.4415; // km^3/(solar s)^2
	public static final double SOLAR_ANGVEL = 0.0000729211585530; // rad/solar sec
	public static final double GAUSSIAN_CONST = 0.01720209895;

	//

	public static final double GRAV_CONST = 6.6725985e-11; // N m^2 kg^-2
	public static final double GRAV_CONST_KM = GRAV_CONST/1e9; // N km^2 kg^-2

	public static final double AU_TO_KM = 149597870.691;

	public static final double SPEED_OF_LIGHT = 299792.458; // km/s
	public static final double LIGHT_YEAR_KM = 9.46053e12; // km

	public static final double DAYS_PER_YEAR = 365.25636053;
	public static final double SECS_PER_DAY = 86400;
	public static final double SECS_PER_YEAR = 86400*DAYS_PER_YEAR;

	public static final double TU_TO_SEC = Math.sqrt(Math.pow(EARTH_RAD,3)/mu_EARTH);
	public static final double AUTU_TO_KMSEC = AU_TO_KM/TU_TO_SEC; //???

	public static final double GEOSYNC_ALT = 35788; //km --todo:improve
	public static final double GEOSTAT_RAD = 42164; //km --todo:improve

	public static final long TICKS_PER_SEC = 1024;
	public static final long TICKS_PER_HOUR = TICKS_PER_SEC*3600;
	public static final long TICKS_PER_DAY = TICKS_PER_HOUR*24;

	// 1/1/2000 12:00 UT1
	public static final long JAVA_MSEC_J2000 = 946728000000l;
	public static final long JAVA_MSEC_2000 = JAVA_MSEC_J2000;
	public static final long EPOCH_2000 = 0l;
	public static final long INVALID_TICK = com.fasterlight.game.Game.INVALID_TICK;

	public static final double J2000 = 2451545.0;
	public static final double JULIAN_DAYS_PER_YEAR = 365.25;
	public static final double JULIAN_DAYS_PER_CENT = 36525;

	public static final float COBE_T0 = 2.726f; // kelvin, bkgnd temp of universe (K)
	public static final float SB_CONST = 5.669e-8f; // Stefan-Boltzmann const (W/m2-K4)
	public static final float BOLTZ_CONST = 1.3807e-23f; // Boltzmann const (J/K)
	public static final float UGC_KG = 287.26f; // J/kg/K, gas constant
	public static final float UGC_MOL = 8.314f; // J/mol/K, gas constant

	public static final float EARTH_MASS = 5.972e24f; // approx earth mass in kg
	public static final double EARTH_G = 9.807/1000; // big G at surface (kg/s^2)

	public static final float ATM_TO_NM2 = 101325f; // convert atmospheres to N/m^2
	public static final float ATM_TO_KPA = 101.325f; // convert atmospheres to kPa

	public static final float KM_TO_M = 1000;
	public static final float M_TO_KM = 1f/KM_TO_M;

	public static final Matrix3d FROM_B1950 = new Matrix3d(
		0.9999256794956877, -0.0111814832204662, -0.0048590038153592,
		0.0111814832391717,  0.9999374848933135, -0.0000271625947142,
		0.0048590037723143, -0.0000271702937440,  0.9999881946023742);

	public static final float KWH_TO_J = 3.6e6f;

	public static final float MACH_FACTOR = 0.3048006096f;

	//

	public static final String EXOFLIGHT_VERSION = "0.1";
	public static final String EXOFLIGHT_COPYRIGHT = "Copyright (c) 2000-2008 Steven E. Hugg";
}
