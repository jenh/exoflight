**velocity** - the rate of change of position, or "speed" of an object.  see standard velocity.


**standard velocity** - the velocity measured relative to the center of the planet. see true velocity, vertical velocity, and tangent velocity for other ways of measuring velocity.


**airspeed** - see true velocity.


**true velocity** - sometimes called "airspeed".  the velocity measured with respect to the planet surface or the atmosphere of the planet.


**tangent velocity (also vertical velocity)** - the velocity in the direction of the planet center.  negative values mean toward the planet (downward) and positive values mean away from the planet (upward).


**tangent velocity (also horizontal velocity)** - the velocity in the direction parallel to the planet surface -- or, if you will, the horizontal direction.  this value is always positive or zero.


**altitude** - the distance above or below the planet surface. since planets are not perfectly smooth, in spacesim this usually refers to the distance from the average radius of the planet, not the height above the actual terrain.  so the actual equation used is (distance from center of planetaverage radius of planet).  see altitude agl.


**altitude agl** - or altitude "above ground level".  this is the height of an object as measured with respect to the terrain.  if you took a laser mounted at the center of the object, and pointed it directly toward the center of the planet, altitude agl would be the distance the laser travelled before it hit the ground.


**mach** - the ratio of true velocity (airspeed) to the speed of sound at the current altitude.  only valid for atmospheric flight.  the definition is mach = (true velocity / speed of sound).


**periapsis** - the lowest point of an orbit, when it is closest to the planet center and travelling the fastest.


**apoapsis** - the highest point of an orbit, when it is furtherest from the planet center and travlling the slowest.


**period** - the time in which an object takes to complete one circuit of an orbit.  this is not defined for hyperbolic orbits, since they never loop back upon themselves.


**dynamic pressure (Q)** - the extra pressure built up around a vehicle as it moves through the air.  this value is related to the rate of heating and drag on the vehicle.


**max-Q** - the time during a launch or reentry when dynamic pressure is the highest.  for a launch, this usually occurs shortly after the vehicle goes supersonic.


--


**attitude** - the orientation of the spacecraft, or the direction in which it is pointing.


**angular velocity** - ?


--


**module** - in spacesim, a component of a spaceship that may separate and function independently from the rest of the ship.


**capability** - in spacesim, refers to a "component" of a spaceship that performs a given function.  a module includes a certain set of capabilities.


**rcs** - reaction control system.  this is a capability that allows a spacecraft to control its attitude using a set of small thrusters.  the thrusters are arranged along the outside of the ship so that two or more thrusters may be fired simultaneously to impart a rotation to the ship.


**gimbal system** - a cpability that allows a spacecraft to change its attitude using the thrust of other engine(s).  the engines are mounted on gimbals which are mechanisms for slightly altering the angle of the engine thrust during a burn.  the slight changes in angle control the rotation of the spacecrarft.


**deadband** - a parameter of the attitude control system that controls when thrusters are fired.  for example, a 1 degree deadband will fire the thrusters when the difference between the current and desired attitude is larger than 1 degree.  larger deadbands will conserve fuel, but smaller deaadbands
will give better accuracy.


**deviation angle** - in the attitude control system, the angle between the current and desired attitude.


**fuel cells** - devices that generate electricty by combining hydrogen and oxygen in a catalyzed reaction.  as a side product, they also produce pure water, which can be used for drinking, cleaning, cooling, or other purposes onboard a spacecraft.


**guidance system** - controls the attitude of the ship, and executes guidance programs that guide the attitude and throttle of the ship during maneuvers.  it is linked to one or more rcs or gimbal capabilities to actually perform the attitude corrections.


**sps** - service propulsion system.  the term applied to the main engine on the service module of the apollo spacecraft.


**primary rcs** - the term for the larger, more powerful rcs thrusters on the shuttle orbiter.


**vernier rcs** - the term for the smaller, less powerful rcs thrusters on the shuttle orbiter.