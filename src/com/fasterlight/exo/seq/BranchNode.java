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
package com.fasterlight.exo.seq;


public class BranchNode
extends SequencerNode
{
	protected int nodeidx;

	public BranchNode()
	{
		this.nodeidx = -1;
	}

	public BranchNode(int nodeidx)
	{
		this.nodeidx = nodeidx;
	}

	public int getNodeIndex()
	{
		return nodeidx;
	}

	public void setNodeIndex(int nodeidx)
	{
		this.nodeidx = nodeidx;
	}

	public void branch(int idx)
	{
		this.status = SUCCESS;
		seq.setIndex(idx);
		seq.start();
	}

	protected void startNode()
	{
		branch(nodeidx);
	}

	protected void stopNode()
	{
	}

	public String toString()
	{
		return getDescription() + "(goto #" + nodeidx + ")";
	}

}
