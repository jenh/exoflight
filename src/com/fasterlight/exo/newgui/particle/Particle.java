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

import javax.media.opengl.GL;

import com.fasterlight.game.Game;
import com.fasterlight.vecmath.Vector3f;

/**
  * A particle, man.
  * Has position, velocity, size, size velocity, and a
  * start and expire time.
  *
  * @see ParticleSystem
  */
public class Particle
{
	public long t0, tend; // start time, expire time
	public float px,py,pz; // position
	public float vx,vy,vz; // velocity
	public float sc=1; // scale
	public float dsc=0; // scale velocity

	//

	public Particle()
	{
	}

	public void remove()
	{
		tend = Game.INVALID_TICK;
	}

	protected void notifyRemoved()
	{
	}

	public boolean update(ParticleSystem psys, long dt)
	{
		if (dt > 0)
		{
			px += vx*dt;
			py += vy*dt;
			pz += vz*dt;
			sc += dsc*dt;
			t0 += dt;
			if ((psys.options & psys.DAMPING) != 0)
			{
				float amt = 1f/(1+psys.damping);
				vx *= amt;
				vy *= amt;
				vz *= amt;
			}
		}
		return true;
	}

	protected void renderStart(ParticleSystem psys, GL gl)
	{
		if ((psys.options & psys.FADE) != 0)
		{
			long t1 = tend-psys.time();
			long t2 = psys.fadeTime;
			float x = t1*1.0f/t2;
			float y = 1-x/2;
			gl.glColor4f(y,y,y,x);
		}
	}

	protected void renderEnd(ParticleSystem psys, GL gl)
	{
		// override me
	}

	public void render(ParticleSystem psys, GL gl, Vector3f a1, Vector3f a2)
	{
		if (!update(psys, psys.time()-t0))
			return;

		renderStart(psys, gl);
		gl.glTexCoord2f(0,0);
		gl.glVertex3f(px-sc*a1.x, py-sc*a1.y, pz-sc*a1.z);
		gl.glTexCoord2f(1,0);
		gl.glVertex3f(px-sc*a2.x, py-sc*a2.y, pz-sc*a2.z);
		gl.glTexCoord2f(1,1);
		gl.glVertex3f(px+sc*a1.x, py+sc*a1.y, pz+sc*a1.z);
		gl.glTexCoord2f(0,1);
		gl.glVertex3f(px+sc*a2.x, py+sc*a2.y, pz+sc*a2.z);
		renderEnd(psys, gl);
	}

}
