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
import java.net.URL;
import java.text.*;
import java.util.*;

import com.fasterlight.exo.ship.*;
import com.fasterlight.game.*;
import com.fasterlight.sound.*;
import com.fasterlight.spif.*;
import com.fasterlight.util.*;

/**
  * Implements the sound for an Exoflight game.
  * It has several components.
  * todo: bad mojo and weird logic here
  * @see SoundServer
  * @see PropertySoundRenderer
  * @see TextToSpeechRenderer
  * @see VoiceCalloutRenderer
  */
public class GameSound
implements PropertyAware
{
	SoundServer sserver;
	PropertySoundRenderer sndrend;
	TextToSpeechRenderer ttsr, alarms;
	VoiceCalloutRenderer vcrend;
	Set old_modules = new HashSet();
	String BASE_URL = com.fasterlight.io.IOUtil.findBaseURLForResource("sounds/sounddef.txt") + "../";

	//

	private static GameSound gamesound;

	public static GameSound getGameSound()
	{
		if (gamesound == null)
		{
			gamesound = new GameSound();
			gamesound.setOpened(soundEnabled);
		}
		return gamesound;
	}

	GameSound()
	{
		vcrend = new VoiceCalloutRenderer();
		try {
			INIFile ini = new CachedINIFile(
				ClassLoader.getSystemResourceAsStream("sounds/calloutdef.txt") );
			vcrend.setINI(ini);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void initSound()
	throws Exception
	{
		if (sserver == null)
			sserver = new JMFSoundServer(new URL(BASE_URL));

		ttsr = new TextToSpeechRenderer();
		ttsr.loadTransTable("sounds/transtbl.txt");
//		ttsr.debug = true;
		ttsr.setSoundServer(sserver);
		ttsr.setPrefix("sounds/");

		alarms = new TextToSpeechRenderer();
		alarms.loadTransTable("sounds/events.txt");
		alarms.setSoundServer(sserver);
		alarms.setPrefix("sounds/");
		alarms.setQueueing(false);
	}

	public boolean isOpened()
	{
		return (sserver != null) && sserver.isOpen();
	}

	public void setOpened(boolean b)
	{
		try {
			if (b)
			{
				initSound();
				reset();
				System.out.println("Sound enabled");
			}
			else if (!b && sserver != null)
			{
				close();
				System.out.println("Sound disabled");
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	  * @deprecated
	  */
	public boolean isActivated()
	{
		return isOpened();
	}

	private void updateShipSounds(SpaceShip ship)
	throws Exception
	{
		Game game = ship.getUniverse().getGame();

		// update sounds for the current ship
		sndrend.setPropertyTop(ship);
		sndrend.updateGroup("SHIPSOUNDS");

		// now do each module
		Structure struct = ship.getStructure();
		Iterator it = struct.getModules().iterator();
		while (it.hasNext())
		{
			Module module = (Module)it.next();
			old_modules.add(module);
		}

		// use old_modules to make sure no modules get left out
		it = old_modules.iterator();
		while (it.hasNext())
		{
			Module module = (Module)it.next();
			sndrend.setPropertyTop(module);
			sndrend.updateGroup(module.getName());
			// remove any modules that ain't in the structure anymore
			if (module.getStructure() != struct)
				it.remove();

			// iterate over capabilities
			Iterator capit = module.getCapabilities().iterator();
			while (capit.hasNext())
			{
				Capability cap = (Capability)capit.next();
				String soundattr = (String)cap.getAttributes().get("sounddef");
				if (soundattr != null)
				{
					sndrend.setPropertyTop(cap);
					sndrend.updateGroup(soundattr);
				}
			}
		}
	}

	private void updateVoiceCallouts(SpaceShip ship)
	{
		Game game = ship.getUniverse().getGame();

		vcrend.setPropertyTop(ship);
		vcrend.update(game.time());
	}

	public void update(SpaceShip ship)
	{
		if (!isOpened())
			return;

		try {
			updateShipSounds(ship);

			if (calloutsEnabled)
			{
				updateVoiceCallouts(ship);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
			try {
				close();
			} catch (Exception exc2) {
			}
		}
	}

	public void setCalloutGroup(String group)
	{
		vcrend.clearCallouts();
		vcrend.addCalloutGroup(group);
	}

	public void close()
	throws Exception
	{
		if (sndrend != null)
		{
			sndrend.close();
			sndrend = null;
		}
		sserver.close();
		vcrend.close();
	}

	public void reset()
	throws Exception
	{
		INIFile ini;

		if (sserver == null)
			return;

		close();
		sserver.open();

		if (!isOpened())
			return;

		ini = new CachedINIFile(
				ClassLoader.getSystemResourceAsStream("sounds/sounddef.txt") );
		sndrend = new PropertySoundRenderer();
		sndrend.setINI(ini);
		sndrend.setSoundServer(sserver);
		sndrend.open();

		vcrend.setTextToSpeechRenderer(ttsr);
		vcrend.open();
	}

	public void playSound(String soundname, int playflags)
	{
		if (isOpened())
		{
			SoundClip clip = sserver.getClip(soundname, 0);
			sserver.play(clip, playflags);
		}
	}

	public void playSound(String soundname)
	{
		playSound(soundname, 0);
	}

	public void queueSound(String soundname)
	{
		playSound(soundname, SoundServer.QUEUE);
	}

	public void setNumberFormat(String fmt)
	{
		this.numfmt = new DecimalFormat(fmt);
	}

	NumberFormat numfmt = new DecimalFormat("###.# ");

	//

	StringBuffer saybuf = new StringBuffer();

	public void saynum(Object num)
	{
		if (ttsr == null || !speechEnabled)
			return;
		double d = PropertyUtil.toDouble(num);
		String s = numfmt.format(d);
		if (s.endsWith(".0"))
			s = s.substring(0,s.length()-2);
		says(s);
	}


	public void says(String s)
	{
		if (ttsr == null || !speechEnabled)
			return;
		saybuf.append(s);
	}

	public void say(String s)
	{
		if (ttsr == null || !speechEnabled)
			return;
		saybuf.append(s);
		ttsr.say(saybuf.toString());
		saybuf.setLength(0);
	}

	public void alertCode(String s)
	{
		if (alarms==null || s==null) // this should never be null, actually
			return;
		alarms.say(s.replace('-',' '));
	}

	// PROPERTIES

	private static PropertyHelper prophelp = new PropertyHelper(GameSound.class);

	static {
		prophelp.registerSet("opened", "setOpened", boolean.class);
		prophelp.registerGet("opened", "isOpened");
		prophelp.registerSet("play", "playSound", String.class);
		prophelp.registerSet("queue", "queueSound", String.class);
		prophelp.registerSet("numfmt", "setNumberFormat", String.class);
		prophelp.registerSet("saynum", "saynum", Object.class);
		prophelp.registerSet("says", "says", String.class);
		prophelp.registerSet("say", "say", String.class);
		prophelp.registerSet("calloutgroup", "setCalloutGroup", String.class);
	}

	public Object getProp(String key)
	{
		return prophelp.getProp(this, key);
	}

	public void setProp(String key, Object value)
	{
		prophelp.setProp(this, key, value);
	}

	// SETTINGS

	static boolean soundEnabled;
	static boolean speechEnabled;
	static boolean calloutsEnabled;

	static SettingsGroup settings = new SettingsGroup(GameSound.class, "Sound")
	{
		public void updateSettings()
		{
			soundEnabled = getBoolean("SoundEnabled", true);
			speechEnabled = getBoolean("SpeechEnabled", true);
			calloutsEnabled = getBoolean("CalloutsEnabled", true);
			if (gamesound != null)
			{
				gamesound.setOpened(soundEnabled);
			}
		}
	};

}

