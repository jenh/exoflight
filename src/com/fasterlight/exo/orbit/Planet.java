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

import com.fasterlight.math.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * A planet-type thing.
  */
public class Planet extends DefaultUniverseThing
{
	double rotperiod, rot0; // time (s) per rotation
	double rotvel; // rads per second
	double axisra, axisdec; // axis angles (rad)
	float albedo, absorb;
	Atmosphere atmosphere;
	ElevationModel elevmodel;
	boolean gaseous;
	float visible_radius;
	double J2;

	Vector3d spinaxis; // normalized spin axis in XYZ
	Vector3d savec;
	Matrix3d mxyz2ijk, mijk2xyz; // conversion to/from XYZ and IJK

	//

	WindModel windmodel;

	class WindModel
	{
		Func1d wx, wy, wz;
		double scale = 0.003;
		WindModel(double freq)
		{
			wx = new NoiseFunc1d(1d / freq); // every 0.01 km, noise changes
			wy = new NoiseFunc1d(1d / freq);
			wz = new NoiseFunc1d(1d / freq);
		}
		Vector3d getWindVectorAtIJK(Vector3d ijk)
		{
			return new Vector3d(wx.f(ijk.x) * scale, wy.f(ijk.y) * scale, wz.f(ijk.z) * scale);
		}
	}

	//

	public Planet()
	{
		setRotationPeriodSecs(0);
		setSpinAxis(0, Math.PI / 2);
	}

	public Vector3d getSpinAxis()
	{
		return new Vector3d(spinaxis);
	}

	public void setSpinAxis(Vector2d vec)
	{
		setSpinAxis(vec.x, vec.y);
	}

	public void setSpinAxis(double ra, double dec)
	{
		// convert J2000 to ecliptic coordinates
		dec += Util.toRadians(90 - 67.43929f);
		this.axisra = ra;
		this.axisdec = dec;
		double sra = Math.sin(ra);
		double cra = Math.cos(ra);
		double sdec = Math.sin(dec);
		double cdec = Math.cos(dec);
		spinaxis = new Vec3d();
		spinaxis.set(sra * cdec, cra * cdec, sdec);
		Vector3d savec = new Vec3d(0, 0, 1);
		double dot = savec.dot(spinaxis);
		savec.cross(savec, spinaxis);
		savec.normalize();
		AxisAngle4d aa;
		double sl = savec.lengthSquared();
		if (!Double.isNaN(1 / sl))
			aa = new AxisAngle4d(savec, -Math.acos(dot));
		else
			aa = new AxisAngle4d(new Vec3d(0, 0, 1), 0);
		//System.out.println("RA=" + Util.toDegrees(ra) + ", dec=" + Util.toDegrees(dec) +
		//	", acos(dot)=" + Util.toDegrees(Math.acos(dot)));
		mxyz2ijk = new Matrix3d();
		mxyz2ijk.set(aa);
		mijk2xyz = new Matrix3d(mxyz2ijk);
		mijk2xyz.invert();
	}

	public Matrix3d getXYZ2IJKMatrix()
	{
		return mxyz2ijk;
	}

	public Matrix3d getIJK2XYZMatrix()
	{
		return mijk2xyz;
	}

	public void setRotation0(double rot0)
	{
		this.rot0 = rot0;
	}

	public double getRotation0()
	{
		return rot0;
	}

	public void setRotationPeriodDays(double rotperiod)
	{
		setRotationPeriodSecs(rotperiod * SECS_PER_DAY);
	}

	public double getRotationPeriodDays()
	{
		return getRotationPeriodSecs() / SECS_PER_DAY;
	}

	public void setRotationPeriodSecs(double rotperiod)
	{
		if (rotperiod == 0)
			this.rotvel = 0;
		else
			this.rotvel = Math.PI * 2 / rotperiod;
		this.rotperiod = rotperiod;
	}

	public double getRotationPeriodSecs()
	{
		return rotperiod;
	}

	/**
	  * Returns rotation of planet in radians
	  */
	public double getRotation(double time)
	{
		return time * rotvel + rot0;
	}

	public double getAngularVel()
	{
		return rotvel;
	}

	public void setTurbulenceMaxVel(double x)
	{
		if (windmodel != null)
		{
			windmodel.scale = x;
		}
	}

	public void setTurbulenceFrequency(double x)
	{
		if (windmodel == null)
		{
			windmodel = new WindModel(x);
		}
	}

	//

	// convert from heliocentric (XYZ) to geocentric (IJK)
	public void xyz2ijk(Vector3d pos)
	{
		mxyz2ijk.transform(pos);
	}

