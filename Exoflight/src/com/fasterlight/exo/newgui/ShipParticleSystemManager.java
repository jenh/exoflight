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
package com.fasterlight.exo.newgui;

import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.newgui.particle.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.CowellTrajectory;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.*;
import com.fasterlight.vecmath.*;

// todo: lots of const

/**
  * Handles particle effects for SpaceShip objects
  */
public class ShipParticleSystemManager
implements Constants
{
	VisualView vv;
	GL gl;
	SpaceShip ship;

	Map capsmap = new HashMap();
	ParticleSystem trailpartsys;
	ParticleSystem smokepartsys;
	Game game;

	private static final float HEAT_RATE_LO =
		Settings.getFloat("Particles", "HeatRateLo", 20000);
	private static final float HEAT_SCALE =
		Settings.getFloat("Particles", "HeatScale", 200000);


	//

	ShipParticleSystemManager(VisualView vv, SpaceShip ship)
	{
		this.vv = vv;
		this.gl = vv.gl;
		this.ship = ship;
		this.game = ship.getUniverse().getGame();
	}

	public ParticleSystemContext getCapParticleSystem(Capability cap,
		boolean create)
	{
		ParticleSystemContext psc = (ParticleSystemContext)capsmap.get(cap);
		if (psc != null && !create && psc.isDone())
		{
			capsmap.remove(cap);
			return null;
		}
		if (psc == null && create)
		{
			String psname = (String)cap.getAttributes().get("partsys");
			if (psname != null)
			{
				System.out.println("Loading " + psname + " PS for " + cap);
				try {
					psc = ParticleSystemContext.load(psname);
				} catch (Exception ioe) {
					ioe.printStackTrace();
					return null;
				}
				capsmap.put(cap, psc);
			} else
				return null;
		}
		if (psc != null)
			psc.setTime(game.time());

		return psc;
	}

	public ParticleSystem getTrailParticleSystem(boolean create)
	{
		if (trailpartsys != null && !create && trailpartsys.isEmpty())
		{
			trailpartsys = null;
			return null;
		}
		if (trailpartsys == null && create)
		{
			trailpartsys = new ParticleSystem(512);
		}
		if (trailpartsys != null)
			trailpartsys.setTime(game.time());

		return trailpartsys;
	}

	public ParticleSystem getSmokeParticleSystem(boolean create)
	{
		if (smokepartsys != null && !create && smokepartsys.isEmpty())
		{
			smokepartsys = null;
			return null;
		}
		if (smokepartsys == null && create)
		{
			smokepartsys = new ParticleSystem(128);
		}
		if (smokepartsys != null)
			smokepartsys.setTime(game.time());

		return smokepartsys;
	}

	void addThrustParticles(ParticleSystemContext psc,
		Module m, Vector3f tdir, Vector3f ofs,
		float period_scale, float vel_scale,
		float size_scale, float life_scale)
	{
		Orientation ort = ship.getOrientation(game.time());
		Vector3f vel = new Vector3f(tdir);
		if (m != null)
			m.getOrientation().transform(vel);
		ort.transform(vel);
		ofs = new Vector3f(ofs);
		ort.transform(ofs);

		psc.updateParticles(ofs, vel, period_scale, vel_scale, size_scale, life_scale);
	}

	void addParticles(ParticleSystem partsys,
		Vector3f vel, float dev, long life, float period,
		Vector3f ofs, float rad, float drad)
	{
		Orientation ort = ship.getOrientation(game.time());
		vel.scale(1f/TICKS_PER_SEC);
		Vector3f dvel = new Vector3f(dev,dev,dev);
		dvel.scale(1f/TICKS_PER_SEC);

		partsys.updateStream(period, ofs, vel, dvel, rad, drad, life, 8);
	}

	void renderParticles()
	{
		renderPropulsionParticles();
		renderTrailParticles();
		renderSmokeParticles();
	}

	/**
	  * Render particles for the reentry cloud.
	  */
	void renderTrailParticles()
	{
		ParticleSystem partsys = null;

		float heat = 0;
		if (ship.getTrajectory() instanceof CowellTrajectory
			&& ship.getParent() instanceof Planet)
		{
			Planet planet = (Planet)ship.getParent();
			if (planet.getAtmosphere() != null)
			{
				heat = ship.getLastHeatingRate() - HEAT_RATE_LO;
				if (heat > 0)
				{
					partsys = getTrailParticleSystem(true);
					partsys.beginStreaming();
					heat *= (1f/HEAT_SCALE);
					float rad = (float)ship.getRadius();
					Vector3f velvec = new Vector3f(
						ship.getTelemetry().getAirRefVelocity());
					Vector3f ofs = new Vector3f(velvec);
					ofs.scale(rad*1/ofs.length());
					velvec.scale(-1);

					long life = (long)(velvec.length()*4);
					float period = 0.5f/Math.max(0.05f, heat);
					// add the plasma stream
					addParticles(partsys, velvec, 0.1f, life*3, period,
						ofs, rad*2, rad/10);
					// now add slow-moving particles
					velvec.scale(0.5f/velvec.length()); // slower for these parts
					addParticles(partsys,
						velvec, 0.2f, life, period,
						ofs, rad*2, rad/3);
					partsys.endStreaming();

				}
			}
		}

		if (partsys == null)
			partsys = getTrailParticleSystem(false);
		if (partsys != null)
		{
			gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_DEPTH_BUFFER_BIT);
			gl.glEnable(GL.GL_TEXTURE_2D);
			vv.texcache.setTexture("cloud1-ALPHA.png");
			gl.glColor3f(
				heat*3-0.66f,
				Math.min(0.7f,heat*2-0.33f),
				Math.max(0.05f,Math.min(0.5f,heat)));
			gl.glDisable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_BLEND);
			gl.glDisable(GL.GL_CULL_FACE);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
			gl.glDepthMask(false);
			Matrix3f mat = vv.getLastViewMatrix();
			partsys.render(gl, mat);
			gl.glPopAttrib();
		}
	}

	/**
	  * Render particles for smoke trail (if exploded)
	  */
	void renderSmokeParticles()
	{
		ParticleSystem partsys = null;

		partsys = getSmokeParticleSystem(ship.isExploded());
		if (partsys == null)
			return;

		partsys.beginStreaming();
		float rad = (float)ship.getRadius();
		Vector3f velvec = new Vector3f(
			ship.getTelemetry().getAirRefVelocity());
		// todo: NaN
		Vector3f ofs = new Vector3f(velvec);
		ofs.scale(rad*0/ofs.length());
		velvec.scale(-1);
		// smoke rises (not in space? oh well..)
		Vector3f cen = new Vector3f(ship.getTelemetry().getCenDistVec());
		cen.scale(0.05f/cen.length());
		velvec.add(cen);

		long life = TICKS_PER_SEC*2;
		float period = TICKS_PER_SEC/6;
		// add the plasma stream
		addParticles(partsys,
			velvec, 0.1f, life*3, period,
			ofs, rad*2, 0.00003f);
		partsys.endStreaming();

		{
			gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_DEPTH_BUFFER_BIT);
			gl.glEnable(GL.GL_TEXTURE_2D);
			vv.texcache.setTexture("cloud1-ALPHA.png");
			gl.glColor4f(0,0,0,0.25f);
			gl.glDisable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_BLEND);
			gl.glDisable(GL.GL_CULL_FACE);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDepthMask(false);
			Matrix3f mat = vv.getLastViewMatrix();
			partsys.render(gl, mat);
			gl.glPopAttrib();
		}
	}

	/**
	  * Render particles for engine exhaust.
	  */
	void renderPropulsionParticles()
	{
		// find all propulsion
		List l = ship.getStructure().getCapabilitiesOfClass(PropulsionCapability.class);
		Iterator it = l.iterator();
		while (it.hasNext())
		{
			PropulsionCapability propcap = (PropulsionCapability)it.next();
			if (propcap.isRunning())
			{
				ParticleSystemContext psc = getCapParticleSystem(propcap, true);
				if (psc != null)
				{
   				psc.beginStreaming();

   				Module m = propcap.getModule();

   				if (propcap instanceof RCSCapability)
   				{
   					if (propcap instanceof com.fasterlight.exo.caps.rcs)
   					{
   						RCSCapability rcscap = (RCSCapability)propcap;
   						int flags = rcscap.getRCSFlags();
   						if (flags != 0)
   						{
   							Vector3f tofs = new Vector3f();
   							Vector3f tdir = new Vector3f();
   							float size=0.001f; // todo: const
   							for (int i=0; i<16; i++)
   							{
   								if ( (flags&(1<<i)) != 0 )
   								{
   									float fac = rcscap.getThrusterFactor(i);
   									if (fac > 0)
   									{
   										fac += 0.20f;
   										rcscap.getThrusterPosition(i, tofs);
   										rcscap.getThrusterDirection(i, 1, tdir);
   										addThrustParticles(psc, m, tdir, tofs, fac, -fac, size, 1);
   									}
   								}
   							}
   						}
   					}
   				}
   				else if (propcap instanceof RocketEngineCapability)
   				{
   					RocketEngineCapability recap = (RocketEngineCapability)propcap;
   					float throt = recap.getPctThrust();
   					// if throt < 1%, don't do anything, because too long a
   					// period causes problems (todo: fix?)
   					if (throt > 0.01)
   					{
   						float size = recap.getExitRadius()*2;
   						if (size <= 0)
   							size = 0.0045f;
   						Vector3f ofs = recap.getCMOffset();
   						ofs.scale(0.001f);
   						Vector3f tdir = recap.getThrustDirection();
   						addThrustParticles(psc, m, tdir, ofs, 1/throt, -1, size, 1);
   					}
   				}

   				psc.endStreaming();
   			}
			}
		}

		// if no part sys, return
		if (capsmap.isEmpty())
			return;

		// let's draw 'em
		gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDepthMask(false);
		gl.glDisable(GL.GL_CULL_FACE);

		// setup stuff for planet-aware
		double altagl = Double.NaN;
		Planet planet = null;
		Vector3f pos = null;
		Matrix3f planetmat = null;
		float radiusAbovePlanet = 0;

		it = capsmap.values().iterator();
		while (it.hasNext())
		{
			ParticleSystemContext psc = (ParticleSystemContext)it.next();
			psc.setTime(game.time());

			// set up planet-aware stuff
			if (psc.isPlanetAware())
			{
				if (Double.isNaN(altagl))
				{
					altagl = ship.getTelemetry().getALTAGL();
					if (planet == null && ship.getParent() instanceof Planet) //  && altagl < 1)
					{
						planet = (Planet)ship.getParent();
						pos = new Vector3f( ship.getTelemetry().getCenDistVec() );
						// gotta convert it to planet's frame of ref.
						planetmat = new Matrix3f(vv.getPlanetRotateMatrix(planet));
						radiusAbovePlanet = (float)( planet.getRadius() + ship.getTelemetry().getELEVATION() );
					}
				}

				// setup planet-detection
				// only if we are 1 km above the surface do we activate the goodness
				if (pos != null)
					psc.setupPlanetAware(radiusAbovePlanet, pos, new Matrix3f(planetmat));
			}

			// render particles
			Matrix3f mat = vv.getLastViewMatrix();
			psc.render(mat);

			// now the sublimated particle sys, if applicable (todo: needs more abstraction?)
			if (pos != null)
				psc.renderSublimated(mat, pos, planet, game.time(), vv);
		}

		gl.glPopAttrib();
	}

}
