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

import com.fasterlight.game.Settings;
import com.fasterlight.math.Func1d;
import com.fasterlight.spif.*;

/**
  * An Atmosphere implementation that uses Func1d.
  */
public class DefaultAtmosphere implements Atmosphere, PropertyAware
{
	private float ceiling;
	private Func1d densityFunc;
	private Func1d temprFunc;
	private float specHeatRatio = 1.44f; // G
	private float molMass = 0.0288f; // kg/mol

	private Atmosphere.Params cached_params;
	private float cached_alt = Float.NaN;

	public static float ATM_RESOLUTION =
		Settings.getFloat("Atmosphere", "Resolution", 1e-4f);
	// 0.1 m

	//

	/**
	  * @return Parameters at a given altitude above the surface (in km)
	  */
	public Atmosphere.Params getParamsAt(float alt)
	{
		alt = Math.min(alt, getCeiling());
		alt = (int) (alt / ATM_RESOLUTION) * ATM_RESOLUTION;
		if (alt == cached_alt)
		{
			return cached_params;
		}

		Atmosphere.Params res = new Atmosphere.Params();
		double dens = densityFunc.f(alt);
		double tempr = temprFunc.f(alt);
		// need pressure
		double pres = dens * (Constants.UGC_KG / 1000) * tempr;
		// found density & temperature, now find speed of sound
		double spdsound =
			Math.sqrt(specHeatRatio * Constants.UGC_MOL * tempr / molMass);

		res.density = (float) dens;
		res.temp = (float) tempr;
		res.airvel = (float) spdsound;
		res.pressure = (float) pres;
		res.specheatratio = specHeatRatio;

		cached_params = res;
		cached_alt = alt;
		return res;
	}

	/**
	  * @return Upper limit of atmosphere, in km
	  */
	public float getCeiling()
	{
		return ceiling;
	}

	public void setCeiling(float ceil)
	{
		this.ceiling = ceil;
	}

	public float getSpecificHeatRatio()
	{
		return specHeatRatio;
	}

	public void setSpecificHeatRatio(float specheat)
	{
		this.specHeatRatio = specheat;
	}

	public Func1d getDensityFunc()
	{
		return densityFunc;
	}

	public void setDensityFunc(Func1d densityFunc)
	{
		this.densityFunc = densityFunc;
	}

	public Func1d getTemperatureFunc()
	{
		return temprFunc;
	}

	public void setTemperatureFunc(Func1d temprFunc)
	{
		this.temprFunc = temprFunc;
	}

	public boolean isValid()
	{
		return (densityFunc != null) && (temprFunc != null);
	}

	// PROPERTIES

	private static PropertyHelper prophelp =
		new PropertyHelper(DefaultAtmosphere.class);

	static {
		prophelp.registerGetSet("ceiling", "Ceiling", float.class);
		prophelp.registerGetSet(
			"specheatratio",
			"SpecificHeatRatio",
			float.class);
		prophelp.registerGetSet("densityfunc", "DensityFunc", Func1d.class);
		prophelp.registerGetSet("temprfunc", "TemperatureFunc", Func1d.class);
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

}
