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

import java.io.IOException;
import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.newgui.VisualView;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.glout.*;
import com.fasterlight.io.IOUtil;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;
import com.fasterlight.vecmath.*;

/**
  * Contains a ParticleSystem and its various settings
  * for adding particles and rendering.
  */
public class ParticleSystemContext implements PropertyAware, Constants
{
	GLOShader shader;

	float period = TICKS_PER_SEC;
	float vel = 1f / TICKS_PER_SEC;
	float dev = 0;
	float size = 1;
	float sizeinc = 0;
	long life = TICKS_PER_SEC;
	float lifedev = 1;

	int maxparts = 128;

	boolean planetAware = false;
	boolean smokeTrail = false;
	boolean depthTest = true;

	//

	// create a chain of these!
	ParticleSystemContext next;

	private ParticleSystem partsys;
	private PlanetAwareParticleSystem pa_partsys;

	private Vector3f _vel = new Vector3f();
	private Vector3f _dvel = new Vector3f();

	//

	public static ParticleSystemContext load(String name) throws IOException
	{
		ParticleSystemContext first_psc = null;
		ParticleSystemContext psc = null;

		INIFile ini = new CachedINIFile(IOUtil.getBinaryResource("particles/" + name + ".ini"));
		List v = ini.getSectionNames();
		Iterator vit = v.iterator();
		while (vit.hasNext())
		{
			String sect = (String) vit.next();
			Properties props = ini.getSection(sect);
			ParticleSystemContext new_psc = load(props);
			if (psc != null)
				psc.next = new_psc;
			else
				first_psc = new_psc;
			psc = new_psc;
		}

		return first_psc;
	}

	public static ParticleSystemContext load(Properties props) throws IOException
	{
		ParticleSystemContext psc = new ParticleSystemContext();
		psc.shader = new GLOShader(props);
		PropertyUtil.setFromProps(psc, props);
		return psc;
	}

	//

	public void setTime(long t)
	{
		getParticleSystem().setTime(t);
		if (next != null)
			next.setTime(t);
	}

	public boolean isDone()
	{
		return (partsys == null || partsys.isDone()) && (next == null || next.isDone());
	}

	public boolean isPlanetAware()
	{
		return planetAware || (next == null || next.isPlanetAware());
	}

	/**
		public boolean isShowingSmokeTrail()
		{
			return (pa_partsys != null && !pa_partsys.isEmpty()) ||
				(next==null || next.isShowingSmokeTrail());
		}
	**/

	public void setupPlanetAware(float radiusAbovePlanet, Vector3f pos, Matrix3f planetmat)
	{
		if (planetAware && pa_partsys != null)
		{
			pa_partsys.setPlanetRadius(radiusAbovePlanet);
			pa_partsys.setCenDist(pos);
			pa_partsys.setXformMatrix(new Matrix3f(planetmat));
		}
		if (next != null)
			next.setupPlanetAware(radiusAbovePlanet, pos, planetmat);
	}

	public void beginStreaming()
	{
		getParticleSystem().beginStreaming();
		if (next != null)
			next.beginStreaming();
	}

	public void endStreaming()
	{
		getParticleSystem().endStreaming();
		if (next != null)
			next.endStreaming();
	}

	public void updateParticles(
		Vector3f ofs,
		Vector3f dir,
		float period_scale,
		float vel_scale,
		float size_scale,
		float life_scale)
	{
		float v = vel * vel_scale * size_scale;
		_vel.set(dir.x * v, dir.y * v, dir.z * v);
		float _dev = dev * size_scale;
		_dvel.set(_dev, _dev, _dev);

		float _period = period * period_scale;
		long _life = (long) (life * life_scale);
		float _size = size * size_scale;
		float _sizeinc = sizeinc * size_scale;

		partsys.updateStream(period, ofs, _vel, _dvel, _size, _sizeinc, _life, lifedev);

		if (next != null)
			next.updateParticles(ofs, dir, period_scale, vel_scale, size_scale, life_scale);
	}

	public void render(Matrix3f mat)
	{
		if (!partsys.isEmpty())
		{
			GLOContext ctx = GLOContext.getCurrent();
			if (shader != null)
				shader.set(ctx);
			GL gl = ctx.getGL();
			if (!depthTest)
				gl.glDisable(GL.GL_DEPTH_TEST);
			partsys.render(gl, mat);
			if (!depthTest)
				gl.glEnable(GL.GL_DEPTH_TEST);
		}

		if (next != null)
			next.render(mat);
	}

