package com.myproject.dao;

import com.myproject.model.TelemetryData;
import com.myproject.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TelemetryDAO {
    
    public void saveTelemetry(TelemetryData data) throws SQLException {
        String sql = "INSERT INTO TelemetryData (device_id_fk, timestamp, latitude, longitude, batteryPercent) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, data.getDeviceId());
            stmt.setTimestamp(2, Timestamp.valueOf(data.getTimestamp()));
            stmt.setDouble(3, data.getLatitude());
            stmt.setDouble(4, data.getLongitude());
            stmt.setInt(5, data.getBatteryPercent());
            
            stmt.executeUpdate();
        }
    }
    
    public List<TelemetryData> getLatestTelemetry() throws SQLException {
        String sql = "SELECT * FROM TelemetryData ORDER BY timestamp DESC LIMIT 10";
        List<TelemetryData> telemetryList = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                TelemetryData data = new TelemetryData(
                    rs.getString("device_id_fk"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getInt("batteryPercent")
                );
                data.setId(rs.getLong("id"));
                data.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                telemetryList.add(data);
            }
        }
        return telemetryList;
    }
    
    public TelemetryData getLatestTelemetryForDevice(String deviceId) throws SQLException {
        String sql = "SELECT * FROM TelemetryData WHERE device_id_fk = ? ORDER BY timestamp DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, deviceId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                TelemetryData data = new TelemetryData(
                    rs.getString("device_id_fk"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getInt("batteryPercent")
                );
                data.setId(rs.getLong("id"));
                data.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                return data;
            }
        }
        return null;
    }
}