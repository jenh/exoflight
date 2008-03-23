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

import java.util.Properties;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.game.Game;
import com.fasterlight.spif.PropertyHelper;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Represents a virtual camera on or near a spacecraft.
  *
  * BEWARE for getModule() may be null, and this causes all
  * sorts of problems :(  yes i am bad...
  */
public class CameraCapability
extends Capability
{
	UniverseThing refthing, target;
	Vec3f center = new Vec3f();
	float fov = (float)Util.toRadians(60);
	float maxfov = (float)Util.toRadians(120);
	float minfov = (float)Util.toRadians(1);
	float viewdist = 0.1f;
	Orientation viewort = new Orientation();
	boolean attrel = true; // relative to attitude of ref object
	boolean interior = false;

	float jitterScale;
	float jitterDecayRate = 0.5f;
	float jitterMax = 0.00025f; // 25 cm
	Vector3f last_force = new Vector3f();
	long last_jitter_time = Game.INVALID_TICK;

	//

	public CameraCapability(Module module)
	{
		super(module);
	}

	public void initialize(Properties props)
	{
		super.initialize(props);

		Vector3f dir = parseVector3f(props.getProperty("dir", "0,0,1"));
		dir.scale(-1);
		Vector3f up = parseVector3f(props.getProperty("up", "0,1,0"));
		viewort.set(dir, up);
		fov = (float)Util.toRadians( Util.parseFloat(props.getProperty("fov", "60")) );
		minfov = (float)Util.toRadians( Util.parseFloat(props.getProperty("minfov", "1")) );
		maxfov = (float)Util.toRadians( Util.parseFloat(props.getProperty("maxfov", "120")) );
		viewdist = Util.parseFloat(props.getProperty("viewdist", viewdist+""));
		attrel = "true".equals( props.getProperty("attrel", "true") );
		interior = "true".equals( props.getProperty("interior", "false") );
		jitterScale = Util.parseFloat(props.getProperty("jitterscale", "0"));
		jitterDecayRate = Util.parseFloat(props.getProperty("jitterdecay", ""+jitterDecayRate));
		jitterMax = Util.parseFloat(props.getProperty("jittermax", ""+jitterMax));
	}

	public Vector3f getViewOffset()
	{
		Module m = getModule();
		if (m != null)
		{
			Vector3f v = new Vector3f(getCMOffset());
			v.scale(-0.001f);
			return v;
		} else {
			return null;
		}
	}

	public boolean isInterior()
	{
		return interior;
	}

	public boolean isJittered()
	{
		return (jitterScale != 0);
	}

	public UniverseThing getReference()
	{
		return (refthing == null && getModule() != null) ? getThing() : refthing;
	}

	public void setReference(UniverseThing ref)
	{
		this.refthing = ref;
	}

	public UniverseThing getTarget()
	{
		return target;
	}

	public void setTarget(UniverseThing target)
	{
		this.target = target;
	}

	public Orientation getViewOrientation()
	{
		return viewort;
	}

	public void setViewOrientation(Orientation o)
	{
		viewort.set(o);
	}

	public Vec3f getCenter()
	{
		return center;
	}

	public void setCenter(Vec3f center)
	{
		this.center = center;
	}

	public float getFOV()
	{
		return fov;
	}

	public void setFOV(float fov)
	{
		this.fov = Math.max(minfov, Math.min(maxfov, fov));
	}

	public float getViewDistance()
	{
		return viewdist;
	}

	public void setViewDistance(float viewdist)
	{
		this.viewdist = viewdist;
	}

	public boolean getAttitudeLocked()
	{
		return attrel;
	}

	public void setAttitudeLocked(boolean attrel)
	{
		this.attrel = attrel;
	}

	public Orientation getOrientation()
	{
		Orientation ort;

		UniverseThing targ = getTarget();
		UniverseThing ref = getReference();
//		Game game = ref.getUniverse().getGame();

		if (ref != null)
		{
			if (targ != null && targ != ref)
			{
				Vector3d pos = ref.getTelemetry().getCenDistVec();
				ort = new Orientation(pos, viewort.getUpVector());
			}
			else if (attrel)
			{
				Orientation refort = ref.getTelemetry().getOrientationFixed();
				ort = new Orientation(viewort);
				ort.concat(refort);
			}
			else
			{
				Orientation refort = ref.getTelemetry().getPlanetRefOrientation();
				ort = new Orientation(viewort);
				ort.concat(refort);
			}
		} else {
			ort = new Orientation(viewort);
		}

		return ort;
	}

	public Matrix3f getOrientationMatrix()
	{
		return getOrientation().getMatrixf();
	}

	public Matrix3f getInverseOrientationMatrix()
	{
		Orientation ort = getOrientation();
		ort.invert();
		return ort.getMatrixf();
	}

	public Matrix3d getOrientationMatrixd()
	{
		return getOrientation().getMatrix();
	}

	public Matrix3d getInverseOrientationMatrixd()
	{
		Orientation ort = getOrientation();
		ort.invert();
		return ort.getMatrix();
	}

	public void zoom(float f)
	{
		setFOV(getFOV()/f);
	}

	public void closer(float f)
	{
		setViewDistance(getViewDistance()/f);
	}

	public String getDescription()
	{
		if (getModule() != null)
			return getModule().getName() + ": " + getName();
		else
			return getName();
	}

	//


	public float getJitterScale()
	{
		return jitterScale;
	}

	public void setJitterScale(float jitterscale)
	{
		this.jitterScale = jitterscale;
	}

	public float getJitterDecay()
	{
		return jitterDecayRate;
	}

	public void setJitterDecay(float jitterdecay)
	{
		this.jitterDecayRate = jitterdecay;
	}

	public float getJitterMax()
	{
		return jitterMax;
	}

	public void setJitterMax(float jittermax)
	{
		this.jitterMax = jittermax;
	}

	public Vector3f getJitterOfs()
	{
		if (!isJittered())
		{
			return null;
		}
		long tick = getGame().time();
		Vector3f accelvec = new Vector3f(getShip().getTelemetry().getAccelVec());
		last_force.sub(accelvec);

		float rate = (last_jitter_time == Game.INVALID_TICK) ?
			jitterDecayRate :
			jitterDecayRate*(tick-last_jitter_time)/TICKS_PER_SEC;
		if (rate > 1)
			rate = 1;
		last_jitter_time = tick;
		last_force.scale(jitterDecayRate);

		last_force.add(accelvec);
		accelvec.set(last_force);
		accelvec.scale(jitterScale);
		accelvec.clamp(-jitterMax, jitterMax);
/*
		double len2 = accelvec.lengthSquared();
		if (len2 > jitterMax*jitterMax)
			accelvec.scale((float)(jitterMax/Math.sqrt(len2)));
*/
//System.out.println("accelvec = " + accelvec);
		return accelvec;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(CameraCapability.class);

	static {
		prophelp.registerGetSet("ref", "Reference", UniverseThing.class);
		prophelp.registerGetSet("target", "Target", UniverseThing.class);
		prophelp.registerGetSet("fov", "FOV", float.class);
		prophelp.registerGetSet("viewdist", "ViewDistance", float.class);
		prophelp.registerGetSet("attlock", "AttitudeLocked", boolean.class);
		prophelp.registerGetSet("center", "Center", Vec3f.class);
		prophelp.registerGetSet("viewort", "ViewOrientation", Orientation.class);
		prophelp.registerSet("zoom", "zoom", float.class);
		prophelp.registerSet("closer", "closer", float.class);
		prophelp.registerGetSet("jitterscale", "JitterScale", float.class);
		prophelp.registerGetSet("jitterdecay", "JitterDecay", float.class);
		prophelp.registerGetSet("jittermax", "JitterMax", float.class);
	}

	public Object getProp(String key)
	{
		if (key.startsWith("%"))
			return new Float(getPercentRemaining(key.substring(1)));
		else
			return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	public static boolean debug = false;

}