	public void renderSublimated(
		Matrix3f mat,
		Vector3f pos,
		Planet planet,
		long time,
		VisualView vv)
	{
		if (pa_partsys != null && !pa_partsys.isDone() && pa_partsys.sublime_psys != null)
		{
			GL gl = GLOContext.getCurrent().getGL();
			pa_partsys.sublime_psys.setTime(time);
			gl.glPushMatrix();
			GLOContext.getCurrent().setShader("particle-smoke");
			// todo: should we use another ParticleSystemContext?
			// we gotta translate to the planet center
			// then rotate by the planet's frame of ref.
			// THEN also transform the trackball matrix.
			// WHEW!
			gl.glTranslatef(-pos.x, -pos.y, -pos.z);
			Matrix3d planmat = vv.rotatePlanet(planet);
			Matrix3f planmat2 = new Matrix3f(planmat);
			planmat2.mul(mat);
			pa_partsys.sublime_psys.render(gl, planmat2);
			// pop the evil
			gl.glPopMatrix();
		}

		if (next != null)
			next.renderSublimated(mat, pos, planet, time, vv);
	}

	protected ParticleSystem makeParticleSystem()
	{
		if (planetAware)
		{
			pa_partsys = new PlanetAwareParticleSystem(maxparts);
			pa_partsys.doSmoke = smokeTrail;
			return pa_partsys;
		}
		else
			return new ParticleSystem(maxparts);
	}

	public ParticleSystem getParticleSystem()
	{
		if (partsys == null)
			partsys = makeParticleSystem();
		return partsys;
	}

	public float getPeriod()
	{
		return period / TICKS_PER_SEC;
	}

	public void setPeriod(float period)
	{
		this.period = period * TICKS_PER_SEC;
	}

	public float getVelocity()
	{
		return vel * TICKS_PER_SEC;
	}

	public void setVelocity(float vel)
	{
		this.vel = vel / TICKS_PER_SEC;
	}

	public float getDeviation()
	{
		return dev * TICKS_PER_SEC;
	}

	public void setDeviation(float dev)
	{
		this.dev = dev / TICKS_PER_SEC;
	}

	public float getSize()
	{
		return size;
	}

	public void setSize(float size)
	{
		this.size = size;
	}

	public float getSizeInc()
	{
		return sizeinc * TICKS_PER_SEC;
	}

	public void setSizeInc(float sizeinc)
	{
		this.sizeinc = sizeinc / TICKS_PER_SEC;
	}

	public float getLife()
	{
		return life * (1f / TICKS_PER_SEC);
	}

	public void setLife(float life)
	{
		this.life = (long) (life * TICKS_PER_SEC);
	}

	public float getLifeDev()
	{
		return lifedev;
	}

	public void setLifeDev(float lifedev)
	{
		this.lifedev = lifedev;
	}

	public int getMaxParticles()
	{
		return maxparts;
	}

	public void setMaxParticles(int maxparts)
	{
		this.maxparts = maxparts;
	}

	public GLOShader getShader()
	{
		return shader;
	}

	public void setShader(GLOShader shader)
	{
		this.shader = shader;
	}

	public boolean getPlanetAware()
	{
		return planetAware;
	}

	public void setPlanetAware(boolean planetAware)
	{
		this.planetAware = planetAware;
	}

	public boolean getSmokeTrail()
	{
		return smokeTrail;
	}

	public void setSmokeTrail(boolean smokeTrail)
	{
		this.smokeTrail = smokeTrail;
	}

	public boolean getDepthTest()
	{
		return depthTest;
	}

	public void setDepthTest(boolean depthTest)
	{
		this.depthTest = depthTest;
	}

	public ParticleSystemContext getNext()
	{
		return this.next;
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(ParticleSystemContext.class);

	static {
		prophelp.registerGetSet("period", "Period", float.class);
		prophelp.registerGetSet("vel", "Velocity", float.class);
		prophelp.registerGetSet("dev", "Deviation", float.class);
		prophelp.registerGetSet("size", "Size", float.class);
		prophelp.registerGetSet("sizeinc", "SizeInc", float.class);
		prophelp.registerGetSet("life", "Life", float.class);
		prophelp.registerGetSet("lifedev", "LifeDev", float.class);
		prophelp.registerGetSet("maxparts", "MaxParticles", int.class);
		prophelp.registerGetSet("shader", "Shader", GLOShader.class);
		prophelp.registerGet("partsys", "getParticleSystem");
		prophelp.registerGetSet("planetaware", "PlanetAware", boolean.class);
		prophelp.registerGetSet("smoketrail", "SmokeTrail", boolean.class);
		prophelp.registerGetSet("depthtest", "DepthTest", boolean.class);
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
