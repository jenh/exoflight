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

import java.util.Date;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.nav.TOFOptimizer;
import com.fasterlight.vecmath.Tuple2f;

public class OptimizeTest
implements Constants
{
	SpaceGame game;

	Date tick2date(long t)
	{
		return new Date((t*1000/TICKS_PER_SEC)+JAVA_MSEC_2000);
	}

	public void solve()
	throws Exception
	{
		long t = game.time();
		// x is time to start
		// y is mission dur
		// t1 is dur
		// t2 is minimum mission len
		long t1 = TICKS_PER_SEC*86400*700;
		long t2 = TICKS_PER_SEC*86400*180;
		System.out.println(game.getBody("Earth"));
		TOFOptimizer opt = new TOFOptimizer(
			game.getBody("Phobos"),
			game.getBody("Earth"),
			game.getBody("Sun"),
			t, t+t1, t+t2, t+t1+t2);
		opt.setMinXSize(1f/700);
		opt.setMinYSize(1f/180);
		for (int i=0; i<150; i++)
		{
			opt.iterate();
		}
		Tuple2f best = opt.getBestPoint();
		System.out.println("Best point is " + best);
		System.out.println("Depart date: " + tick2date((long)(best.x*1d*t1)+t));
		System.out.println("Arrive date: " + tick2date((long)(best.y*1d*t1)+t+t2));
		Conic o = opt.getConicFor(best.x, best.y);
		System.out.println(o);
	}


   //Construct the application
   public OptimizeTest()
   throws Exception
   {
      game = new SpaceGame();
      // set game time to current "real" time
//		Date now = new Date(79,1,1);
		Date now = new Date();
		game.setGameStartTime(now);
		game.start();
      System.out.println("Set time to " + game.time() + ", delta is " + (game.time()-EPOCH_2000));

      // solve
      solve();

   }

   //GravTest method
   public static void main(String[] args)
   throws Exception
   {
      new OptimizeTest();
   }
}