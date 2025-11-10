package com.mymonitor.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/drone_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Adhil"; // PostgreSQL password
    private static HikariDataSource dataSource;

	static {
		initializeDataSource();
	}

	private static synchronized void initializeDataSource() {
		try {
			if (dataSource != null && !dataSource.isClosed()) {
				return;
			}
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(URL);
			config.setUsername(USER);
			config.setPassword(PASSWORD);
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(2);
			config.setIdleTimeout(300000); // 5 minutes
			config.setConnectionTimeout(20000); // 20 seconds
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			try {
				dataSource = new HikariDataSource(config);
			} catch (RuntimeException ex) {
				if (String.valueOf(ex.getMessage()).contains("does not exist")) {
					createDatabaseIfMissing();
					try {
						dataSource = new HikariDataSource(config);
					} catch (RuntimeException re) {
						dataSource = null;
					}
				} else {
					dataSource = null;
				}
			}
			if (dataSource != null) {
				try (Connection c = dataSource.getConnection()) {
					ensureSchema(c);
				} catch (Exception ignored) {}
			}
		} catch (Exception ignored) {}
	}

    public static Connection getConnection() throws SQLException {
		if (dataSource == null) {
			throw new SQLException("Database is not connected");
		}
		return dataSource.getConnection();
    }

	public static boolean isConnected() {
		try {
			if (dataSource == null || dataSource.isClosed()) {
				return false;
			}
			try (Connection conn = dataSource.getConnection(); Statement s = conn.createStatement()) {
				s.execute("SELECT 1");
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static synchronized void ensureConnected() {
		if (isConnected()) {
			return;
		}
		try {
			if (dataSource != null) {
				dataSource.close();
			}
		} catch (Exception ignored) {}
		initializeDataSource();
	}

	private static void createDatabaseIfMissing() {
		String adminUrl = "jdbc:postgresql://localhost:5432/postgres";
		try (Connection adminConn = DriverManager.getConnection(adminUrl, USER, PASSWORD);
			 Statement stmt = adminConn.createStatement()) {
			stmt.executeUpdate("CREATE DATABASE drone_db");
		} catch (Exception ignored) {
			// If creation fails (permissions, already exists), ignore; caller will handle
		}
	}

	private static void ensureSchema(Connection conn) {
		// Create tables if not exist (portable SQL without psql commands)
		final String createDevices = "CREATE TABLE IF NOT EXISTS devices (\n" +
			"    id SERIAL PRIMARY KEY,\n" +
			"    device_id VARCHAR(50) UNIQUE NOT NULL,\n" +
			"    name VARCHAR(100),\n" +
			"    model VARCHAR(100),\n" +
			"    status VARCHAR(20),\n" +
			"    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
			"    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
			")";

		final String createTelemetry = "CREATE TABLE IF NOT EXISTS telemetry_data (\n" +
			"    id SERIAL PRIMARY KEY,\n" +
			"    device_id VARCHAR(50) REFERENCES devices(device_id),\n" +
			"    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
			"    latitude DOUBLE PRECISION,\n" +
			"    longitude DOUBLE PRECISION,\n" +
			"    altitude DOUBLE PRECISION,\n" +
			"    speed DOUBLE PRECISION,\n" +
			"    battery_level INTEGER,\n" +
			"    temperature DOUBLE PRECISION,\n" +
			"    humidity DOUBLE PRECISION,\n" +
			"    pressure DOUBLE PRECISION,\n" +
			"    heading DOUBLE PRECISION,\n" +
			"    status VARCHAR(20)\n" +
			")";

		final String indexTelemetry = "CREATE INDEX IF NOT EXISTS idx_telemetry_device_time ON telemetry_data(device_id, timestamp)";

		try (Statement s = conn.createStatement()) {
			s.execute(createDevices);
			s.execute(createTelemetry);
			s.execute(indexTelemetry);
		} catch (Exception ignored) {}
	}
}