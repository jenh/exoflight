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
package com.fasterlight.exo.ship;


/**
  * A ResourceSet that is immutable, and wraps another ResourceSet.
  */
public class ImmutableResourceSet
extends ResourceSet
{
	public ImmutableResourceSet(ResourceSet res)
	{
		super.set(res);
	}

	public void set(String spec)
	{
		throw new IllegalArgumentException();
	}

	public void clear()
	{
		throw new IllegalArgumentException();
	}

	public void setAmount(Resource r, float amt)
	{
		throw new IllegalArgumentException();
	}

	public void addAmount(Resource r, float amt)
	{
		throw new IllegalArgumentException();
	}

	public void add(ResourceSet a)
	{
		throw new IllegalArgumentException();
	}

	public void add(ResourceSet a, float scale)
	{
		throw new IllegalArgumentException();
	}

	public void scale(float scale)
	{
		throw new IllegalArgumentException();
	}

	public void set(ResourceSet a)
	{
		throw new IllegalArgumentException();
	}

	public void sub(ResourceSet a)
	{
		throw new IllegalArgumentException();
	}

	public boolean intersect(ResourceSet a)
	{
		throw new IllegalArgumentException();
	}

	public boolean intersect(ResourceSet a, float scale)
	{
		throw new IllegalArgumentException();
	}

	public boolean union(ResourceSet a)
	{
		throw new IllegalArgumentException();
	}

	public void div(ResourceSet res)
	{
		throw new IllegalArgumentException();
	}

	public ResourceSet normalize()
	{
		throw new IllegalArgumentException();
	}

}

