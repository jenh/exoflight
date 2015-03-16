# A Background and Architectural Discussion of EXOFLIGHT #

by Steven Hugg

Copyright © 2001-2008 Faster Light LLC

## 1 Introduction ##

The purpose of this document is to give a high-level overview of the goals of EXOFLIGHT, and the architecture decisions made thus far to support those goals.

### 1.1 Design Goals ###

The short description of EXOFLIGHT as specified in the original design document is:

A historically-based and future-looking spaceflight simulator. The player can fly several space vehicles and rockets, travel to the bodies of the solar system, and complete a   variety of missions.

The intended audience was intended to be space enthusiasts of all ages and skill levels. However, the complex nature of spaceflight and desire for accurate astrodynamics simulation made it apparent that the sim was mainly for more "hardcore" simmers. Casual users may still enjoy the eye-candy provided by EXOFLIGHT's graphics engine.

### 1.2 Features ###

From the original design document:

  * Fly several spacecraft from history and the future, including the Apollo spacecraft, Saturn V, and space shuttle.

  * Breathtaking 3D views of the solar system. Planets and moons are accurately modeled, including terrain and atmosphere.

  * Realistic orbital mechanics -- to complete missions successfully, you must plan your trajectory carefully, as well as watch your fuel.

  * Design your own rockets, space vehicles, and space stations by connecting different modules.

  * Complete challenging missions like moon landings, space rescues, and comet interceptions. Save the day by recovering from a spacecraft malfunction.

  * Open architecture -- add new spacecraft, modules, instrument panels, planets, and more from fellow users. Import TLE data from NASA and play with the satellites in orbit!

  * Large many-paged manual to explain how the heck to play this silly thing.

### 1.3 Major Challenges ###

#### 1.3.1 Pioneering Genre ####

