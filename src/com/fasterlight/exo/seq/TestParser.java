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

public class TestParser
{
  public static void main(String args[])
  {
    SeqlangParser parser;

    if (args.length == 0) {
      System.out.println("SeqParser Version 1.1:  Reading from standard input . . .");
      parser = new SeqlangParser(System.in);
    } else if (args.length == 1) {
      System.out.println("SeqParser Version 1.1:  Reading from file " + args[0] + " . . .");
      try {
        parser = new SeqlangParser(new java.io.FileInputStream(args[0]));
      } catch (java.io.FileNotFoundException e) {
        System.out.println("SeqParser Version 1.1:  File " + args[0] + " not found.");
        return;
      }
    } else {
      System.out.println("SeqParser Version 1.1:  Usage is one of:");
      System.out.println("         java SeqlangParser < inputfile");
      System.out.println("OR");
      System.out.println("         java SeqlangParser inputfile");
      return;
    }

    Game game = new Game();
    Sequencer seq = new Sequencer(game);
    parser.setSequencer(seq);

    try {
      parser.parse();
      System.out.println("SeqParser Version 1.1:  Seq program parsed successfully.");
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      System.out.println("SeqParser Version 1.1:  Encountered errors during parse.");
    }

    for (int i=0; i<seq.getNodeCount(); i++)
    {
    	SequencerNode sn = seq.getNode(i);
    	System.out.println(i + "\t" + sn);
    }
  }

}
