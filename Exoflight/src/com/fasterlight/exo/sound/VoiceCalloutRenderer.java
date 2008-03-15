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
package com.fasterlight.exo.sound;

import java.io.IOException;
import java.util.*;

import com.fasterlight.sound.TextToSpeechRenderer;
import com.fasterlight.spif.*;
import com.fasterlight.util.INIFile;

/**
  * This class manages "voice callouts" for a variety of uses.
  * Most often it is meant to be used for reading telemetry from
  * a spaceship.
  *
  */
public class VoiceCalloutRenderer
{
	INIFile ini;
	TextToSpeechRenderer ttsrend;
	PropertyAware top;
	HashMap defmap = new HashMap();
	List activedefs = new ArrayList();
	List activedefnames = new ArrayList();

	public void setINI(INIFile ini)
	{
		this.ini = ini;
	}

	public VoiceCallout loadVoiceCallout(String name)
	throws IOException
	{
		Properties props = ini.getSection(name);

		try {
			String clazz = props.getProperty("class");
			if (clazz == null)
				return null;
			if (clazz.indexOf(".") < 0)
				clazz = "com.fasterlight.exo.sound." + clazz;
			props.remove("class");

			VoiceCallout def = (VoiceCallout)Class.forName(clazz).newInstance();
			PropertyUtil.setFromProps(def, props);
			return def;
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

	public VoiceCallout getVoiceCallout(String name)
	{
		try {
			Object def = defmap.get(name);
			if (def == Boolean.FALSE)
				return null;
			if (def == null)
				def = loadVoiceCallout(name);
			defmap.put(name, (def != null) ? def : Boolean.FALSE);
			return (VoiceCallout)def;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	public void addCallout(String name)
	{
		VoiceCallout def = getVoiceCallout(name);
		if (def != null)
		{
			activedefs.add(def);
			activedefnames.add(name);
		}
		System.out.println("added callout " + name + ": " + def);
	}

	public void removeCallout(String name)
	{
		VoiceCallout def = getVoiceCallout(name);
		if (def != null)
		{
			activedefs.remove(def);
			activedefnames.remove(name);
		}
	}

	public void clearCallouts()
	{
		activedefs.clear();
		activedefnames.clear();
	}

	public void open()
	{
		// reload callouts
		for (int i=0; i<activedefnames.size(); i++)
		{
			activedefs.set(i, getVoiceCallout(activedefnames.get(i).toString()));
		}
	}

	public void close()
	{
		defmap.clear();
	}

	public void addCalloutGroup(String group)
	{
		if (ini == null)
			throw new RuntimeException("Set INI file first");
		String groupkey = '*' + group;
		try {
			int i=0;
			do {
				String tmp = ini.getString(groupkey, "def"+i, "").trim();
				if (tmp.length() == 0)
					break;
				addCallout(tmp);
				i++;
			} while (true);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	//

	public void setPropertyTop(PropertyAware obj)
	{
		this.top = obj;
	}

	public void setTextToSpeechRenderer(TextToSpeechRenderer ttsrend)
	{
		this.ttsrend = ttsrend;
	}

	//

	public void update(long time)
	{
		// if no tts renderer, or is currently queueing another
		// sound, don't update
		if (ttsrend == null || ttsrend.getSoundServer().hasQueued())
			return;

		float bestprio = 1;
		VoiceCallout bestdef = null;

		Iterator it = activedefs.iterator();
		while (it.hasNext())
		{
			VoiceCallout def = (VoiceCallout)it.next();
			def.setTop(this.top);
			float prio = def.getPriority(time);
			if (prio > bestprio)
			{
				bestprio = prio;
				bestdef = def;
			}
		}

		if (bestdef != null)
		{
			String msg = bestdef.getMessage(time);
			System.out.println(">> " + msg);
			ttsrend.say(msg);
			bestdef.reset(time);
		}
	}

	//

	boolean debug = false;

}
