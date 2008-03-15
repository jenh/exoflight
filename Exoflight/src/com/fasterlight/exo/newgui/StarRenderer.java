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

public class StarRenderer
implements Constants
{
	int starsList;

   List stars;
   float MAX_STAR_MAG = 2f;

	public StarRenderer()
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

   public void render(GL gl)
   {
   	gl.glCallList(starsList);
   }

	public void setup(GL gl)
	{
		// draw stars into display list
		starsList = gl.glGenLists( 1 );
		gl.glNewList( starsList, GL.GL_COMPILE );
		renderGL(gl);
		gl.glEndList();
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

	public void renderGL(GL gl)
	{
		if (stars == null)
			return;

		gl.glBegin( GL.GL_POINTS );
		Iterator it = stars.iterator();
		while (it.hasNext())
		{
			SkyObject skyobj = (SkyObject)it.next();
			Vector3f gp = skyobj.getGalPos();
			float b = MAX_STAR_MAG/skyobj.getMag();
			float col = getStarColor(skyobj.getSpectralClass());
			gl.glColor3f(b*col, b, b/col);
			gl.glVertex3f(gp.x, gp.y, gp.z);
		}
		gl.glEnd();
	}

}
