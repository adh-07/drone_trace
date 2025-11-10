@echo off
setlocal

:: Set Java environment
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Run the application with JavaFX modules from the JAR
echo Starting DroneFrontEnd application...
"%JAVA_HOME%\bin\java" ^
    --enable-preview ^
    -Djavafx.platform=win ^
    --module-path "%~dp0\target\DroneFrontEnd-1.0-SNAPSHOT.jar" ^
    --add-modules=ALL-MODULE-PATH ^
    -cp "%~dp0\target\DroneFrontEnd-1.0-SNAPSHOT.jar" ^
    com.mymonitor.SingleFileMonitorApp

if errorlevel 1 (
    echo Application terminated with an error.
    pause
    exit /b 1
)

pause