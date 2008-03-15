package com.fasterlight.exo.test;

import java.io.File;
import java.util.Random;

import javax.sound.sampled.*;

public class SoundTest
{
    public static void main(String args[])
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

		for (int i=0; i<32; i++)
		{
        Clip clip = (Clip)mixer.getLine(info);
        audioinputstream = AudioSystem.getAudioInputStream(file);
        clip.open(audioinputstream);
        clip.loop(-1);
        /*
        FloatControl floatcontrol = (FloatControl)clip.getControl(javax.sound.sampled.FloatControl.Type.SAMPLE_RATE);
        for(int j = 0; j < 44000; j += 250)
        {
            Thread.sleep(10L);
            System.out.println(j);
            floatcontrol.setValue(j);
        }
        */
        FloatControl floatcontrol = (FloatControl)clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
        for(float j = -90; j < 0; j += 0.1f)
        {
            Thread.sleep(10L);
            System.out.println(j);
            floatcontrol.setValue(j);
        }
      }

    }

    static Random rnd = new Random();

}
