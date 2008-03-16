@echo off
set MAINCLASS=com.fasterlight.exo.main.Exoflight
java -Djava.library.path=.\lib -cp lib/FLCore.jar;lib/Exoflight.jar;lib/jogl.jar;lib/gluegen-rt.jar;lib/sdljava.jar;lib/png.jar;. %MAINCLASS% 
cd ..
