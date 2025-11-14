@echo off
setlocal

:: Set Java environment
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================
echo Running JavaFX Application WITHOUT Maven
echo ========================================
echo.

:: Check if JAR exists
if not exist "target\DroneFrontEnd-1.0-SNAPSHOT.jar" (
    echo ERROR: JAR file not found!
    echo Please build the project first using Maven:
    echo   .\apache-maven-3.9.5\bin\mvn.cmd clean package -DskipTests
    echo.
    pause
    exit /b 1
)

:: Run the application directly with Java (no Maven needed)
echo Starting application...
echo Main Class: com.mymonitor.Launcher
echo Note: All dependencies are bundled in the JAR (no Maven required)
echo.

"%JAVA_HOME%\bin\java" ^
    --enable-preview ^
    --enable-native-access=ALL-UNNAMED ^
    -Dprism.order=sw ^
    -Djavafx.platform=win ^
    -classpath "target\DroneFrontEnd-1.0-SNAPSHOT.jar" ^
    com.mymonitor.Launcher

if errorlevel 1 (
    echo.
    echo Application terminated with an error.
    pause
    exit /b 1
)

echo.
echo Application closed successfully.
pause
