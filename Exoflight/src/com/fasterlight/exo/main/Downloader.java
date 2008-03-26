package com.fasterlight.exo.main;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class Downloader implements Runnable
{
	static boolean TEST = false;
	
	static String BASEURL = "http://exoflight.googlecode.com/files/";
	
	class ErrorDialog extends Frame
	{
		public ErrorDialog(String msg, String btn)
		{
			super("Error");
			add(new Label(msg, Label.CENTER), BorderLayout.CENTER);
			add(new Button(btn), BorderLayout.SOUTH);
			setSize(800,100);
		    Toolkit tk = Toolkit.getDefaultToolkit();
		    Dimension screenSize = tk.getScreenSize();
		    int screenHeight = screenSize.height;
		    int screenWidth = screenSize.width;
		    setLocation(screenWidth/2 - getWidth()/2, screenHeight/2 - getHeight()/2);
		    setVisible(true);
		}
	}

	class ProgressDialog extends Frame
	{
		Label title = new Label("EXOFLIGHT is now downloading extra data files from:", Label.CENTER);
		Label zipfile = new Label("", Label.CENTER);
		Label filename = new Label("", Label.CENTER);
		//Button cancel = new Button("Cancel");
		
		public ProgressDialog(Fileset fs)
		{
			super("Downloading...");
			setCursor(Cursor.WAIT_CURSOR);
			add(title, BorderLayout.NORTH);
			add(zipfile, BorderLayout.CENTER);
			add(filename, BorderLayout.SOUTH);
			//add(cancel, BorderLayout.SOUTH);
			setSize(600,100);
		    Toolkit tk = Toolkit.getDefaultToolkit();
		    Dimension screenSize = tk.getScreenSize();
		    int screenHeight = screenSize.height;
		    int screenWidth = screenSize.width;
		    setLocation(screenWidth/2 - getWidth()/2, screenHeight/2 - getHeight()/2);
		    setVisible(true);
		}
	}
	
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
			ProgressDialog dlg = new ProgressDialog(this);
			try {
				System.out.println("Downloading " + url);
				dlg.zipfile.setText(url);
				URL url2 = new URL(url);
				URLConnection conn = url2.openConnection();
				//int ziplen = conn.getContentLength();
				//int zipofs = 0;
				InputStream in = conn.getInputStream();
				ZipInputStream zin = new ZipInputStream(in);
				do {
					ZipEntry ze = zin.getNextEntry();
					if (ze == null)
						break;
					System.out.println("Unpacking " + ze);
					dlg.filename.setText(ze.getName());
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
						try {
							out.write(data);
						} finally {
							out.close();
						}
					}
				} while (true);
			} finally {
				dlg.setVisible(false);
			}
		}
	}
	
	ArrayList filesets = new ArrayList();
	
	Downloader()
	{
		filesets.add(new Fileset("", "lib/Exoflight.jar"));
		filesets.add(new Fileset(BASEURL + "Exoflight-0.1-media.zip", "texs/grid.png"));
		filesets.add(new Fileset(BASEURL + "Exoflight-0.1-ephemeris.zip", "eph/de405-2000.ser"));
		filesets.add(new Fileset(BASEURL + "basemaps-low.zip", "texs/Earth/options.txt"));
	}
	
	void checkAll()
	{
		Iterator it = filesets.iterator();
		while (it.hasNext())
		{
			Fileset fs = (Fileset)it.next();
			if (!fs.exists())
			{
				if (fs.url.isEmpty())
				{
					new ErrorDialog(
							"Something's wrong -- I don't think we started with the right current directory, "
									+ "since I can't find the main program.",
							"Close");
					return;
				}
				try {
					fs.download();
				} catch (Exception e) {
					e.printStackTrace();
					fs.rollback();
				}
			}
		}
	}
	
	public void run() 
	{
		checkAll();
	}

	public static void main(String[] args)
	{
		Downloader.TEST = true;
		Downloader.BASEURL = "file:///s:/";
		Downloader dl = new Downloader();
		dl.checkAll();
	}

}
