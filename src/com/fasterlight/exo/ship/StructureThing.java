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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.LandedTrajectory;
import com.fasterlight.game.*;
import com.fasterlight.util.Vec3d;
import com.fasterlight.vecmath.*;

/**
  * A UniverseThing that contains a Structure.
  * Current only used as base class for SpaceShip.
  * @see SpaceShip
  */
public class StructureThing extends DefaultUniverseThing
{
	protected Structure structure;
	protected float last_qrate;

	public StructureThing(Structure struct)
	{
		// DynamicOrbitTrajectory asks for getStructure() :-p
		// so we have to call setTrajectory after
		this.structure = struct;
		structure.setThing(this);

		setTrajectory(traj);
	}

	public StructureThing(Game game, Planet planet, Vector3d llr, Structure struct)
	{
		this(struct);
		setTrajectory(new LandedTrajectory(game, planet, llr));
	}

	public Structure getStructure()
	{
		return structure;
	}

	public double getMass()
	{
		return structure.getMass();
	}

	public double getMass(long time)
	{
		return structure.getMassAtTime(time);
	}

	public double getRadius()
	{
		return structure.getRadius();
	}

	// DRAG STUFF

	public Perturbation getDragPerturbation(Trajectory traj)
	{
		return new DragPerturbation(traj);
	}

	public float getMaxDragCoeff(float mach)
	{
		return structure.getMaxDragCoeff(mach);
	}

	/**
	  * Get a list of ContactPoint records, or null if
	  * this object does not support contact points
	  */
	public ContactPoint[] getContactPoints()
	{
		return structure.getContactPoints();
	}

	public boolean isExpendable()
	{
		return false;
	}

	public class DragPerturbation implements Perturbation
	{
		Trajectory traj;
		double lastq;
		double lastv2;
		long lasttime = INVALID_TICK;

