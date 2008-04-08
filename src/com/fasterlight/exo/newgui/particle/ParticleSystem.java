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

import java.util.Random;

import javax.media.opengl.GL;

import com.fasterlight.game.Game;
import com.fasterlight.glout.GLOContext;
import com.fasterlight.vecmath.*;

/**
  * A class that lets you extend your own particle system.
  * It uses a circular array to store the particles.
  * (Add to tail, remove from head.)
  */
public class ParticleSystem

{
	int maxparts;
	Particle[] parts;
	int head,tail;
	long curtime = Game.INVALID_TICK;
	long lastadd = Game.INVALID_TICK;
	static Random rnd = new Random();

	int fadeTime;
	float damping;

	int options;

	public static final int FADE    = 1;
	public static final int DAMPING = 2;

	// for FPS management
	int skipEvery=0;
	long lastSkipTime = Game.INVALID_TICK;
	static long SKIP_INTERVAL = 1200;

	static final int FPS_LO=8;
	static final int FPS_HI=20;

	//

	public ParticleSystem(int maxparts)
	{
		this.maxparts = maxparts;
		this.parts = new Particle[maxparts];
		for (int i=0; i<maxparts; i++)
			parts[i] = newParticle();
	}

	public long time()
	{
		return curtime;
	}

	protected Particle newParticle()
	{
		return new Particle();
	}

	protected void update(long dt)
	{
		// delete all particles at the head of the list
		// that are dead until we get a live one
		while (!isEmpty())
		{
			Particle p = getParticle(head);
			if (isAlive(p))
				break;
			p.notifyRemoved();
			head = (head+1) % maxparts;
		}
	}

	public boolean isEmpty()
	{
		return (head==tail);
	}

	public boolean isDone()
	{
		return isEmpty();
	}

	public boolean isFull()
	{
		return ((head-tail+maxparts) % maxparts) == 1;
	}

	public int capacity()
	{
		return (tail-head+maxparts) % maxparts;
	}

	public Particle getParticle(int i)
	{
		return (parts[i % maxparts]);
	}

	public void setTime(long t)
	{
		if (t > curtime)
		{
			long dt = curtime-t;
			curtime = t;
			update(dt);
		} else
			curtime = t;
	}

	public boolean isAlive(Particle p)
	{
		return (p.tend > curtime);
	}

	public boolean isVisible(Particle p)
	{
		return isAlive(p);
	}

	public boolean addParticle(float px, float py, float pz,
		float vx, float vy, float vz,
		float sc, float dsc,
		long t0, long t1)
	{
		if (isFull())
			return false;

		Particle p = getParticle(tail);
		p.px = px;
		p.py = py;
		p.pz = pz;
		p.vx = vx;
		p.vy = vy;
		p.vz = vz;
		p.sc = sc;
		p.dsc = dsc;
		p.t0 = t0;
		p.tend = t1;
		tail = (tail+1) % maxparts;
		return true;
	}

	public boolean addParticle(Vector3f pos, Vector3f vel,
		float sc, float dsc, long t0, long t1)
	{
		return addParticle(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z,
			sc, dsc, t0, t1);
	}

	public void beginStreaming()
	{
		if (lastadd == Game.INVALID_TICK)
			lastadd = curtime;
	}

	public void endStreaming()
	{
		lastadd = curtime;
	}

	/**
	  * Updates the particle stream by adding a measured number
	  * of particles, depending on when the last update was,
	  * and on the passed "rate" parameter.
	  * @param period max. period between particle emissions
	  */
	public void updateStream(
		float period,
		Vector3f pos, Vector3f vel, Vector3f dvel,
		float sc, float dsc, long life, float lifedev)
	{
		long dt = curtime - lastadd;
		if (dt <= 0)
			return;
		long lowt = Math.max(lastadd, curtime-life);
		long tend,t;
		float tf=0; // t-curtime
		float f; // fractional value of t
		float x,y,z;
		float vx,vy,vz;

		x = pos.x;
		y = pos.y;
		z = pos.z;

		boolean dolifedev = (lifedev != 0);
//		System.out.println("curtime = " + curtime + ", life = " + life + ", lowt=" + lowt);
		do
		{
			tf += rnd.nextFloat()*period;
			t = curtime - (long)tf;
			if (t < lowt)
				break;
			vx = vel.x + (rnd.nextFloat()-0.5f) * dvel.x;
			vy = vel.y + (rnd.nextFloat()-0.5f) * dvel.y;
			vz = vel.z + (rnd.nextFloat()-0.5f) * dvel.z;

			f = tf-(int)tf;
			long life2 = life;
			if (dolifedev)
			{
				life2 /= (1+rnd.nextFloat()*lifedev);
			}
//			System.out.println("tf=" + tf + ", t=" + t + ", f=" + f);
			if (!addParticle(x+vx*f,y+vy*f,z+vz*f,vx,vy,vz,sc,dsc,t,t+life2))
				break;
		} while (true);
	}


	/**
	  * Render the particle system
	  * @param mat - matrix representing the camera orientation,
	  * so the "billboarding" effect will work.
	  */
	public void render(GL gl, Matrix3f mat)
	{
		Vector3f a1 = new Vector3f(1,0,0);
		mat.transform(a1);
		Vector3f a2 = new Vector3f(0,1,0);
		mat.transform(a2);

		gl.glBegin(GL.GL_QUADS);
		int i=head;
		int skip=0;
		while (i!=tail)
		{
			Particle part = getParticle(i);
			if (isVisible(part))
			{
				if (skip==0)
					part.render(this, gl, a1, a2);
				else
					part.update(this, time()-part.t0);
			}
			if (skipEvery > 0 && ++skip == skipEvery)
				skip=0;
			i = (i+1) % maxparts;
		}
		gl.glEnd();

		// measure time and adjust skipEvery accordingly
		// todo: use the highest fps this thing has seen
		GLOContext ctx = GLOContext.getCurrent();
		if (lastSkipTime == Game.INVALID_TICK ||
			ctx.getFrameStartMillis()-SKIP_INTERVAL > lastSkipTime)
		{
			float fps = GLOContext.getCurrent().getLastFPS();
			if (fps > FPS_HI) {
				skipEvery = Math.max(0, skipEvery-1);
				lastSkipTime = ctx.getFrameStartMillis();
			}
			else if (fps < FPS_LO) {
				skipEvery++;
				lastSkipTime = ctx.getFrameStartMillis();
			}
		}
	}

}
