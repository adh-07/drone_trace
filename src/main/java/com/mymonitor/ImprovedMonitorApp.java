package com.mymonitor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.mymonitor.model.BluetoothDevice;
import com.mymonitor.service.AdvancedBluetoothService;
import com.mymonitor.service.AdvancedLocationService;

import java.net.URI;
import java.net.URL;
import java.util.List;
import netscape.javascript.JSObject;

public class ImprovedMonitorApp extends Application {

    // --- UI Components ---
    private Label statusLabel;
    private Label locationLabel;
    private Label deviceIdLabel;
    private ProgressBar batteryBar;
    private Label batteryPercentLabel;
    private Label connectionLabel;

    private WebView webView;
    private WebEngine webEngine;

    private WebSocketClient wsClient;
    private static final String SERVER_URI = "ws://localhost:7070/ws/dashboard";

    private boolean isMapReady = false;
    
    // Bluetooth Service
    private AdvancedBluetoothService bluetoothService;
    private BluetoothDevice currentDevice;

    private static class ConsoleBridge {
        public void log(String message) { System.out.println(message); }
        public void warn(String message) { System.out.println(message); }
        public void error(String message) { System.err.println(message); }
    }

    @Override
    public void start(Stage primaryStage) {

        // --- 1. Create Layout Panes ---
        BorderPane root = new BorderPane();
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(15, 20, 15, 20));
        rightPanel.setPrefWidth(350);
        rightPanel.setStyle("-fx-background-color: #f5f5f5;");

