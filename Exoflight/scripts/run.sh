#!/bin/sh
WIDTH=1024
HEIGHT=768
FULLSCRN=true
MAINCLASS=com.fasterlight.exo.main.Exoflight
CPATH=lib/FLCore.jar:lib/Exoflight.jar:lib/jogl.jar:lib/gluegen-rt.jar:lib/joal.jar:lib/jinput.jar:lib/png.jar:.
EXTRAARGS="-Dapple.laf.useScreenMenuBar=true -Dcom.apple.mrj.application.apple.menu.about.name=Exoflight"
java $EXTRAARGS -Djava.library.path=lib/ -Dexo.fullscreen=$FULLSCRN -Dexo.scrnwidth=$WIDTH -Dexo.scrnheight=$HEIGHT -cp $CPATH $MAINCLASS
