package com.mymonitor;

/**
 * Launcher class for running the JavaFX application from a shaded JAR
 * This class does not extend Application, avoiding JavaFX initialization issues
 */
public class Launcher {
    public static void main(String[] args) {
        // Launch the JavaFX application
        SingleFileMonitorApp.main(args);
    }
}
