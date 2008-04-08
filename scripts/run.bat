@echo off
set WIDTH=1024
set HEIGHT=768
set FULLSCRN=true
set MAINCLASS=com.fasterlight.exo.main.Exoflight
set CPATH=lib/FLCore.jar;lib/Exoflight.jar;lib/jogl.jar;lib/gluegen-rt.jar;lib/joal.jar;lib/jinput.jar;lib/png.jar;.
set VMARGS="-Djava.library.path=.\lib -Dexo.fullscreen=%FULLSCRN% -Dexo.scrnwidth=%WIDTH% -Dexo.scrnheight=%HEIGHT%"
set ARGS="-stdout stdout.txt -stderr stderr.txt"
java %VMARGS% -cp %CPATH% %MAINCLASS% %ARGS%
