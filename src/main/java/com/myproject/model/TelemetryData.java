package com.myproject.model;

import java.time.LocalDateTime;

public class TelemetryData {
    private Long id;
    private String deviceId;
    private LocalDateTime timestamp;
    private double latitude;
    private double longitude;
    private int batteryPercent;
    
    public TelemetryData(String deviceId, double latitude, double longitude, int batteryPercent) {
        this.deviceId = deviceId;
        this.timestamp = LocalDateTime.now();
        this.latitude = latitude;
        this.longitude = longitude;
        this.batteryPercent = batteryPercent;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(int batteryPercent) { this.batteryPercent = batteryPercent; }
}