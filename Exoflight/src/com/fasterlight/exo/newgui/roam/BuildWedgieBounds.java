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

import java.io.*;
import java.util.BitSet;

import com.fasterlight.exo.game.SpaceGame;
import com.fasterlight.exo.orbit.Planet;
import com.fasterlight.vecmath.Vector3f;

/**
  * Iterate through all triangles up to a certain level of the
  * sphere and build a n-bit variance tree.
  */
public class BuildWedgieBounds
{
	Planet p;
	ROAMPlanet roam;
	int maxlevel = 12;
	BitSet bits = new BitSet();
	float minrad, maxrad;
	float[] vartree;
	byte[] ivartree;

	float SCALE = 65536.0f; //(1<<30)*1.0f;

	double LN_2 = Math.log(2);

	boolean debug = false;

	//

	public BuildWedgieBounds(Planet p)
	{
		this.p = p;
		this.roam = new ROAMPlanet(null, (float)p.getRadius());
		roam.setElevationModel(p.getElevationModel());
		minrad = roam.minrad;
		maxrad = roam.maxrad;
//		roam.debug = roam.debug2 = true;
	}

	int tris4level(int l)
	{
		return (l>0) ? tris4level(l-1) + 8*(1<<l) : 8;
	}

	public void start()
	{
		System.out.println("Splitting to level " + maxlevel + "...");
		roam.setMaxTriangles( tris4level(maxlevel+1)*2 );
		int vtl = tris4level(maxlevel)+8;
		vartree = new float[vtl];
		ivartree = new byte[vtl*2];
		roam.renderSetup(null, new Vector3f(maxrad*2, 0, 0));
		roam.forceSplit( maxlevel+1 );
		System.out.println("Split, tris = " + roam.numtris);
		int expect = tris4level( maxlevel+1 );
		if (expect != roam.numtris)
		{
			System.out.println /*throw new RuntimeException */
			("Node count mismatch! " + expect + " expected");
		}

		// walk the tree and generate nice little indices
		for (int i=0; i<roam.numseeds; i++)
		{
			TriNode n = roam.trinodes[i];
			walkTree(n, i+8, 0);
		}
		System.out.println("Bitset: " + bits.length() + " " + bits.size());
		int total=0;
		for (int i=0; i<bits.length(); i++)
			if (bits.get(i)) total++;
		System.out.println("Used " + total + " bits");
	}

	void walkTree(TriNode n, int index, int level)
	{
		if (bits.get(index))
			System.out.println("Already had bit " + index);
		bits.set(index);

		// variance
		int ivar;
		float var;
		float alt = 0.5f;

		var = (roam.getMidpointDisplacement2(n)*SCALE);
		ivar = Math.max(0, (int)Math.round(Math.log(var)/LN_2));

		// recurse
		if (level < maxlevel)
		{
			int ni = ((index>>3)<<4) + ((index&7)<<1);
			walkTree(n.lc, ni, level+1);
			walkTree(n.rc, ni+1, level+1);
			var = Math.max(var, Math.max(vartree[ni], vartree[ni+1]));
			ivar = Math.max(ivar, Math.max(ivartree[ni]&0xff, ivartree[ni+1]&0xff));
		}
		if (n.isSplit()) {
			alt = Math.max(0, Math.min(1, (n.lc.p1.length()-minrad)/(maxrad-minrad)));
		} else {
			System.out.println("not split! " + index);
			alt = 0.5f;
		}

		vartree[index] = var;
		int ialt = (int)(alt*255.45f);
		ivartree[index*2] = (byte)ivar;
		ivartree[index*2+1] = (byte)ialt;

		if (debug)
		{
			for (int i=0; i<level; i++)
				System.out.print(' ');
			System.out.println(index + "\t" + ivar + "\t" + var + "\t" + alt);
		}
	}

	public void writeTree(OutputStream out)
	throws IOException
	{
		out.write((byte)maxlevel);
		out.write(ivartree);
		out.write((byte)0xde);
		out.write((byte)0xad);
		out.write((byte)0xbe);
		out.write((byte)0xad);
	}

	//

	public static void main(String[] args)
	throws Exception
	{
		SpaceGame game = new SpaceGame();
		game.start();
		Planet planet = (Planet)game.getBody(args[0]);
		BuildWedgieBounds bwb = new BuildWedgieBounds(planet);
		int maxlev = 12;
		if (args.length > 1)
			maxlev = Integer.parseInt(args[1]);
		bwb.maxlevel = maxlev;
		bwb.start();

		String destfile = planet.getName() + ".vtr";
		System.out.println("writing " + destfile);
		bwb.writeTree(new FileOutputStream(destfile));
	}
}
