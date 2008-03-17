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

import java.util.*;

/**
  * Stores x/y coordinates for the picking mechanism.
  */
public class PickList
{
	List recs = new ArrayList();
	Map objs = new HashMap();

	//

	class PickRec
	{
		float x,y,radius;
		Object obj;
	}

	public PickList()
	{
	}

	public void addPickRec(float x, float y, float rad, Object o)
	{
		PickRec rec = new PickRec();
		rec.x = x;
		rec.y = y;
		rec.radius = rad;
		rec.obj = o;
		recs.add(rec);
		objs.put(o, rec);
	}

	public PickRec getPickRecFor(Object o)
	{
		return (PickRec)objs.get(o);
	}

	public Object pickObject(float x, float y)
	{
		PickRec bestrec = pickObjectRec(x,y);
		return (bestrec != null) ? bestrec.obj : null;
	}

	public PickRec pickObjectRec(float x, float y)
	{
		float bestscore = 0;
		PickRec bestrec = null;
		for (int i=0; i<recs.size(); i++)
		{
			PickRec rec = (PickRec)recs.get(i);
			float dx = x-rec.x;
			float dy = y-rec.y;
			float d = (float)Math.sqrt(dx*dx+dy*dy);
//			System.out.println("d=" + d + ",radius=" + rec.radius);
			if (d < rec.radius)
			{
				float score = (rec.radius-d)*100f/(rec.radius*rec.radius);
				if (score > bestscore)
				{
					bestscore = score;
					bestrec = rec;
				}
			}
		}
		return bestrec;
	}

	public void clear()
	{
		recs.clear();
		objs.clear();
	}
}
