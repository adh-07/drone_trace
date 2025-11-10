package com.mymonitor.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;

/**
 * Visual Distance Meter with Needle Gauge
 * Shows how close or far a Bluetooth device is
 * Based on RSSI (signal strength)
 */
public class DistanceMeter extends StackPane {
    private Canvas canvas;
    private double currentDistance = 0; // 0 = Very Close, 100 = Very Far
    private String statusText = "Searching...";
    
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int CENTER_X = WIDTH / 2;
    private static final int CENTER_Y = HEIGHT - 30;
    private static final int RADIUS = 120;
    
    public DistanceMeter() {
        canvas = new Canvas(WIDTH, HEIGHT);
        getChildren().add(canvas);
        drawMeter();
    }
    
    /**
     * Update the meter based on signal strength (RSSI)
     * @param rssi Signal strength (-100 to 0 dBm)
     */
    public void updateBySignalStrength(int rssi) {
        // Convert RSSI to distance percentage
        // -30 dBm = Very Close (0%)
        // -100 dBm = Very Far (100%)
        
        if (rssi >= -30) {
            currentDistance = 0;
            statusText = "Very Close";
        } else if (rssi >= -50) {
            currentDistance = 20;
            statusText = "Close";
        } else if (rssi >= -70) {
            currentDistance = 50;
            statusText = "Medium";
        } else if (rssi >= -85) {
            currentDistance = 75;
            statusText = "Far";
        } else {
            currentDistance = 95;
            statusText = "Very Far";
        }
        
        drawMeter();
    }
    
    /**
     * Update meter with manual distance percentage
     * @param distance 0-100 (0=very close, 100=very far)
     */
    public void updateDistance(double distance) {
        currentDistance = Math.max(0, Math.min(100, distance));
        
        if (distance < 20) statusText = "Very Close";
        else if (distance < 40) statusText = "Close";
        else if (distance < 60) statusText = "Medium";
        else if (distance < 80) statusText = "Far";
        else statusText = "Very Far";
        
        drawMeter();
    }
    
    /**
     * Set searching/disconnected state
     */
    public void setSearching() {
        currentDistance = 50;
        statusText = "Searching...";
        drawMeter();
    }
    
    public void setDisconnected() {
        currentDistance = 100;
        statusText = "Disconnected";
        drawMeter();
    }
    
    private void drawMeter() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Clear canvas
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        
        // Draw background
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw title
        gc.setFill(Color.rgb(50, 50, 50));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.fillText("DEVICE PROXIMITY", CENTER_X - 70, 25);
        
        // Draw colored arc zones
        drawColoredZones(gc);
        
        // Draw scale markers and labels
        drawScaleMarkers(gc);
        
        // Draw needle
        drawNeedle(gc);
        
        // Draw center circle
        gc.setFill(Color.rgb(80, 80, 80));
        gc.fillOval(CENTER_X - 10, CENTER_Y - 10, 20, 20);
        
        // Draw status text
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        Color statusColor = getStatusColor();
        gc.setFill(statusColor);
        gc.fillText(statusText, CENTER_X - gc.getFont().getSize() * statusText.length() / 4, HEIGHT - 5);
    }
    
    private void drawColoredZones(GraphicsContext gc) {
        // Green zone (Very Close) - 0-20%
        gc.setStroke(Color.rgb(34, 197, 94));
        gc.setLineWidth(15);
        drawArc(gc, -140, 28);
        
        // Light Green zone (Close) - 20-40%
        gc.setStroke(Color.rgb(74, 222, 128));
        drawArc(gc, -112, 28);
        
        // Yellow zone (Medium) - 40-60%
        gc.setStroke(Color.rgb(250, 204, 21));
        drawArc(gc, -84, 28);
        
        // Orange zone (Far) - 60-80%
        gc.setStroke(Color.rgb(251, 146, 60));
        drawArc(gc, -56, 28);
        
        // Red zone (Very Far) - 80-100%
        gc.setStroke(Color.rgb(239, 68, 68));
        drawArc(gc, -28, 28);
    }
    
    private void drawArc(GraphicsContext gc, double startAngle, double extent) {
        gc.strokeArc(CENTER_X - RADIUS, CENTER_Y - RADIUS, 
                    RADIUS * 2, RADIUS * 2, 
                    startAngle, extent, javafx.scene.shape.ArcType.OPEN);
    }
    
    private void drawScaleMarkers(GraphicsContext gc) {
        gc.setStroke(Color.rgb(100, 100, 100));
        gc.setLineWidth(2);
        gc.setFill(Color.rgb(80, 80, 80));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        
        String[] labels = {"VERY\nCLOSE", "CLOSE", "MEDIUM", "FAR", "VERY\nFAR"};
        
        for (int i = 0; i <= 4; i++) {
            double angle = Math.toRadians(-140 + (i * 35));
            double x1 = CENTER_X + Math.cos(angle) * (RADIUS - 20);
            double y1 = CENTER_Y + Math.sin(angle) * (RADIUS - 20);
            double x2 = CENTER_X + Math.cos(angle) * RADIUS;
            double y2 = CENTER_Y + Math.sin(angle) * RADIUS;
            
            gc.strokeLine(x1, y1, x2, y2);
            
            // Draw labels
            double labelX = CENTER_X + Math.cos(angle) * (RADIUS + 25);
            double labelY = CENTER_Y + Math.sin(angle) * (RADIUS + 25);
            
            if (i < labels.length) {
                gc.fillText(labels[i], labelX - 20, labelY + 5);
            }
        }
    }
    
    private void drawNeedle(GraphicsContext gc) {
        // Calculate needle angle based on distance
        // 0% = -140 degrees, 100% = 0 degrees
        double angle = -140 + (currentDistance * 1.4);
        double radians = Math.toRadians(angle);
        
        // Needle coordinates
        double needleLength = RADIUS - 15;
        double needleX = CENTER_X + Math.cos(radians) * needleLength;
        double needleY = CENTER_Y + Math.sin(radians) * needleLength;
        
        // Draw needle shadow
        gc.setStroke(Color.rgb(0, 0, 0, 0.3));
        gc.setLineWidth(6);
        gc.strokeLine(CENTER_X + 2, CENTER_Y + 2, needleX + 2, needleY + 2);
        
        // Draw needle
        gc.setStroke(Color.rgb(220, 38, 38));
        gc.setLineWidth(5);
        gc.strokeLine(CENTER_X, CENTER_Y, needleX, needleY);
        
        // Draw needle tip
        gc.setFill(Color.rgb(220, 38, 38));
        gc.fillOval(needleX - 6, needleY - 6, 12, 12);
    }
    
    private Color getStatusColor() {
        if (statusText.equals("Searching...") || statusText.equals("Disconnected")) {
            return Color.rgb(100, 100, 100);
        } else if (statusText.equals("Very Close") || statusText.equals("Close")) {
            return Color.rgb(34, 197, 94);
        } else if (statusText.equals("Medium")) {
            return Color.rgb(250, 204, 21);
        } else {
            return Color.rgb(239, 68, 68);
        }
    }
}
