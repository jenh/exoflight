package com.fasterlight.exo.ship.sys;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.spif.PropertyRejectedException;

public class ShipLandingSystem extends ShipSystem {

	public ShipLandingSystem(SpaceShip ship) 
	{
		super(ship);
	}

	protected Sequencer loadSequencer() 
	{
		if (ship.getShipTargetingSystem().getTarget() == null)
			throw new PropertyRejectedException("No target for approach");

		Sequencer seq = ship.loadProgram("landing");
		// TODO? seq. zero time?
		seq.setZeroTime(getGame().time() + SpaceGame.TICKS_PER_SEC*60);
		return seq;
	}

}
