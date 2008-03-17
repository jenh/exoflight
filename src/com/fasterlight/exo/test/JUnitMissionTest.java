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
package com.fasterlight.exo.test;

import junit.framework.*;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.exo.sound.GameSound;
import com.fasterlight.exo.strategy.Mission;
import com.fasterlight.spif.PropertyEvaluator;

public class JUnitMissionTest extends TestCase implements Constants
{
	SpaceShip ship;

	Universe u;
	SpaceGame game;

	long t0, dt, startt, finalt;
	double deltat = 1.0;
	double tlimit = 1e30;

	boolean SOUND;
	boolean printOutput = true;
	GameSound gsound;
	boolean fatal;

	String categname;
	String missname;

	String failReason;
	int seqState = 0;

	String[] getprops =
		{
			"ship.telemetry.alt",
			"ship.telemetry.apoapsis",
			"ship.telemetry.periapsis",
			"ship.telemetry.velocity",
			"ship.telemetry.vertvel",
			"ship.telemetry.tangvel",
			"ship.telemetry.altagl",
			"ship.telemetry.vaccel",
			"ship.telemetry.gaccel",
			"ship.telemetry.airvelvec",
			"ship.telemetry.airvelvecshipref",
			"ship.telemetry.airvelvecplanetref",
			"ship.telemetry.lateralvel",
			"ship.telemetry.forwardvel",
			"ship.telemetry.lat",
			"ship.telemetry.long",
			"ship.telemetry.cendist" };

	//

	public JUnitMissionTest(String name)
	{
		super(name);
	}

	public JUnitMissionTest(String categName, String missionName, boolean isFatal)
	{
		this(categName + ": " + missionName + " test " + (isFatal ? " [must fail]" : ""));
		this.categname = categName;
		this.missname = missionName;
		this.fatal = isFatal;
		this.SOUND = false;
		this.printOutput = false;
		this.tlimit = 60*60; // in seconds
	}

	public void runTest() throws Exception
	{
		boolean results = this.doTest();
		assertTrue("Mission failed - " + (failReason != null ? failReason : ""), results != fatal);
	}

	public boolean doTest() throws Exception
	{
		System.out.println("Loading mission \"" + categname + "/" + missname + '"');
		Sequencer seq = null;
		boolean success = true;

		try
		{

			game = new SpaceGame();
			Mission m = Mission.getMission(categname, missname);
			assertTrue("mission was not found", m != null);
			m.prepare(game);

			game.setDebug(debug);

			seq = m.getSequencer();
			assertTrue("mission sequencer was not found", seq != null);
			seq.setDebug(true);

			if (SOUND)
			{
				gsound = GameSound.getGameSound();
				game.setValue("gsound", gsound); // bad
				System.out.println("sound open = " + gsound.isOpened());
			}

			seq.start();

			dt = (long) (Constants.TICKS_PER_SEC * deltat);
			startt = game.time();
			finalt = TICKS_PER_SEC * 200;

			System.out.println("Ship     = " + seq.getShip());
			System.out.println("Zerotime = " + seq.getZeroTime());
			System.out.println("Curtime  = " + game.time());
			System.out.println("TotalDv  = " + seq.getShip().getStructure().getTotalDeltaV());

			game.update(1000);

			for (int i = 0; true; i++)
			{
				long tt = (game.time() - seq.getZeroTime()) / TICKS_PER_SEC;
				if (tt > tlimit)
				{
					failReason = " Time expired";
					success = false;
					break;
				}
				ship = seq.getShip();
				if (ship != null)
				{
					Sequencer shipseq = ship.getSequencer();
					if (shipseq != null)
					{
						shipseq.setDebug(debug);
						if (shipseq.hasFailed())
						{
							success = false;
							failReason =
								" Sequencer failed: " + ship.getShipWarningSystem().getWarnings();
							break;
						}
						if (seqState == 0 && shipseq.isStarted())
						{
							seqState++;
						}
						else if (seqState == 1 && !shipseq.isStarted())
						{
							success = true;
							break;
						}
					}
					Telemetry telem = ship.getTelemetry();
					double realel = telem.getValue("ppitch");
					double realyaw = telem.getValue("pyaw");
					double realroll = telem.getValue("proll");
					if (printOutput)
						System
							.out
							.println(
								"t="
								+ tt
								+ " agl="
								+ AstroUtil.toDistance(telem.getValue("altagl"))
								+ " vvel="
								+ AstroUtil.toDistance(telem.getValue("vertvel"))
								+ "/s"
								+ " M="
								+ AstroUtil.format(telem.getValue("mach"))
								+ " G="
								+ AstroUtil.format(telem.getValue("gaccel"))
								+ " G"
								+
						//					" temp=" + ship.getStructure().getTemperature()+" K"
						" P="
							+ AstroUtil.format(Math.toDegrees(realel))
							+ " Y="
							+ AstroUtil.format(Math.toDegrees(realyaw))
							+
						//					" R=" + AstroUtil.format(Math.toDegrees(realroll))
						//					" rng=" + AstroUtil.format(ship.getShipTargetingSystem().getTargetRange()) +
						//					" rt=" + AstroUtil.format(ship.getShipTargetingSystem().getTargetClosure())
						"");
					/*
									System.out.println("*** " +
										AstroUtil.format(Math.toDegrees(telem.getValue("fpitch"))) + " " +
										AstroUtil.format(Math.toDegrees(ship.getTrajectory().getAngularVelocity().x))
										);
					*/
					for (int k = 0; k < getprops.length; k++)
					{
						PropertyEvaluator.get(seq, getprops[k]);
					}
					if (ship.isExploded())
					{
						failReason = "Ship crashed! " + ship;
						success = false;
						break;
					}
				}
				game.update(dt, 5000);

				if (gsound != null)
					gsound.update(ship);
			}
			return success;

		}
		finally
		{
			if (game != null && seq != null)
				System.out.println("End, " + game.time() + " " + seq.getZeroTime());
			if (gsound != null)
				gsound.close();
		}
	}

	boolean debug = "true".equals(System.getProperty("JUnitMissionTest.debug"));

	//

	public static Test suite()
	{
		TestSuite suite = new TestSuite(JUnitMissionTest.class.getName());
		suite.addTest(new JUnitMissionTest("Apollo Missions", "Apollo 11 Launch", false));
		suite.addTest(new JUnitMissionTest("Apollo Missions", "Apollo 11 Landing", false));
		suite.addTest(new JUnitMissionTest("Gemini Missions", "Gemini 6 Launch", false));
		suite.addTest(new JUnitMissionTest("Shuttle Missions", "STS-61 Launch", false));
		suite.addTest(new JUnitMissionTest("Shuttle Missions", "STS-61 Deorbit", false));
		suite.addTest(new JUnitMissionTest("Shuttle Missions", "Shuttle Docking", false));
		suite.addTest(new JUnitMissionTest("Test Missions", "Titan Ground Hug", true));
		suite.addTest(new JUnitMissionTest("Test Missions", "disaster", true));
		suite.addTest(new JUnitMissionTest("Test Missions", "X-15 Test", true));
		suite.addTest(new JUnitMissionTest("Test Missions", "Gemini Chute Test", false));
		suite.addTest(new JUnitMissionTest("Test Missions", "tank reentry test", true));
		suite.addTest(new JUnitMissionTest("Test Missions", "Mir Reentry", true));
		return suite;
	}

	//

	public static void main(String[] args) throws Exception
	{
		new JUnitMissionTest(args[0], args[1], false).runTest();
	}
}
