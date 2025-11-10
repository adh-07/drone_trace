@echo off
REM Start the WebSocket server in a new window
start cmd /k "mvnw exec:java -Dexec.mainClass=com.myproject.DroneServer"

REM Wait for a moment to let the server start
timeout /t 5

REM Start the JavaFX application
mvnw clean javafx:run