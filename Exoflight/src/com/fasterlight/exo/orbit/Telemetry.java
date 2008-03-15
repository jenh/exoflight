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

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.traj.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.Vector3d;

/**
  * Class used for returning various and sundry values
  * about a spacecraft/object.
  *
  * TODO: these values aren't necc. idempotent -- consider
  * if the GUI asks for a value, then something happens to
  * change that value (say, a throttle change) and then the
  * simulation asks for a value -- the sim will get the old
  * value that the GUI did, but it wouldn't if the GUI hadn't
  * asked for it ... see?
  */
public abstract class Telemetry
implements PropertyAware
{
	protected long t;

	UniverseThing thing;

	Conic _conic;
	KeplerianElements _params;
	double _parentrad;
	Vector3d _pos, _vel;
	Vector3d _ll;
	double _pos_l, _vel_l, _vvel, _hvel;
	Atmosphere _atm;
	Atmosphere.Params _atmres;
	Vector3d _airvel = new Vec3d();
	double _dragcoeff, _losarea;
	float _gndelev;
	boolean _atmsetup, _dragsetup, _lossetup, _gndsetup, _kepsetup;
	double _trueanom, _meananom;
	Orientation _ortf, _ortp, _orta, _ortpref, _ortaref;
	Vector3d _eulerf, _eulerp, _eulera;

	public Telemetry(UniverseThing thing)
	{
		this.thing = thing;
	}

	public UniverseThing getThing()
	{
		return thing;
	}

	public long getTime()
	{
		return t;
	}

	public void setTime(long t)
	{
		this.t = t;
		_conic = null;
		_params = null;
		_pos = null;
		_ll = null;
		_atmsetup = _lossetup = _dragsetup = _gndsetup = _kepsetup = false;
		_ortf = _ortp = _orta = _ortpref = _ortaref = null;
		_eulerf = _eulerp = _eulera = null;
	}

	// orbit values

	void setupMisc()
	{
		_parentrad = thing.getParent().getRadius();
	}

	void setupConic()
	{
		setupMisc();
		if (_conic == null)
		{
			_conic = UniverseUtil.getGeocentricConicFor(thing);
		}
	}

	void setupParams()
	{
		setupConic();
		if (_params == null && _conic != null)
		{
			_params = _conic.getElements_unsafe();
		}
	}

	void setupKepler()
	{
		setupParams();
		if (_params != null) {
			// todo: faster?
			_trueanom = _params.getTrueAnomalyAtTime(AstroUtil.tick2dbl(t));
			_meananom = _params.getMeanAnomalyAtTime(AstroUtil.tick2dbl(t));
		} else {
			_trueanom = Double.NaN;
			_meananom = Double.NaN;
		}
		_kepsetup = true;
	}

	void setupPosition()
	{
		setupMisc();
		if (_pos == null)
		{
			_pos = thing.getTrajectory().getPos(t);
			_vel = thing.getTrajectory().getVel(t);
			_pos_l = _pos.length();
			_vel_l = _vel.length();
			_vvel = _vel.dot(_pos)/_pos_l;
			// hv = sin(acos(vv/|v|)*|v|
			// hv = sqrt(1-(vv/|v|)^2)*|v|
			// hv = |v|^2 - vv^2
			_hvel = Math.sqrt(_vel_l*_vel_l - _vvel*_vvel);
		}
	}

	void setupLatLong()
	{
		setupMisc();
		if (_ll == null)
		{
			_ll = UniverseUtil.getLatLong(thing, t);
		}
	}

	void setupLOS()
	{
		if (!_lossetup)
		{
			_lossetup = true;
			SpaceGame game = thing.getUniverse().getGame();
			_losarea = UniverseUtil.getLOSArea(thing, game.getBody("Sun"), thing.getParent(), game.time());
		}
	}

	void setupAtmosphere()
	{
		if (!_atmsetup)
		{
			_atmsetup = true;
			_atm = null;
			_atmres = null;
			Trajectory traj = thing.getTrajectory();
			if (traj.getParent() instanceof Planet)
			{
				Planet planet = (Planet)traj.getParent();
				setupPosition();
				_airvel.set(planet.getAirVelocity(_pos, t*(1d/Constants.TICKS_PER_SEC)));
				_atm = planet.getAtmosphere();
				if (_atm != null)
				{
					double alt = getALT();
					if (alt < _atm.getCeiling())
					{
						_atmres = _atm.getParamsAt((float)alt);
					}
				}
			} else
				_airvel.set(0,0,0);
		}
	}

	// POSITION VALUES

	public double getCENDIST()
	{
		setupPosition();
		return _pos_l;
	}

	public Vector3d getCenDistVec()
	{
		setupPosition();
		return new Vec3d(_pos);
	}

	public double getVELOCITY()
	{
		setupPosition();
		return _vel_l;
	}

	public Vector3d getVelocityVec()
	{
		setupPosition();
		return new Vec3d(_vel);
	}

	public double getTANGVEL()
	{
		setupPosition();
		return _hvel;
	}

	public double getVERTVEL()
	{
		setupPosition();
		return _vvel;
	}

	public double getALT()
	{
		setupPosition();
		return _pos_l - _parentrad;
	}

	public double getLAT()
	{
		setupLatLong();
		return (_ll != null) ? _ll.y : Double.NaN;
	}

	public double getLONG()
	{
		setupLatLong();
		return (_ll != null) ? _ll.x : Double.NaN;
	}

	public Vector3d getLatLongVec()
	{
		setupLatLong();
		return (_ll != null) ? new Vec3d(_ll) : null;
	}

	public float getELEVATION()
	{
		if (_gndsetup)
			return _gndelev;

		_gndelev = 0;
		Trajectory traj = thing.getTrajectory();
		if (traj.getParent() instanceof Planet)
		{
			Planet planet = (Planet)traj.getParent();
			if (planet.getElevationModel() != null)
			{
				setupLatLong();
				if (_ll != null)
				{
					_gndelev = (float)planet.getElevationAt( _ll.y, _ll.x );
				}
			}
		}
		return _gndelev;
	}

	public double getALTAGL()
	{
		float gelev = getELEVATION();
		return getALT() - gelev;
	}


	// ORBIT VALUES

	public double getSEMIMAJOR()
	{
		setupConic();
		return (_conic != null) ? _conic.getSemiMajorAxis() : Double.NaN;
	}

	public double getSEMILATUS()
	{
		setupConic();
		return (_conic != null) ? _conic.getSemiLatusRectum() : Double.NaN;
	}

	public double getECCENT()
	{
		setupConic();
		return (_conic != null) ? _conic.getEccentricity() : Double.NaN;
	}

	public double getAPOAPSIS()
	{
		setupConic();
		return (_conic != null) ? _conic.getApoapsis()-_parentrad : Double.NaN;
	}

	public double getPERIAPSIS()
	{
		setupConic();
		return (_conic != null) ? _conic.getPeriapsis()-_parentrad : Double.NaN;
	}

	public double getINCL()
	{
		setupParams();
		return (_params != null) ? _params.i : Double.NaN;
	}

	public double getRAAN()
	{
		setupParams();
		return (_params != null) ? _params.O : Double.NaN;
	}

/**
  * @deprecated
  */
	public double getLONGASCNODE()
	{
		setupParams();
		return (_params != null) ? _params.O : Double.NaN;
	}

	public double getARGPERI()
	{
		setupParams();
		return (_params != null) ? _params.W : Double.NaN;
	}

	public double getTRUEANOM()
	{
		//todo :not correct when orbit traj
		setupKepler();
		return _trueanom;
	}

	public double getMEANANOM()
	{
		//todo :not correct when orbit traj
		setupKepler();
		return _meananom;
	}

	public double getPERIOD()
	{
		setupConic();
		return (_conic != null) ? _conic.getPeriod() : Double.NaN;
	}

	public Conic getConic()
	{
		setupConic();
		return (_conic != null) ? new Conic(_conic) : null;
	}

	// THING-SPECIFIC

	public double getMASS()
	{
		return thing.getMass();
	}

	public double getINFRAD()
	{
		return thing.getInfluenceRadius(t);
	}

	public double getRADIUS()
	{
		return thing.getRadius();
	}

	// MISC

	public double getGACCEL()
	{
		return getACCEL()/Constants.EARTH_G;
	}

	public double getGRAVACCEL()
	{
		if (thing.getParent() instanceof Planet)
		{
			setupPosition();
			double U = thing.getParent().getMass()*Constants.GRAV_CONST_KM;
			return -U/(_pos_l*_pos_l);
		} else {
			return 0;
		}
	}

	public Vector3d getGravAccelVec()
	{
		double acc = getGRAVACCEL();
		if (acc != 0)
		{
			Vector3d av = new Vec3d(_pos);
			av.scale(acc/_pos_l);
			return av;
		} else {
			return new Vec3d();
		}
	}

	// todo: doesn't always return correct
	public Vector3d getAccelVec()
	{
		Vector3d a; // this will be the acceleration
		setupPosition();

		Trajectory traj = thing.getTrajectory();
		if (traj instanceof CowellTrajectory)
		{
			PerturbForce pf = ((CowellTrajectory)traj).getLastPerturbForce();
			if (pf != null) {
				a = new Vec3d(pf.a);
				a.scaleAdd(1d/getMASS(), pf.f, a);
			} else
				a = new Vec3d();
		}
		else if (traj instanceof OrbitTrajectory)
		{
			double dt = t*(1d/Constants.TICKS_PER_SEC);
			a = ((OrbitTrajectory)traj).getConic().getAccelAtPos(_pos);
		}
		else
		{
			return new Vec3d();
		}

		// now compute centrifugal accel (v^2/r)
		a.add(getCentripetalAccel());

		return a;
	}

	// centripetal points in the direction of the position vector
	// that is, "up"
	// todo: no such thing as negative centripetal?
	public Vector3d getCentripetalAccel()
	{
		setupPosition();
		Vector3d v = new Vec3d(_pos);
		v.scale(_hvel*_hvel/(_pos_l*_pos_l));
		return v;
	}

	public double getACCEL()
	{
		return getAccelVec().length();
	}

	public double getVACCEL()
	{
		return getAccelVec().dot(_pos)/_pos_l;
	}

	public double getATMPRES()
	{
		setupAtmosphere();
		// todo
		return _atmres != null ? _atmres.pressure : Double.NaN;
	}

	public double getATMTEMP()
	{
		setupAtmosphere();
		return _atmres != null ? _atmres.temp : Double.NaN;
	}

	public double getATMDENS()
	{
		setupAtmosphere();
		return _atmres != null ? _atmres.density : Double.NaN;
	}

	public double getSPDSOUND()
	{
		setupAtmosphere();
		return _atmres != null ? _atmres.airvel/1000 : Double.NaN;
	}

	public double getTRUEVEL()
	{
		setupAtmosphere();
		return getAirRefVelocity().length();
	}

	public double getINDAIRSPEED()
	{
		return getTRUEVEL()*Math.sqrt(_atmres.density)*(1/Constants.ATM_TO_KPA);
	}

	// todo: cache better

	public Vector3d getAirRefVelocity()
	{
		Vector3d v = new Vec3d(getVelocityVec());
		v.sub(getAirVelocityVector());
		return v;
	}

	public Vector3d getAirRefVelocityPlanetRef()
	{
		Vector3d v = new Vec3d(getAirRefVelocity());
		getPlanetRefOrientation().invTransform(v);
		return v;
	}

	public Vector3d getAirRefVelocityShipRef()
	{
		Vector3d v = new Vec3d(getAirRefVelocity());
		getOrientationFixed().invTransform(v);
		return v;
	}

	public double getLATERALVEL()
	{
		return getAirRefVelocityPlanetRef().x;
	}

	public double getFORWARDVEL()
	{
		return getAirRefVelocityPlanetRef().z;
	}

	public Vector3d getAirVelocityVector()
	{
		setupAtmosphere();
		return new Vec3d(_airvel);
	}

	public double getMACH()
	{
		setupAtmosphere();
		return _atmres != null ? getTRUEVEL()/getSPDSOUND() :
			Double.NaN;
	}

	public double getADJMACH()
	{
		setupAtmosphere();
		return (_atmres == null || getATMDENS() < 1e-3) ?
			getVELOCITY()/Constants.MACH_FACTOR :
			getMACH();
	}

	// LINE-OF-SIGHT

	public double getLOS()
	{
		setupLOS();
		return _losarea > 0 ? 1 : 0;
	}

	public double getLOSAMT()
	{
		setupLOS();
		return _losarea;
	}

	// ORIENATION

	public Orientation getOrientationFixed()
	{
		if (_ortf == null)
		{
			_ortf = thing.getOrientation(t);
		}
		return new Orientation(_ortf);
	}

	public Orientation getOrientationPlanet()
	{
		if (_ortp == null)
		{
			_ortp = new Orientation(getOrientationFixed());
			_ortp.concatInverse(getPlanetRefOrientation());
		}
		return new Orientation(_ortp);
	}

	public Orientation getOrientationAir()
	{
		if (_orta == null)
		{
			_orta = new Orientation(getOrientationFixed());
			_orta.concatInverse(getAirRefOrientation());
		}
		return new Orientation(_orta);
	}

	public Orientation getPlanetRefOrientation()
	{
		if (_ortpref == null)
		{
			setupPosition();
			if (thing.getParent() instanceof Planet)
			{
				_ortpref = ((Planet)thing.getParent()).getOrientation(_pos);
			} else {
				_ortpref = new Orientation();
			}
		}
		return new Orientation(_ortpref);
	}

	public Orientation getAirRefOrientation()
	{
		if (_ortaref == null)
		{
			Vector3d v = getAirRefVelocity();
			_ortaref = new Orientation(v, _pos);
		}
		return new Orientation(_ortaref);
	}

	public Vector3d getEulerFixed()
	{
		if (_eulerf == null)
		{
			_eulerf = getOrientationFixed().getEulerPYR();
		}
		return new Vec3d(_eulerf);
	}

	public Vector3d getEulerPlanet()
	{
		if (_eulerp == null)
		{
			_eulerp = getOrientationPlanet().getEulerPYR();
		}
		return new Vec3d(_eulerp);
	}

	public Vector3d getEulerAirVel()
	{
		if (_eulera == null)
		{
			_eulera = getOrientationAir().getEulerPYR();
		}
		return new Vec3d(_eulera);
	}

	public double getFixedPitch()
	{
		return getEulerFixed().x;
	}

	public double getFixedYaw()
	{
		return getEulerFixed().y;
	}

	public double getFixedRoll()
	{
		return getEulerFixed().z;
	}

	public double getPlanetPitch()
	{
		return getEulerPlanet().x;
	}

	public double getPlanetYaw()
	{
		return getEulerPlanet().y;
	}

	public double getPlanetRoll()
	{
		return getEulerPlanet().z;
	}

	//

	private double timesToDouble(double[] tarr)
	{
		Game game = thing.getUniverse().getGame();
		long t = AstroUtil.getNearestTime(tarr, game.time());
		if (t == Constants.INVALID_TICK)
			return Double.NaN;
		return (t-game.time()) * (1d/Constants.TICKS_PER_SEC);
	}

	public double getTimeUntilPeriapsis()
	{
		Conic o = getConic();
		if (o == null)
			return Double.NaN;
		return timesToDouble( o.getTimesAtRadius(o.getPeriapsis()) );
	}

	public double getTimeUntilApoapsis()
	{
		Conic o = getConic();
		if (o == null)
			return Double.NaN;
		return timesToDouble( o.getTimesAtRadius(o.getApoapsis()) );
	}

	public double getTimeUntilCrash()
	{
		Conic o = getConic();
		if (o == null)
			return Double.NaN;
		return timesToDouble( o.getTimesAtRadius(thing.getParent().getRadius()) );
	}

	public double getTimeUntilExit()
	{
		Conic o = getConic();
		if (o == null)
			return Double.NaN;
		Game game = thing.getUniverse().getGame();
		return timesToDouble( o.getTimesAtRadius(thing.getParent().getInfluenceRadius(game.time())) );
	}

	// PROPERTY AWARE

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	static PropertyHelper prophelp = new PropertyHelper(Telemetry.class);

	static {
		prophelp.registerGet("cendist", "getCENDIST");
		prophelp.registerGet("position", "getCENDIST");
		prophelp.registerGet("velocity", "getVELOCITY");
		prophelp.registerGet("tangvel", "getTANGVEL");
		prophelp.registerGet("vertvel", "getVERTVEL");
		prophelp.registerGet("alt", "getALT");
		prophelp.registerGet("lat", "getLAT");
		prophelp.registerGet("long", "getLONG");
		prophelp.registerGet("altagl", "getALTAGL");
		prophelp.registerGet("elevation", "getELEVATION");
		prophelp.registerGet("semimajor", "getSEMIMAJOR");
		prophelp.registerGet("semilatus", "getSEMILATUS");
		prophelp.registerGet("eccent", "getECCENT");
		prophelp.registerGet("apoapsis", "getAPOAPSIS");
		prophelp.registerGet("periapsis", "getPERIAPSIS");
		prophelp.registerGet("incl", "getINCL");
		prophelp.registerGet("longascnode", "getLONGASCNODE");
		prophelp.registerGet("raan", "getRAAN");
		prophelp.registerGet("argperi", "getARGPERI");
		prophelp.registerGet("trueanom", "getTRUEANOM");
		prophelp.registerGet("period", "getPERIOD");
		prophelp.registerGet("mass", "getMASS");
		prophelp.registerGet("infrad", "getINFRAD");
		prophelp.registerGet("radius", "getRADIUS");
		prophelp.registerGet("accel", "getACCEL");
		prophelp.registerGet("gravaccel", "getGRAVACCEL");
		prophelp.registerGet("vaccel", "getVACCEL");
		prophelp.registerGet("gaccel", "getGACCEL");
		prophelp.registerGet("atmtemp", "getATMTEMP");
		prophelp.registerGet("atmpres", "getATMPRES");
		prophelp.registerGet("atmdens", "getATMDENS");
		prophelp.registerGet("spdsound", "getSPDSOUND");
		prophelp.registerGet("mach", "getMACH");
		prophelp.registerGet("adjmach", "getADJMACH");
		prophelp.registerGet("truevel", "getTRUEVEL");
		prophelp.registerGet("los", "getLOS");
		prophelp.registerGet("losamt", "getLOSAMT");
		prophelp.registerGet("ort", "getOrientationFixed");
		prophelp.registerGet("ortfixed", "getOrientationFixed");
		prophelp.registerGet("ortplanet", "getOrientationPlanet");
		prophelp.registerGet("ortplanetref", "getPlanetRefOrientation");
		prophelp.registerGet("eulerfixed", "getEulerFixed");
		prophelp.registerGet("eulerplanet", "getEulerPlanet");
		prophelp.registerGet("fpitch", "getFixedPitch");
		prophelp.registerGet("fyaw", "getFixedYaw");
		prophelp.registerGet("froll", "getFixedRoll");
		prophelp.registerGet("ppitch", "getPlanetPitch");
		prophelp.registerGet("pyaw", "getPlanetYaw");
		prophelp.registerGet("proll", "getPlanetRoll");
		prophelp.registerGet("airvelvec", "getAirVelocityVector");
		prophelp.registerGet("airvelvecref", "getAirRefVelocity");
		prophelp.registerGet("airvelvecplanetref", "getAirRefVelocityPlanetRef");
		prophelp.registerGet("airvelvecshipref", "getAirRefVelocityShipRef");
		prophelp.registerGet("lateralvel", "getLATERALVEL");
		prophelp.registerGet("forwardvel", "getFORWARDVEL");
		prophelp.registerGet("llrvec", "getLatLongVec");
		prophelp.registerGet("timetoperi", "getTimeUntilPeriapsis");
		prophelp.registerGet("timetoapo", "getTimeUntilApoapsis");
		prophelp.registerGet("timetocrash", "getTimeUntilCrash");
		prophelp.registerGet("timetoexit", "getTimeUntilExit");
		prophelp.registerGet("posvec", "getCenDistVec");
		prophelp.registerGet("velvec", "getVelocityVec");
	}

	/**********/

	public double getValue(String id)
	{
		return PropertyUtil.toDouble(getProp(id));
	}

	public String getValueStr(String id)
	{
		// todo: add units
		double d = getValue(id);
		return Double.isNaN(d) ? "." : AstroUtil.format(d);
	}

	// UNITS CONVERSIONS

	public String format_(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.format(x);
	}

	public String format_(double x, String units)
	{
		return Double.isNaN(x) ? "." : AstroUtil.format(x) + " " + units;
	}

	public String format_km(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toDistance(x);
	}

	public String format_km_s(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toDistance(x) + "/s";
	}

	public String format_km_s_2(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toDistance(x) + "/s^2";
	}

	public String format_rad(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toDegrees(x);
	}

	public String format_deg(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.formatDegrees(x);
	}

	public String format_kg(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toMass(x);
	}

	public String format_s(double x)
	{
		return Double.isNaN(x) ? "." : AstroUtil.toDuration(x);
	}

}
