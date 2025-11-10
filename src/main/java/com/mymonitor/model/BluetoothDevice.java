package com.mymonitor.model;

import java.time.Instant;

public class BluetoothDevice {
    private String deviceId;
    private String deviceName;
    private String address;
    private double latitude;
    private double longitude;
    private int batteryLevel;
    private boolean connected;
    private Instant lastSeen;
    private String deviceType;
    private int rssi; // Signal strength in dBm (-100 to 0)

    public BluetoothDevice(String deviceId, String deviceName, String address) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.address = address;
        this.connected = false;
        this.batteryLevel = 0;
        this.lastSeen = Instant.now();
    }

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public int getRssi() { return rssi; }
    public void setRssi(int rssi) { this.rssi = rssi; }
}


