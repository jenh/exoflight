### What do I do with this? ###

Good question. As of version 0.1.3 there is a "Free Flight" mission which loads when Exoflight starts. This features the Space Wagon, which is good for just puttering around. Look at the KeyboardShortcuts page to get started.

But the best way to learn your way around the simulator right now is to try the Gemini Missions (Ctrl-N). These are four tutorial missions that take you through the phases of launch, interception, docking, and reentry. Just follow the on-screen directions.

You could also try the `File | Custom Mission` (the menu pops down from the top in Windows) or `File | Set Location` to place your to place a spaceship at an arbitrary location in the Solar System and mess around.

If you're astrodynamically-inclined, you could also take a shot at making custom missions with the [Exoflight Sequencer Language](http://code.google.com/p/exoflight/wiki/SequencerLanguage) -- see the `missions` and `programs` directory for detailed (the source code may help here until we get all this doc'ed)

### I don't see files automatically getting downloaded after startup. ###

If you are behind a proxy your best bet is to download them manually. Go to the [Downloads](http://code.google.com/p/exoflight/downloads/list) tab and download all the .zip files with the "Required" tag. Then unzip them all to the `C:\Program Files\Exoflight` directory (on Windows) or the `Exoflight.app/Contents/Resources` directory (Mac OS X).

If you create a file called `.X.tag` (where `X` is the name of the zip file) it won't try to download them again.

### Doesn't run on OS X 10.6 Snow Leopard, what gives? ###

Exoflight depends on certain native libraries that have not been tested with 64-bit Java. The workaround is below (thanks zolotkey):

  1. Download the app, unpack the .tgz
  1. Click on Exoflight.app in Finder
  1. Right-click and select Get Info (or hit Command-I)
  1. Expand the General group and check "Run in 32-bit Mode"
  1. Run the application