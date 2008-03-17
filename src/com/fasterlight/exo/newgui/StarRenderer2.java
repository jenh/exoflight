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

import java.io.*;
import java.util.*;

import javax.media.opengl.GL;

import com.fasterlight.exo.orbit.*;
import com.fasterlight.vecmath.Vector3f;

public class StarRenderer2
implements Constants
{
	static final int NUM_DLISTS = 28;
	static final float DLIST_GRAN = 0.333f;
	static final float ALPHA_MULT = (float)(Math.pow(2, -DLIST_GRAN));

	int starsList;
	float alpha_fudge = 1.075f;

	// nextver: use quadtree for star db
   List stars;

   static final float MIN_STAR_MAG = -1.47f;
   static final float MIN_ALPHA_SCALE = 15/256f;

	public StarRenderer2()
	{
	}

	public void loadStars()
	{
		try {
		  BufferedReader in = new BufferedReader(new InputStreamReader(
   	   	ClassLoader.getSystemResourceAsStream("orbits/adc5050.txt")));
			stars = SkyObject.readSkyObjects(in);
		  in.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
   }

   public void render(GUIContext guictx, float alpha_scale)
   {
		if (alpha_scale < MIN_ALPHA_SCALE)
			return;
   	GL gl = guictx.getGL();
		gl.glPushAttrib(GL.GL_ENABLE_BIT);
		gl.glBindTexture(GL.GL_TEXTURE_2D, guictx.tex_ints[0]);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glDisable(GL.GL_DEPTH_TEST);
		for (int i=0; i<NUM_DLISTS; i++)
		{
			if (alpha_scale < MIN_ALPHA_SCALE)
				break;
			render(gl, i, alpha_scale);
//			System.out.println(i + ", as=" + alpha_scale);
			alpha_scale *= ALPHA_MULT*alpha_fudge;
		}
		gl.glPopAttrib();
   }

   private void render(GL gl, int nlist, float alpha)
   {
   	if (alpha > 1)
   		alpha = 1;
		gl.glTexCoord1f((alpha+0.5f)/2);
		gl.glCallList(starsList + nlist);
   }

	public void setup(GUIContext guictx)
	{
		// draw stars into display list
		GL gl = guictx.getGL();
		starsList = gl.glGenLists( NUM_DLISTS );
		for (int i=0; i<NUM_DLISTS; i++)
		{
			gl.glNewList( starsList+i, GL.GL_COMPILE );
			renderGL(gl, MIN_STAR_MAG+i*DLIST_GRAN, MIN_STAR_MAG+(i+1)*DLIST_GRAN);
			gl.glEndList();
		}
		stars = null; // to save memory
	}

	float getStarColor(char ch)
	{
		switch (ch)
		{
			case 'O' : return 0.6f;
			case 'B' : return 0.8f;
			case 'A' : return 1f;
			case 'F' : return 1.1f;
			case 'G' : return 1.2f;
			case 'K' : return 1.3f;
			case 'M' : return 1.4f;
			default : return 1f;
		}
	}

	private void renderGL(GL gl, float minmag, float maxmag)
	{
		if (stars == null)
			return;

		gl.glBegin( GL.GL_POINTS );
		Iterator it = stars.iterator();
		int ndrawn = 0;
		while (it.hasNext())
		{
			SkyObject skyobj = (SkyObject)it.next();
			float mag = skyobj.getMag();
			if (mag >= minmag && mag < maxmag)
			{
				Vector3f gp = skyobj.getGalPos();
				float col = getStarColor(skyobj.getSpectralClass());
				float b = (col<0) ? col : 1.0f/col;
				gl.glColor3f(b*col, b, b/col);
				gl.glVertex3f(gp.x, gp.y, gp.z);
				ndrawn++;
			}
		}
		gl.glEnd();
//		System.out.println("# stars = " + ndrawn);
	}

}