The field of space simulation is still in its infancy. To date, there have been only a handful of space simulators aimed at the consumer, none of which has gained mass market appeal. None yet have successfully simulated a complete historical mission in detail, with the exception of [A-OK! The Wings of Mercury(TM)](http://www.aokwom.com/), which simulates a Mercury mission in great detail. Since there is little prior art to build upon, EXOFLIGHT must introduce many pioneering elements if it is to be successful.

#### 1.3.2 Accuracy of Simulation ####

Spaceflight is, by nature, a very precise occupation. A difference in velocity of a few meters/sec can propagate to errors of millions of miles later in the vehicle's flight path. Therefore, it is necessary to have precise models for trajectory propagation, gravity models, and propulsion systems to accurately simulate a mission. The needs are different than that of a traditional flight simulator, where the importance is placed more on "feel" and less on precise vehicle performance.

#### 1.3.3 Usability/Fun ####

Users have different perceptions of what makes simulation "fun." The difficulty with space simulation is that astrodynamics is a very complicated subject, and requires mathematical precision. How do we make a usable simulation without compromising realism?

#### 1.3.4 Rendering Planetary Bodies ####

The problem of rendering an entire planet in 3D is a tough one. Conventional flight simulators take many shortcuts in rendering the world, like assuming that the surface is flat, clipping scenery at an arbitrary distance, and simplifying lighting conditions. EXOFLIGHT must render planetary bodies at distances from meters above the surface to millions of miles away, support irregular bodies like asteroids, have believable lighting conditions and atmospheric conditions, show eclipses, and display ring systems. A challenging task!

The main goal in the visual display of EXOFLIGHT is "seamless from ground to orbit." This means that the user should not perceive a distinct level-of-detail (LOD) transition at any point in egress/ingress to a planet's surface. Many simulators have a couple of LOD levels, and depending on distance from the ground, a given scenery set is loaded. This is not acceptable in EXOFLIGHT, as this will ruin the sense of realism.

### 1.4 Language/Technology Choices ###

#### 1.4.1 Java ####

The Java language and Sun HotSpot VM were chosen for EXOFLIGHT. This is a dangerous choice, since no major commercial games have been implemented in Java, which has a reputation for being slow, buggy, and unsuitable for game programming. These are half-truths. Here are some refutations of common Java "myths":

  * **Java is slow.** New VMs like HotSpot execute programs at around 80% C++ speeds on average.

  * **Java is buggy.** Modern VMs are extremely stable, and the language semantics prevent many common coding errors.

  * **Java is not suited for games.** True, Java is a bit slower and is not as memory-efficient as other languages. Still, the performance is sufficient for a space-simulation application such as EXOFLIGHT.

  * **Java's UI is horrible.** The Java AWT/Swing libraries are bloated and buggy, but these are not used in EXOFLIGHT -- instead, a new OpenGL-based GUI was created from scratch, which besides being functional, looks pretty slick.

Java does have some drawbacks, however:

  * Interfacing with Microsoft technologies such as DirectX is not supported, forcing some technology decisions.

  * Java's garbage collection causes some "burps" in the execution when GC is performed.

  * Lack of control over memory allocation makes some features performance-poor (ROAM rendering, for example).

But my reasons for choosing Java are simple: Java has an excellent packaging system, super-fast compile speed, and pleasant language semantics. Also, the exception mechanism and ability to print stack traces make postmortem debugging much easier.

#### 1.4.2 OpenGL ####

OpenGL was selected as the graphics API because there is no DirectX8 wrapper for Java. The GL4Java wrapper was selected because it is the most-heavily-maintained wrapper available. OpenGL is a good choice for a space simulation application, but the unsure future of the API on Windows platforms makes this decision risky.

#### 1.4.3 SDL ####

SDL (Simple Directmedia Layer) is used for certain game-related functions, right now just for joystick access. Later it can be used for input, sound, and 2D graphics if the need arises. Another fringe benefit of using this API is that it is theoretically possible to run EXOFLIGHT on Linux, MacOS X, and other platforms that support OpenGL and SDL.

## 2 Major Subsystems ##

This section describes some of the major subsystems of EXOFLIGHT, and details how they were designed and implemented.

### 2.1 Modular Spacecraft ###

A spacecraft in EXOFLIGHT is composed of one or more modules. These modules connect to each other at right angles to form the vehicle structure. For instance, a two-stage rocket might have two modules: the lower and upper stage. The payload might be yet another module.

Each module in turn contains one or more components. Components include propulsion systems, reaction control systems (RCS), life support, batteries, fuel tanks, solar panels and other miscellaneous devices.

A component can contain a given amount of resources. Resources are things like fuel, oxidizer, supplies, and electricity. Each capability has a certain limit on the amounts and types of resources it can contain -- for example, a tank may only store hydrogen and oxidizer, and a battery may only store electricity. There are several different classes of capabilities, depending on function:

  * **Tank** - The simplest kind of capability -- it just stores resources.

  * **Propulsion** - There are several types of propulsion -- rocket, jet, nuclear, etc. It consumes resources and provides thrust.

  * **Attitude** - A special class of propulsion system that modifies the attitude of the spacecraft. This includes RCS thrusters, gimbals, and aerodynamic surfaces.

  * **Guidance** - Controls the attitude control components of the spacecraft, and performs guidance functions.

  * **ECS** - Environment Control System, or life support.

  * **Deployable** - A capability that can deploy another module -- a parachute, for instance.

  * **Dockable** - Allows docking to the module at a given position.

  * **Camera** - Used as a positional reference for the 3D visual view. You can select from any of the cameras in a spacecraft as your viewpoint -- for example, you may have a camera  looking out of the cockpit, one positioned from the docking hatch, and one on the outside hull facing aft.

### 2.2 Event-Driven Model ###

EXOFLIGHT uses a different execution model from most flight simulators -- an event-based model, rather than the traditional fixed time-slice model. A detailed overview of this model is given here: http://www.flipcode.com/tfiles/steven03.shtml The basic main loop of this type of simulation looks like this, in pseudo-code:

```

while (game_is_running)

{

   world.draw_to_screen(); 

   world.get_player_input(); 

   world.consume_events(current_time + time_step); 

   current_time += time_step; 

}

```

An event-based model is more complicated to implement, but provides some features not possible in a fixed-timeslice model. The main reason for using one in EXOFLIGHT is the necessity of providing arbitrary time-acceleration without loss of accuracy. Some missions in EXOFLIGHT may take years to complete, and even a 32x acceleration option would be insufficient. You'd need over 1,000,000x acceleration for a usable sim, which is difficult to do in a time-slice model. With the event-based model, we get arbitrary time rates, from 1 s = 7 ms to 1 s = 1 yr.

Changing the time rate does not change the behavior of the sim, which is a critical feature. If there is not enough CPU power available to run the simulator at the desired rate, the time rate is simply decreased until it is low enough for the CPU to handle.

### 2.3 Trajectories ###

One of the main functions of EXOFLIGHT is to simulate the trajectories of space-bound objects. All objects in space follow a trajectory, and the act of determining the path of an object from a given time and state vector is called trajectory propogation. We have an object-oriented system in EXOFLIGHT for performing this function.

Here are the various trajectory classes in EXOFLIGHT:

  * **Landed** - Describes an object that is stationary relative to a planet surface. Note that although the object is not moving with respect to the ground, it is moving in the EXOFLIGHT coordinate system (which is inertial) if the planet is rotating.

  * **Conic** - Used when the object describes a standard 6-element Keplerian conic trajectory.

  * **PolyElements** - A variation of the Conic trajectory class, which allows each parameter to be of the form f(t) where f is a polynomial function, and t is time.

  * **Cowell** - The trajectory of the object is integrated using an adaptive Runge-Kutta 4th order propagator. This is the most commonly used trajectory for dynamic objects such as spaceships. The same propagator is used for atmospheric flight (which requires a small time-step) as is used for orbital flight (which can use a longer time-step). This is also where ground interaction is modeled.

  * **Ephemeris** - Uses the DE405 ephemeris from JPL to retrieve the state vectors of the major planets. The DE405 data files are stored on disk and loaded as-needed.

Besides providing a state vector (position + velocity) the trajectory objects also track attitude and rotational velocity. This is done mostly in the CowellTrajectory class, and the trajectory switches off rotational integration when there is no torque applied to the spacecraft.

### 2.4 User Interface ###

The goals of the user interface design for EXOFLIGHT had many conflicting aspects:

  * The UI should be similar for different vehicles.

  * The UI should allow control of functions specific to a particular vehicle.

  * The UI should invoke the feeling of actually controlling a spacecraft.

A spacecraft in EXOFLIGHT can take different configurations based on which modules are attached.

### 2.5 Sequencer ###

The Exoflight Sequencer Language (ESL) provides a way to make flight programs for vehicles. In essence, it is a simple scripting language. Besides writing flight programs, it is good for scripting short sequences of actions. ESL sequences consist of a number of statements in linear order. Statements are executed in order until the sequence ends, the user halts the sequence, or if a statement fails.

## 3 Conclusion ##

### 3.1 Lessons Learned ###

### 3.2 Changes to be Made ###