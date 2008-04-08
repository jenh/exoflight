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
package com.fasterlight.exo.newgui.roam;

import java.awt.BorderLayout;
import java.io.*;
import java.util.Random;

import javax.media.opengl.*;
import javax.swing.JFrame;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.*;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.glout.*;
import com.fasterlight.model.ModelRenderer;
import com.fasterlight.vecmath.*;

public class TestPlanet
extends GLOAWTComponent
{
	ROAMPlanet rs;
	ROAMPlanet cloudrs;
	ParamWindow paramwnd;

	String planetname = "Earth";

	float minrad,maxrad;
	float h = 100;
	float oldh = 0;
	Vector3f vp = new Vector3f(0f, 1f, 0.1f);

	double sun_lat;
	double sun_long=Math.toRadians(-90);

	Vector3f vel = new Vector3f();
	Vector3f curaccel = new Vector3f();

	boolean cullface=true;
	boolean depthtest=true;

	boolean doFog = false;
	boolean doClouds = false;
	boolean doBumpMap = false;
	boolean showAxes = false;
	boolean withSectors = false;
	boolean trackGround = true;

	Random rnd = new Random();

	SpaceGame game;
	Planet planet;

	GUIContext guictx;

	boolean printstats;
	boolean noRender;

	int framenum = 0;
	int recurse_skip = 1;

	float ACCEL = 0.01f;
	float MIN_ACCEL_PER_RAD = 0.0005f;
	float DRAG = 0.1f;
	float MIN_HEIGHT = 3 / 1000.0f;
	float ACCEL_SCALE = 0.1f;

	PlanetRenderer prend;

	//

	public TestPlanet(int w, int h)
	{
    	super(w,h);
		game = new SpaceGame();
		game.start();
	}

	public void setupPlanet()
	{

		planet = (Planet)game.getBody(planetname);

		minrad = (float)planet.getMinRadius();
		maxrad = (float)planet.getMaxRadius();

/**
    	rs = new ROAMPlanet(guictx, (float)planet.getRadius());
    	cloudrs = new ROAMPlanet(guictx, minrad+10);
**/
	}

	//

	protected GLOContext makeContext()
	{
		guictx = new GUIContext(game);
    	setupPlanet();
		return guictx;
	}

	class ROAMCanvas
	extends com.fasterlight.exo.newgui.ViewBase
	//extends GLODefault3DCanvas
	{
//		PlanetTextureCache ptc, otc, ctc;

		ROAMCanvas()
		{
			super(null);
			setViewDistance(0.005f);
			neardist = 1f;
			fardist = 1e6f;
			setup();
		}

		void setSunPos()
		{
			Vector3d sunpos = new Vector3d(sun_long, sun_lat, 1);
			planet.llr2ijk(sunpos);
			prend.setSunPos(sunpos);
		}

		void setup()
		{
			prend = new PlanetRenderer( ((GUIContext)this.ctx), planet );
			rs = prend.roam;
			vp.scale( -(maxrad+h) / vp.length() );
//			vp.set(-2527.1597f, -3299.1497f, 4988.4165f);
			vp.set(-2339.3757f, -3271.1052f, 5005.832f);
			ball.setTarget(new Quat4d(0.15047944, 0.30913278, 0.85774875, 0.38217786));
		}

		public void renderObject(GLOContext ctx)
		{
			this.gl = ctx.getGL();
			this.glu = ctx.getGLU();

			setSunPos();

 			if (rs.doNormals)
 			{
 				GL gl = this.gl;
 				float ambient_mag = 0.25f;
 				float diffuse_mag = 1.0f;
 				float[] lightAmbient = { ambient_mag, ambient_mag, ambient_mag, 1.0f };
 				float[] lightDiffuse = { diffuse_mag, diffuse_mag, diffuse_mag, 1.0f };
 				gl.glLightfv( GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient, 0 );
 				gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse, 0 );
 				Vector3d sunpos = prend.getSunPos();
				float[] lightPosition = { (float)sunpos.x, (float)sunpos.y, (float)sunpos.z, 0.0f };
 				gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition, 0 );
 				gl.glEnable( GL.GL_LIGHT0 );
 				gl.glEnable( GL.GL_NORMALIZE );
 			}

			// draw stars!

			((GUIContext)ctx).renderStars(4);

			move();

			float oldh = h;
			try {
				h = paramwnd.getHeightValue();
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			if (h != oldh)
			{
				vp.scale( (maxrad+h) / vp.length() );
//				System.out.println(vp);
				oldh = h;
			}
			GL gl = ctx.getGL();
			gl.glTranslatef(-vp.x, -vp.y, -vp.z);

			gl.glPushAttrib(GL.GL_ENABLE_BIT);
			gl.glEnable(GL.GL_TEXTURE_2D);

			if (cullface)
				gl.glEnable(GL.GL_CULL_FACE);
			else
				gl.glDisable(GL.GL_CULL_FACE);

			if (depthtest)
				gl.glEnable(GL.GL_DEPTH_TEST);
			else
				gl.glDisable(GL.GL_DEPTH_TEST);

			if (rs.doNormals)
				gl.glEnable(GL.GL_LIGHTING);
			else
				gl.glDisable(GL.GL_LIGHTING);

			if (showAxes)
			{
				gl.glPushMatrix();
					float ar = (float)(minrad*1.25);
					gl.glScalef(ar,ar,ar);
					ModelRenderer mrend = rendcache.getModelRenderer("xyz",
						ModelRenderer.NO_NORMALS | ModelRenderer.COLORS);
					mrend.render();
					gl.glColor3f(1,1,1);
				gl.glPopMatrix();
			}

			try {
				if (withSectors) {
					double prad = planet.getRadius();
					double pixrad = (prad*h1*(90/getFOV())/(vp.length() - prad));
					int level = AstroUtil.log2((int)pixrad);
					prend.renderWithSectors(level, vp, vp.length());
					fardist = (float)(planet.getHorizonDist(vp.length()-prad+prad/100));
				} else {
					prend.render(vp, vel, this, fov, h1);
					fardist = 1e7f;
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}

			gl.glPopAttrib();

			framenum++;

			if (printstats) {
				rs.printStatistics();
				printstats = false;
			}
		}

		void move()
		{
			Quat4f q = ball.getQuat();
			Vector3f v = new Vector3f(curaccel);
			Matrix3f mat = new Matrix3f();
			mat.set(q);
			mat.transform(v);
			float acc = (float)Math.max(ACCEL_SCALE,
				MIN_ACCEL_PER_RAD*(vp.length() - planet.getRadius()) );
			v.scale(acc);
			vel.add(v);

			// drag :)
			float damping = (1-DRAG);
			vel.scale(damping);
			vp.add(vel);

			// rot = time*rv+r0
			// time = (0-r0)/rv
			double t = -planet.getRotation0()/planet.getAngularVel();
			double pelev = planet.getElevationAt(new Vector3d(vp), t)
				+ planet.getRadius() + MIN_HEIGHT;
			if (trackGround || vp.length() < pelev)
			{
				//System.out.println("BONK! " + (pelev - planet.getRadius()));
				vp.scale((float)(pelev/vp.length()));
			}
		}

		//

		PrintStream logps;
		PrintStream oldout;

		void startLog(String s)
		{
			try {
				oldout = System.out;
				logps = new PrintStream(new FileOutputStream(s));
				System.setOut(logps);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		void endLog()
		{
			System.setOut(oldout);
			logps.close();
		}

		public boolean handleEvent(GLOEvent event)
		{
			if (event instanceof GLOMouseButtonEvent)
			{
				GLOMouseButtonEvent mbe = (GLOMouseButtonEvent)event;
				if (mbe.isPressed(2))
				{
					return true;
				}
				else if (mbe.isReleased(2))
				{
					return true;
				}
			}
			else if (event instanceof GLOKeyEvent)
			{
				GLOKeyEvent gke = (GLOKeyEvent)event;
				if ( (gke.getFlags() & (gke.MOD_CTRL|gke.MOD_SHIFT)) == 0 )
				{
					switch (gke.getKeyCode())
					{
   					case GLOKeyEvent.VK_D:
   						if (gke.isPressed())
	   						curaccel.z += -1;
	   					else
	   						curaccel.z = 0;
   						break;
   					case GLOKeyEvent.VK_S:
   						if (gke.isPressed())
	   						curaccel.z += 1;
	   					else
	   						curaccel.z = 0;
   						break;
   					case GLOKeyEvent.VK_A:
   						if (gke.isPressed())
	   						curaccel.y += -1;
	   					else
	   						curaccel.y = 0;
   						break;
   					case GLOKeyEvent.VK_W:
   						if (gke.isPressed())
	   						curaccel.y += 1;
	   					else
	   						curaccel.y = 0;
   						break;
   					case GLOKeyEvent.VK_Z:
   						if (gke.isPressed())
	   						curaccel.x += -1;
	   					else
	   						curaccel.x = 0;
   						break;
   					case GLOKeyEvent.VK_C:
   						if (gke.isPressed())
	   						curaccel.x += 1;
	   					else
	   						curaccel.x = 0;
   						break;
   				}
				}
				else if ( (gke.getFlags() & gke.MOD_SHIFT) != 0 )
				{
					switch (gke.getKeyCode())
					{
   					case GLOKeyEvent.VK_D:
   						if (gke.isPressed())
	   						curaccel.z += -0.1f;
	   					else
	   						curaccel.z = 0;
   						break;
   					case GLOKeyEvent.VK_S:
   						if (gke.isPressed())
	   						curaccel.z += 0.1f;
	   					else
	   						curaccel.z = 0;
   						break;
   					case GLOKeyEvent.VK_A:
   						if (gke.isPressed())
	   						curaccel.y += -0.1f;
	   					else
	   						curaccel.y = 0;
   						break;
   					case GLOKeyEvent.VK_W:
   						if (gke.isPressed())
	   						curaccel.y += 0.1f;
	   					else
	   						curaccel.y = 0;
   						break;
   					case GLOKeyEvent.VK_Z:
   						if (gke.isPressed())
	   						curaccel.x += -0.1f;
	   					else
	   						curaccel.x = 0;
   						break;
   					case GLOKeyEvent.VK_C:
   						if (gke.isPressed())
	   						curaccel.x += 0.1f;
	   					else
	   						curaccel.x = 0;
   						break;
   				}
				}
				else if (gke.isPressed() && (gke.getFlags() & gke.MOD_CTRL) != 0)
				{
					switch (gke.getKeyCode())
					{
						case GLOKeyEvent.VK_P:
							System.out.println("vp=" + vp);
							System.out.println("ort=" + ball.getDest());
							break;
						case GLOKeyEvent.VK_O:
							startLog("roamtris.out");
							rs.dumpTriangles();
							endLog();
							break;
						case GLOKeyEvent.VK_Z:
							zoom(1.25f);
							break;
						case GLOKeyEvent.VK_X:
							zoom(1/1.25f);
							break;
						case GLOKeyEvent.VK_D:
							depthtest = !depthtest;
							System.out.println("Depth test = " + depthtest);
							break;
						case GLOKeyEvent.VK_A:
							showAxes = !showAxes;
							System.out.println("Show Axes = " + showAxes);
							break;
						case GLOKeyEvent.VK_K:
							cullface = !cullface;
							System.out.println("Cull face = " + cullface);
							break;
						case GLOKeyEvent.VK_C:
							prend.doClouds = !prend.doClouds;
							System.out.println("doClouds = " + prend.doClouds);
							break;
						case GLOKeyEvent.VK_B:
							prend.doLightMap = !prend.doLightMap;
							System.out.println("doLightMap = " + prend.doLightMap);
							break;
						case GLOKeyEvent.VK_M:
							prend.useMeshes = !prend.useMeshes;
							System.out.println("doMeshes = " + prend.useMeshes);
							break;
						case GLOKeyEvent.VK_1:
							rs.debug = !rs.debug;
							return true;
						case GLOKeyEvent.VK_2:
							rs.debug2 = !rs.debug2;
							return true;
						case GLOKeyEvent.VK_W:
							rs.renderMode++;
							if (rs.renderMode > rs.MODE_MAX)
								rs.renderMode = 0;
							System.out.println("mode = " + rs.renderMode);
							return true;
						case GLOKeyEvent.VK_F:
							prend.doFog = !prend.doFog;
							System.out.println("fog = " + doFog);
							return true;
						case GLOKeyEvent.VK_5:
							noRender = !noRender;
							System.out.println("noRender = " + noRender);
							return true;
						/*
						case GLOKeyEvent.VK_T:
							if (rs.ptcache != null)
								rs.setPlanetTextureCache(null);
							else
								rs.setPlanetTextureCache(ptc);
							return true;
						*/
						case GLOKeyEvent.VK_R:
							rs.reset();
							return true;
						case GLOKeyEvent.VK_S:
							withSectors = !withSectors;
							return true;
						case GLOKeyEvent.VK_N:
							prend.doNormals = !prend.doNormals;
							System.out.println("rs.doNormals = " + rs.doNormals);
							return true;
						case GLOKeyEvent.VK_RIGHT:
							sun_long += 0.01;
							System.out.println("sun_long = " + Math.toDegrees(sun_long));
							return true;
						case GLOKeyEvent.VK_LEFT:
							sun_long -= 0.01;
							System.out.println("sun_long = " + Math.toDegrees(sun_long));
							return true;
						case GLOKeyEvent.VK_ENTER:
							printstats = true;
							rs.clearStatistics();
							rs.viewvol.printStats(System.out);
							return true;
						case GLOKeyEvent.VK_F1:
							planetname = "Earth";
							setupPlanet(); setup();
							return true;
						case GLOKeyEvent.VK_F2:
							planetname = "Luna";
							setupPlanet(); setup();
							return true;
						case GLOKeyEvent.VK_F3:
							planetname = "Mars";
							setupPlanet(); setup();
							return true;
						case GLOKeyEvent.VK_F5:
							prend.setFrozen(!prend.getFrozen());
							return true;
						case GLOKeyEvent.VK_F6:
							prend.setCached(!prend.getCached());
							return true;
						case GLOKeyEvent.VK_F7:
							prend.doSkyBox = !prend.doSkyBox;
							return true;
						default:
							System.out.println(gke.getKeyCode() + " " + gke.getFlags());
					}
				}
			}
			return super.handleEvent(event);
		}

   	public Object getProp(String key)
   	{
   		if ("tris".equals(key))
   			return new Integer(rs.numtris);
   		/*
   		else if ("splitqueuesize".equals(key))
   			return new Integer(rs.splitqueue.size());
   		else if ("mergequeuesize".equals(key))
   			return new Integer(rs.mergequeue.size());
   		*/
   		else
   			return super.getProp(key);
   	}

	}

	//

	//

	class ParamWindow
	extends GLOWindow
	{
		GLOEditBox heightEdit;

		float getHeightValue()
		{
			return Float.parseFloat(heightEdit.getText());
		}

		ParamWindow()
		{
			GLOTableContainer glotab = new GLOTableContainer(2,5);
			this.setContent(glotab);

			glotab.add(new GLOLabel("Height:"));
			heightEdit = new GLOEditBox(8);
			heightEdit.setText("100.0");
			glotab.add(heightEdit);

			glotab.add(new GLOLabel("FPS:"));
			NumericLabel FPSLabel = new NumericLabel();
			FPSLabel.setMinChars(7);
			FPSLabel.setPropertyForText("fps");
			glotab.add(FPSLabel);

			glotab.add(new GLOLabel("Tris:"));
			NumericLabel TrisLabel = new NumericLabel();
			TrisLabel.setMinChars(7);
			TrisLabel.setPropertyForText("$$Visual.tris");
			glotab.add(TrisLabel);

			glotab.add(new GLOLabel("SplitQ:"));
			TrisLabel = new NumericLabel();
			TrisLabel.setMinChars(7);
			TrisLabel.setPropertyForText("$$Visual.splitqueuesize");
			glotab.add(TrisLabel);

			glotab.add(new GLOLabel("MergeQ:"));
			TrisLabel = new NumericLabel();
			TrisLabel.setMinChars(7);
			TrisLabel.setPropertyForText("$$Visual.mergequeuesize");
			glotab.add(TrisLabel);
		}
	}

	protected void makeComponents()
	{
		super.makeComponents();

		ROAMCanvas ts = new ROAMCanvas();
		ts.setName("Visual");
		ts.setSize( ctx.getWidth(), ctx.getHeight() );
		ctx.add(ts);

		paramwnd = new ParamWindow();
		paramwnd.setName("Param Window");
		ctx.add(paramwnd);
		paramwnd.layout();

	}

	public static void main(String[] args)
	throws Exception
	{
//		PrintStream ps = new PrintStream(new FileOutputStream("err.log"));
//		System.setErr(ps);

		TestPlanet test = new TestPlanet(1024,768);
		/*
		for (int i=0; i<args.length; i++)
		{
			test.planetname = args[i];
		}*/

		GLCanvas canvas = test.createGLCanvas();
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(canvas, BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);

		test.start(canvas, canvas);

	}

}
