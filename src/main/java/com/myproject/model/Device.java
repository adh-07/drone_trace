package com.myproject.model;

public class Device {
    private String deviceId;
    private String deviceName;
    private String model;
    
    public Device(String deviceId, String deviceName, String model) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.model = model;
    }
    
    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}