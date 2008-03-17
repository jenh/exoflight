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

import javax.media.opengl.GL;

import com.fasterlight.glout.*;

/**
  * Manages "hints" (bits of text that appear on the screen
  * when the mouse is moved over something interesting)
  * associated with a PickList
  * todo: min. time for hint
  * todo: smart positioning
  * todo: multiple hits
  */
public class HintRenderer
{
	GLOComponent cmpt;

	int HINT_EXPIRE_TIME = 3000;
	int HINT_SUSTAIN_TIME = 2250;
	Map hints = new HashMap();

	public HintRenderer(GLOComponent cmpt)
	{
		this.cmpt = cmpt;
	}

	public GLOContext getContext()
	{
		return cmpt.getContext();
	}

	public int size()
	{
		return hints.size();
	}

	class HintRec
	{
		Object obj;
		String text;
		long t0;
		HintRec(Object o, String str)
		{
			this.obj=o;
			this.text=str;
			this.t0 = getContext().getFrameStartMillis();
		}
		float getAlpha()
		{
			long t = getContext().getFrameStartMillis() - t0;
			if (t > HINT_EXPIRE_TIME)
				return -1;
			else if (t < HINT_SUSTAIN_TIME)
				return 1;
			else
				return (HINT_EXPIRE_TIME-t)*1f/(HINT_EXPIRE_TIME-HINT_SUSTAIN_TIME);
		}
	}

	public void showHintFor(Object obj, String str)
	{
		HintRec hr = new HintRec(obj, str);
		hints.put(obj, hr);
	}

	public void renderHints(GLOContext ctx, PickList picklist)
	{
		GLOShader shader = ctx.getShader("hinttext");
		if (shader == null)
			return;
		shader.set(ctx);
//		Point o = this.getOrigin();

		Iterator it = hints.values().iterator();
		while (it.hasNext())
		{
			HintRec hrec = (HintRec)it.next();
			float a = hrec.getAlpha();
			if (a < 0) {
				it.remove();
			} else {
				PickList.PickRec prec = picklist.getPickRecFor(hrec.obj);
				if (prec != null)
				{
					renderHint(shader, hrec, prec.x, prec.y, a);
				}
			}
		}
	}

	public void renderHint(GLOShader shader, HintRec hrec, float x, float y, float a)
	{
		GL gl = getContext().getGL();
		shader.setColor(gl, a);
		getContext().getFontServer().drawText(hrec.text, x, y);
	}

}
