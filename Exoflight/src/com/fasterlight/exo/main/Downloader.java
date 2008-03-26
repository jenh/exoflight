package com.fasterlight.exo.main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class Downloader {

	static boolean TEST = false;
	
	static String baseurl = "http://exoflight.googlecode.com/files/";
	
	class Fileset
	{
		public Fileset(String string, String string2) {
			this.url = string;
			this.localfile = string2;
		}
		String url;
		String localfile;
		
		public boolean exists()
		{
			if (TEST)
				return false;
			return new File(localfile).exists();
		}
		
		public void rollback()
		{
			if (TEST)
				return;
			new File(localfile).delete();
		}
		
		public void download() throws MalformedURLException, IOException
		{
			System.out.println("Downloading " + url);
			InputStream in = new URL(url).openStream();
			ZipInputStream zin = new ZipInputStream(in);
			do {
				ZipEntry ze = zin.getNextEntry();
				if (ze == null)
					break;
				System.out.println("Unpacking " + ze);
				int len = (int)ze.getSize();
				byte[] data = new byte[len];
				int ofs = 0;
				while (ofs < len)
				{
					int nread = zin.read(data, ofs, len-ofs);
					if (nread < 0)
						break;
					ofs += nread;
				}
				File f = new File(ze.getName());
				if (f.isAbsolute() || ze.getName().startsWith(".."))
					throw new IllegalArgumentException(ze.getName());
				if (!TEST)
				{
					FileOutputStream out = new FileOutputStream(f);
					out.write(data);
					out.close();
				}
			} while (true);
		}
	}
	
	ArrayList filesets = new ArrayList();
	
	Downloader()
	{
		filesets.add(new Fileset("", "lib/Exoflight.jar"));
		filesets.add(new Fileset(baseurl + "Exoflight-0.1-media.zip", "texs/grid.png"));
		filesets.add(new Fileset(baseurl + "Exoflight-0.1-ephemeris.zip", "eph/de405-2000.ser"));
		filesets.add(new Fileset(baseurl + "basemaps-low.zip", "texs/Earth/options.txt"));
	}
	
	void checkAll()
	{
		Iterator it = filesets.iterator();
		while (it.hasNext())
		{
			Fileset fs = (Fileset)it.next();
			if (!fs.exists())
			{
				try {
					fs.download();
				} catch (Exception e) {
					e.printStackTrace();
					fs.rollback();
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		Downloader.TEST = true;
		Downloader.baseurl = "file:///s:/";
		Downloader dl = new Downloader();
		dl.checkAll();
	}

}
