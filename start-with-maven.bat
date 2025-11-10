@echo off
echo Starting Drone Monitor System

REM Set JAVA_HOME
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot

REM Set M2_HOME to the Maven installation
set M2_HOME=c:\Users\moham\Downloads\java\apache-maven-3.9.5
set PATH=%M2_HOME%\bin;%PATH%

REM First, compile and package the application
call mvn clean package

if errorlevel 1 (
    echo Failed to build the application
    pause
    exit /b 1
)

REM Create target/dependency directory if it doesn't exist
if not exist target\dependency mkdir target\dependency

REM Copy dependencies to target/dependency
cd target\dependency
jar xf ..\DroneFrontEnd-1.0-SNAPSHOT.jar
cd ..\..

REM Start the WebSocket Server
start "Drone WebSocket Server" cmd /c "java -cp target\DroneFrontEnd-1.0-SNAPSHOT.jar;target\dependency\* com.mymonitor.DroneWebSocketServer"

REM Wait a moment for the server to start
timeout /t 2

REM Start the JavaFX frontend
call mvn javafx:run

pause