package com.mymonitor.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginPage {
    private Stage loginStage;
    private Runnable onLoginSuccess;
    
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    
    public LoginPage(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }
    
    public void show() {
        loginStage = new Stage();
        loginStage.setTitle("Drone Monitoring System - Login");
        loginStage.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a1a1a, #2a2a2a);");
        
        // Title
        Label titleLabel = new Label("Real-Time Drone Monitoring");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0096C9;");
        
        Label subtitleLabel = new Label("Login to continue");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc;");
        
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        
        // Login Form
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10; -fx-padding: 30;");
        
        // Username Field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 14px;");
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            usernameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 14px;");
        });
        
        // Password Field
        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 14px;");
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 14px;");
        });
        
        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        
        // Login Button
        Button loginButton = new Button("Login");
        loginButton.setStyle("""
            -fx-background-color: #0096C9;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 5;
            -fx-padding: 12 40;
            -fx-cursor: hand;
        """);
        
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("""
            -fx-background-color: #007AA3;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 5;
            -fx-padding: 12 40;
            -fx-cursor: hand;
        """));
        
        loginButton.setOnMouseExited(e -> loginButton.setStyle("""
            -fx-background-color: #0096C9;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 5;
            -fx-padding: 12 40;
            -fx-cursor: hand;
        """));
        
        // Login Action
        Runnable performLogin = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter both username and password");
                errorLabel.setVisible(true);
                return;
            }
            
            // Simple authentication (in production, use database)
            if (authenticate(username, password)) {
                errorLabel.setVisible(false);
                loginStage.close();
                if (onLoginSuccess != null) {
                    Platform.runLater(onLoginSuccess);
                }
            } else {
                errorLabel.setText("Invalid username or password");
                errorLabel.setVisible(true);
                passwordField.clear();
            }
        };
        
        loginButton.setOnAction(e -> performLogin.run());
        passwordField.setOnAction(e -> performLogin.run());
        
        // Close Button
        Button closeButton = new Button("✕");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-font-size: 18px; -fx-cursor: hand;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-font-size: 18px; -fx-cursor: hand;"));
        closeButton.setOnAction(e -> {
            loginStage.close();
            Platform.exit();
        });
        
        // Layout
        HBox closeButtonBox = new HBox();
        closeButtonBox.setAlignment(Pos.TOP_RIGHT);
        closeButtonBox.getChildren().add(closeButton);
        HBox.setHgrow(closeButtonBox, Priority.ALWAYS);
        
        formBox.getChildren().addAll(
            usernameLabel,
            usernameField,
            passwordLabel,
            passwordField,
            errorLabel,
            loginButton
        );
        
        root.getChildren().addAll(
            closeButtonBox,
            titleBox,
            formBox
        );
        
        Scene scene = new Scene(root, 450, 500);
        loginStage.setScene(scene);
        loginStage.setResizable(false);
        loginStage.centerOnScreen();
        
        // Focus on username field
        Platform.runLater(() -> usernameField.requestFocus());
        
        loginStage.show();
    }
    
    private boolean authenticate(String username, String password) {
        // Simple authentication (in production, authenticate against database)
        return DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password);
    }
    
    public void close() {
        if (loginStage != null) {
            loginStage.close();
        }
    }
}

