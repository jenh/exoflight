/*
 * Created on Apr 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.fasterlight.exo.test;

import java.awt.*;
import java.awt.event.*;

public class FullScreenTest
{

	public static void main(String[] args)
	{
		Frame frame = new Frame();
		Window w = new Window(frame)
		{
			public void paint(Graphics g)
			{
				g.setColor(Color.red);
				g.drawOval(0, 0, 1024, 768);
			}
		};
		w.addMouseListener(new MouseListener()
		{
			public void mouseClicked(MouseEvent e)
			{
				e.getComponent().repaint();
			}
			public void mousePressed(MouseEvent e)
			{
			}
			public void mouseReleased(MouseEvent e)
			{
			}
			public void mouseEntered(MouseEvent e)
			{
			}
			public void mouseExited(MouseEvent e)
			{
			}
		});
		GraphicsEnvironment env =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice dev = env.getDefaultScreenDevice();
		dev.setFullScreenWindow(w);
		//		w.setVisible(true);
		//		Graphics g = w.getGraphics();
		w.validate();
	}
}
