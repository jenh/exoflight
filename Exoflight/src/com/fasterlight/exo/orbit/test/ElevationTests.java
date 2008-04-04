package com.fasterlight.exo.orbit.test;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.Planet;

import junit.framework.TestCase;

public class ElevationTests extends TestCase
{
	public ElevationTests(String name)
	{
		super(name);
	}
	
	static SpaceGame game;

	public static void setUpTest()
	{
		if (game == null)
		{
			game = new SpaceGame();
			game.start();
		}
	}

	public void setUp()
	{
		setUpTest();
	}

	//
	public void testElevationModel()
	{
		Planet p = (Planet) game.getBody("Phobos");
		double elev = p.getElevationAt(0,0);
		System.out.println("elev="+elev);
	}

}
