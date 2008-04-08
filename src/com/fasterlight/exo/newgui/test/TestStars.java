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

public class TestStars extends GLOAWTComponent
{
	SpaceGame game;

	public TestStars(int w, int h)
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
		MyCanvas()
		{
			super();
		}


		public void renderObject(GLOContext ctx)
		{
			GL gl = ctx.getGL();
			float sr = fardist/2;
			gl.glPushMatrix();
			gl.glScalef(sr, sr, sr);
			((GUIContext)ctx).renderStars(5);
			gl.glPopMatrix();
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
		TestStars test = new TestStars(640, 480);
		JFrame f = new JFrame();
		GLCanvas canvas = test.createGLCanvas();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(canvas, BorderLayout.CENTER);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.show();

		test.start(canvas, canvas);
	}

}
