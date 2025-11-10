@echo off
setlocal

:: Set Java environment
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Running application with Maven JavaFX plugin...
call .\apache-maven-3.9.5\bin\mvn javafx:run

if errorlevel 1 (
    echo Application failed to start.
    pause
    exit /b 1
)

pause