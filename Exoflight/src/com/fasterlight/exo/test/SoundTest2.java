package com.fasterlight.exo.test;

import java.io.File;
import java.util.Random;

import javax.sound.sampled.*;

public class SoundTest2
implements LineListener
{
   public static void main(String args[])
       throws Exception
   {
	   SoundTest2 test = new SoundTest2();
	   test.start(args);
   }

   public void start(String args[])
   throws Exception
   {
       javax.sound.sampled.Mixer.Info ainfo[] = AudioSystem.getMixerInfo();
       for(int i = 0; i < ainfo.length; i++)
           System.out.println(i + " " + ainfo[i]);

       Mixer mixer = AudioSystem.getMixer(ainfo[0]);
       File file = new File(args[0]);
       AudioInputStream audioinputstream = AudioSystem.getAudioInputStream(file);
       AudioFormat audioformat = audioinputstream.getFormat();

       javax.sound.sampled.DataLine.Info info = new DataLine.Info(
       		Clip.class, audioinputstream.getFormat(),
       		(int)audioinputstream.getFrameLength() * audioformat.getFrameSize());
       System.out.println("max lines = " + mixer.getMaxLines(info));

       clip = (Clip)mixer.getLine(info);
       audioinputstream = AudioSystem.getAudioInputStream(file);
       clip.open(audioinputstream);
       clip.addLineListener(this);
       clip.start();
   }

   Clip clip;

   public void update(LineEvent event)
   {
   	if (event.getType() == LineEvent.Type.STOP)
   	{
	   	clip.setFramePosition(1);
   		clip.start();
   	}
   	System.out.println(event);
   }

   static Random rnd = new Random();

}
