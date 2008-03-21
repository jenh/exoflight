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
package com.fasterlight.exo.main;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Date;

import javax.media.opengl.*;

import com.fasterlight.exo.game.*;
import com.fasterlight.exo.newgui.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.orbit.traj.LandedTrajectory;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.exo.ship.sys.ShipAttitudeSystem;
import com.fasterlight.exo.sound.GameSound;
import com.fasterlight.exo.strategy.*;
import com.fasterlight.game.*;
import com.fasterlight.glout.*;
import com.fasterlight.io.IOUtil;
import com.fasterlight.vecmath.Vector3f;

/**
  * The EXOFLIGHT main program.
  */
public class Exoflight implements Runnable, Constants, NotifyingEventObserver
{
	MenuedFrame mainframe;
	Container mainwindow;

	MainAWTComponent awtadapter;
	MainGUIContext guictx;

	Universe u;
	SpaceGame game;
	Agency agency;

	GLOPageStack topstack;

	long starttime = -1;

	JoystickManager joy;

	//   SDLSurface sdl_surf;

	GameSound gsound;

	String missname, categname;

	Engine engine;

	boolean showTitle;

	static final int WORLD_WIDTH = 1024;
	static final int WORLD_HEIGHT = 768;

	static int SCRN_WIDTH =
		Integer.parseInt(System.getProperty("exo.scrnwidth", "1024"));
	static int SCRN_HEIGHT =
		Integer.parseInt(System.getProperty("exo.scrnheight", "768"));

	static final String FRAME_ICON_PATH =
		"com/fasterlight/exo/main/exoicon1-16.png";
	static final String FRAME_TITLE = "EXOFLIGHT";

	static boolean FULL_SCREEN =
		"true".equals(System.getProperty("exo.fullscreen", "false"));
	static boolean USE_NATIVE_MENU =
		System.getProperty("os.name").startsWith("Mac OS");
	static boolean ADD_LISTENERS = true;
//		System.getProperty("os.name").startsWith("Linux");

	//

	long t1 = System.currentTimeMillis();
	long t2;
	private GLCanvas glcanvas;

	/**
	 * This thread just pops events off of the EventQueue, and sends them
	 * to the outer Exoflight class.
	 */
	class Engine extends Thread
	{
		boolean running = true;
		Engine()
		{
			super("Game Event Dispatcher");
		}
		public void run()
		{
			while (running)
			{
				try
				{
					EventQueue.invokeAndWait(Exoflight.this);
					Thread.yield();
				} catch (Throwable ee)
				{
					ee.printStackTrace(System.out);
				}
			}
		}
	}

	public void run()
	{
		try
		{
			if (mainwindow.isShowing()
				&& mainframe.getState() != Frame.ICONIFIED)
			{
				glcanvas.display();
			}
			if (game == null)
				return;

			updateControls();
			game.getGovernor().update();
			if (getTracked() instanceof SpaceShip)
				gsound.update((SpaceShip) getTracked());
		} catch (Throwable exc)
		{
			exc.printStackTrace(System.out);
		}
	}

	void updateControls()
	{
		if (guictx == null)
			return;
		if (joy == null || joy.getNumSticks() == 0)
			return;
		{
			SpaceShip ship = guictx.getCurrentShip();
			if (ship == null)
				return;
			if (ship.getShipAttitudeSystem().getRCSManual()
				!= ShipAttitudeSystem.RCS_MANINHIBIT)
			{
				joy.updateDevices();
				float dz = 0.15f;
				float x = joy.getAxis(JoystickManager.Z_AXIS) / 32767f;
				float y = joy.getAxis(JoystickManager.Y_AXIS) / 32767f;
				float z = joy.getAxis(JoystickManager.X_AXIS) / 32767f;
				if (Math.abs(x) < dz)
					x = 0;
				else
					x = AstroUtil.sign(x) * (Math.abs(x) - dz) / (1 - dz);
				if (Math.abs(y) < dz)
					y = 0;
				else
					y = AstroUtil.sign(y) * (Math.abs(y) - dz) / (1 - dz);
				if (Math.abs(z) < dz)
					z = 0;
				else
					z = AstroUtil.sign(z) * (Math.abs(z) - dz) / (1 - dz);
				Vector3f v = new Vector3f(-y, -x, z);
				ship.getShipAttitudeSystem().setRotationController(v);

				// do throttle
				if (ship.getShipAttitudeSystem().getThrottleManual()
					== ShipAttitudeSystem.THROT_MANUAL)
				{
					float t = 0.5f - (joy.getAxis(JoystickManager.THROT_AXIS) / 65535f);
					if (t < 0.01f)
						t = 0;
					ship.getShipAttitudeSystem().setManualThrottle(t);
				}
			}
		}
	}

