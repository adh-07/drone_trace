package com.mymonitor.util;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing database connection...");
            var connection = DatabaseConnection.getConnection();
            System.out.println("Successfully connected to PostgreSQL database!");
            System.out.println("Connection details:");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Schema: " + connection.getSchema());
            System.out.println("URL: " + connection.getMetaData().getURL());
            connection.close();
            System.out.println("Connection closed successfully.");
        } catch (Exception e) {
            System.err.println("Failed to connect to the database!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}