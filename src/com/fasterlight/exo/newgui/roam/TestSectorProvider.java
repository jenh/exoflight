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

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.orbit.Planet;
import com.fasterlight.glout.*;
import com.fasterlight.proctex.TexKey;
import com.fasterlight.vecmath.Vector3d;

public class TestSectorProvider
extends GLOAWTComponent
{
	SectorMeshProvider smp;
	PlanetTextureCache ptc;

	String planetname = "Luna";

	SpaceGame game;
	Planet planet;

	GUIContext guictx;

	Vector3d sunpos = new Vector3d(1,1,1);

	boolean doNormals = true;
	boolean depthtest = true;
	boolean cullface = false;

	int meshlevel = 12;

	int framenum;

	//

	public TestSectorProvider(int w, int h)
	{
    	super(w,h);
		game = new SpaceGame();
		game.start();
	}

	public void setupSectorProvider()
	{
		planet = (Planet)game.getBody(planetname);
		smp = new SectorMeshProvider(planet, gl);
		ptc = new PlanetTextureCache(planet, gl, ptc.SRC_COLOR, GL.GL_RGB);
	}

	//

	protected GLOContext makeContext()
	{
		guictx = new GUIContext(game);
    	setupSectorProvider();
		return guictx;
	}

	class YourCanvas
	extends com.fasterlight.exo.newgui.ViewBase
	//extends GLODefault3DCanvas
	{
		YourCanvas()
		{
			super(null);
			setViewDistance(10000);
			neardist = 0.01f;
			fardist = 1e5f;
			MAX_DIST = 1e9f;
			MIN_DIST = 1;
		}

		public void renderForeground(GLOContext ctx)
		{
			GL gl = ctx.getGL();

			if (doNormals)
 			{
 				float ambient_mag = 0.25f;
 				float diffuse_mag = 1.0f;
 				float[] lightAmbient = { ambient_mag, ambient_mag, ambient_mag, 1.0f };
 				float[] lightDiffuse = { diffuse_mag, diffuse_mag, diffuse_mag, 1.0f };
 				gl.glLightfv( GL.GL_LIGHT0, GL.GL_AMBIENT, lightAmbient, 0 );
 				gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, lightDiffuse, 0 );
				float[] lightPosition = { (float)sunpos.x, (float)sunpos.y, (float)sunpos.z, 0.0f };
 				gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition, 0);
 				gl.glEnable( GL.GL_LIGHT0 );
 				gl.glEnable( GL.GL_NORMALIZE );
 			}
		}

		public void renderObject(GLOContext ctx)
		{
			GL gl = ctx.getGL();
			GLU glu = new GLU();

			gl.glPushAttrib(GL.GL_ENABLE_BIT);

			if (depthtest)
				gl.glEnable(GL.GL_DEPTH_TEST);
			else
				gl.glDisable(GL.GL_DEPTH_TEST);

			if (doNormals)
				gl.glEnable(GL.GL_LIGHTING);
			else
				gl.glDisable(GL.GL_LIGHTING);

			if (cullface)
				gl.glEnable(GL.GL_CULL_FACE);
			else
				gl.glDisable(GL.GL_CULL_FACE);

//			this.guictx.texcache.setTexture("detail1.png");
			gl.glEnable(GL.GL_TEXTURE_2D);
			ptc.debug = true;

			try {
				int l = 1<<(meshlevel-8);
//				l=4;
				for (int y=0; y<l; y++)
				{
					for (int x=0; x<l*2; x++)
					{
						TexKey key = new TexKey(x, y, meshlevel);
						SectorMeshProvider.Mesh mesh = smp.getMesh(key);

						int texint = ptc.getTextureInt(key);
						gl.glBindTexture(GL.GL_TEXTURE_2D, texint);

						mesh.setTexCoords(key);
						float col = (x^y&1)!=0 ? 1f : 0.75f;
						gl.glColor3f(col,col,col);
						mesh.render();
					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}

			gl.glPopAttrib();

			ptc.update();

			framenum++;
		}

		public boolean handleEvent(GLOEvent event)
		{
			if (event instanceof GLOKeyEvent)
			{
				GLOKeyEvent gke = (GLOKeyEvent)event;
				if (gke.isPressed())
				{
					switch (gke.getKeyCode())
					{
						case GLOKeyEvent.VK_D:
							depthtest = !depthtest;
							System.out.println("Depth test = " + depthtest);
							break;
						case GLOKeyEvent.VK_K:
							cullface = !cullface;
							System.out.println("Cull face = " + cullface);
							break;
						case GLOKeyEvent.VK_B:
							doNormals = !doNormals;
							System.out.println("doNormals = " + doNormals);
							break;
						case GLOKeyEvent.VK_Z:
							closer(1.05f);
							break;
						case GLOKeyEvent.VK_X:
							closer(1/1.05f);
							break;
						case GLOKeyEvent.VK_UP:
							meshlevel++;
							break;
						case GLOKeyEvent.VK_DOWN:
							meshlevel--;
							break;
						default:
							System.out.println(gke.getKeyCode() + " " + gke.getFlags());
					}
				}
			}
			return super.handleEvent(event);
		}

	}

	//

	protected void makeComponents()
	{
		super.makeComponents();

		YourCanvas ts = new YourCanvas();
		ts.setName("Visual");
		ts.setSize( ctx.getWidth(), ctx.getHeight() );
		ctx.add(ts);

	}

	public static void main(String[] args)
	throws Exception
	{
		TestSectorProvider test = new TestSectorProvider(1024,768);
		GLCanvas canvas = test.createGLCanvas();
		JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(canvas, BorderLayout.CENTER);
		f.pack();
		f.show();

		for (int i=0; i<args.length; i++)
		{
			test.planetname = args[i];
		}

		test.start(canvas, canvas);
	}

}
