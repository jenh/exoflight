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
package com.fasterlight.exo.newgui.test;

import java.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import sdljava.SDLException;
import sdljava.joystick.SDLJoystick;

import com.fasterlight.exo.newgui.*;
import com.fasterlight.exo.newgui.particle.ParticleSystem;
import com.fasterlight.exo.orbit.Orientation;
import com.fasterlight.glout.*;
import com.fasterlight.vecmath.*;

public class GeekyGame
extends GLOAWTComponent
{
	static Random rnd = new Random();

	ModelRenderCache rendcache;
	TextureCache texcache;
	String TEX_URL_BASE = "file:./texs/";

	GL gl;
	GLU glu;
	StarRenderer srend;
	SDLJoystick stick;

	Thing ship;
	List rocks;

	Camera camera;

	ViewVolume viewvol = new ViewVolume();

	long lastupd = System.currentTimeMillis();

	GLOMeter meter;

	ParticleSystem ship_exhaust;

	GameCanvas gcanv;


	static float rndflt()
	{
		return rnd.nextFloat()-0.5f;
	}

	static Vector3f rndvec(float s)
	{
		Vector3f v = new Vector3f(rndflt(), rndflt(), rndflt());
		v.scale(s/v.length());
		return v;
	}


	public GeekyGame(int w, int h) throws SDLException
	{
		super(w,h);

		// start stick
		if (SDLJoystick.numJoysticks() > 0)
			stick = SDLJoystick.joystickOpen(0);
		srend = new StarRenderer();
		srend.loadStars();

		reset();
	}

	void reset()
	{
		ship = new Thing();
		ship.friction = 0.003f;
		ship.minfriction = 0.25f;
		rocks = new LinkedList();
		for (int i=0; i<200; i++)
		{
			Thing t = new Thing();
			t.model = "twistyrock";
			t.pos = rndvec(50);
			t.angvel = rndvec(0.1f);
			t.vel = rndvec(0.1f);
			t.maxLOD = 2;
			rocks.add(t);
		}
		camera = new Camera();
		camera.target = ship;

		ship_exhaust = new ParticleSystem(256);
	}

	//

	class Thing
	{
		Orientation ort = new Orientation();
		Vector3f pos = new Vector3f();
		Vector3f vel = new Vector3f();
		Vector3f angvel = new Vector3f();
		float throttle = 0;
		float friction = 0;
		float minfriction = 0;
		int maxLOD = 1;
		String model = "geekship";
		float radius = 1;

		void update(float dt)
		{
			pos.scaleAdd(dt, vel, pos);
			ort.mul(angvel, dt);
			if (throttle != 0)
			{
				Vector3f dvel = new Vector3f();
				dvel.scaleAdd(throttle*dt, new Vector3f(ort.getDirection()), dvel);
				vel.add(dvel);
				//System.out.println(ship.throttle + " " + dvel);
				//System.out.println(vel.length());
			}
			if (friction > 0)
			{
				vel.scale(1/(1+(minfriction+vel.lengthSquared()*friction)*dt));
			}
		}

		void render(GL gl)
		{
			if (!viewvol.intersectsSphere(pos, 1))
				return;

			int lod = 1;
			if (maxLOD > 1)
			{
				double cameradist = Math.abs(camera.pos.x-pos.x) + Math.abs(camera.pos.y-pos.y) + Math.abs(camera.pos.z-pos.z);
				double ang = getHeight()*radius/cameradist;
				if (ang < 1)
					lod = 0;
				else if (ang < 16)
					lod = 2;
			}

			gl.glPushMatrix();
			gl.glPushAttrib(GL.GL_ENABLE_BIT);
			gl.glTranslatef(pos.x,pos.y,pos.z);
			if (lod > 0)
			{
				gl.glMultMatrixf(GLOUtil.toArray(ort.getInvertedMatrix()), 0);
				try {
					String mname = (lod == 2) ? model+"-d2" : model;
					rendcache.getModelRenderer(mname).render();
				} catch (Exception e) {
					System.out.println(e);
				}
			} else {
				gl.glColor3f(1,1,1);
				gl.glBegin(GL.GL_POINTS);
				gl.glVertex3f(pos.x,pos.y,pos.z);
				gl.glEnd();
			}
			gl.glPopAttrib();
			gl.glPopMatrix();
		}
	}

	float MAX_ANGVEL = 0.5f;
	float DEADZONE = 32768/8;
	float dt = 0.1f;
	float dtchg = 0.1f;
	float THROT_SCALE = 50;

	long time;

	void updateNegroid()
	{
		long t = System.currentTimeMillis();
		long tl = t-lastupd;
		float dt1 = tl/1000f;
		dt += (dt1-dt)*dtchg;
		lastupd = t;
		time += (int)(dt*1000);

		if (stick != null)
		{
			SDLJoystick.joystickUpdate();
			Vector3f cmd = new Vector3f(
				-stick.joystickGetAxis(1), -stick.joystickGetAxis(0), stick.joystickGetAxis(3));
			if (Math.abs(cmd.x) < DEADZONE)
				cmd.x = 0;
			if (Math.abs(cmd.y) < DEADZONE)
				cmd.y = 0;
			if (Math.abs(cmd.z) < DEADZONE*2)
				cmd.z = 0;
			cmd.scale(MAX_ANGVEL/32768);
			//cmd.sub(ship.angvel);
			//cmd.scale(0.1f);
			ship.ort.transform(cmd);
			ship.angvel.set(cmd);

			float throt = Math.max(0, (32767-stick.joystickGetAxis(2))/68000f);
			ship.throttle = throt*THROT_SCALE;
		}
		ship.update(dt);
		Iterator it = rocks.iterator();
		while (it.hasNext())
		{
			Thing thing = (Thing)it.next();
			thing.update(dt);
		}
	}

	void addExhaustParticles()
	{
		ship_exhaust.beginStreaming();
		Vector3f pos = new Vector3f(ship.pos);
		Vector3f vel = new Vector3f(ship.ort.getDirection());
		vel.scale(-0.05f);
		pos.scaleAdd(20, vel, pos);
		float k = 0.002f;
		Vector3f dvel = new Vector3f(k,k,k);
		float sc = 0.5f;
		float dsc = 0.01f;
		long life = 500;
		ship_exhaust.updateStream(55-(ship.throttle/THROT_SCALE)*40, pos, vel, dvel, sc, dsc, life, 0);
		ship_exhaust.endStreaming();
	}


	//

	protected GLOContext makeContext()
	{
		return new GLOContext();
	}

	class GameCanvas
	extends GLODefault3DCanvas
	{

		GameCanvas()
		{
			super();
			setViewDistance(10f);
			neardist = 1f;
			fardist = 1e5f;
			setTargetFOV(75);
			setup();
		}

		void setup()
		{

		}

		protected void transformForView(GL gl)
		{
			gl.glLoadIdentity();
			Orientation o = camera.getOrt();
			Matrix3d mat = camera.ort.getMatrix();
			gl.glMultMatrixf(GLOUtil.toArray(mat), 0);
			gl.glTranslatef(-camera.pos.x, -camera.pos.y, -camera.pos.z);
		}

		public void renderObject(GLOContext ctx)
		{
			if (gl == null)
			{
				gl = ctx.getGL();
				glu = ctx.getGLU();
				srend.setup(gl);
				texcache = new TextureCache(TEX_URL_BASE, gl, glu, new java.awt.Frame());
				texcache.init();
				rendcache = new ModelRenderCache(gl, texcache);
			}

			viewvol.setup(gl);

			gl.glPushMatrix();
			gl.glScalef(fardist/2, fardist/2, fardist/2);
			gl.glDisable(GL.GL_TEXTURE_2D);
			gl.glDisable(GL.GL_BLEND);
			srend.render(gl);
			gl.glPopMatrix();

			updateNegroid();

			gl.glPushAttrib(GL.GL_ENABLE_BIT);
			gl.glEnable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_LIGHT0);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glEnable(GL.GL_DEPTH_TEST);

			ship.render(gl);
			Iterator it = rocks.iterator();
			while (it.hasNext())
			{
				Thing thing = (Thing)it.next();
				thing.render(gl);
			}

			// draw exhaust
			texcache.setTexture("cloud1.png");
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			gl.glDisable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glEnable(GL.GL_BLEND);
			gl.glDisable(GL.GL_CULL_FACE);
			gl.glDisable(GL.GL_DEPTH_TEST);
			Matrix3f mat = new Matrix3f(camera.ort.getMatrix());
			gl.glColor3f(0.3f, 0.5f, 1.0f);
			ship_exhaust.setTime(time);
			ship_exhaust.render(gl, mat);
			addExhaustParticles();

			gl.glPopAttrib();

			camera.update();

			if (meter != null)
			{
				meter.setValue(ship.vel.length());
			}
		}

		public boolean handleEvent(GLOEvent event)
		{
			if (event instanceof GLOMouseButtonEvent)
			{
			}
			else if (event instanceof GLOKeyEvent)
			{
				GLOKeyEvent gke = (GLOKeyEvent)event;
				if (gke.isPressed())
				{
					switch (gke.getKeyCode())
					{
						case GLOKeyEvent.VK_F1:
							reset();
							break;
						default:
					}
				}
			}
			return super.handleEvent(event);
		}
	}

	//

	class Camera
	{
		Vector3f pos = new Vector3f();
		Vector3f vel = new Vector3f();
		Orientation ort = new Orientation();
		Thing target;

		void update()
		{
			Vector3f dir = new Vector3f(target.ort.getDirection());
			Vector3f up = new Vector3f(target.ort.getUpVector());
			Vector3f targpos = new Vector3f(target.pos);
			targpos.scaleAdd(-20, dir, targpos);
			targpos.scaleAdd(8, up, targpos);

			targpos.sub(pos);
			targpos.scale(0.1f);
			pos.add(targpos);

			Orientation o2 = target.ort;
			targpos.set(target.pos);
			targpos.sub(pos);
			targpos.scale(-1);
			o2 = new Orientation(new Vector3d(targpos), new Vector3d(up));
			Vector3d axis = ort.getShortestSlerpAxis(o2);
			if (axis.lengthSquared() > 0)
				ort.mul(axis, 0.1f);
			//System.out.println(axis +" "+ o2);
			//ort.interpolate(o2, 0.2f);
			/*
			System.out.println("pos=" + pos + " target.pos=" + target.pos);
			System.out.println("ort.dir=" + ort.getDirection() + ", ort.up=" + ort.getUpVector());
			*/
		}

		Orientation getOrt()
		{
			return new Orientation(ort);
		}
	}

	//

	protected void makeComponents()
	{
		super.makeComponents();

		gcanv = new GameCanvas();
		gcanv.setSize( ctx.getWidth(), ctx.getHeight() );
		ctx.add(gcanv);

		meter = new GLOMeter();
		meter.setSize(32,128);
		meter.setDisplayRange(0, 40);
		meter.setTickScale(5);
		ctx.add(meter);
	}

	public static void main(String[] args) throws SDLException
	{
		GeekyGame test = new GeekyGame(1024,768);
		JFrame f = new JFrame();
		GLCanvas canvas = test.createGLCanvas();

		f.getContentPane().setLayout(new java.awt.BorderLayout());
		f.getContentPane().add(canvas, java.awt.BorderLayout.CENTER);
		f.pack();
		f.show();

		test.start(canvas, canvas);
	}

}
