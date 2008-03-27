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

import javax.media.opengl.GL;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.newgui.GUIContext;
import com.fasterlight.exo.orbit.*;
import com.fasterlight.proctex.*;

/**
  * A sector mesh is a 18x18 grid of triangles that is generated
  * from a texture quad.  That's a total of 648 tris. (18*18*2)
  * Since quads have a 1 pixel border, some of the quad will overlap.
  * No matter.  We'll overlap the quads when rendering!
  *
  * Issues:
  * - with just a 1 pixel border, we are not always going to be able to
  *   do overlap with just 1 quad.  We may have to fetch a neighboring quad.
  * - there may still be small cracks, we will fill them using the Z buffer.

  if we have a 1 pixel border, we have 254x254 grid squares to mesh.

  */
public class SectorMeshProvider
{
	Planet planet;
	GL gl;
	float refrad;
	ElevationModel elevmodel;

	public static final int GRID_SPACING = 16;
	public static final int GRID_SIZE = 18; // maximum
	public static final int NUM_VERTS = GRID_SIZE*GRID_SIZE; // maximum
	public static final int BORDER = GUIContext.BORDER;

	int levelScale = 4;

	Acme.LruHashtable meshCache = new Acme.LruHashtable(1024); //const

	//

	public SectorMeshProvider(Planet planet, GL gl)
	{
		this.planet = planet;
		this.gl = gl;
		this.refrad = (float)planet.getRadius();
		this.elevmodel = planet.getElevationModel();
	}

	Mesh getMeshNoCache(TexKey key)
	{
		if (key.level < 8+levelScale)
			throw new IllegalArgumentException(key + " has too low a level");
		Mesh mesh = new Mesh(key);
		int sc = levelScale;
		TexQuad quad = elevmodel.getTexQuad(key.x>>sc, key.y>>sc, key.level-sc);
		mesh.build(quad);
		return mesh;
	}

	public Mesh getMesh(TexKey key)
	{
		Mesh mesh = (Mesh)meshCache.get(key);
		if (mesh == null)
		{
			mesh = getMeshNoCache(key);
			meshCache.put(new TexKey(key), mesh);
		}
		return mesh;
	}


	//

	public class Mesh
	{
		TexKey key;
		TexKey elev_key;
		TexKey texture_key;
		float[] verts;
		int[][] inds;
		int w,h;
		int px,py;

		Mesh(TexKey key)
		{
			this.key = new TexKey(key);
		}

		public void render()
		{
			GL gl = SectorMeshProvider.this.gl;

			gl.glPushClientAttrib( GL.GL_CLIENT_VERTEX_ARRAY_BIT );
			gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
// todo: why can't lock?
//			gl.glLockArraysEXT(0, verts.length);
			// TODO: this just wont work so i wont bother porting it
			/*
			gl.glInterleavedArrays( GL.GL_T2F_N3F_V3F, 0, verts );
			for (int y=0; y<h-1; y++)
				gl.glDrawElements( GL.GL_TRIANGLE_STRIP, w*2, GL.GL_UNSIGNED_INT, inds[y] );
				*/
//			gl.glUnlockArraysEXT();
			gl.glPopClientAttrib();
		}

		// first get integer coordinate of point, then translate to lat/long

		public double getLat(TexKey q, int ypos)
		{
			int yy = (q.y*(GUIContext.USABLE_TEX_SIZE))+ypos-BORDER;
			int denom = (1<<q.level) - (BORDER<<(q.level-7));
			return Math.PI/2 - yy*Math.PI/denom;
		}

		public double getLon(TexKey q, int xpos)
		{
			int xx = (q.x*(GUIContext.USABLE_TEX_SIZE))+xpos-BORDER;
			int denom = (1<<q.level) - (BORDER<<(q.level-7));
			return -Math.PI + xx*Math.PI/denom;
 		}

 		/**
 		  * Sets the tex coords based on the texture coming from "tq"
 		  */
 		public void setTexCoords(TexKey tq)
 		{
 			if (texture_key != null && texture_key.equals(tq))
 				return; // already built
 			this.texture_key = new TexKey(tq);

 			int i = 0;
			float tx,ty,tx0,ty0,txinc,tyinc;

			// find scaling parameters and origin for tex
			int levdiff = key.level - tq.level;
			txinc = tyinc = 1f/((1<<levdiff)*(GRID_SIZE-1));
			tx0 = (key.x - (tq.x<<levdiff))*1f/(1<<levdiff);
			ty0 = (key.y - (tq.y<<levdiff))*1f/(1<<levdiff);

System.err.println("setting " + key + " for tex " + tq);
System.err.println("  orig=" + tx0+","+ty0 + " inc=" + txinc+","+tyinc);

			ty = ty0;
			for (int y=0; y<h; y++)
			{
				tx = tx0;
				for (int x=0; x<w; x++)
				{
					// tex coords
					verts[i] = tx;
					verts[i+1] = ty;
					i += 8;
					tx += txinc;
				}
				ty += tyinc;
			}
 		}

