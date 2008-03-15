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

import com.fasterlight.exo.ship.Module;
import com.fasterlight.glout.TextureCache;
import com.fasterlight.io.IOUtil;
import com.fasterlight.model.*;
import com.fasterlight.util.ArrayKey;

/**
  * A cache for ModelRenderer objects.
  */
public class ModelRenderCache
{
	Map models = new WeakHashMap();
   Map mrends = new HashMap();
   GL gl;
   TextureCache texcache;

   //

	public ModelRenderCache(GL gl, TextureCache texcache)
	{
		this.gl = gl;
		this.texcache = texcache;
	}

   protected Model3d loadModel(String name)
   {
   	try {
   		Model3d model = (Model3d)IOUtil.readSerializedObject(name + ".esm", true);
   		if (model != null)
   			return model;
   	} catch (Exception ioe) {
   		System.out.println(ioe);
   	}

   	try {
   		String path = "models/" + name + ".lwo";
   		InputStream in = ClassLoader.getSystemResourceAsStream(path);
   		if (in == null)
   		{
   			System.out.println("Couldn't find model " + name);
   			return null;
   		}
   		DataInputStream din = new DataInputStream(in);
   		LWOReader lr = new LWOReader(din);
   		lr.readLWO();
   		lr.close();
   		return lr.getModel();
   	} catch (IOException ioe) {
   		ioe.printStackTrace();
   		return null;
   	}
   }

   protected Model3d getModel(String name)
   {
   	Object o = models.get(name);
   	if (o == null)
   	{
   		System.out.println("Loading model " + name);
   		Model3d model = loadModel(name);
   		if (model == null)
   		{
   			System.out.println("Couldn't load model " + name);
   			models.put(name, Boolean.FALSE);
   		} else
	   		models.put(name, model);
   		return model;
   	} else {
   		if (o == Boolean.FALSE)
   			return null;
   		else
		   	return (Model3d)o;
	   }
   }

   public ModelRenderer getModelRenderer(Module m)
   {
   	String name = m.getName();
   	return getModelRenderer(name);
   }

   public ModelRenderer getModelRenderer(String name, int flags)
   {
   	ArrayKey key = new ArrayKey(new Object[] { name, new Integer(flags) });
   	Object mrend = mrends.get(key);
   	if (mrend == Boolean.FALSE)
   		return null;
   	else if (mrend == null)
   	{
   		Model3d model = getModel(name);
   		if (model == null)
   		{
   			mrends.put(key, Boolean.FALSE);
   			return null;
   		}
	   	mrend = new ModelRenderer(model, gl, texcache, flags);
	   	mrends.put(key, mrend);
   	}
   	return (ModelRenderer)mrend;
   }

   public ModelRenderer getModelRenderer(String name)
   {
   	return getModelRenderer(name, 0);
   }
}