	// convert from geocentric (IJK) to heliocentric (XYZ)
	public void ijk2xyz(Vector3d pos)
	{
		mijk2xyz.transform(pos);
	}

	// convert from IJK to LLR (lat-long-radius)
	public void ijk2llr(Vector3d p, double time)
	{
		double r = p.length();
		double xx = p.x;
		double yy = p.y;
		double zz = p.z;
		double lon = Math.atan2(yy, xx);
		double lat = Math.atan2(zz, Math.sqrt(xx * xx + yy * yy));
		// limit lon to (0..pi*2)
		lon -= getRotation(time);
		lon = AstroUtil.fixAngle2(lon);
		p.set(lon, lat, r);
		//		System.out.println("geo2llr: " + pos + " -> " + v + " (t=" + time +")");
	}

	// convert from IJK to LLR (lat-long-radius)
	public void ijk2llr(Vector3d p)
	{
		double r = p.length();
		double xx = p.x;
		double yy = p.y;
		double zz = p.z;
		double lon = Math.atan2(yy, xx);
		double lat = Math.atan2(zz, Math.sqrt(xx * xx + yy * yy));
		// limit lon to (0..pi*2)
		lon = AstroUtil.fixAngle2(lon);
		p.set(lon, lat, r);
	}

	public void llr2ijk(Vector3d ll, double time)
	{
		ll.x += getRotation(time);
		llr2ijk(ll);
	}

	public void llr2ijk(Vector3d ll)
	{
		// x=long (RA), y=lat (Decl), z=rad
		double xx = ll.x;
		double yy = ll.y;
		double r = ll.z;
		double cx = Math.cos(xx);
		double sx = Math.sin(xx);
		double cy = Math.cos(yy);
		double sy = Math.sin(yy);
		xx = r * cy * cx;
		yy = r * cy * sx;
		double zz = r * sy;
		ll.set(xx, yy, zz);
	}

	// convert from geocentric to earth-fixed
	public Vector3d geo2llr(Vector3d pos, double time)
	{
		// convert helio to geo
		Vector3d p = new Vec3d(pos);
		xyz2ijk(p);
		ijk2llr(p, time);
		return p;
	}

	// convert from earth-fixed to geocentric
	public Vector3d llr2geo(Vector3d ll, double time)
	{
		Vector3d p = new Vec3d(ll);
		llr2ijk(p, time);
		ijk2xyz(p);
		return p;
	}

	/**
	  * Returns velocity of point at lat/long/rad
	  * in XYZ coord frame
	  */
	public Vector3d llr2vel2(Vector3d ll, double time)
	{
		Vector3d v = new Vec3d(ll);
		llr2ijk(v, time);
		ijk2velvec(v);
		ijk2xyz(v);
		return v;
	}

	/**
	  * Returns velocity of point at lat/long/rad
	  * in XYZ coord frame
	  */
	public Vector3d llr2vel(Vector3d ll, double time)
	{
		// x=long (RA), y=lat (Decl), z=rad
		double lon = ll.x + getRotation(time);
		double lat = ll.y;
		double r = ll.z;
		double cx = Math.cos(lon);
		double sx = Math.sin(lon);
		double cy = Math.cos(lat);
		double sy = Math.sin(lat);
		double rv = rotvel;
		double xx = -r * rv * cy * sx;
		double yy = r * rv * cy * cx;
		double zz = 0;
		Vector3d v = new Vec3d(xx, yy, zz);
		ijk2xyz(v);
		return v;
	}

	/**
	  * Converts AED (azimuth/elevation/z) to SEZ (south/east/z) coordinate frame
	  */
	public static Vector3d aed2sez(Vector3d ll)
	{
		double azimuth = ll.x;
		double elev = ll.y;
		double z = ll.z;
		double ce = Math.cos(elev);
		double se = Math.sin(elev);
		double ca = Math.cos(azimuth);
		double sa = Math.sin(azimuth);
		double ps = -z * ce * ca;
		double pe = z * ce * sa;
		double pz = z * se;
		Vector3d v = new Vec3d(ps, pe, pz);
		return v;
	}

	/**
	  * Given coordinate set in IJK, sets ijk equal to unit vector
	  * of velocity of point in IJK coords.
	  */
	public void ijk2unitvel(Vector3d ijk)
	{
		ijk.set(-ijk.y, ijk.x, 0);
		ijk.normalize();
	}

	/**
	  * Given coordinate set in IJK, sets ijk equal to
	  * velocity of point in IJK coords.
	  */
	public void ijk2velvec(Vector3d ijk)
	{
		ijk.set(-ijk.y, ijk.x, 0);
		ijk.scale(rotvel);
	}

