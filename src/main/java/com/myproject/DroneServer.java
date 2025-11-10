package com.myproject;

import com.myproject.dao.TelemetryDAO;
import com.myproject.model.TelemetryData;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DroneServer {
    private static Map<String, WsContext> wsConnections = new ConcurrentHashMap<>();
    private static TelemetryDAO telemetryDAO = new TelemetryDAO();
    
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> {
                it.allowHost("http://localhost:3000"); // Update with your frontend URL
                it.allowHost("http://localhost:8080");
            }));
        }).start(7070);
        
        // WebSocket endpoint
        app.ws("/ws/dashboard", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("New WebSocket connection: " + ctx.getSessionId());
                wsConnections.put(ctx.getSessionId(), ctx);
                
                // Send latest telemetry on connect
                try {
                    TelemetryData latestData = telemetryDAO.getLatestTelemetryForDevice("Drone-Alpha-001");
                    if (latestData != null) {
                        JSONObject json = new JSONObject()
                            .put("deviceId", latestData.getDeviceId())
                            .put("latitude", latestData.getLatitude())
                            .put("longitude", latestData.getLongitude())
                            .put("batteryPercent", latestData.getBatteryPercent());
                        
                        ctx.send(json.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            ws.onClose(ctx -> {
                System.out.println("WebSocket closed: " + ctx.getSessionId());
                wsConnections.remove(ctx.getSessionId());
            });
            
            ws.onError(ctx -> {
                System.out.println("WebSocket error: " + ctx.error().getMessage());
                wsConnections.remove(ctx.getSessionId());
            });
        });
        
        // REST endpoint to receive telemetry updates
        app.post("/api/telemetry", ctx -> {
            JSONObject json = new JSONObject(ctx.body());
            
            TelemetryData telemetry = new TelemetryData(
                json.getString("deviceId"),
                json.getDouble("latitude"),
                json.getDouble("longitude"),
                json.getInt("batteryPercent")
            );
            
            // Save to database
            telemetryDAO.saveTelemetry(telemetry);
            
            // Broadcast to all connected WebSocket clients
            String telemetryJson = new JSONObject()
                .put("deviceId", telemetry.getDeviceId())
                .put("latitude", telemetry.getLatitude())
                .put("longitude", telemetry.getLongitude())
                .put("batteryPercent", telemetry.getBatteryPercent())
                .toString();
            
            wsConnections.values().forEach(ws -> ws.send(telemetryJson));
            
            ctx.status(200).result("Telemetry received");
        });
        
        System.out.println("Drone Server started on port 7070");
    }
}