	public boolean notifyObservedEvent(NotifyingEvent event)
	{
		// if we get an alert, know the code& play sound
		if (event instanceof AlertEvent)
		{
			String code = ((AlertEvent) event).getCode();
			if (code != null)
			{
				gsound.alertCode(code);
			}
		}
		if (event.getMessage() == null)
			return false;
		return game.getGovernor().decreaseTimeScaleTo(1.0f);
	}

	//

	void reloadUI()
	{
		// make top page stack component
		guictx.removeAllChildren();
		guictx.reset();

		// set current ship
		java.util.List ships = game.getAgency().getShips();
		if (ships.size() > 0)
		{
			guictx.setProp("ship", ships.get(0));
		}

		game.setValue("gsound", gsound);

		GUILoader loader = new GUILoader(null, game, guictx);

		try
		{
			topstack = (GLOPageStack) loader.load("panels/pages.txt");
			topstack.setSize(guictx.getSize());
			guictx.add(topstack);

			loader.setTopComponent(guictx);
			// todo: WHY does this thing have to have a parent to layout?
			if (USE_NATIVE_MENU)
			{
				GLOMenu gloMenu = new GLOMenu();
				gloMenu.load("panels/main.mnu");
				mainframe.setGLOMenu(gloMenu);
			} else
			{
				GLOComponent menu = loader.load("panels/menubar.txt");
			}
			GLOComponent misc = loader.load("panels/misc.txt");
		} catch (IOException ioe)
		{
			ioe.printStackTrace(System.out);
		}

		guictx.reloadCommands();
		mainframe.setCmdmgr(guictx.getCommandManager());
	}

	class MainAWTComponent extends GLOAWTComponent
	{
		public MainAWTComponent(int w, int h)
		{
			super(w, h);
		}
		protected GLOContext makeContext()
		{
			GLOContext ctx = new MainGUIContext(game);
			return ctx;
		}
		protected void makeComponents()
		{
			super.makeComponents();
			guictx = (MainGUIContext) getContext();
			guictx.setSize(WORLD_WIDTH, WORLD_HEIGHT);
			guictx.setViewSize(SCRN_WIDTH, SCRN_HEIGHT);
		}
		public void init(GLAutoDrawable drawable)
		{
			super.init(drawable);

			// show title
			loadTitleScreen();
			//sDisplay();
		}
	}

	synchronized void startEngine()
	{
		if (engine == null)
		{
			engine = new Engine();
			engine.start();
			engine.setPriority(Thread.NORM_PRIORITY - 1); //todo: const
		}
	}

	//

	class MainGUIContext extends GUIContext
	{

		public MainGUIContext(SpaceGame game)
		{
			super(game);
			cmdmgr = new GLOCommandManager(this);
		}
		public void setGame(SpaceGame game)
		{
			super.setGame(game);
		}
		void reloadCommands()
		{
			try
			{
				cmdmgr.loadCommands("commands/commands.txt");
				cmdmgr.loadControlBindings("commands/keys.txt");
			} catch (IOException ioe)
			{
				ioe.printStackTrace(System.out);
			}
		}
		private boolean keyPressed(GLOKeyEvent e)
		{
			boolean exec = cmdmgr.executeControl(e);
			if (exec)
				return true;
			if (e.getFlags() != 0)
				return false;
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_F10 :
					try
					{
						gsound.reset();
					} catch (Exception ex)
					{
						ex.printStackTrace(System.out);
						throw new RuntimeException(ex.toString());
					}
					return true;
				case KeyEvent.VK_F11 :
					guictx.clearCaches();
					return true;
				case KeyEvent.VK_F12 :
					game.setDebug(!game.getDebug());
					return true;
				case KeyEvent.VK_O :
					Conic o =
						UniverseUtil.getGeocentricConicFor(getCurrentShip());
					if (o == null)
						return false;
					System.out.println(o.getElements());
					System.out.println(
						AstroUtil.gameTickToJavaDate(
							AstroUtil.dbl2tick(o.getInitialTime())));
					return true;
				case KeyEvent.VK_T :
					if (getSelected() != null && getCurrentShip() != null)
					{
						getCurrentShip().getShipTargetingSystem().setTarget(
							getSelected());
						// todo: message
					}
					return true;
				case KeyEvent.VK_R :
					if (getSelected() instanceof SpaceShip)
					{
						setCurrentShip((SpaceShip) getSelected());
					} else
					{
						setTracked(getSelected());
					}
					// todo: message
					return true;
				case KeyEvent.VK_1 :
				case KeyEvent.VK_2 :
				case KeyEvent.VK_3 :
				case KeyEvent.VK_4 :
				case KeyEvent.VK_5 :
				case KeyEvent.VK_6 :
				case KeyEvent.VK_7 :
				case KeyEvent.VK_8 :
				case KeyEvent.VK_9 :
					{
						java.util.List ships = agency.getShips();
						int i = e.getKeyCode() - KeyEvent.VK_1;
						if (i < ships.size())
						{
							SpaceShip ship = (SpaceShip) ships.get(0);
							//   						setCurrentShip(ship);
							setSelected(ship);
						}
						return true;
					}
				case KeyEvent.VK_0 :
					if (getCurrentShip() != null
						&& getCurrentShip().getShipTargetingSystem().getTarget()
							!= null)
					{
						setSelected(
							getCurrentShip()
								.getShipTargetingSystem()
								.getTarget());
					}
					return true;
				case KeyEvent.VK_H :
					if (getSelected() instanceof SpaceShip)
					{
						Trajectory traj = getSelected().getTrajectory();
						if (traj instanceof LandedTrajectory)
							 ((LandedTrajectory) traj).setLockedDown(false);
					}
					break;
				case KeyEvent.VK_F9 :
					Vehicle.reloadVehicles();
					guictx.loadDefaultShaders();
					reloadUI();
					SettingsGroup.updateAll();
					break;
				default :
					System.out.println(e);
					break;
			}
			return false;
		}

