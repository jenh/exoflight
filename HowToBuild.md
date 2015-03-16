## Running from Eclipse ##

You need to check out both the Exoflight project and the [FLCore library](http://code.google.com/p/flcore/). Be sure to look at the [FLCore HowToBuild](http://code.google.com/p/flcore/wiki/HowToBuild) wiki page.

While you're doing that, download all the files in the "Featured" section of the [Downloads](http://code.google.com/p/exoflight/downloads/list) tab and unzip them to the `Exoflight/data` directory. Some like the ephemeris files might be redundant.

When done load `build.xml` and execute the `run` Ant task to start the sim.

To execute it directly from Eclipse, the main class file is `com.fasterlight.exo.main.Exoflight`. You will have to add the string `-Djava.library.path=..\FLCore\lib\<platform>` to the VM args section of the Run dialog.