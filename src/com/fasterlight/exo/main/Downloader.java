package com.fasterlight.exo.main;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

public class Downloader implements Runnable
{
	static final String SENTINEL_PATH = "./init/SolarSystem.seq";

	static boolean TEST = false;
	
	static String ROOTDIR = ".";
	
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
		private String baseurl;
		private String filename;
		private String tagPath;

		public Fileset(String baseurl, String filename) 
		{
			this.baseurl = baseurl;
			this.filename = filename;
			this.tagPath = ROOTDIR + File.separatorChar + '.' + filename + ".tag";
		}

		public boolean exists()
		{
			if (TEST)
				return false;
			return new File(tagPath).exists();
		}
		
		public void commit() throws IOException 
		{
			if (TEST)
				return;
			new File(tagPath).createNewFile();
		}

		public void rollback()
		{
			if (TEST)
				return;
			new File(tagPath).delete();
		}
		
		public boolean download() throws MalformedURLException, IOException
		{
			ProgressDialog dlg = new ProgressDialog(this);
			try {
				System.out.println("Downloading " + filename);
				URL url2 = new URL(new URL(baseurl), filename);
				dlg.zipfile.setText(url2.toString());
				URLConnection conn = url2.openConnection();
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
						if (ze.isDirectory())
						{
							f.mkdirs();
						} else {
							f.getParentFile().mkdirs();
							FileOutputStream out = new FileOutputStream(f);
							try {
								out.write(data);
							} finally {
								out.close();
							}
						}
					}
				} while (true);
			} finally {
				dlg.dispose();
			}
			return true;
		}
	}
	
	ArrayList filesets = new ArrayList();
	
	Downloader()
	{
		filesets.add(new Fileset(BASEURL, "Exoflight-0.1-media.zip"));
		filesets.add(new Fileset(BASEURL, "Exoflight-0.1-ephemeris.zip"));
		filesets.add(new Fileset(BASEURL, "basemaps-low.zip"));
		filesets.add(new Fileset(BASEURL, "Exoflight-0.1.3-patch.zip"));
	}
	
	void checkAll()
	{
		Iterator it = filesets.iterator();
		while (it.hasNext())
		{
			Fileset fs = (Fileset)it.next();
			if (!fs.exists())
			{
				if (!TEST && !new File(SENTINEL_PATH).exists())
				{
					error("Can't download, not sure current directory contains main program");
					return;
				}
				try {
					if (!fs.download())
						return; // we cancelled
					fs.commit();
				} catch (Exception e) {
					error("Couldn't download \"" + fs.filename + "\"\n" + e);
					e.printStackTrace();
					fs.rollback();
					return;
				}
			}
		}
	}
	
	private void error(String string) 
	{
		JOptionPane.showMessageDialog(null, string, null, JOptionPane.ERROR_MESSAGE);
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
