package com.mymonitor.dao;

import com.mymonitor.model.TelemetryData;
import com.mymonitor.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TelemetryDAO {
    
    public void saveTelemetry(TelemetryData telemetry) throws SQLException {
        // First, ensure device exists in devices table
        ensureDeviceExists(telemetry.getDeviceId());
        
        String sql = "INSERT INTO telemetry_data (device_id, latitude, longitude, altitude, speed, battery_level, temperature, humidity, pressure, heading, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, telemetry.getDeviceId());
            pstmt.setDouble(2, telemetry.getLatitude());
            pstmt.setDouble(3, telemetry.getLongitude());
            pstmt.setDouble(4, telemetry.getAltitude());
            pstmt.setDouble(5, telemetry.getSpeed());
            pstmt.setInt(6, telemetry.getBatteryLevel());
            pstmt.setDouble(7, telemetry.getTemperature());
            pstmt.setDouble(8, telemetry.getHumidity());
            pstmt.setDouble(9, telemetry.getPressure());
            pstmt.setDouble(10, telemetry.getHeading());
            pstmt.setString(11, telemetry.getStatus());
            
            pstmt.executeUpdate();
        }
    }
    
    private void ensureDeviceExists(String deviceId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM devices WHERE device_id = ?";
        String insertSql = "INSERT INTO devices (device_id, name, status) VALUES (?, ?, 'ACTIVE') ON CONFLICT (device_id) DO NOTHING";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, deviceId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Device doesn't exist, create it
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, deviceId);
                        insertStmt.setString(2, deviceId);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }
    
    public TelemetryData getLatestTelemetryForDevice(String deviceId) throws SQLException {
        String sql = "SELECT * FROM telemetry_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    TelemetryData data = new TelemetryData(
                        rs.getString("device_id"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getInt("battery_level")
                    );
                    data.setAltitude(rs.getDouble("altitude"));
                    data.setSpeed(rs.getDouble("speed"));
                    data.setTemperature(rs.getDouble("temperature"));
                    data.setHumidity(rs.getDouble("humidity"));
                    data.setPressure(rs.getDouble("pressure"));
                    data.setHeading(rs.getDouble("heading"));
                    data.setStatus(rs.getString("status"));
                    data.setTimestamp(rs.getTimestamp("timestamp").toInstant());
                    return data;
                }
            }
        }
        return null;
    }
    
    public List<TelemetryData> getDeviceTelemetryHistory(String deviceId, int limit) throws SQLException {
        String sql = "SELECT * FROM telemetry_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?";
        List<TelemetryData> history = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TelemetryData data = new TelemetryData(
                        rs.getString("device_id"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getInt("battery_level")
                    );
                    data.setAltitude(rs.getDouble("altitude"));
                    data.setSpeed(rs.getDouble("speed"));
                    data.setTemperature(rs.getDouble("temperature"));
                    data.setHumidity(rs.getDouble("humidity"));
                    data.setPressure(rs.getDouble("pressure"));
                    data.setHeading(rs.getDouble("heading"));
                    data.setStatus(rs.getString("status"));
                    data.setTimestamp(rs.getTimestamp("timestamp").toInstant());
                    history.add(data);
                }
            }
        }
        return history;
    }
}