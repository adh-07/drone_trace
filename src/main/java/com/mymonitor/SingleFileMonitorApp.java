package com.mymonitor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.mymonitor.model.BluetoothDevice;
import com.mymonitor.model.TelemetryData;
import com.mymonitor.service.AdvancedBluetoothService;
import com.mymonitor.dao.TelemetryDAO;
import com.mymonitor.ui.LoginPage;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleFileMonitorApp extends Application {

    // --- UI Components ---
    private Label statusLabel;
    private Label locationLabel;
    private ProgressBar batteryBar;
    private Label batteryPercentLabel;
    private Label deviceIdLabel;
    private Label lastUpdateLabel;
    private Label connectionAttemptsLabel;
    private Button reconnectButton;
    private Button settingsButton;
    private Button showMapButton;
    private TextArea logArea;
    private ScrollPane logScroll;
    private Timeline autoReconnectTimeline;
    private AtomicInteger connectionAttempts = new AtomicInteger(0);
    private AdvancedBluetoothService bluetoothService;
    private BluetoothDevice currentBluetoothDevice;
    private TelemetryDAO telemetryDAO;
    private LoginPage loginPage;
    

    // --- Configuration ---
    private WebSocketClient wsClient;
    private static final String SERVER_URI = "ws://localhost:7070/ws/dashboard";
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // --- CSS Styles ---
    private static final String STYLE_SHEET = """
            .root {
                -fx-background-color: linear-gradient(to bottom right, #0b0f14, #0f1720);
            }
            .label {
                -fx-text-fill: #C9D1D9;
            }
            .title-label {
                -fx-font-size: 26px;
                -fx-font-weight: bold;
                -fx-text-fill: #00E5FF;
                -fx-effect: dropshadow(gaussian, rgba(0,229,255,0.35), 10, 0.3, 0, 0);
                -fx-letter-spacing: 0.5px;
            }
            .status-label {
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #C9D1D9;
            }
            .data-label {
                -fx-font-size: 13px;
                -fx-text-fill: #A6B2BD;
            }
            .button {
                -fx-background-color: linear-gradient(#00BCD4, #0097A7);
                -fx-text-fill: #0b0f14;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-border-radius: 8;
                -fx-border-color: rgba(0,229,255,0.35);
                -fx-border-width: 1;
                -fx-effect: dropshadow(gaussian, rgba(0,229,255,0.25), 12, 0.2, 0, 0);
            }
            .button:hover {
                -fx-background-color: linear-gradient(#00E5FF, #00BCD4);
            }
            .progress-bar {
                -fx-pref-width: 220;
                -fx-accent: #00E5FF;
                -fx-background-color: rgba(255,255,255,0.08);
                -fx-background-radius: 4;
            }
            .progress-bar > .bar {
                -fx-background-insets: 1;
                -fx-background-radius: 3;
            }
            .text-area, .log-area {
                -fx-text-fill: #9FB3C8;
                -fx-control-inner-background: #0e141b;
                -fx-background-insets: 0;
                -fx-background-color: #0e141b;
                -fx-border-color: rgba(0,229,255,0.15);
                -fx-border-width: 1;
                -fx-border-radius: 6;
            }
            .scroll-pane {
                -fx-background-color: transparent;
            }
            .grid-pane {
                -fx-background-color: rgba(255,255,255,0.03);
                -fx-background-radius: 10;
                -fx-padding: 10;
            }
            """;

    @Override
    public void start(Stage primaryStage) {
        // Show login page first
        loginPage = new LoginPage(() -> {
            loginPage.close();
            showMainApplication(primaryStage);
        });
        
        loginPage.show();
    }
    
    private void showMainApplication(Stage primaryStage) {
        // --- Create the Layout and Components ---
        BorderPane mainRoot = new BorderPane();
        
        // Main Data Panel (Full View)
        VBox dataPanel = createDataPanel();
        
        // Set data panel as center (full view)
        mainRoot.setCenter(dataPanel);
        
        // Create Scene and show
        Scene scene = new Scene(mainRoot, 900, 700);
        scene.getStylesheets().add("data:text/css," + STYLE_SHEET.replace("\n", ""));
        
        primaryStage.setTitle("Real-Time Drone Monitoring");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        
        // Start services
        connectWebSocket();
        telemetryDAO = new TelemetryDAO();
        startBluetoothMonitoring();
        log("Application started");
        
        // Cleanup on close
        primaryStage.setOnCloseRequest(e -> {
            if (autoReconnectTimeline != null) {
                autoReconnectTimeline.stop();
            }
            if (wsClient != null) {
                wsClient.close();
            }
            if (bluetoothService != null) {
                bluetoothService.stopScanning();
                bluetoothService.shutdown();
            }
        });
    }
    
    private VBox createDataPanel() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: transparent;");

        // Title and Status Section
        Label titleLabel = new Label("🛰️ Drone Operations Console");
        titleLabel.getStyleClass().add("title-label");

        // Status Box with Reconnect Button
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox statusRow = new HBox(10);
        Label statusTitle = new Label("Status:");
        statusTitle.getStyleClass().add("status-label");
        statusLabel = new Label("Connecting..."); 
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setTextFill(Color.ORANGE);
        statusRow.getChildren().addAll(statusTitle, statusLabel);
        
        connectionAttemptsLabel = new Label("Connection Attempts: 0");
        connectionAttemptsLabel.setStyle("-fx-text-fill: #000000; -fx-font-size: 12px;");
        statusBox.getChildren().addAll(statusRow, connectionAttemptsLabel);

        reconnectButton = new Button("↻ Reconnect");
        reconnectButton.setOnAction(e -> reconnectWebSocket());
        
        settingsButton = new Button("⚙ Settings");
        settingsButton.setOnAction(e -> showSettings());

        controlBox.getChildren().addAll(statusBox, reconnectButton, settingsButton);

        // Main Data Grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));
        grid.setAlignment(Pos.CENTER);

        // Device ID Row
        Label deviceIdTitle = new Label("Device ID:");
        deviceIdTitle.setStyle("-fx-text-fill: #C9D1D9; -fx-font-size: 13px; -fx-font-weight: bold;");
        deviceIdLabel = new Label("N/A");
        deviceIdLabel.setStyle("-fx-text-fill: #A6B2BD; -fx-font-size: 13px;");
        grid.add(deviceIdTitle, 0, 0);
        grid.add(deviceIdLabel, 1, 0);
        
        // Location Row with Show Map Button
        Label locationTitle = new Label("Location:");
        locationTitle.setStyle("-fx-text-fill: #C9D1D9; -fx-font-size: 13px; -fx-font-weight: bold;");
        locationLabel = new Label("N/A");
        locationLabel.setStyle("-fx-text-fill: #A6B2BD; -fx-font-size: 13px;");
        
        HBox locationBox = new HBox(10, locationLabel);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        
        grid.add(locationTitle, 0, 1);
        grid.add(locationBox, 1, 1);
        
        // Battery Row
        Label batteryTitle = new Label("Battery:");
        batteryTitle.setStyle("-fx-text-fill: #C9D1D9; -fx-font-size: 13px; -fx-font-weight: bold;");
        grid.add(batteryTitle, 0, 2);
        
        batteryBar = new ProgressBar(0.0);
        batteryPercentLabel = new Label("0%");
        batteryPercentLabel.setStyle("-fx-text-fill: #A6B2BD; -fx-font-size: 13px;");
        HBox batteryBox = new HBox(5, batteryBar, batteryPercentLabel);
        batteryBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(batteryBox, 1, 2);

        // Last Update Time
        Label lastUpdateTitle = new Label("Last Update:");
        lastUpdateTitle.setStyle("-fx-text-fill: #C9D1D9; -fx-font-size: 13px; -fx-font-weight: bold;");
        lastUpdateLabel = new Label("Never");
        lastUpdateLabel.setStyle("-fx-text-fill: #A6B2BD; -fx-font-size: 13px;");
        grid.add(lastUpdateTitle, 0, 3);
        grid.add(lastUpdateLabel, 1, 3);

        // Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        logArea.getStyleClass().add("log-area");
        logArea.setWrapText(true);
        
        logScroll = new ScrollPane(logArea);
        logScroll.setFitToWidth(true);
        logScroll.setPrefViewportHeight(100);
        
        // Add all components to root
        root.getChildren().addAll(titleLabel, controlBox, grid, logScroll);
        
        return root;
    }

    // --- WebSocket Client Logic ---
    private void connectWebSocket() {
        try {
            if (wsClient != null) {
                wsClient.close();
            }

            connectionAttempts.incrementAndGet();
            updateConnectionAttempts();
            
            wsClient = new WebSocketClient(new URI(SERVER_URI)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Platform.runLater(() -> {
                        connectionAttempts.set(0); // Reset counter on successful connection
                        updateStatus(true, "Connected");
                        log("Connected to WebSocket server");
                        reconnectButton.setDisable(false);
                        if (autoReconnectTimeline != null) {
                            autoReconnectTimeline.stop();
                        }
                    });
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject data = new JSONObject(message);
                        log("Received update from device: " + data.getString("deviceId"));
                        updateTelemetry(
                            data.getString("deviceId"),
                            data.getDouble("latitude"),
                            data.getDouble("longitude"),
                            data.getInt("batteryPercent")
                        );
                    } catch (Exception e) {
                        log("Error parsing message: " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> {
                        updateStatus(false, "Disconnected");
                        log("Disconnected from server: " + reason);
                        reconnectButton.setDisable(false);
                        scheduleReconnect();
                    });
                }

                @Override
                public void onError(Exception ex) {
                    Platform.runLater(() -> {
                        updateStatus(false, "Error: " + ex.getMessage());
                        log("Error: " + ex.getMessage());
                        reconnectButton.setDisable(false);
                        scheduleReconnect();
                    });
                }
            };
            
            reconnectButton.setDisable(true);
            wsClient.connect();
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                updateStatus(false, "Error: " + e.getMessage());
                log("Error creating WebSocket: " + e.getMessage());
                reconnectButton.setDisable(false);
                scheduleReconnect();
            });
        }
    }

    private void scheduleReconnect() {
        if (autoReconnectTimeline != null) {
            autoReconnectTimeline.stop();
        }
        
        if (connectionAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
            autoReconnectTimeline = new Timeline(
                new KeyFrame(Duration.seconds(RECONNECT_DELAY_SECONDS), e -> reconnectWebSocket())
            );
            autoReconnectTimeline.play();
            log("Scheduling reconnect in " + RECONNECT_DELAY_SECONDS + " seconds...");
        } else {
            log("Maximum reconnection attempts reached. Please check server status.");
            updateStatus(false, "Connection failed");
        }
    }

    private void reconnectWebSocket() {
        if (connectionAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
            updateStatus(false, "Reconnecting... (Attempt " + (connectionAttempts.get() + 1) + "/" + MAX_RECONNECT_ATTEMPTS + ")");
            log("Attempting to reconnect...");
            connectWebSocket();
        } else {
            updateStatus(false, "Connection failed");
            reconnectButton.setDisable(false);
        }
    }

    private void showSettings() {
        // Create a settings dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("WebSocket Connection Settings");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField urlField = new TextField(SERVER_URI);
        grid.add(new Label("Server URL:"), 0, 0);
        grid.add(urlField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return urlField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(url -> {
            if (!url.isEmpty() && !url.equals(SERVER_URI)) {
                log("Updating server URL to: " + url);
                // In a real app, you'd want to persist this setting
                reconnectWebSocket();
            }
        });
    }
    
    

    // --- UI Update Methods (Called by WebSocket client) ---
    private void updateStatus(boolean isConnected, String text) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setTextFill(isConnected ? Color.GREEN : Color.RED);
        });
    }

    private void updateConnectionAttempts() {
        Platform.runLater(() -> {
            connectionAttemptsLabel.setText("Connection Attempts: " + connectionAttempts.get());
        });
    }

    private void log(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            logArea.appendText(String.format("[%s] %s%n", timestamp, message));
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void updateTelemetry(String deviceId, double latitude, double longitude, int battery) {
        Platform.runLater(() -> {
            deviceIdLabel.setText(deviceId);
            
            // Update location with compass directions
            String latDir = latitude >= 0 ? "N" : "S";
            String lonDir = longitude >= 0 ? "E" : "W";
            locationLabel.setText(String.format("%.6f° %s, %.6f° %s", 
                Math.abs(latitude), latDir, 
                Math.abs(longitude), lonDir
            ));
            
            // Update battery
            double batteryLevel = battery / 100.0;
            batteryBar.setProgress(batteryLevel);
            batteryPercentLabel.setText(battery + "%");
            
            if (batteryLevel < 0.2) {
                batteryBar.setStyle("-fx-accent: red;");
                log("WARNING: Battery level critical (" + battery + "%)");
            }
            else if (batteryLevel < 0.5) batteryBar.setStyle("-fx-accent: orange;");
            else batteryBar.setStyle("-fx-accent: #0096C9;");
            
            // Update last update time
            lastUpdateLabel.setText(LocalDateTime.now().format(TIME_FORMATTER));
            
            
        });
    }

    // --- Advanced Bluetooth Monitoring ---
    private void startBluetoothMonitoring() {
        bluetoothService = new AdvancedBluetoothService();
        
        // Check if Bluetooth service and radio are enabled
        boolean serviceOn = bluetoothService.isBluetoothEnabled();
        boolean radioOn = bluetoothService.isRadioOn();

        if (!serviceOn || !radioOn) {
            handleBluetoothStatus(false);
            log("WARNING: Bluetooth is disabled or the radio is turned off. Prompting user to enable.");

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Bluetooth Disabled");
            alert.setHeaderText("Bluetooth is turned off");
            alert.setContentText("Please turn on Bluetooth to detect and monitor devices.");
            ButtonType openSettings = new ButtonType("Open Settings");
            ButtonType retry = new ButtonType("Retry");
            alert.getButtonTypes().setAll(openSettings, retry, ButtonType.CLOSE);
            alert.showAndWait().ifPresent(bt -> {
                if (bt == openSettings) {
                    openBluetoothSettings();
                }
            });

            // Poll until Bluetooth is enabled, then start scanning
            Timeline btWait = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    boolean svc = bluetoothService.isBluetoothEnabled();
                    boolean rad = bluetoothService.isRadioOn();
                    if (svc && rad) {
                        ((Timeline) ((KeyFrame) e.getSource()).getParent()).stop();
                        log("Bluetooth enabled. Starting scan...");
                        beginBluetoothScan();
                    }
                })
            );
            btWait.setCycleCount(Timeline.INDEFINITE);
            btWait.play();
            return;
        }

        // Start immediately if enabled
        beginBluetoothScan();
    }

    private void beginBluetoothScan() {
        bluetoothService.startScanning(
            devices -> Platform.runLater(() -> updateBluetoothDevices(devices))
        );
        log("Advanced Bluetooth monitoring started - using native API with 2-second location updates");
    }
    
    private void handleBluetoothStatus(boolean enabled) {
        if (!enabled) {
            // Show alert to turn on Bluetooth
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Bluetooth Not Enabled");
            alert.setHeaderText("Bluetooth is turned off");
            alert.setContentText("Please turn on Bluetooth to detect and monitor devices.");
            alert.showAndWait();
            
            // Set disconnected state
            updateDisconnectedState();
            log("Bluetooth is turned off - Please enable Bluetooth");
        } else {
            log("Bluetooth is enabled");
        }
    }

    private void openBluetoothSettings() {
        try {
            new ProcessBuilder("powershell", "-NoProfile", "-Command", "Start ms-settings:bluetooth").start();
        } catch (Exception ignored) {}
    }
    
    private void updateDisconnectedState() {
        deviceIdLabel.setText("Disconnected");
        locationLabel.setText("N/A");
        batteryBar.setProgress(0.0);
        batteryPercentLabel.setText("0%");
        batteryBar.setStyle("-fx-accent: #666666;");
        updateStatus(false, "Bluetooth Disconnected");
        lastUpdateLabel.setText(LocalDateTime.now().format(TIME_FORMATTER));
    }

    private void updateBluetoothDevices(List<BluetoothDevice> devices) {
        log("Received callback with " + (devices == null ? "null" : devices.size()) + " device(s)");
        
        if (devices == null || devices.isEmpty()) {
            if (currentBluetoothDevice != null) {
                log("Bluetooth device disconnected: " + currentBluetoothDevice.getDeviceName());
                currentBluetoothDevice = null;
            }
            updateDisconnectedState();
            return;
        }
        
        // Log all devices received
        for (BluetoothDevice device : devices) {
            log("Device in callback: " + device.getDeviceName() + " | Battery: " + 
                device.getBatteryLevel() + "% | Connected: " + device.isConnected() + 
                " | Location: " + device.getLatitude() + ", " + device.getLongitude());
        }

        

        // Find the first connected Bluetooth device (prioritize devices with "drone" in name)
        BluetoothDevice selectedDevice = devices.stream()
            .filter(BluetoothDevice::isConnected)
            .filter(d -> d.getDeviceName().toLowerCase().contains("drone") || 
                        d.getDeviceName().toLowerCase().contains("quadcopter") ||
                        d.getDeviceName().toLowerCase().contains("uav"))
            .findFirst()
            .orElse(devices.stream()
                .filter(BluetoothDevice::isConnected)
                .findFirst()
                .orElse(null));
        
        log("Selected device: " + (selectedDevice != null ? selectedDevice.getDeviceName() : "null"));

        if (selectedDevice != null) {
            boolean isNewDevice = currentBluetoothDevice == null || 
                                  !currentBluetoothDevice.getAddress().equals(selectedDevice.getAddress());

            if (isNewDevice) {
                log("Bluetooth device connected: " + selectedDevice.getDeviceName() + 
                    " (" + selectedDevice.getAddress() + ") - Location tracked");
            }

            currentBluetoothDevice = selectedDevice;

            Platform.runLater(() -> {
                log("Updating UI with device: " + selectedDevice.getDeviceName());
                deviceIdLabel.setText(selectedDevice.getDeviceName());
                
                double lat = selectedDevice.getLatitude();
                double lon = selectedDevice.getLongitude();
                String latDir = lat >= 0 ? "N" : "S";
                String lonDir = lon >= 0 ? "E" : "W";
                locationLabel.setText(String.format("%.6f° %s, %.6f° %s", 
                    Math.abs(lat), latDir, 
                    Math.abs(lon), lonDir
                ));
                
                log("Device location: " + String.format("%.6f, %.6f", lat, lon));
                
                int battery = selectedDevice.getBatteryLevel();
                double batteryLevel = battery / 100.0;
                batteryBar.setProgress(batteryLevel);
                batteryPercentLabel.setText(battery + "%");
                
                if (batteryLevel < 0.2) {
                    batteryBar.setStyle("-fx-accent: red;");
                    log("WARNING: Battery level critical (" + battery + "%)");
                } else if (batteryLevel < 0.5) {
                    batteryBar.setStyle("-fx-accent: orange;");
                } else {
                    batteryBar.setStyle("-fx-accent: #0096C9;");
                }
                
                lastUpdateLabel.setText(LocalDateTime.now().format(TIME_FORMATTER));
                updateStatus(true, "Connected via Bluetooth");
                log("UI update completed");
            });

            try {
                TelemetryData telemetry = new TelemetryData(
                    selectedDevice.getDeviceName(),
                    selectedDevice.getLatitude(),
                    selectedDevice.getLongitude(),
                    selectedDevice.getBatteryLevel()
                );
                telemetry.setAltitude(0.0);
                telemetry.setSpeed(0.0);
                telemetry.setTemperature(25.0);
                telemetry.setHumidity(50.0);
                telemetry.setPressure(1013.25);
                telemetry.setHeading(0.0);
                telemetry.setStatus("CONNECTED");
                telemetryDAO.saveTelemetry(telemetry);
            } catch (SQLException e) {
                log("Error saving Bluetooth device data to database: " + e.getMessage());
            }

            if (selectedDevice.getBatteryLevel() > 0) {
                updateStatus(true, "Connected via Bluetooth");
            }
        } else if (currentBluetoothDevice != null) {
            log("Bluetooth device disconnected: " + currentBluetoothDevice.getDeviceName());
            currentBluetoothDevice = null;
        }
    }
    
    // The main method that launches the app
    public static void main(String[] args) {
        launch(args);
    }
}