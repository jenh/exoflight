# Introduction #

The Exoflight Sequencer Language (ESL) provides a way to make flight programs for vehicles. In essence, it is a simple scripting language. Besides writing flight programs, it is good for scripting short sequences of actions.
ESL sequences consist of a number of statements in linear order. Statements are executed in order until the sequence ends, the user halts the sequence, or if a statement fails.

Note: ESL is not meant to be a full-featured programming language -- it is simply meant to sequence steps of simple operations together (i.e. "checklists"). If there is a feature ESL cannot implement it is simple and much more efficient to write Java class files and bind them to ESL scripts.

The  primary uses of ESL are:

## Initialization ##

ESL is a simple way to configure various program parameters. The "init" directory contains scripts for building the solar system objects.

## Missions ##

Exoflight missions are defined in ESL. This includes defining the vehicles involved, their initial positions, and their systems. The sequence can then automate the mission, waiting on conditions or event times, and/or give feedback to the user during events. These live in the `missions` directory.

## Programs ##

Each vehicle's module has a number of ModulePrograms. These are sequences designed to implement various phases of flight. Typically defined are launch, reentry, docking, intercept, landing.

# Syntax #

Each statement is terminated by a semicolon. Descriptions and labels are prefixes of statements and do not require semicolons.

Comments are designated by two hyphens; the text after the hyphens is considered a comment. For example:
```
-- this is a comment
	ship.name = "Foo"; -- this is also a comment
```

## Set Statement ##
The most common type of statement is the set statement. This takes the form of:
```
	<property> = <value>
```
The left side of the argument must be a property type, the right side can be any value type. ESL supports several value types:

Properties. A property consists of a number of identifiers separated by periods. An example is ship.parent.name. Valid identifier characters are letters, numbers, _, and $. In the case where identifiers contain spaces or other non-identifier characters, you can use the notation 'property'. For example: ship.structure.'$Apollo CM'.$engine._

Literals. Integers, floating-point values, strings, and booleans are all supported. Adding deg after a numeric value indicates that it is to be converted into radians. The keyword null denotes a null reference. In addition, a vector with between two and four elements is represented like this: [x1,x2...]

Local Variables. Use the notation @varname to declare a variable local to the sequence. Variables have the value null as default.

New Object. To create a new object use the notation new classname. If you do not specify the package name, the interpreter will search through a predefinied collection of packages to find the class. Most of the classes you will use will not need a package specification, unless you introduce user-defined classes.

Here are some examples of set statements:
```
	ship.structure.'$Orbiter'.'$guidance system'.active = true;
	@S_I = ship.structure.'$S-IC';
	ship.attctrl.guidance.roll = 180.0 deg;
	@traj.landpos = [-110 deg, 29 deg, 6384];
```
A set statement normally fails if the property set on the left side of the = is rejected, or if the property fetch on the right side fails. An optional set statement is a set statement where the equals sign is replaced by "=?". In this case the failure will be ignored, and execution will proceed normally. Example:
```
	@gsound.say =? "Main engine cutoff.";
```
## Wait Statements ##
There are three types of wait statements, which suspend the sequencer until a given condition is met or a given time elasped.

wait for `duration`

The relative wait statement suspends the sequencer until a given number of seconds have elapsed. The number of seconds must be a constant numeric value, that is, is cannot be specified at runtime.

wait until `mission-time`

The absolute wait statement suspends the sequencer until a given offset of zero mission time is reached. For example, wait until -10 suspends until T=-10 seconds. The duration value must also be a constant value. This statement fails if the time specified is in the past.

wait for condition `(condition)` interval value `[timeout duration]`

The conditional wait statement suspends the sequencer until a given condition is met, or until the timeout value (if specified) is reached. Condition is a Boolean expression. The interval value, given in seconds, specifies how often to poll the condition. Both the interval value and timeout value must be constants.

Examples of wait statements in use:
```
"Descent module separation"
        @ldm.detach = true;
        wait for 0.2;
"Ignite ascent engine"
```
```
        wait until -8.9;
"Activate main engines"
        @S_I.'$engine 1'.fired = 1;
```
```
"Verify attitude lock"
        wait for condition (ship.attctrl.locked == true) interval 0.25 timeout 15;
```

## Set/Cancel Abort Statements ##
An ESL sequence can have one or more abort modes. An abort mode is a mapping between a string and a label, and are added with the set abort statement. When a given abort mode is activated, control is passed to the label associated with the abort mode string.

As an example, this code sets up the "ABORT STAGE" mode for the Apollo LM:
```
	set abort "ABORT STAGE" goto Abort_Stage;
	...
	... landing program continues

Abort_Stage:
"ABORT STAGE"
	@ldm.'$descent engine'.throttle = 0;
	...
	... abort program continues
```

A previously designated abort mode can be removed with the cancel abort statement:
```
	-- no longer need abort mode I
	cancel abort "Abort Mode I";
```

## Stop Statement ##
The stop command is used to terminate the sequence. It is often used to separate the nominal portion of a sequence from the abort logic. Example:
```
"---END LANDING SEQUENCE---"
        stop;

Abort_NoStage:
"ABORT"
        @ldm.'$descent engine'.throttle = 1;
```

## Goto Statement ##
The goto statement passes control to a specified label. It is used to create looping constructs inside of sequences.

## Descriptions ##
Any statement can be preceded by a string in double quotes, which indicates a description for the statement that it precedes. Use descriptions in vehicle programs to give feedback on the progress of the sequence. For example:
```
		@retro = ship.structure.'$Retro Module';
	"Perform retro burn"
	        @retro.'$rocket 1'.fired = true;
		@retro.'$rocket 2'.fired = true;
```