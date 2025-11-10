package com.mymonitor;

import com.mymonitor.dao.TelemetryDAO;
import com.mymonitor.model.TelemetryData;
import com.mymonitor.util.DatabaseConnection;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;

public class DroneWebSocketServer extends WebSocketServer {
    private final TelemetryDAO telemetryDAO = new TelemetryDAO();
    private final Set<WebSocket> connections = new HashSet<>();

    public DroneWebSocketServer() {
        super(new InetSocketAddress("localhost", 7070));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from " + conn.getRemoteSocketAddress());
        connections.add(conn);
        
        try {
            // Send latest telemetry data for active devices
            TelemetryData latestData = telemetryDAO.getLatestTelemetryForDevice("Drone-Alpha-001");
            if (latestData != null) {
                JSONObject data = new JSONObject()
                    .put("deviceId", latestData.getDeviceId())
                    .put("latitude", latestData.getLatitude())
                    .put("longitude", latestData.getLongitude())
                    .put("batteryLevel", latestData.getBatteryLevel())
                    .put("altitude", latestData.getAltitude())
                    .put("speed", latestData.getSpeed())
                    .put("temperature", latestData.getTemperature())
                    .put("humidity", latestData.getHumidity())
                    .put("pressure", latestData.getPressure())
                    .put("heading", latestData.getHeading())
                    .put("status", latestData.getStatus());
                
                conn.send(data.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection to " + conn.getRemoteSocketAddress());
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject json = new JSONObject(message);
            
            // Save telemetry update to database
            TelemetryData telemetry = new TelemetryData(
                json.getString("deviceId"),
                json.getDouble("latitude"),
                json.getDouble("longitude"),
                json.getInt("batteryPercent")
            );
            
            telemetryDAO.saveTelemetry(telemetry);
            
            // Broadcast to all connected clients
            for (WebSocket client : connections) {
                client.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred on connection " + conn.getRemoteSocketAddress());
        ex.printStackTrace();
        if (conn != null) {
            connections.remove(conn);
        }
    }

    @Override
    public void onStart() {
        // Ensure database connectivity on server start
        DatabaseConnection.ensureConnected();
        if (DatabaseConnection.isConnected()) {
            System.out.println("Database connection OK (PostgreSQL)");
        } else {
            System.err.println("Database connection FAILED. Verify PostgreSQL is running and credentials are correct.");
        }
        System.out.println("WebSocket server started on ws://localhost:7070/ws/dashboard");
    }

    public static void main(String[] args) {
        DroneWebSocketServer server = new DroneWebSocketServer();
        server.start();
    }
}