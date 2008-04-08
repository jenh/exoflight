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

import com.fasterlight.proctex.TexQuad;
import com.fasterlight.vecmath.Vector3d;

/**
  * Terrain should be represented in 2 square height fields,
  * one for w hemi and one for e.
  * Precision means the level of detail.
  * Precision = 0 means interpolate the topmost 4 points.
  * Each successive precision means 180/(1<<prec) degrees
  *   of resolution.
  * Units returned for height are km relative to a reference
  *   radius (not specified here).
  */
public interface ElevationModel
{
	public int getMinPrecision();
	public int getMaxPrecision();
	public float getMinDisplacement();
	public float getMaxDisplacement();
	public float getDisplacement(double lat, double lon, int precision);
	public void getNormal(double lat, double lon, int precision, Vector3d nml);
	public TexQuad getTexQuad(int x, int y, int level);
	public TexQuad getTexQuad(double x, double y, int level);
}
