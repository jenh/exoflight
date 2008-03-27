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

import com.fasterlight.exo.seq.*;
import com.fasterlight.glout.*;
import com.fasterlight.spif.*;

/**
  * A list that displays the steps in a Sequencer
  */
public class SequencerList
extends GLOAbstractList
{
	private Sequencer seq;
	private List rows = new ArrayList();
	private TreeMap seq_to_row = new TreeMap(); // links sequence indices to rows
	int selrow,selindex;
	int rowheight, rowwidth;
	int minchars = 30;
	int rowspacing = 2;
	String prop_sequencer;

	public SequencerList()
	{
	}

	public void setSequencer(Sequencer seq)
	{
		if (seq == this.seq)
			return;
		this.seq = seq;

		rows.clear();
		seq_to_row.clear();
		selrow = -1;
		selindex = -1;
		if (seq == null)
			return;

		// build indices of the nodes with descriptions
		// todo: missing 1st row?
		for (int i=0; i<seq.getNodeCount(); i++)
		{
			SequencerNode node = seq.getNode(i);
			if (node.getDescription() != null)
			{
				Integer ii = new Integer(i);
				seq_to_row.put(ii, new Integer(rows.size()));
				rows.add(ii);
			}
		}
	}

	public Sequencer getSequencer()
	{
		return seq;
	}

	public void setPropertyForSequencer(String prop)
	{
		this.prop_sequencer = prop;
	}

	public String getPropertyForSequencer()
	{
		return prop_sequencer;
	}

	public SequencerNode getNodeForRow(int i)
	{
		if (seq == null)
			return null;
		return seq.getNode( ((Integer)rows.get(i)).intValue() );
	}

	///

	public boolean isClickable()
	{
		return false;
	}

	public boolean isSelected(int row)
	{
		return selrow == row;
	}

	public void selectRow(int row)
	{
		if (row != selrow)
		{
			selrow = row;
			centerOnRow(row);
		}
	}

	public int getRowHeight()
	{
		return rowheight;
	}

	public int getRowWidth()
	{
		return rowwidth;
	}

	public int getRowCount()
	{
		return rows.size();
	}

	public void drawRow(GLOContext ctx, int row, int xpos, int ypos, boolean selected)
	{
		GLFontServer fs = ctx.getFontServer();
		SequencerNode node = getNodeForRow(row);
		if (node != null)
		{
			setShader(selected ? "seltext" : "text");
			char statchar = SequencerNode.STATUS_CHARS.charAt(node.getStatus());
			fs.drawText(statchar + " ", xpos, ypos);
			String desc = node.getDescription();
			if (desc == null)
				desc = "---";
			fs.drawText(desc);
		}
	}

	void updateSelectedRow()
	{
		if (seq != null)
		{
			int index = seq.getIndex();
			if (index != selindex)
			{
				selindex = index;
				SortedMap headmap = seq_to_row.headMap(new Integer(index+1));
				if (!headmap.isEmpty())
				{
					Integer rowi = (Integer)seq_to_row.get(headmap.lastKey());
					if (rowi != null)
						selectRow(rowi.intValue());
					else
						selectRow(0);
				} else
					selectRow(0);
			}
		}
	}

	public void render(GLOContext ctx)
	{
		if (prop_sequencer != null)
		{
			setSequencer((Sequencer)getForPropertyKey(prop_sequencer));
		}

		setShader("text");
		GLFontServer fs = ctx.getFontServer();
		rowheight = (int)fs.getTextHeight()+rowspacing;
		rowwidth = (int)fs.getTextWidth()*minchars;

		super.render(ctx);

		updateSelectedRow();
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(SequencerList.class);

	static {
		prophelp.registerGetSet("sequencer", "Sequencer", Sequencer.class);
		prophelp.registerGetSet("sequencer_prop", "PropertyForSequencer", String.class);
	}

	public Object getProp(String key)
	{
		Object o = prophelp.getProp(this, key);
		if (o == null)
			o = super.getProp(key);
		return o;
	}

	public void setProp(String key, Object value)
	{
		try {
			prophelp.setProp(this, key, value);
		} catch (PropertyRejectedException e) {
			super.setProp(key, value);
		}
	}

}
