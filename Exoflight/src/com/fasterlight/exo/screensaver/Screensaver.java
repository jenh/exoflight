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
package com.fasterlight.exo.screensaver;

import java.awt.*;
import java.io.IOException;

import javax.swing.JFrame;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.*;
import com.fasterlight.exo.orbit.Constants;
import com.fasterlight.exo.strategy.Mission;
import com.fasterlight.glout.*;
import com.fasterlight.util.INIFile;

public class Screensaver
implements Runnable, Constants
{
   JFrame mainframe;
   GLOAWTComponent awtadapter;
   GUIContext guictx;

   SpaceGame game;

	GLOComponent topcmpt;

	java.util.List missions;
	int curmissidx;
	Mission mission;

	String CATEG_NAME = "scrnsave/Screen Saver Missions";

	//

	class Engine extends Thread
	{
		boolean running = true;
		public void run()
		{
			while (running)
			{
				try {
					EventQueue.invokeAndWait(Screensaver.this);
				} catch (Exception ee) {
					ee.printStackTrace(System.out);
				}
			}
		}
	}

	public void run()
	{
		if (mainframe.isShowing() && mainframe.getState() != Frame.ICONIFIED)
		{
			//awtadapter.display(awtadapter);
		}

		try {
			updateControls();
			game.getGovernor().update();
		} catch (Exception exc) {
			exc.printStackTrace(System.out);
		}
	}

	void updateControls()
	{
	}

	void reloadUI()
	{
		// make top page stack component
		guictx.removeAllChildren();

      GUILoader loader = new GUILoader(null, game, guictx);

      INIFile ini;
      try {
      	topcmpt = loader.load("panels/scrnsaver/main.txt");
	      topcmpt.setSize(guictx.getSize());
	      guictx.add(topcmpt);
/*
			GLOComponent menu = loader.load("panels/menubar.txt");
      	guictx.add(menu);
*/
	   } catch (IOException ioe) {
	   	ioe.printStackTrace(System.out);
	   }
/*
		try {
			cmdmgr.loadCommands("commands/commands.txt");
			cmdmgr.loadControlBindings("commands/keys.txt");
			GLOMenuTable.setCommandManager(cmdmgr);
		} catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
*/
	}

	void startEngine()
	{
		new Engine().start();
	}

	class AWTThing
	extends GLOAWTComponent
	{
		public AWTThing(int w, int h)
		{
			super(w,h);
		}
		protected GLOContext makeContext()
		{
			return new MyGLOContext();
		}
		protected void makeComponents()
		{
			super.makeComponents();
			guictx = (GUIContext)getContext();

	      newMission();

			reloadUI();

			// start event queue going
			startEngine();
		}
	}

	class MyGLOContext
	extends GUIContext
	{
		public MyGLOContext()
		{
			super();
			cmdmgr = new GLOCommandManager(this);
		}

		private boolean keyPressed(GLOKeyEvent gke)
		{
			switch (gke.getKeyCode())
			{
				case GLOKeyEvent.VK_ENTER:
					newMission();
					return true;
			}
			return super.handleEvent(gke);
		}

   	public boolean handleEvent(GLOEvent event)
   	{
   		if (event instanceof GLOKeyEvent)
   		{
	   		boolean exec = cmdmgr.executeControl((GLOKeyEvent)event);
   			if (exec)
   				return true;
   			if ( ((GLOKeyEvent)event).isPressed() )
   			{
   				return keyPressed((GLOKeyEvent)event);
   			}
   		}
   		return super.handleEvent(event);
   	}
	}

	void exitProgram()
	{
		System.exit(0); //todo??
	}

   void newMission()
   {
   	game = new SpaceGame();
   	mission = (Mission)missions.get(curmissidx);
   	curmissidx++;
   	if (curmissidx >= missions.size())
   		curmissidx = 0;
   	mission.prepare(game);

   	System.out.println(game + " tick=" + game.time());
   	System.out.println(mission);

		mission.getSequencer().setVar("ui", guictx);
   	mission.getSequencer().setDebug(true);
   	mission.getSequencer().start();

   	guictx.setProp("tracked", mission.getSequencer().getShip());

//   	game.setDebug(true);
   	// advance a little bit to set up mission
   	game.update(TICKS_PER_SEC);
   }

	void start()
	{
		missions = Mission.getMissions(CATEG_NAME);

      mainframe = new JFrame();
      Container pane = mainframe.getContentPane();

      pane.setLayout(new BorderLayout());

		awtadapter = new AWTThing(1024, 768);
      //pane.add(awtadapter, BorderLayout.CENTER);

      //Validate frames that have preset sizes
      //Pack frames that have useful preferred size info, e.g. from their layout
      mainframe.pack();
      mainframe.setVisible(true);

	}

   //Construct the application
   public Screensaver()
   throws Exception
   {
   }

   ///

   public static void main(String[] args)
   throws Exception
   {
      Screensaver ot2 = new Screensaver();
      ot2.start();
   }
}
