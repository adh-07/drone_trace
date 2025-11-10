package com.mymonitor.model;

import java.time.Instant;

public class TelemetryData {
    private String deviceId;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private int batteryLevel;
    private double temperature;
    private double humidity;
    private double pressure;
    private double heading;
    private String status;
    private Instant timestamp;

    public TelemetryData(String deviceId, double latitude, double longitude, int batteryLevel) {
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.batteryLevel = batteryLevel;
        this.timestamp = Instant.now();
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
    public double getSpeed() { return speed; }
    public int getBatteryLevel() { return batteryLevel; }
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getPressure() { return pressure; }
    public double getHeading() { return heading; }
    public String getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }

    // Setters
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public void setPressure(double pressure) { this.pressure = pressure; }
    public void setHeading(double heading) { this.heading = heading; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}