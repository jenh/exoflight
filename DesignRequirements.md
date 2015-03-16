GAMEPLAY

There are several modes of game play:

TEST FLIGHT - The player chooses a spacecraft and is free to fly around the
solar system as they choose.  They may launch from the surface of Earth (or
another planet), start in orbit, or docked at a space station.  The player
may control multiple spacecraft, which allows for docking scenarios and
other missions.

MISSIONS - the player selects from a predefined mission and must complete it
within certain constraints.  If successful, the mission is scored based on
certain criteria, like time to complete, accuracy, etc.  Missions include
lunar landings, space rescues, and comet interceptions, as well as
historical scenarios like the Apollo and Gemini missions.  The mission
may include scripted events, like malfunctions, or voice comms.

--

USER INTERFACE

The user will have several screens available to monitor and control the
spacecraft.

VEHICLE PREP - Choose a launch vehicle and payload, and the base where
it will be stationed.

GROUND TRACK - Displays a global map of a planet and the satellites around
it, including the ground track of a selected body.  Can zoom in & out, pan.

TACTICAL - Like a 3-D ground track -- shows a globe of the current planet,
with satellites around it in 3-D.  The user can rotate the view with the
mouse, as well as zoom in and out.  Orbits for selected bodies are shown.

VISUAL - Shows a 3-D view from the spacecraft, or an external view of a
spacecraft or base.  The view can also be rotated with the mouse.

STRUCTURAL - Shows a schematic of the current spacecraft.  Allows the user
to select modules, and shows the consumables remaining for each module.
Allows the user to inspect and control the systems of each module.
Also allows for separation of modules.

RCS - A control that shows the current attitude of the spacecraft, and
allows dial-in of a particular attitude.  Allows for manual rotational and
translational movement.

SPS - Controls engine thrust.  Turn on/off engine(s), select thrust
levels (if supported).  Monitor thrust levels & status.

SEQUENCER - Allows monitoring of the mission events controller (sequencer),
pausing/resuming of the sequence, and changing of sequence (to abort, for
instance)

INFO - Text display giving orbital information, position, velocity,
and other misc. values.

GUIDANCE - Allows for launch, intercepts, orbital maneuvers, and other
automatically-sequenced maneuvers.

TELEMETRY GRAPH - Used to graph arbitrary values from the spacecraft.

CROSSPOINTERS - A special display to help line up for docking and landing.

STATUS - mission timer, simulation rate, master caution light, abort button,
current spacecraft name, display select buttons.

These displays will be spread out among several screens, one for each
mission mode -- for instance, there will be a launch screen, rendezvous
screen, orbit tracking screen, etc.  The user will also be able to
customize his displays.

Each module also has its own display that shows consumables and
component status, and has controls for arming engines, etc.

--

MODES

---


SELECT MISSION

Select from list of missions and description of each.
Game loads mission, sets game time and world contents.

FREE FLIGHT

Starts by selecting vehicle & start location (base, lat/long
of planet, or orbit).  Shows picture, description of vehicle,
and stats (mass, delta-v, etc)

TUTORIAL

?

LAUNCH PHASE

Select vehicle (on ground).
Choose launch program -- every vehicle has default program,
> maybe multiple programs.
Manual ascent:
- choose azimuth or inclination
- optional: choose longascnode
- set launch time.
Intercept:
- choose destination body, will automatically set incl &
> longascnode to intercept, as long as body orbits same planet as
> launch vehicle.  This also sets the countdown time.
Set program-specific parameters.
After done, press "arm" button.  This enables the countdown.

DURING LAUNCH

Monitor graph of altitude, deflection angle, other params.
Monitor numerical values.
Monitor sequencer.
Monitor engine thrust levels.
If problems occur:
- "Abort" button jumps sequencer to an abort sequence, depending
> on the phase of the launch.
- "Detonate" explodes the vehicle.
- Can override sequencer and control spacecraft manually.

MANUAL SPACECRAFT CONTROL

Manual attitude control in 6 rot. axes and 6 trans. directions.
(Mouse, keyboard or joystick)
Or, activate guidance system, and dial in yaw, pitch, roll.
Both RCS engine and guidance system can be selected; default
is the one that makes "sense".  Sometimes RCS is fixed for a
given guidance system.
Monitor 8-ball, pitch, yaw, and roll rate, and error values
(desired vs. actual).
Can change reference among fixed, planet-centric, or object-centric
(for docking, landing)

ORBITAL MANEUVERS

Select SPS engine & guidance system (and maybe RCS engine, if allowed).
Dial in maneuver parameters.  Press "Load" button to load into guidance
system.  Sequencer shows steps to complete maneuver.

Circularize: Select altitude. System will apply burn at perigee until
desired apogee reached, then burn at apogee until orbit is circularized.

Plane change: Change incl or longascnode. System applies burn at
node crossing until orbit changed.

Lambert maneuver: Select body to intercept, departure time range,
and mission duration range. System computes graph of intercept times
vs. time-of-flight vs. delta-V, and chooses cheapest option.  User
can choose another option if desired.  Intercepting extraplanetary
bodies will have to be TBD.

Various parameters for these maneuvers can also be set, TBD.
Monitor attitude, and desired vs. actual orbit delta during the
burn.
Also monitor engines, and other things as during launch phase.

