package com.fasterlight.exo.ship.sys;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.nav.Lambert;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.exo.ship.progs.ApproachProgram;
import com.fasterlight.game.Game;
import com.fasterlight.util.UserException;
import com.fasterlight.vecmath.Vector3d;

public class ShipLandingSystem extends ShipSystem {

	public ShipLandingSystem(SpaceShip ship) 
	{
		super(ship);
	}

	protected Sequencer loadSequencer() 
	{
		UniverseThing target = ship.getShipTargetingSystem().getTarget();
		if (target == null)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOTARGET", "No target for approach program");
			return null;
		}
		UniverseThing ref = ship.getParent();
		if (target.getParent() != ref)
		{
			ship.getShipWarningSystem().setWarning("GUID-REF", "Ship must orbit same body as target");
			return null;
		}

		Sequencer seq = ship.loadProgram("landing");
		// compute time required for braking
		// first figure out max acceleration of ship
		double maxdv = ship.getMaxAccel();
		if (maxdv == 0)
		{
			ship.getShipWarningSystem().setWarning("GUID-NOACCEL", "No current acceleration capability");
			return null;
		}
		// now figure out angle between source & target
		Telemetry telem = ship.getTelemetry();
		Game game = ship.getUniverse().getGame();
		long t = game.time();
		Vector3d r1 = telem.getCenDistVec();
		Vector3d v1 = telem.getVelocityVec();
		Vector3d r2 = target.getPosition(ref, t);
		double ang = AstroUtil.acos(r1.dot(r2) / (r1.length()*r2.length()));
		// compute time that we will arrive @ target on this trajectory
		Conic conic = telem.getConic();
		// TODO: compute for hyperbolic orbtis?
		double E0 = conic.getElements().getEccentricAnomaly();
		double E = convertTrueAnomToEcc(conic.getEccentricity(), telem.getTRUEANOM() + ang);
		double[] times = conic.getTimesAtEccAnom(conic.getSemiMajorAxis(), conic.getEccentricity(), E0, E);
		double timeAtTarget = times[0]; // TODO?
		// determine velocity @ target, and time to thrust
		// TODO: better solution solving for sv + time using newton
		double timeToTarget = timeAtTarget - conic.getInitialTime();
		StateVector sv = conic.getStateVectorAtTime(conic.getInitialTime() + timeToTarget);
		v1.sub(sv.v);
		double timeToThrust = v1.length()/maxdv;
		if (timeToThrust > timeToTarget)
		{
			ship.getShipWarningSystem().setWarning("GUID-TIME", "Not enough time left for braking");
		}
		seq.setZeroTime(t + AstroUtil.dbl2tick(timeToTarget - timeToThrust));
		return seq;
	}

	// convert true anom to ecc anom given orbit eccentricity and true anomaly
	double convertTrueAnomToEcc(double e, double v)
	{
		double E = 0.0;

		E = Math.atan(Math.sqrt((1 - e) / (1 + e)) * Math.tan(v / 2));

		if (E < 0.0)
			E += 3 / 2 * Math.PI;

		E *= 2.0;

		return E;
	}
}