        // --- 2. Create Telemetry Panel (Right Side) ---
        Label titleLabel = new Label("🛰 Live Device Monitor");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // Connection Status
        HBox statusBox = new HBox(10);
        Label statusTitle = new Label("WebSocket:");
        statusTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        statusLabel = new Label("Connecting...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        statusLabel.setTextFill(Color.ORANGE);
        statusBox.getChildren().addAll(statusTitle, statusLabel);

        // Bluetooth Status
        HBox btStatusBox = new HBox(10);
        Label btTitle = new Label("Bluetooth:");
        btTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        connectionLabel = new Label("Scanning...");
        connectionLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        connectionLabel.setTextFill(Color.BLUE);
        btStatusBox.getChildren().addAll(btTitle, connectionLabel);

        GridPane grid = createTelemetryGrid();
        rightPanel.getChildren().addAll(titleLabel, statusBox, btStatusBox, grid);

        // --- 3. Create Map Panel (Center) ---
        webView = new WebView();
        webEngine = webView.getEngine();

        // Load the map.html file from resources
        URL mapHtmlUrl = getClass().getResource("/map.html");
        if (mapHtmlUrl == null) {
            System.err.println("ERROR: Cannot find map.html in resources!");
            System.err.println("Make sure it exists at: src/main/resources/map.html");
        } else {
            // Listen for map load completion
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    isMapReady = true;
                    System.out.println("✓ Map loaded successfully (Leaflet)");

                    // Expose console bridge to JS so we can see errors from the page
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("appConsole", new ConsoleBridge());
                        // Pipe console.* to Java and hook Leaflet tile events
                        String hook = String.join("\n",
                            "(function(){",
                            "  var oldLog = console.log, oldErr = console.error, oldWarn = console.warn;",
                            "  console.log = function(){ try{ appConsole.log([].slice.call(arguments).join(' ')); }catch(e){} oldLog.apply(console, arguments); };",
                            "  console.error = function(){ try{ appConsole.error([].slice.call(arguments).join(' ')); }catch(e){} oldErr.apply(console, arguments); };",
                            "  console.warn = function(){ try{ appConsole.warn([].slice.call(arguments).join(' ')); }catch(e){} oldWarn.apply(console, arguments); };",
                            "  window.addEventListener('error', function(ev){ try{ appConsole.error('JS Error: ' + ev.message); }catch(e){} });",
                            "  if (window.L && window.L.tileLayer) {",
                            "    setTimeout(function(){",
                            "      try {",
                            "        var mapEl = document.getElementById('map');",
                            "        if (mapEl && map && map.on) {",
                            "          map.on('tileerror', function(e){ appConsole.error('Tile error: ' + (e && e.tile && e.tile.src)); });",
                            "          map.on('load', function(){ appConsole.log('Leaflet map load event'); });",
                            "        }",
                            "      } catch(e) { appConsole.error('Hook error: ' + e); }",
                            "    }, 0);",
                            "  }",
                            "})();"
                        );
                        webEngine.executeScript(hook);
                    } catch (Exception ex) {
                        System.err.println("Failed to install JS console bridge: " + ex.getMessage());
                    }
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("✗ WebView failed to load map.html");
                }
            });
            webEngine.load(mapHtmlUrl.toExternalForm());
        }

        // --- 4. Assemble the Window ---
        root.setCenter(webView);
        root.setRight(rightPanel);

        // --- 5. Start Services ---
        connectWebSocket();
        startBluetoothMonitoring();

        // --- 6. Show the Scene ---
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("🛰 Real-Time Device Tracking");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (wsClient != null) wsClient.close();
            if (bluetoothService != null) {
                bluetoothService.stopScanning();
                bluetoothService.shutdown();
            }
            Platform.exit();
        });
    }

    // Helper method to create the telemetry grid
    private GridPane createTelemetryGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(15, 0, 0, 0));
        
        int row = 0;
        
        // Device ID
        Label deviceIdTitle = new Label("Device ID:");
        deviceIdTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        grid.add(deviceIdTitle, 0, row);
        deviceIdLabel = new Label("N/A");
        deviceIdLabel.setFont(Font.font("System", 13));
        grid.add(deviceIdLabel, 1, row++);
        
        // Location
        Label locationTitle = new Label("Location:");
        locationTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        grid.add(locationTitle, 0, row);
        locationLabel = new Label("N/A");
        locationLabel.setFont(Font.font("System", 12));
        grid.add(locationLabel, 1, row++);
        
        // Battery
        Label batteryTitle = new Label("Battery:");
        batteryTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        grid.add(batteryTitle, 0, row);
        batteryBar = new ProgressBar(0.0);
        batteryBar.setPrefWidth(150);
        batteryPercentLabel = new Label("0%");
        batteryPercentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        HBox batteryBox = new HBox(8, batteryBar, batteryPercentLabel);
        grid.add(batteryBox, 1, row++);
        
        return grid;
    }

    // --- Bluetooth Monitoring ---
    private void startBluetoothMonitoring() {
        bluetoothService = new AdvancedBluetoothService();
        
        bluetoothService.startScanning(devices -> {
            Platform.runLater(() -> {
                if (devices == null || devices.isEmpty()) {
                    connectionLabel.setText("No devices found");
                    connectionLabel.setTextFill(Color.RED);
                    return;
                }
                
                connectionLabel.setText(devices.size() + " device(s) found");
                connectionLabel.setTextFill(Color.GREEN);
                
                // Get first connected device
                BluetoothDevice device = devices.stream()
                    .filter(BluetoothDevice::isConnected)
                    .findFirst()
                    .orElse(devices.get(0));
                
                if (device != null) {
                    currentDevice = device;
                    updateTelemetryPanel(
                        device.getDeviceName(),
                        device.getLatitude(),
                        device.getLongitude(),
                        device.getBatteryLevel()
                    );
                }
            });
        });
        
        System.out.println("✓ Bluetooth monitoring started (2-second intervals)");
    }

    // --- WebSocket Client Logic ---
    private void connectWebSocket() {
        try {
            wsClient = new WebSocketClient(new URI(SERVER_URI)) {
                @Override 
                public void onOpen(ServerHandshake h) { 
                    updateStatus(true, "Connected"); 
                    System.out.println("✓ WebSocket connected");
                }
                
                @Override 
                public void onClose(int c, String r, boolean re) { 
                    updateStatus(false, "Disconnected");
                    System.out.println("✗ WebSocket disconnected: " + r);
                }
                
                @Override 
                public void onError(Exception ex) { 
                    updateStatus(false, "Error");
                    System.err.println("✗ WebSocket error: " + ex.getMessage());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject data = new JSONObject(message);
                        Platform.runLater(() -> {
                            updateTelemetryPanel(
                                data.getString("deviceId"),
                                data.getDouble("latitude"),
                                data.getDouble("longitude"),
                                data.getInt("batteryPercent")
                            );
                        });
                    } catch (Exception e) {
                        System.err.println("Error parsing WebSocket message: " + e.getMessage());
                    }
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            System.err.println("Failed to connect WebSocket: " + e.getMessage());
        }
    }

    // --- UI Update Methods ---
    private void updateStatus(boolean isConnected, String text) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setTextFill(isConnected ? Color.GREEN : Color.RED);
        });
    }

    // Update both telemetry panel AND map
    private void updateTelemetryPanel(String deviceId, double latitude, double longitude, int battery) {
        // 1. Update text labels
        deviceIdLabel.setText(deviceId);
        
        String latDir = latitude >= 0 ? "N" : "S";
        String lonDir = longitude >= 0 ? "E" : "W";
        locationLabel.setText(String.format("%.6f° %s, %.6f° %s", 
            Math.abs(latitude), latDir, 
            Math.abs(longitude), lonDir));
        
        double batteryLevel = battery / 100.0;
        batteryBar.setProgress(batteryLevel);
        batteryPercentLabel.setText(battery + "%");
        
        // Color code battery
        if (batteryLevel < 0.2) {
            batteryBar.setStyle("-fx-accent: red;");
        } else if (batteryLevel < 0.5) {
            batteryBar.setStyle("-fx-accent: orange;");
        } else {
            batteryBar.setStyle("-fx-accent: green;");
        }
        
        // 2. Update the map (if ready)
        if (isMapReady && (latitude != 0.0 || longitude != 0.0)) {
            try {
                String script = String.format(
                    "updateMarkerWithInfo(%.6f, %.6f, '%s', %d)", 
                    latitude, longitude, 
                    deviceId.replace("'", "\\'"), 
                    battery
                );
                webEngine.executeScript(script);
                System.out.println("Map updated: " + deviceId + " at " + latitude + ", " + longitude);
            } catch (Exception e) {
                System.err.println("Error updating map: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