		public boolean handleEvent(GLOEvent event)
		{
			if (event instanceof GLOKeyEvent)
			{
				boolean exec = cmdmgr.executeControl((GLOKeyEvent) event);
				if (exec)
					return true;
				if (((GLOKeyEvent) event).isPressed())
				{
					return keyPressed((GLOKeyEvent) event);
				}
			} else if (event instanceof GLOActionEvent)
			{
				Object act = ((GLOActionEvent) event).getAction();
				if (act instanceof SpaceGame)
				{
					System.out.println("Resetting to game " + act);
					resetGame((SpaceGame) act);
					return true;
				}
				if (act instanceof String) // todo? Command object?
				{
					String s = act.toString();
					if (s.equals("Game:Exit"))
					{
						exitProgram();
						return true;
					} else if (s.equals("Game:Reset"))
					{
						if (game != null && game.getCurrentMission() != null)
						{
							resetGame(
								guictx.createNewGame(game.getCurrentMission()));
						} else
							throw new GLOUserException("No mission is currently active.");
						return true;
					} else if (cmdmgr != null)
					{
						if (cmdmgr.execute(s))
							return true;
					}
				}
			}
			return super.handleEvent(event);
		}
	}

	void exitProgram()
	{
		game = null;
		if (mainframe != null)
			mainframe.hide();
		if (mainwindow != null)
			mainwindow.setVisible(false);
		try
		{
			engine.running = false;
			engine.join(1000);
		} catch (Exception exc)
		{
			exc.printStackTrace();
		}
		try
		{
			if (joy != null)
				joy.close();
			if (gsound != null)
				gsound.close();
			Thread.sleep(500);
		} catch (Exception exc)
		{
			exc.printStackTrace();
		}
		System.exit(0); //todo??
	}

	void resetGame(SpaceGame newgame)
	{
		if (game != null)
		{
			game.removeObserver(this);
		}

		// show title
		loadTitleScreen();
		//awtadapter.sDisplay();

		game = newgame;
		game.addObserver(this);
		u = game.getUniverse();
		agency = game.getAgency();
		if (guictx != null)
		{
			guictx.setGame(game);
			this.reloadUI();
			try
			{
				gsound.reset();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		showTitle = false;
	}

	void startDefaultGame()
	{
		SpaceGame newgame = new SpaceGame();

		if (categname != null && missname != null)
		{
			Mission m = Mission.getMission(categname, missname);
			m.prepare(newgame);
			m.getSequencer().setVar("ui", guictx);
			m.getSequencer().start();
		} else
		{
			// set game time to current "real" time
			if (starttime == -1)
			{
				Date now = new Date();
				newgame.setGameStartTime(now);
			} else
			{
				newgame.setGameStartTime(starttime);
			}
			newgame.start();
			newgame.setupEarthSats();
		}

		resetGame(newgame);
		// try to force GC (todo: danger!)
		System.gc();
	}

	Image getImage(String path)
	{
		try
		{
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			byte[] imgarr = IOUtil.readBytes(IOUtil.getBinaryResource(path));
			Image image = toolkit.createImage(imgarr);
			return image;
		} catch (Exception ioe)
		{
			ioe.printStackTrace();
			return null;
		}
	}

	Image getIconImage()
	{
		return getImage(FRAME_ICON_PATH);
	}

	void showMainWindow()
	{
		mainframe = new MenuedFrame(FRAME_TITLE);
		GraphicsDevice scrnDevice = null;
		if (FULL_SCREEN)
		{
			mainframe.setUndecorated(true);
			mainwindow = mainframe.getContentPane();
			//			mainwindow = new Window(mainframe);
			//			scrnDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			//			System.out.println("setting full screen: " + scrnDevice.getDisplayMode());
		} else
			mainwindow = mainframe.getContentPane();

		Image iconimage = getIconImage();
		if (iconimage != null)
			mainframe.setIconImage(iconimage);
		else
			System.err.println("could not find icon image");

		// so that Java MIGHT quite when we close the manu
		mainframe.addWindowListener(new WindowAdapter()
		{
			public void windowClosed(WindowEvent e)
			{
				// again, gl4java thwarts us
				//guictx.loadDialog("panels/dialogs/exit.txt");
				exitProgram();
			}
			public void windowClosing(WindowEvent e)
			{
				windowClosed(e);
			}
		});

		//Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, e.g. from their layout
		System.out.println("showing mainframe");

		awtadapter = new MainAWTComponent(SCRN_WIDTH, SCRN_HEIGHT);
		glcanvas = awtadapter.createGLCanvas();
		mainwindow.setLayout(new BorderLayout());
		mainwindow.add(glcanvas, BorderLayout.CENTER);

		mainframe.pack();
		if (FULL_SCREEN)
		{
			Rectangle desktopRect =
				GraphicsEnvironment
					.getLocalGraphicsEnvironment()
					.getMaximumWindowBounds();
			System.out.println("desktop = " + desktopRect);
			mainframe.setBounds(desktopRect);
		}

		mainframe.setVisible(true);
		showTitle = true;
		if (ADD_LISTENERS)
		{
			awtadapter.addListeners(glcanvas);
			System.out.println("Added listeners");
		}
	}

	void loadTitleScreen()
	{
		GUILoader loader = new GUILoader(null, game, guictx);
		loader.setTopComponent(guictx);
		try
		{
			loader.load("panels/loading_screen.txt");
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	void start()
	{
		showMainWindow();

		// start stick
		initializeJoystick();

		gsound = GameSound.getGameSound();
		System.out.println("sound opened = " + gsound.isOpened());

		// load default game
		startDefaultGame();

		// start event queue going
		startEngine();
	}

	private void initializeJoystick()
	{
		try
		{
			joy = new JoystickManager();
			joy.open();
			System.out.println("Found " + joy.getNumSticks() + " joysticks");
		} catch (Throwable e)
		{
			System.out.println("Could not initialize joystick: " + e);
			joy = null;
		}
	}

	//Construct the application
	public Exoflight() throws Exception
	{
	}

	///

	public UniverseThing getSelected()
	{
		return (UniverseThing) guictx.getProp("selected");
	}

	public void setSelected(UniverseThing selected)
	{
		guictx.setProp("selected", selected);
	}

	public UniverseThing getTracked()
	{
		return (UniverseThing) guictx.getProp("tracked");
	}

	public void setTracked(UniverseThing tracked)
	{
		guictx.setProp("tracked", tracked);
	}

	///

	public static void main(String[] args) throws Exception
	{
		try
		{
			// use settings.ini, create one & populate if its not there
			Settings.setFilename("settings.ini");
			Settings.setWriteable(true);

			// redirect stdout, stderr
			PrintStream out = null;
			String outfile = null;
			for (int i = 0; i < args.length; i++)
			{
				if (args[i].equals("-stdout"))
				{
					String fn = args[++i];
					if (!fn.equals(outfile))
					{
						outfile = fn;
						out = new PrintStream(new FileOutputStream(fn));
					}
					System.setOut(out);
				} else if (args[i].equals("-stderr"))
				{
					String fn = args[++i];
					if (!fn.equals(outfile))
					{
						outfile = fn;
						out = new PrintStream(new FileOutputStream(fn));
					}
					System.setErr(out);
				} else
					System.err.println("Unrecognized argument: " + args[i]);
			}

			Exoflight mainprog = new Exoflight();
			// todo: args
			mainprog.start();
		} catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
}