	/**
	  * @deprecated
	  */
	private static void rotateVec(Vector3d vec, double lat, double lon)
	{
		double ct = Math.cos(lon);
		double st = Math.sin(lon);
		double cl = Math.cos(lat);
		double sl = Math.sin(lat);
		Matrix3d mat = new Matrix3d(sl * ct, -st, cl * ct, sl * st, ct, cl * st, -cl, 0, sl);
		mat.transform(vec);
	}

	/**
	  * @deprecated
	  */
	public void rotateVecByLL(Vector3d vec, Vector3d ll, double time)
	{
		rotateVec(vec, ll.y, ll.x + getRotation(time));
		ijk2xyz(vec);
	}

	public Orientation getOrientation(Vector3d _pos)
	{
		Vector3d vvel = new Vector3d(_pos);
		xyz2ijk(vvel);
		ijk2unitvel(vvel);
		// create the NORTH vector
		vvel.cross(_pos, vvel);
		// the orientation's directional vector should point NORTH,
		// and the UP vector should point to the sky
		return new Orientation(vvel, _pos);
	}

	public Orientation getOrientation(long tt)
	{
		Orientation ort = new Orientation();
		ort.set(getRotateMatrix(tt));
		return ort;
	}

	public Matrix3d getRotateMatrix(long tt)
	{
		double t = tt * (1d / TICKS_PER_SEC);
		double rot = getRotation(t);

		Matrix3d mat = new Matrix3d();
		mat.set(new AxisAngle4d(0, 0, 1, -rot));
		mat.mul(mat, mxyz2ijk);
		return mat;
	}

	// temperature stuff

	public Atmosphere getAtmosphere()
	{
		return atmosphere;
	}

	public void setAtmosphere(Atmosphere atmo)
	{
		this.atmosphere = atmo;
	}

	public Atmosphere.Params getAtmosphereParamsAt(Vector3d xyz)
	{
		if (atmosphere == null)
			return null;
		float alt = (float) (xyz.length() - getRadius());
		return atmosphere.getParamsAt(alt);
	}

	// old methods
	// todo: test, then deprecate

	public Vector3d getAirVelocity2(Vector3d pos, double time)
	{
		return llr2vel(geo2llr(pos, time), time);
	}

	public Vector3d getAirVelocity2(Vector3d pos)
	{
		return getAirVelocity(pos, 0);
	}

	public Vector3d getVelocityVector2(Vector3d pos)
	{
		// todo: what if rotper == 0?
		Vector3d v = getAirVelocity(pos);
		v.normalize();
		return v;
	}

	// new methods

	public Vector3d getAirVelocity(Vector3d pos, double time)
	{
		Vector3d v = new Vector3d(pos);
		xyz2ijk(v);
		ijk2velvec(v);
		ijk2xyz(v);
		return v;
	}

	public Vector3d getAirVelocity(Vector3d pos)
	{
		return getAirVelocity(pos, 0);
	}

	public Vector3d getVelocityVector(Vector3d pos)
	{
		Vector3d v = new Vector3d(pos);
		xyz2ijk(v);
		ijk2unitvel(v);
		ijk2xyz(v);
		return v;
	}

	public Vector3d getAirVelocityWithWind(Vector3d pos, double time)
	{
		Vector3d v = new Vector3d(pos);
		xyz2ijk(v);
		Vector3d ijk = new Vector3d(v);
		ijk2velvec(v);
		if (windmodel != null)
			v.add(windmodel.getWindVectorAtIJK(ijk));
		ijk2xyz(v);
		return v;
	}

	//

	public float getAvgTemperature(long time)
	{
		float Tsun = Constants.COBE_T0;
		UniverseThing sun = getParent();
		while (sun != null)
		{
			if (sun instanceof Star)
			{
				Tsun = ((Star) sun).getRadiantTemp();
				break;
			}
			sun = sun.getParent();
		}
		if (sun == null)
			return Tsun;
		Vector3d pos = getPosition(sun, time);
		float Rsun = (float) sun.getRadius();
		double dist = pos.length() - sun.getRadius();
		double x = Math.sqrt(1 - albedo - absorb / 2);
		return (float) (Tsun * Math.sqrt(Rsun * x / (2 * dist)));
	}

	//

