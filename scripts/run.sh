#!/bin/sh
WIDTH=1024
HEIGHT=768
FULLSCRN=true
MAINCLASS=com.fasterlight.exo.main.Exoflight
CPATH=lib/FLCore.jar:lib/Exoflight.jar:lib/jogl.jar:lib/gluegen-rt.jar:lib/joal.jar:lib/jinput.jar:lib/png.jar:.
VMARGS="-Xdock:icon=data/etc/Exoflight.icns -Dapple.laf.useScreenMenuBar=true -Dcom.apple.mrj.application.apple.menu.about.name=Exoflight -Djava.library.path=lib/ -Dexo.fullscreen=$FULLSCRN -Dexo.scrnwidth=$WIDTH -Dexo.scrnheight=$HEIGHT"
ARGS="-stdout stdout.txt -stderr stderr.txt"
java $VMARGS -cp $CPATH $MAINCLASS $ARGS
