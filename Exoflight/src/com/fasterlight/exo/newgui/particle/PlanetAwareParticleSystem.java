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
package com.fasterlight.exo.newgui.particle;

import com.fasterlight.vecmath.*;

/**
  * A particle system that "sublimates" smoke when it dies
  * or when it hits the planet surface.
  * todo: add property settings
  */
public class PlanetAwareParticleSystem
extends ParticleSystem
{
	float planetrad, planetrad2;
	Vector3f cendist = new Vector3f();
	ParticleSystem sublime_psys;

	long sub_lifetime = 60000;
	float sub_scale = 2;
	float sub_sizeinc = 0.005f/1000;
	float sub_vel = 0.03f/1000;
	int sub_maxparts = 2500;
	float sub_damping = 0.03f;

	Matrix3f xform;
	int intseq;
	int SUBLIMATE_FREQ = 127;
	int MAKESMOKE_FREQ = 63;
	/**
	  * Do we emit a smoke trail?
	  */
	boolean doSmoke = false;
	/**
	  * Do particles "skate" along the ground?
	  */
	boolean doBounce = true;

	//

	public PlanetAwareParticleSystem(int maxparts)
	{
		super(maxparts);
	}

	public boolean isDone()
	{
		return super.isDone() && (sublime_psys == null || sublime_psys.isDone());
	}

	void setPlanetRadius(float rad)
	{
		planetrad = rad;
		planetrad2 = rad*rad;
	}

	void setCenDist(Vector3d v)
	{
		cendist.set(v);
	}

	void setCenDist(Vector3f v)
	{
		cendist.set(v);
	}

	void setXformMatrix(Matrix3f xform)
	{
		this.xform = xform;
	}

	protected ParticleSystem newSublimatedParticleSystem()
	{
		ParticleSystem psys = new ParticleSystem(sub_maxparts);
		psys.setTime(time());
		psys.options |= (psys.FADE | psys.DAMPING);
		psys.damping = sub_damping;
		psys.fadeTime = (int)sub_lifetime;
		return psys;
	}

	public ParticleSystem getSublimatedParticleSystem()
	{
		if (sublime_psys == null)
		{
			sublime_psys = newSublimatedParticleSystem();
		}
		return sublime_psys;
	}

	void addSublimatedParticle(YodaParticle p, float x, float y, float z,
		boolean hitGround)
	{
		// only do it once every so often

		getSublimatedParticleSystem();

		{
			Vector3f pos = new Vector3f(x,y,z);
			Vector3f vel = new Vector3f(
				(rnd.nextFloat()-0.5f)*sub_vel,
				(rnd.nextFloat()-0.5f)*sub_vel,
				(rnd.nextFloat()-0.5f)*sub_vel);
			if (doBounce && hitGround)
			{
				// cancel out vertical velocity
				Vector3f velvec = new Vector3f(p.vx,p.vy,p.vz);
				float f = velvec.dot(pos)/pos.lengthSquared();
				vel.x += p.vx - pos.x*f;
				vel.y += p.vy - pos.y*f;
				vel.z += p.vz - pos.z*f;
			}
			if (xform != null)
			{
				xform.transform(pos);
				xform.transform(vel);
			}
			float sc = p.sc*sub_scale;
			float dsc = sub_sizeinc;
			long t0 = time();
			long t1 = t0 + (long)(rnd.nextFloat()*sub_lifetime);
			sublime_psys.addParticle(pos.x,pos.y,pos.z,vel.x,vel.y,vel.z,sc,dsc,t0,t1);
		}
	}

	protected Particle newParticle()
	{
		return new YodaParticle();
	}

	//

	class YodaParticle extends Particle
	{
		public YodaParticle()
		{
		}

		public boolean update(ParticleSystem psys, long dt)
		{
			if (!super.update(psys, dt))
				return false;

			// check to see if below ground
			// if so, "sublimate" the particle
			if (planetrad2 > 0)
			{
				float x,y,z,r2;
				x = px+cendist.x;
				y = py+cendist.y;
				z = pz+cendist.z;
				r2 = x*x+y*y+z*z;
				if (r2 < planetrad2)
				{
					this.remove();
					// only actually create a particle every so often
					if ((intseq++ & SUBLIMATE_FREQ) == 0)
					{
						// scale to planet surface
						double ratio = planetrad/Math.sqrt(r2);
						x *= ratio;
						y *= ratio;
						z *= ratio;
						// make a new particle!
						addSublimatedParticle(this,x,y,z,true);
//System.out.println("sublim " + x + " " + y + " " + z);
					}
					return false;
				}
			}
			return true;
		}

		public void notifyRemoved()
		{
			super.notifyRemoved();

			// leave a smoke trail?
			if (doSmoke && (intseq++ & MAKESMOKE_FREQ) == 0)
			{
				float x,y,z,r2;
				x = px+cendist.x;
				y = py+cendist.y;
				z = pz+cendist.z;
				// make a new particle!
				addSublimatedParticle(this,x,y,z,false);
//System.out.println("smoke " + x + " " + y + " " + z);
			}
		}

	}


}