	/**
	  * Converts orbit from XYZ to IJK coordinates
	  */
	public Conic xyz2ijk(Conic o)
	{
		StateVector sv = o.getStateVectorAtEpoch();
		Vector3d r0 = new Vec3d(sv.r);
		xyz2ijk(r0);
		Vector3d v0 = new Vec3d(sv.v);
		xyz2ijk(v0);
		return new Conic(r0, v0, o.getMu(), o.getInitialTime());
	}

	/**
	  * Converts orbit from IJK to XYZ coordinates
	  */
	public Conic ijk2xyz(Conic o)
	{
		StateVector sv = o.getStateVectorAtEpoch();
		Vector3d r0 = new Vec3d(sv.r);
		ijk2xyz(r0);
		Vector3d v0 = new Vec3d(sv.v);
		ijk2xyz(v0);
		return new Conic(r0, v0, o.getMu(), o.getInitialTime());
	}

	//

	public ElevationModel getElevationModel()
	{
		return elevmodel;
	}

	public void setElevationModel(ElevationModel elevmodel)
	{
		this.elevmodel = elevmodel;
	}

	public double getElevationAt(double lat, double lon)
	{
		return (elevmodel == null)
			? 0
			: elevmodel.getDisplacement(lat, lon, elevmodel.getMaxPrecision() + 1);
	}

	// get elev at an interal pos
	public double getElevationAt(Vector3d xyz, double time)
	{
		if (elevmodel == null)
			return 0;
		Vector3d v = new Vec3d(xyz);
		xyz2ijk(v);
		ijk2llr(v, time);
		return elevmodel.getDisplacement(v.y, v.x, elevmodel.getMaxPrecision() + 1);
	}

	// returns normal, in ijk coordinates
	public Vector3d getNormalAt(Vector3d xyz, double time)
	{
		Vector3d v = new Vec3d(xyz);
		if (elevmodel == null)
		{
			xyz2ijk(v);
			v.normalize();
			return v;
		}
		//	v.normalize();
		//	System.out.println("  v = " + v);
		xyz2ijk(v);
		ijk2llr(v, time);
		double lat = v.y;
		double lon = v.x;
		elevmodel.getNormal(v.y, v.x, elevmodel.getMaxPrecision() + 1, v);
		v.z *= getRadius() * Math.PI * 2;
		v.normalize();
		//	System.out.println("sez = " + v);
		rotateVecByLL(v, new Vector3d(lon, lat, 1), time);
		//	System.out.println("nml = " + v);
		return v;
	}

	public double getMinRadius()
	{
		return (elevmodel == null) ? getRadius() : getRadius() + elevmodel.getMinDisplacement();
	}

	public double getMaxRadius()
	{
		return (elevmodel == null) ? getRadius() : getRadius() + elevmodel.getMaxDisplacement();
	}

	public float getVisibleRadius()
	{
		return (visible_radius > 0) ? visible_radius : (float) getMaxRadius();
	}

	public void setVisibleRadius(float visrad)
	{
		this.visible_radius = visrad;
	}

	public boolean isGaseous()
	{
		return gaseous;
	}

	public void setGaseous(boolean gaseous)
	{
		this.gaseous = gaseous;
	}

	public double getJ2()
	{
		return J2;
	}

	public void setJ2(double J2)
	{
		this.J2 = J2;
	}

	/**
	  * Returns the distance to the horizon at a given
	  * distance from the viewer.
	  */
	public double getHorizonDist(double h)
	{
		return Math.sqrt(h * (h + 2 * getMinRadius()));
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(Planet.class);

	static {
		prophelp.registerSet("spinaxis", "setSpinAxis", Vector2d.class);
		prophelp.registerGetSet("rot0", "Rotation0", double.class);
		prophelp.registerGetSet("rotperiod", "RotationPeriodDays", double.class);
		prophelp.registerGetSet("rotperiodsecs", "RotationPeriodSecs", double.class);
		prophelp.registerGetSet("atmosphere", "Atmosphere", Atmosphere.class);
		prophelp.registerGetSet("elevmodel", "ElevationModel", ElevationModel.class);
		prophelp.registerSet("gaseous", "setGaseous", boolean.class);
		prophelp.registerGet("gaseous", "isGaseous");
		prophelp.registerGet("angularvel", "getAngularVel");
		prophelp.registerGetSet("visradius", "VisibleRadius", float.class);
		prophelp.registerGetSet("J2", "J2", double.class);
		prophelp.registerSet("turbulence_max", "setTurbulenceMaxVel", double.class);
		prophelp.registerSet("turbulence_freq", "setTurbulenceFrequency", double.class);
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
		try
		{
			prophelp.setProp(this, key, value);
		}
		catch (PropertyRejectedException e)
		{
			super.setProp(key, value);
		}
	}

}
