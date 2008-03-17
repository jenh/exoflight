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
package com.fasterlight.exo.newgui;

import java.awt.Point;
import java.nio.IntBuffer;

import javax.media.opengl.GL;

import com.fasterlight.exo.orbit.Conic;
import com.fasterlight.exo.orbit.nav.*;
import com.fasterlight.exo.ship.SpaceShip;
import com.fasterlight.glout.*;
import com.fasterlight.util.Rect4f;
import com.sun.opengl.util.BufferUtil;

/**
  * Delta-V graph of the TOFOptimizer's output.
  */
public class PorkchopPlot extends GLOComponent
{
	int lastqtsize;
	int texint = -1;
	int texw = 128;
	int texh = 128;
	IntBuffer texdata = BufferUtil.newIntBuffer(texw * texh);

	//

	TOFOptimizer getTOFOptimizer()
	{
		SpaceShip ship = (SpaceShip) getForPropertyKey("ship");
		return ship.getShipTargetingSystem().getTOFOptimizer();
	}

	void processMouse(GLOMouseEvent mev)
	{
		TOFOptimizer opt = getTOFOptimizer();

		Point o = this.getOrigin();
		int mx = mev.getX() - o.x;
		int my = mev.getY() - o.y;
		if (opt != null)
		{
			float x = mx * 1f / this.getWidth();
			float y = my * 1f / this.getHeight();
			long t1 = opt.x2time(x);
			long t2 = opt.y2time(y);
			long t = ((GUIContext) ctx).game.time();
			double cost = opt.getCost(x, y);
			//			deltavLabel.setText(AstroUtil.toDistance(cost) + "/s");

			//todo
			Conic selorbit;
			long seltof, seltime;
			try
			{
				selorbit = opt.getConicFor(x, y);
				seltof = t2;
				seltime = t1;
			}
			catch (NavigationException nave)
			{
				selorbit = null;
			}
		}
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOMouseButtonEvent)
		{
			GLOMouseButtonEvent mbe = (GLOMouseButtonEvent) event;
			if (mbe.isPressed(1))
			{
				processMouse(mbe);
				this.beginDrag(event);
				return true;
			}
			else
			{
				this.endDrag(event);
				return true;
			}
		}
		else if (event instanceof GLOMouseMovedEvent)
		{
			if (this.isDragging())
			{
				processMouse((GLOMouseMovedEvent) event);
				return true;
			}
		}
		return super.handleEvent(event);
	}

	void renderTexture(GL gl)
	{
		TOFOptimizer opt = getTOFOptimizer();

		int width = this.getWidth();
		int height = this.getHeight();
		float mincost = (float) opt.getMinCost();
		float maxcost = (float) opt.getMaxCost();
		maxcost = Math.min(25, maxcost); // todo: const
		Rect4f r = opt.getBounds();

		int size = opt.getSize();
		lastqtsize = size;

		float rad = (1 / 24f);
		float xx, yy;
		int i = 0, color, lum;

		texdata.rewind();
		for (int y = 0; y < texh; y++)
		{
			yy = y * 1f / texh;
			for (int x = 0; x < texw; x++)
			{
				xx = x * 1f / texw;
				lum = (int) (opt.getInterpCost(xx, yy, rad) * 255.0f / maxcost);
				if (lum > 255)
					lum = 255;
				color = lum | (((lum == 0) ? 0 : 255 - lum) << 8);
				texdata.put(color);
			}
		}


		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, texw, texh, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, texdata);
	}

	public void render(GLOContext ctx)
	{
		GL gl = ctx.getGL();
		TOFOptimizer opt = getTOFOptimizer();

		if (opt != null)
		{
			if (texint < 0)
			{
				int[] arr = new int[1];
				gl.glGenTextures(1, arr, 0);
				texint = arr[0];
				gl.glBindTexture(GL.GL_TEXTURE_2D, texint);
				gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
				IntBuffer zerobytes = BufferUtil.newIntBuffer(texw * texh);
				gl.glTexImage2D(
					GL.GL_TEXTURE_2D,
					0,
					GL.GL_RGB5,
					texw,
					texh,
					0,
					GL.GL_RGBA,
					GL.GL_UNSIGNED_BYTE,
					zerobytes);
			}
			if (opt.getSize() != lastqtsize)
			{
				gl.glBindTexture(GL.GL_TEXTURE_2D, texint);
				renderTexture(gl);
			}
		}

		if (texint >= 0)
		{
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glBindTexture(GL.GL_TEXTURE_2D, texint);
			Point o = this.getOrigin();
			drawTexturedBox(ctx, o.x, o.y + this.getHeight(), this.getWidth(), -this.getHeight());
		}

	}
}
