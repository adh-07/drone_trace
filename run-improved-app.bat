@echo off
echo Running Improved Monitor Application...
echo.

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d "%~dp0"

.\apache-maven-3.9.5\bin\mvn.cmd javafx:run -Djavafx.mainClass=com.mymonitor.ImprovedMonitorApp

pause
