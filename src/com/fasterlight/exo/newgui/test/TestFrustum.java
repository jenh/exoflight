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
package com.fasterlight.exo.newgui.test;

import java.awt.BorderLayout;
import java.util.Random;

import javax.media.opengl.*;
import javax.swing.JFrame;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.glout.*;
import com.fasterlight.vecmath.Vector3f;

public class TestFrustum extends GLOAWTComponent
{
	SpaceGame game;

	public TestFrustum(int w, int h)
	{
		super(w, h);
	}

	//

	protected GLOContext makeContext()
	{
		game = new SpaceGame();
		return new GUIContext(game);
	}

	//

	class MyCanvas extends GLODefault3DCanvas
	{
		Random rnd = new Random();
		ViewVolume vvol = new ViewVolume();
		int numballs = 500;

		MyCanvas()
		{
			super();
		}

		float rnd(float range)
		{
			return (rnd.nextFloat() * range * 2) - range;
		}

		float rnd()
		{
			return rnd.nextFloat();
		}

		void drawSphere(GL gl)
		{
			float r = 1;
			Vector3f p = new Vector3f(rnd(25), rnd(25), rnd(25));
			float[] materialcol = { rnd() * r, rnd() * r, rnd() * r, 1 };
			double mindist = 0;

			for (int i = 0; i < 6; i++)
			{
				double d = vvol.distFromPlane(p, i);
				if (d < mindist)
					mindist = d;
				//			System.out.print(vvol.distFromPlane(p,i) + " ");
			}

			materialcol[3] = (float) (1 + mindist);
			if (mindist < -1)
				return;

			//			if (vvol.intersectsSphere(p, 1))
			//			if (vvol.getFrustumFlags(p) == 0)
			{
				gl.glPushMatrix();
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

				gl.glTranslatef(p.x, p.y, p.z);
				gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, materialcol, 0);
				gl.glCallList(((GUIContext) this.getContext()).getNmlSphereIndex(2));

				gl.glPopMatrix();
			}
		}

		public void renderObject(GLOContext ctx)
		{
			GL gl = ctx.getGL();
			TextureCache texcache = ((GUIContext) ctx).texcache;

			gl.glEnable(GL.GL_CULL_FACE);
			gl.glEnable(GL.GL_DEPTH_TEST);

			gl.glEnable(GL.GL_LIGHT0);
			gl.glEnable(GL.GL_LIGHTING);

			rnd.setSeed(0);

			vvol.setup(gl);

			for (int i = 0; i < numballs; i++)
				drawSphere(gl);

		}

	}

	//

	protected void makeComponents()
	{
		super.makeComponents();

		MyCanvas ts = new MyCanvas();
		ts.setSize(ctx.getWidth(), ctx.getHeight());
		ctx.add(ts);

	}

	public static void main(String[] args)
	{
		TestFrustum test = new TestFrustum(640, 480);
		JFrame f = new JFrame();
		GLCanvas canvas = test.createGLCanvas();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(canvas, BorderLayout.CENTER);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.pack();
		f.show();

		test.start(canvas, canvas);
	}

}