DOCKING

Select parent vehicle, and module to dock to (also direction).
Enable docking beacon on parent.

Take child vehicle to vicinity of parent.  Select SIGHT mode.
Maneuver vehicle to predetermined distance from dock target
(1 km or so), monitor range to make sure collision does not occur.

When position reached, change docking mode to DOCK, and approach
slowly.  Monitor range, velocity, attitude, visual.

Automatic docking will also follow these procedures.

POWERED LANDING

On the moon, or whereever...

Sort of like docking except you have no dock beacon.
Lower apogee to 5 km or so, then (at "low gate") begin braking.
Monitor range to target, velocity, attitude, visual.

GLIDER LANDING

What is this, a flight sim?!?! :)

Provide minimal support for lift and other wanky aerodynamics
so that kneebiters can land the stupid shuttle.  Need control
surface components, landing gear, ILS.

--

VEHICLE DESIGN

Vehicles are made of modules.  A module can be a rocket stage, or a
spacecraft like the CM, SM, or LM.  Modules contain resources (like O2,
fuel, elec) that are drained during the mission.

Modules can be connected in 7 ways: n, s, e, w, up, down, and "inside" for
inside a payload bay or nose cone.

For instance, a Saturn-V would consist minimally of 3 stages + nose fairing
connected at u/d.  The CSM/LM would be connected together at the u/d and
connected to the nose fairing module via the 'in' direction.  The CM, SM,
LAM and LDM will be connected together via u/d.

The drag of the total spacecraft will be automatically guesstimated.  We'll
take the topmost modules opposite propulsion and compute the surface area of
those.  So we need the area of each module pointing "into the wind", and
whether or not it's "pointy".

--

TIME

There will be three main timers kept: universal time (UT), mission
time, and "event time".  Event time is measured when a specific
maneuver is executed, such as a launch or orbit change.

The timescale can be user-adjusted from 1 s = 1 s to 1 s = 30 days.
There will be an option to slow down to less, or pause time.
When the computation for a given frame exceeds a given value (default
now is 250 ms) then no more processing will be done for that frame,
and the timescale will be backed off a notch.

When significant events occur -- component failure, stage separation, sphere
of influence enter/exit -- time will be paused until the player resets the
alarm, and then reset to 1 s = 1 s.

--

TRAJECTORIES

The trajectory of spacecraft will not be simulated exactly as it would in
the real world.  Each spacecraft will have one body as its primary
influence, with no perturbations from other bodies.  The only other force
modeled besides gravity will be drag.  When a spacecraft exits the body's
sphere of influence, it changes its primary body.  Likewise when it enters
another body's sphere of influence.

There will be multiple types of trajectories for spacecraft depending on
the environment:

2-Body: Standard 2-body Keplerian orbit.  Used when out of atmosphere and
> not thrusting.

Cowell: All forces on spacecraft are integrated with RK4 using variable
> time step.  Used when thrusting, or when in atmosphere.  Typical forces
> are thrust, lift, drag, and gravity.

Landed: Ship is attached to surface of planet.

--

HEATING

Spacecraft will be heated by the sun, the atmosphere (through conduction
or friction of reentry), and by internal systems (engines, electricity,
other systems).  Most spacecraft will have a thermal management system
to maintain the desired temperature.

Heat of aero drag must be computed -- during each Cowell integration step,
when drag is computed, heat will be added.  If spacecraft has an ablative
heat shield and is oriented properly, mass is taken from this thing and heat
is dissipated appropriately.  Shuttle has thermal tiles which are
non-ablative.

--

CREW

For this release, the crew are just bags of salty water and minerals that
must be maintained at specific temperatures, pressures, and G-levels, fed
and watered periodically, and given O2 to breathe.

--

INSTRUMENTS

User interface will be described by a series of text files where each
component is laid out in x/y coordinates.  Colors, blend modes, and
textures are all customizable.  Gauges can map to "telemetry" items
of the spacecraft.

Values of instruments will be read/written using a hierarchical
properties scheme. Examples:
```
game.tick - returns the current game time tick
ship.telemetry.altitude - returns the altitude of the current ship
ship.name - returns the name of current ship
ship.telemetry.fixedort.roll - returns the current roll, w.r.s. to fixed coord. system
ship.structure.modulecount - returns # of modules in spaceship
ship.structure.supply.O2 - returns amount of oxygen in ship
universe.Earth - returns planet Earth
universe.Luna.parent.parent - returns Sun
```
--

VEHICLES FOR VERSION 1.0

Space Shuttle
Saturn V
Apollo Spacecraft
Mir
ISS
(SSTO vehicle)
(Fusion ship)


PLANETS

Earth, Moon, Mars - Terrain down to 5' resolution
all other major planets, major satellites


MAIN MENU
```
Game
	New Mission...
	Save State...
	Load State...
	---
	Pause
	---
	Reset
	--
	Exit
Screen
	Mission Control
	3D Map
	Cockpit
	Full Visual
	---
	Launch...
	Intercept...
	Maneuver...
Help
	Glossary...
	---
	About...
```

KEYS
```
F1 - groundtrack/cockpit view
F2 - visual/cockpit view
F3 - 3D map view
F4 - full visual view
F5 - structure view

F - faster
S - slower
P - pause
```