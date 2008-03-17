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

import com.fasterlight.exo.orbit.*;
import com.fasterlight.exo.ship.*;
import com.fasterlight.game.Game;
import com.fasterlight.glout.*;
import com.fasterlight.vecmath.Vector3d;

// todo: use reference object for orientation
// todo: add position display
// todo: lights for RCS activity
// todo: auto-mode

/**
  * Allows player to control spaceship manually --
  * RCS rotation & translation.
  */
public class RCSControlWindow
extends SpaceShipWindow
{
	GLOTableContainer toptab, controltab, transpanel, rotpanel, infopanel;
//	GLOComboBox rcscapCombo;
	GLOSelectionBox rcscapSelect;
	FilteringCapabilityListModel rcscapModel;
	GLOLabel pitchLabel, pitchRateLabel, yawLabel, yawRateLabel, rollLabel, rollRateLabel;
	GLOLabel trackDevLabel;

	// used for auto-center
	AttitudeController oldattctrl, attctrl;
	boolean centering;

	public RCSControlWindow()
	{
		toptab = new GLOTableContainer(1,3);
		toptab.setPadding(0,8);

		infopanel = new GLOTableContainer(5,4);
		infopanel.setPadding(4,4);
		infopanel.setColumnFlags(0, GLOTableContainer.HALIGN_RIGHT);
		infopanel.setColumnFlags(1, GLOTableContainer.HALIGN_RIGHT);
		infopanel.setColumnFlags(2, GLOTableContainer.HALIGN_RIGHT);
		infopanel.setColumnFlags(3, GLOTableContainer.HALIGN_RIGHT);

		int mc = 8;
		infopanel.add(new GLOLabel("Pitch:"));
		infopanel.add(pitchLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel(" Pitch rate:"));
		infopanel.add(pitchRateLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel("rpm"));

		infopanel.add(new GLOLabel("Yaw:"));
		infopanel.add(yawLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel(" Yaw rate:"));
		infopanel.add(yawRateLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel("rpm"));

		infopanel.add(new GLOLabel("Roll:"));
		infopanel.add(rollLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel(" Roll rate:"));
		infopanel.add(rollRateLabel = new GLOLabel(mc));
		infopanel.add(new GLOLabel("rpm"));

		infopanel.add(new GLOLabel("Deviation:"));
		infopanel.add(trackDevLabel = new GLOLabel(mc));
		infopanel.add(new GLOEmpty());
		infopanel.add(new GLOEmpty());
		infopanel.add(new GLOEmpty());

		toptab.add(infopanel);

		controltab = new GLOTableContainer(2,2);
		controltab.setPadding(16,16);

		controltab.add(new GLOLabel("Translate"));
		controltab.add(new GLOLabel("Rotate"));

		transpanel = new GLOTableContainer(2, 1);
		transpanel.setPadding(16, 0);
		addZControl(transpanel, false);
		addXYControl(transpanel, false);
		controltab.add(transpanel);

		rotpanel = new GLOTableContainer(2, 1);
		rotpanel.setPadding(16, 0);
		addXYControl(rotpanel, true);
		addZControl(rotpanel, true);
		controltab.add(rotpanel);

		toptab.add(controltab);

		GLOTableContainer rcstab = new GLOTableContainer(2,1);
		rcstab.add(new GLOLabel("Using:"));

		rcscapModel = new FilteringCapabilityListModel(this, RCSCapability.class);
		/*
		rcscapCombo = new GLOComboBox(30);
		rcscapCombo.setModel(rcscapModel);
		rcstab.add(rcscapCombo);
		*/
		rcscapSelect = new GLOSelectionBox(30);
		rcscapSelect.setModel(rcscapModel);
		rcstab.add(rcscapSelect);

		toptab.add(rcstab);

		this.setContent(toptab);
	}

	public void render(GLOContext ctx)
	{
		SpaceShip ship = getSpaceShip();
		if (ship != null)
		{
			Game game = ship.getUniverse().getGame();
			Orientation ort = ship.getOrientation(game.time());
			// transform orientation by planet's
			/*
			Vector3d pos = ship.getTrajectory().getPos(game.time());
			pos.scale(-1);
			ort.concat(new Orientation(pos));
			*/
			Vector3d pyr = ort.getEulerPYR();
			pitchLabel.setText(AstroUtil.toDegrees(pyr.x));
			yawLabel.setText(AstroUtil.toDegrees(pyr.y));
			rollLabel.setText(AstroUtil.toDegrees(pyr.z));

			Vector3d angvel = ship.getTrajectory().getAngularVelocity();
			double k = 60;
			pitchRateLabel.setText(AstroUtil.format(angvel.x*k));
			yawRateLabel.setText(AstroUtil.format(angvel.y*k));
			rollRateLabel.setText(AstroUtil.format(angvel.z*k));

			AttitudeController attctrl = ship.getAttitudeController();
			if (attctrl != null)
			{
				trackDevLabel.setText(AstroUtil.toDegrees(attctrl.getDeviationAngle()));
			} else
				trackDevLabel.setText("---");
		}
		super.render(ctx);
	}

	void addZControl(GLOTableContainer panel, boolean rotate)
	{
		GLOTableContainer ctrl = new GLOTableContainer(1,3);
		ctrl.add(new GLOButton("+Z", new RCSAction(4, rotate), true ));
		ctrl.add(new GLOContainer());
		ctrl.add(new GLOButton("-Z", new RCSAction(5, rotate), true ));
		panel.add(ctrl);
	}

	void addXYControl(GLOTableContainer panel, boolean rotate)
	{
		GLOTableContainer ctrl = new GLOTableContainer(3,3);
		ctrl.add(new GLOContainer());
		ctrl.add(new GLOButton("+Y", new RCSAction(2, rotate), true ));
		ctrl.add(new GLOContainer());
		ctrl.add(new GLOButton("-X", new RCSAction(1, rotate), true ));
		ctrl.add(new GLOButton("0", new RCSAction(6, rotate), true ));
		ctrl.add(new GLOButton("+X", new RCSAction(0, rotate), true ));
		ctrl.add(new GLOContainer());
		ctrl.add(new GLOButton("-Y", new RCSAction(3, rotate), true ));
		ctrl.add(new GLOContainer());
		panel.add(ctrl);
	}

	public RCSCapability getRCSCapability()
	{
		Object o = rcscapModel.getSelectedItem();
		if (!(o instanceof RCSCapability))
			return null;
		else
			return (RCSCapability)o;
	}

	public float getRCSStrength()
	{
		return 1.0f;
	}

	class RCSAction
	{
		int flags;
		RCSAction(int dir, boolean rot)
		{
			// todo: translate?
			this.flags = (1<<dir) << (rot?0:8);
		}
	}

	public boolean handleEvent(GLOEvent event)
	{
		if (event instanceof GLOActionEvent)
		{
			Object act = ((GLOActionEvent)event).getAction();
			SpaceShip ship = getSpaceShip();
			Game game = ship.getUniverse().getGame();
			RCSCapability rcscap = getRCSCapability();

			if (act instanceof RCSAction)
			{
				RCSAction rcsact = (RCSAction)act;
				if (rcscap != null)
				{
					switch (rcsact.flags)
					{
						case (1<<6):
							if (ship != null && game != null) {
								attctrl.setTargetOrientation( ship.getOrientation(game.time()) );
								centering = true;
								return true;
							}
						default:
							rcscap.setRCSFlags(rcsact.flags);
							return true;
					}
				}
			}
			else if (act == null)
			{
				if (rcscap != null)
				{
					if (centering)
					{
						centering = false;
						return true;
					} else {
						rcscap.setRCSFlags(0);
						return true;
					}
				}
			}
		}

		return super.handleEvent(event);
	}

}
