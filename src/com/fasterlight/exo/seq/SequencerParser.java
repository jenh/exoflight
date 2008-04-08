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

import java.io.InputStream;

import com.fasterlight.game.Game;

/**
  * Helper functions for loading sequences.
  */
public class SequencerParser
{
	public static Sequencer loadSequence(Game game, String path)
	throws RuntimeException
	{
		try {
			InputStream in = com.fasterlight.io.IOUtil.getBinaryResource(path);
			SeqlangParser parser = new SeqlangParser(in);
			Sequencer seq = new Sequencer(game);
			parser.setSequencer(seq);
			parser.parse();
			in.close();
			System.out.println("Parsed program \"" + path + '"');
			return seq;
		} catch (Exception exc) {
			exc.printStackTrace(System.out);
			throw new RuntimeException("Program parse failed: \"" + path + '"');
		}
	}
}
