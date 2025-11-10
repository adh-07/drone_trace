@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

java --enable-preview ^
     --module-path "%~dp0\target\DroneFrontEnd-1.0-SNAPSHOT.jar;%JAVA_HOME%\lib" ^
     --add-modules=javafx.controls,javafx.graphics ^
     -jar target/DroneFrontEnd-1.0-SNAPSHOT.jar

pause