-- Create the drone_db database
CREATE DATABASE drone_db;

-- Connect to the drone_db database
\c drone_db

-- Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    model VARCHAR(100),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create telemetry_data table
CREATE TABLE IF NOT EXISTS telemetry_data (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(50) REFERENCES devices(device_id),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    battery_level INTEGER,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    status VARCHAR(20)
);

-- Create index for faster queries
CREATE INDEX idx_telemetry_device_time ON telemetry_data(device_id, timestamp);

-- Add some example data
INSERT INTO devices (device_id, name, model, status) VALUES
    ('DRONE001', 'Scout Drone 1', 'DJI Phantom 4', 'ACTIVE'),
    ('DRONE002', 'Survey Drone 1', 'DJI Mavic Air 2', 'ACTIVE'),
    ('DRONE003', 'Delivery Drone 1', 'DJI Matrice 300', 'STANDBY');