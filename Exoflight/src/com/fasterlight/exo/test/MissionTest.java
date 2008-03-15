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

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.LandedTrajectory;
import com.fasterlight.exo.seq.Sequencer;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.exo.sound.GameSound;
import com.fasterlight.exo.strategy.Mission;
import com.fasterlight.game.Settings;
import com.fasterlight.spif.PropertyEvaluator;

public class MissionTest
implements Constants
{
   SpaceShip ship;

   Universe u;
   SpaceGame game;

   long t0,dt,startt,finalt;
   double deltat = 1.0;
   double tlimit = 1e30;

   String categname = "Apollo Missions";
	String missname = "Apollo 11 Landing";

	boolean SOUND;
	GameSound gsound;

	String[] getprops = {
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
		"ship.telemetry.cendist"
	};

  	public void doTest()
  	throws Exception
  	{
		System.out.println("Loading mission \"" + categname + "/" + missname + '"');

      game = new SpaceGame();
      Mission m = Mission.getMission(categname, missname);
      m.prepare(game);

     	game.setDebug(debug);

      Sequencer seq = m.getSequencer();
      seq.setDebug(true);

      if (SOUND) {
      	gsound = GameSound.getGameSound();
			game.setValue("gsound", gsound); // bad
			System.out.println("sound open = " + gsound.isOpened());
      }

      seq.start();

		dt = (long)(Constants.TICKS_PER_SEC*deltat);
		startt = game.time();
		finalt = TICKS_PER_SEC*200;

		System.out.println("Ship     = " + seq.getShip());
		System.out.println("Zerotime = " + seq.getZeroTime());
		System.out.println("Curtime  = " + game.time());
		System.out.println("TotalDv  = " + seq.getShip().getStructure().getTotalDeltaV());

		game.update(1000);

		for (int i=0; true; i++)
		{
			long tt = (game.time()-seq.getZeroTime())/TICKS_PER_SEC;
			if (tt > tlimit)
				break;
			ship = seq.getShip();
			if (ship != null)
			{
				if (ship.getSequencer() != null)
				{
					ship.getSequencer().setDebug(true);
//					System.out.println("ship seq 0 time = " + ship.getSequencer().getZeroTime());
				}
				Telemetry telem = ship.getTelemetry();
				double realel = telem.getValue("ppitch");
				double realyaw = telem.getValue("pyaw");
				double realroll = telem.getValue("proll");
				System.out.println("t=" + tt +
					" agl=" + AstroUtil.toDistance(telem.getValue("altagl")) +
					" vvel=" + AstroUtil.toDistance(telem.getValue("vertvel"))+"/s" +
					" M=" + AstroUtil.format(telem.getValue("mach")) +
					" G=" + AstroUtil.format(telem.getValue("gaccel"))+" G"+
//					" temp=" + ship.getStructure().getTemperature()+" K"
					" P=" + AstroUtil.format(Math.toDegrees(realel)) +
					" Y=" + AstroUtil.format(Math.toDegrees(realyaw)) +
//					" R=" + AstroUtil.format(Math.toDegrees(realroll))
//					" rng=" + AstroUtil.format(ship.getShipTargetingSystem().getTargetRange()) +
//					" rt=" + AstroUtil.format(ship.getShipTargetingSystem().getTargetClosure())
""
				);
/*
				System.out.println("*** " +
					AstroUtil.format(Math.toDegrees(telem.getValue("fpitch"))) + " " +
					AstroUtil.format(Math.toDegrees(ship.getTrajectory().getAngularVelocity().x))
					);
*/
				for (int k=0; k<getprops.length; k++)
				{
					PropertyEvaluator.get(seq, getprops[k]);
				}
				if (tt > 600 && ship.getTrajectory() instanceof LandedTrajectory)
				{
					System.out.println("CRASH!!!");
					break;
				}
			}
			game.update(dt, 5000);

			if (gsound != null)
				gsound.update(ship);
		}

		System.out.println("End, " + game.time() + " " + seq.getZeroTime());
		if (gsound != null)
			gsound.close();
  	}

  	boolean debug = false;


   public static void main(String[] args)
   throws Exception
   {
   	Settings.setFilename("settings.ini");
   	MissionTest os = new MissionTest();
   	for (int i=0; i<args.length; i++)
   	{
   		String s = args[i];
	   	if (args[i].equals("-d"))
   			os.debug = true;
   		else if (args[i].equals("-m"))
   		{
   			s = args[++i];
   			os.missname = s.replace('_',' ');
   		}
   		else if (args[i].equals("-c"))
   		{
   			s = args[++i];
   			os.categname = s.replace('_',' ');
   		}
   		else if (args[i].equals("-s"))
   		{
				os.SOUND = true;
   		}
   		else if (args[i].equals("-r"))
   		{
   			s = args[++i];
   			os.deltat = Double.parseDouble(s);
   		}
   		else if (args[i].equals("-l"))
   		{
   			s = args[++i];
   			os.tlimit = Double.parseDouble(s);
   		}
   		else if (args[i].equals("-d"))
   		{
   			os.debug = true;
   		}
   	}
   	os.doTest();
   }
}
