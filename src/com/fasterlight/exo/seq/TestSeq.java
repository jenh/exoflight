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
package com.fasterlight.exo.seq;

import com.fasterlight.game.Game;

public class TestSeq
{
	void doTest()
	{
		Game game = new Game();
		Sequencer seq = new Sequencer(game);
		seq.addNode( new TimeWaitNode(10, TimeWaitNode.RELATIVE) );
		seq.addNode( new TimeWaitNode(20, TimeWaitNode.ABSOLUTE) );
		seq.addNode( new TimeWaitNode(10, TimeWaitNode.DURATION) );

		game.setDebug(true);
		seq.setDebug(true);

		seq.start();
		game.update(25);
		seq.stop();
		game.update(75);
		seq.start();
		game.update(100);

		System.out.println(game + " " + seq);
	}

	public static void main(String[] args)
	{
		TestSeq test = new TestSeq();
		test.doTest();
	}

}
