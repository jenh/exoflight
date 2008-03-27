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

import com.fasterlight.glout.GLOUtil;

/**
  * Contains the variance tree for a ROAMPlanet.
  */
public class VarianceTree
{
	int maxlevel;
	short[] tree;

	static final float SCALE = 65536.0f;

	//

	public VarianceTree(int maxlevel)
	{
		this.maxlevel = maxlevel;
		allocTree();
	}

	private void allocTree()
	{
		tree = new short[getTotalTrisForLevel(maxlevel)+8];
	}

	public VarianceTree(String path)
	throws IOException
	{
		InputStream in = GLOUtil.getInputStream(path);
		in = new BufferedInputStream(in);
		maxlevel = in.read() & 0xff;
		allocTree();
		for (int i=0; i<tree.length; i++)
		{
			int x1 = in.read();
			int x2 = in.read();
			if (x1<0 || x2<0)
				throw new IOException("Error reading " + path);
			int x = (x1&0xff) + ((x2&0xff)<<8);
			tree[i] = (short)x;
//			System.out.println(x1 + " " + x2 + " " + Integer.toString(x, 16));
		}
		in.close();
	}

	public float getVarianceForByte(int i)
	{
		return (1l<<(tree[i]&0xff))*(1f/SCALE);
	}

	public float getElevationRatio(int i)
	{
		return ((tree[i]>>>8)&0xff)*(1f/255f);
	}

	public int length()
	{
		return tree.length;
	}

	public static int getTotalTrisForLevel(int l)
	{
		return (l>0) ? getTotalTrisForLevel(l-1) + 8*(1<<l) : 8;
	}
}