		DragPerturbation(Trajectory traj)
		{
			this.traj = traj;
		}
		public double getLastQ()
		{
			return lastq;
		}
		public double getLastV2()
		{
			return lastv2;
		}
		private void addGroundInteractionForce(
			PerturbForce force,
			Vector3d r,
			Vector3d v,
			Orientation ort,
			Vector3d w,
			long time,
			Planet planet,
			double rl)
		{
			double altagl =
				rl
					- planet.getRadius()
					- planet.getElevationAt(r, time * (1d / Constants.TICKS_PER_SEC));
			altagl -= getRadius();
			if (altagl < 0)
			{
				Vec3d f = new Vec3d(r);
				double BOUNCE_FACTOR = 1e4;
				f.scale(altagl * altagl * BOUNCE_FACTOR / rl);
				//				System.out.println(f);
				//				force.add(f);
			}
		}
		public void addPerturbForce(
			PerturbForce force,
			Vector3d r,
			Vector3d v,
			Orientation ort,
			Vector3d w,
			long time)
		{
			lastq = 0;
			if (!(traj.getParent() instanceof Planet))
				return;
			Planet planet = (Planet) traj.getParent();
			Atmosphere atmosphere = planet.getAtmosphere();
			StructureThing thing = (StructureThing) traj.getThing();
			Structure struct = thing.getStructure();
			double radl = r.length();

			// now do drag calc
			float alt = (float) (radl - planet.getRadius());
			if (alt < atmosphere.getCeiling())
			{
				// compute velocity rel to air
				Vector3d vd = new Vector3d(v);
				if (!isExpendable())
					vd.sub(planet.getAirVelocityWithWind(r, time * (1d / TICKS_PER_SEC)));
				else
					vd.sub(planet.getAirVelocity(r, time * (1d / TICKS_PER_SEC)));

				double vvl = vd.length();
				// don't consider relative velocities too low
				if (vvl > MIN_AIR_VEL)
				{
					Game game = getUniverse().getGame();
					Atmosphere.Params res = atmosphere.getParamsAt(alt);
					// all units should be in meters
					float density = res.density;

					double v2 = vvl * vvl * (1000 * 1000); // convert to m/s

					// compute mach #
					float mach = (float) (Math.sqrt(v2) / res.airvel);

					// get normalized, localized velocity vector
					Vector3d vv = new Vector3d(vd); // vv in km/s
					ort.invTransform(vv);

					vv.scale(1 / vvl);
					AeroForces af = struct.calculateDragCoeff(new Vector3f(vv), mach, time);

					double Q = 0.5 * density * v2; // kg/s*m^2
					double T = Q / 1000; // convert to km

					if (lasttime == INVALID_TICK)
						lasttime = game.time();
					if (game.time() > lasttime)
					{
						// Seifert & Brown, p. 458
						double J = 1;
						double M = mach;
						double y = 1.4;
						double Pr = 0.71;
						double delta = 11.8;
						double He = res.temp * AIR_SPEC_HEAT + v2 / (2 * J);
						double Hw = struct.getWallEnthalpy(); //todo:??
						double dinf = res.density;
						double de = dinf * delta;
						double ue = (5e-8 * 48) * Math.pow(He, 0.333); // viscosity
						double pinf = res.pressure * 9.8066; // convert N/m^2 to kg/m^2
						double ps = y * pinf * M * M * (1 - 1 / (2 * delta));
						double du_dx = Math.sqrt(ps / (af.area * de));
						double qrate =
							(Pr / Math.pow(Pr, 2f / 3)) * Math.sqrt(de * ue * du_dx) * (He - Hw);
						double heat = qrate * af.area;
						double atmoheat = res.temp;
						// hacked heat!
						struct.addHeat(
							heat * (game.time() - lasttime) / TICKS_PER_SEC,
							Math.max(res.temp * density, COBE_T0));
						if (debug)
						{
							System.out.println(
								struct.getTemperature()
									+ " "
									+ thing.getName()
									+ " qrate="
									+ qrate
									+ "\n"
									+ " He="
									+ He
									+ " Hw="
									+ Hw
									+ " ue="
									+ ue
									+ " du/dx="
									+ du_dx
									+ "\n"
									+ " ps="
									+ ps
									+ " pinf="
									+ pinf
									+ " de="
									+ de
									+ " dinf="
									+ dinf
									+ " area="
									+ af.area);
						}
						lasttime = game.time() + MIN_HEAT_INTERVAL;
						thing.last_qrate = (float) qrate;
					}

					// add force
					Vector3d F = new Vector3d(af.pf.f);
					ort.transform(F);
					force.f.scaleAdd(T, F, force.f);

					// add torque
					Vector3d m = new Vector3d(af.pf.T);

					lastq = Q;
					lastv2 = v2;

					// cheesy damping
					// proportional to angular*density^2

					Vector3d dm = new Vec3d(w);
					ort.invTransform(dm);

					double wl = w.length();
					double dampfactor = density * wl;

					Vector3f totalDamping = struct.getTotalDamping();

					// add damping factor
					m.x -= totalDamping.x * dm.x * dampfactor;
					m.y -= totalDamping.y * dm.y * dampfactor;
					m.z -= totalDamping.z * dm.z * dampfactor;

					// transform, and add total torque to 'force'
					ort.transform(m);
					force.T.scaleAdd(T, m, force.T);
				}
			}
		}
	}

	public float getLastHeatingRate()
	{
		return last_qrate;
	}

	// PROPERTY AWARE

	public Object getProp(String key)
	{
		if ("structure".equals(key))
			return getStructure();
		else if ("qrate".equals(key))
			return new Float(getLastHeatingRate());
		else
			return super.getProp(key);
	}

	//

	static boolean debug = false;

	// SETTINGS

	static final long MIN_HEAT_INTERVAL = 128;
	static final double AIR_SPEC_HEAT = 1006.43; // J/kg-K

	static double DAMPING_FACTOR; // not used
	static float MAX_DAMPING;
	static float MIN_AIR_VEL;

	static SettingsGroup settings = new SettingsGroup(StructureThing.class, "Ship")
	{
		public void updateSettings()
		{
			DAMPING_FACTOR = Settings.getDouble("Aerodynamics", "StructureDamping", 0.5);
			// m^3 to km^3?
			MAX_DAMPING = Settings.getFloat("Aerodynamics", "MaxDamping", 1000);
			MIN_AIR_VEL = Settings.getFloat("Aerodynamics", "MinAirvel", 1e-3f);
		}
	};

}
