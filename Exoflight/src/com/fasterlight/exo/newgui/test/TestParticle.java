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

import javax.media.opengl.*;
import javax.swing.JFrame;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.newgui.particle.ParticleSystemContext;
import com.fasterlight.glout.*;
import com.fasterlight.vecmath.*;

public class TestParticle extends GLOAWTComponent
{
	SpaceGame game;
	String partsysname = "Orange Exhaust";

	public TestParticle(int w, int h)
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

	class ParticleCanvas extends GLOZoomable3DCanvas
	{
		ParticleSystemContext psc;

		ParticleCanvas()
		{
			super();

			reloadParticleSystem();

			GLOLabel lab = new GLOLabel(30)
			{
				protected String computeText()
				{
					StringBuffer st = new StringBuffer();
					ParticleSystemContext cur_psc = psc;
					while (cur_psc != null)
					{
						st.append(cur_psc.getParticleSystem().capacity());
						st.append(' ');
						cur_psc = cur_psc.getNext();
					}
					return st.toString();
				}
			};
			add(lab);
			lab.setPosition(8, 8);
		}

		void reloadParticleSystem()
		{
			try
			{
				psc = ParticleSystemContext.load(partsysname);
			}
			catch (Exception exc)
			{
				exc.printStackTrace();
			}
		}

		public void renderObject(GLOContext ctx)
		{
			gl.glPushAttrib(GL.GL_ENABLE_BIT);

			psc.setTime(System.currentTimeMillis());

			Vector3f pos = new Vector3f(0, 0, 0);
			Vector3f vel = new Vector3f(1, 0, 0);

			psc.beginStreaming();
			psc.updateParticles(pos, vel, 1, 1, 1, 1);
			psc.endStreaming();

			Matrix3f mat = getTrackballMatrix();
			psc.render(mat);

			gl.glPopAttrib();
		}

		public boolean handleEvent(GLOEvent event)
		{
			if (event instanceof GLOKeyEvent)
			{
				GLOKeyEvent ke = (GLOKeyEvent) event;
				switch (ke.getKeyCode())
				{
					case GLOKeyEvent.VK_F9 :
						reloadParticleSystem();
						break;
				}
			}

			return super.handleEvent(event);
		}

	}

	//

	protected void makeComponents()
	{
		super.makeComponents();

		ParticleCanvas ts = new ParticleCanvas();
		ts.setSize(ctx.getWidth(), ctx.getHeight());
		ctx.add(ts);

	}

	public static void main(String[] args)
	{
		TestParticle test = new TestParticle(640, 480);
		JFrame f = new JFrame();
		GLCanvas canvas = test.createGLCanvas();

		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(canvas, BorderLayout.CENTER);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.pack();
		f.show();

		if (args.length > 0)
			test.partsysname = args[0];

		test.start(canvas, canvas);
	}

}
