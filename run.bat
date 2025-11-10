@echo off
echo Starting Drone Monitor System

REM Set JAVA_HOME (update this path if your JDK is installed elsewhere)
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot

REM Build the project
call mvnw.cmd clean package

REM Start the WebSocket Server
start "Drone WebSocket Server" cmd /c "java -cp target\DroneFrontEnd-1.0-SNAPSHOT.jar com.mymonitor.DroneWebSocketServer"

REM Wait a moment for the server to start
timeout /t 2

REM Start the JavaFX frontend
call mvnw.cmd javafx:run
timeout /t 2

REM Start the Monitor Application
start "Drone Monitor" cmd /c "java -cp target\classes;target\dependency\* com.mymonitor.SingleFileMonitorApp"