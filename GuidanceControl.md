Every SpaceShip has a "primary guidance" object -- which is a component of
class GuidanceCapability. Whenever a GuidanceCapability is activated, it
calls setPrimaryGuidance(), passing itself.  When a primary guidance is
changed, the old one is shut off.

This is done so that we can control the primary guidance from the main
cockpit console.  There will be a switch that lets you turn the guidance
component on/off.

A SpaceShip also has an AttitudeController.  This thing is the brains of
the system, containing parameters for dead zone, speed, etc.