		void build(TexQuad quad)
		{
			this.elev_key = new TexKey(quad);

			this.verts = new float[(2+3+3)*NUM_VERTS]; // GL.GL_T2F_N3F_V3F
			this.inds = new int[GRID_SIZE-1][GRID_SIZE*2];

			// figure out what our upper-left coordinate of this
			// tex quad is, and the scaling factor
			int levdiff = key.level - quad.level;
			int sc = GUIContext.TEX_SIZE/((1<<levdiff)*GRID_SPACING);
			int px = (key.x - (quad.x<<levdiff))*GRID_SPACING;
			int py = (key.y - (quad.y<<levdiff))*GRID_SPACING;
			w = Math.min(GRID_SIZE, 256-px);
			h = Math.min(GRID_SIZE, 256-py);
			if (levdiff < 0 || px < 0 || py < 0 || px > 256-w || py > 256-h)
				throw new IllegalArgumentException(key + " is not contained in " + quad + ": " + px + " " + py);
			this.px = px;
			this.py = py;
if (debug) {
	System.err.println(key + " from " + quad);
	System.err.println(levdiff + ", sc=" + sc);
	System.err.println(px + "," + py);
}

			// build table of sin/cos for longitude
			float[] sin_lon = new float[GRID_SIZE];
			float[] cos_lon = new float[GRID_SIZE];
			double lat0 = getLat(quad,py);
			double lon0 = getLon(quad,px);
			double latinc = (getLat(quad,py+1) - lat0);
			double loninc = (getLon(quad,px+1) - lon0);
if (debug) {
	System.err.println("lat=" + lat0 + ", latinc=" + latinc);
	System.err.println("lon=" + lon0 + ", loninc=" + loninc);
}
			for (int i=0; i<w; i++)
			{
				double ang = lon0 + loninc*i;
				sin_lon[i] = (float)Math.sin(ang);
				cos_lon[i] = (float)Math.cos(ang);
			}

			byte[] arr = quad.getByteData();
			int p = px+py*GUIContext.TEX_SIZE;
			int i = 0;
			double curlat = lat0;

if (outputObj) {
	System.out.println("g mesh");
}
			for (int y=0; y<h; y++)
			{
				float sy = (float)Math.sin(curlat);
				float cy = (float)Math.cos(curlat);
				curlat += latinc;
				for (int x=0; x<w; x++)
				{
					byte b = arr[p];
					p += sc;
					float r = refrad + quad.byteToValue(b);
					float sx = sin_lon[x];
					float cx = cos_lon[x];
					// skip tex coords for now
					i += 2;
					// normals
					verts[i++] = r*cy*cx;
					verts[i++] = r*cy*sx;
					verts[i++] = r*sy;
					// vertex
					verts[i++] = r*cy*cx;
					verts[i++] = r*cy*sx;
					verts[i++] = r*sy;
if (outputObj) {
	System.out.println("v " + verts[i-3] + " " + verts[i-2] + " " + verts[i-1]);
}
				}
				p += (GUIContext.TEX_SIZE - (w*sc))*sc;
			}

			// now draw tris
			p = 0;
			i = 0;
			for (int y=0; y<h-1; y++)
			{
				for (int x=0; x<w; x++)
				{
					inds[y][x*2] = p;
					inds[y][x*2+1] = p+w;
					p++;
if (outputObj && x < w-1 && y < h-1) {
	System.out.println("f " + (p) + " " + (p+1) + " " + (p+w));
	System.out.println("f " + (p+1) + " " + (p+1+w) + " " + (p+w));
}
				}
			}
		}
	}

	static boolean debug = false;
	static boolean outputObj = false;

	//

	public static void main(String[] args)
	throws Exception
	{
		int x = Integer.parseInt(args[1]);
		int y = Integer.parseInt(args[2]);
		int l = Integer.parseInt(args[0]);

		SpaceGame game = new SpaceGame();
		game.start();
		Planet planet = (Planet)game.getBody("Luna");
		SectorMeshProvider smp = new SectorMeshProvider(planet, null);

		TexKey key = new TexKey(x,y,l);
		Mesh mesh = smp.getMesh(key);
		System.out.println(mesh);
	}
}
