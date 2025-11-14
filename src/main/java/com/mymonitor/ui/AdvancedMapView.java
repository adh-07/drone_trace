package com.mymonitor.ui;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import javafx.scene.layout.BorderPane;
import com.mymonitor.model.BluetoothDevice;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Advanced Map View with Google Maps JavaScript API
 * Professional-grade interactive mapping with real-time device tracking
 * Updates every 2 seconds for Bluetooth device location
 */
public class AdvancedMapView extends BorderPane {
    private WebView webView;
    private boolean mapInitialized = false;
    private Map<String, DeviceMarker> deviceMarkers = new HashMap<>();
    
    // Map API Configuration - Using Mapbox (free, works immediately)
    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyDemoKey-ReplaceWithYourKey";
    private static final String MAPBOX_TOKEN = "pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw";
    private static final boolean USE_GOOGLE_MAPS = false; // Set to false to use Mapbox (works out of the box)
    
    private static class DeviceMarker {
        double latitude;
        double longitude;
        String name;
        int batteryLevel;
        boolean isConnected;
        
        DeviceMarker(double lat, double lon, String name, int battery, boolean connected) {
            this.latitude = lat;
            this.longitude = lon;
            this.name = name;
            this.batteryLevel = battery;
            this.isConnected = connected;
        }
    }
    
    public AdvancedMapView() {
        webView = new WebView();
        webView.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setCenter(webView);
        setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Enable JavaScript console logging
        webView.getEngine().setJavaScriptEnabled(true);
        
        // Debug: Listen to console messages
        webView.getEngine().setOnError(event -> {
            System.err.println("WebView Error: " + event.getMessage());
        });
        
        loadInitialMap();
    }
    
    /**
     * Update single device location
     */
    public void updateLocation(double latitude, double longitude, String deviceName, int batteryLevel) {
        if (latitude == 0.0 && longitude == 0.0) return;
        
        String deviceId = deviceName != null ? deviceName : "default";
        deviceMarkers.put(deviceId, new DeviceMarker(latitude, longitude, deviceName, batteryLevel, true));
        
        if (mapInitialized) {
            updateDeviceMarker(deviceId, latitude, longitude, deviceName, batteryLevel, true);
        } else {
            loadInitialMap();
        }
    }
    
    /**
     * Update multiple Bluetooth devices
     */
    public void updateBluetoothDevices(List<BluetoothDevice> devices) {
        if (devices == null || devices.isEmpty()) return;
        
        for (BluetoothDevice device : devices) {
            String deviceId = device.getAddress();
            deviceMarkers.put(deviceId, new DeviceMarker(
                device.getLatitude(),
                device.getLongitude(),
                device.getDeviceName(),
                device.getBatteryLevel(),
                device.isConnected()
            ));
        }
        
        if (mapInitialized) {
            updateAllMarkers();
        } else {
            loadInitialMap();
        }
    }
    
    /**
     * Load initial map with Google Maps or Mapbox
     */
    private void loadInitialMap() {
        double centerLat = 40.7128;
        double centerLon = -74.0060;
        int zoom = 13;
        
        // Calculate center from devices
        if (!deviceMarkers.isEmpty()) {
            double sumLat = 0, sumLon = 0;
            int count = 0;
            for (DeviceMarker marker : deviceMarkers.values()) {
                if (marker.latitude != 0.0 || marker.longitude != 0.0) {
                    sumLat += marker.latitude;
                    sumLon += marker.longitude;
                    count++;
                }
            }
            if (count > 0) {
                centerLat = sumLat / count;
                centerLon = sumLon / count;
            }
        }
        
        String html = USE_GOOGLE_MAPS ? 
            generateGoogleMapsHTML(centerLat, centerLon, zoom) : 
            generateMapboxHTML(centerLat, centerLon, zoom);
        
        webView.getEngine().loadContent(html, "text/html");
        
        // Wait for map to load
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                mapInitialized = true;
                Platform.runLater(this::updateAllMarkers);
            }
        });
    }
    
    /**
     * Generate Google Maps JavaScript API HTML
     */
    private String generateGoogleMapsHTML(double centerLat, double centerLon, int zoom) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Google Maps Device Tracking</title>");
        
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("html, body { height: 100%; width: 100%; overflow: hidden; }");
        html.append("#map { height: 100%; width: 100%; }");
        html.append("</style></head><body>");
        html.append("<div id='map'></div>");
        
        html.append("<script>");
        html.append("let map, markers = {};");
        
        html.append("function initMap() {");
        html.append("  map = new google.maps.Map(document.getElementById('map'), {");
        html.append("    center: { lat: " + centerLat + ", lng: " + centerLon + " },");
        html.append("    zoom: " + zoom + ",");
        html.append("    mapTypeId: 'hybrid',");  // Satellite with labels
        html.append("    mapTypeControl: true,");
        html.append("    streetViewControl: true,");
        html.append("    fullscreenControl: true");
        html.append("  });");
        html.append("}");
        
        html.append("function getMarkerIcon(battery, connected) {");
        html.append("  let color = 'blue';");
        html.append("  if (!connected) color = 'grey';");
        html.append("  else if (battery < 20) color = 'red';");
        html.append("  else if (battery < 50) color = 'orange';");
        html.append("  else color = 'green';");
        html.append("  return 'http://maps.google.com/mapfiles/ms/icons/' + color + '-dot.png';");
        html.append("}");
        
        html.append("window.updateDeviceMarker = function(id, lat, lon, name, battery, connected) {");
        html.append("  if (!map) return;");
        html.append("  if (markers[id]) {");
        html.append("    markers[id].setPosition({ lat: lat, lng: lon });");
        html.append("    markers[id].setIcon(getMarkerIcon(battery, connected));");
        html.append("    markers[id].infoContent = '<div style=\"padding:5px;\"><h3 style=\"margin:0 0 5px;\">' + name + '</h3>' +");
        html.append("      '<div style=\"font-size:12px;\">Battery: ' + battery + '%<br>' +");
        html.append("      'Status: ' + (connected ? 'Connected' : 'Disconnected') + '<br>' +");
        html.append("      'Lat: ' + lat.toFixed(6) + '<br>Lon: ' + lon.toFixed(6) + '</div></div>';");
        html.append("  } else {");
        html.append("    const marker = new google.maps.Marker({");
        html.append("      position: { lat: lat, lng: lon },");
        html.append("      map: map,");
        html.append("      title: name,");
        html.append("      icon: getMarkerIcon(battery, connected)");
        html.append("    });");
        html.append("    marker.infoContent = '<div style=\"padding:5px;\"><h3 style=\"margin:0 0 5px;\">' + name + '</h3>' +");
        html.append("      '<div style=\"font-size:12px;\">Battery: ' + battery + '%<br>' +");
        html.append("      'Status: ' + (connected ? 'Connected' : 'Disconnected') + '<br>' +");
        html.append("      'Lat: ' + lat.toFixed(6) + '<br>Lon: ' + lon.toFixed(6) + '</div></div>';");
        html.append("    const infoWindow = new google.maps.InfoWindow();");
        html.append("    marker.addListener('click', function() {");
        html.append("      infoWindow.setContent(marker.infoContent);");
        html.append("      infoWindow.open(map, marker);");
        html.append("    });");
        html.append("    markers[id] = marker;");
        html.append("  }");
        html.append("};");
        
        html.append("window.updateAllMarkers = function(devices) {");
        html.append("  devices.forEach(dev => {");
        html.append("    window.updateDeviceMarker(dev.id, dev.lat, dev.lon, dev.name, dev.battery, dev.connected);");
        html.append("  });");
        html.append("};");
        
        html.append("window.clearMarkers = function() {");
        html.append("  Object.keys(markers).forEach(id => {");
        html.append("    markers[id].setMap(null);");
        html.append("  });");
        html.append("  markers = {};");
        html.append("};");
        
        html.append("</script>");
        
        // Load Google Maps API
        html.append("<script async defer ");
        html.append("src='https://maps.googleapis.com/maps/api/js?key=" + GOOGLE_MAPS_API_KEY + "&callback=initMap'>");
        html.append("</script>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Generate Mapbox GL JS HTML
     */
    private String generateMapboxHTML(double centerLat, double centerLon, int zoom) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Advanced Device Tracking</title>");
        
        // Mapbox GL JS
        html.append("<script src='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js'></script>");
        html.append("<link href='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css' rel='stylesheet' />");
        
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("html, body { height: 100%; width: 100%; overflow: hidden; }");
        html.append("#map { position: absolute; top: 0; bottom: 0; width: 100%; }");
        html.append(".marker { background-size: cover; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }");
        html.append(".marker-green { background-color: #10b981; }");
        html.append(".marker-orange { background-color: #f59e0b; }");
        html.append(".marker-red { background-color: #ef4444; }");
        html.append(".marker-gray { background-color: #6b7280; }");
        html.append(".mapboxgl-popup { max-width: 200px; font-family: Arial, sans-serif; }");
        html.append(".mapboxgl-popup-content { padding: 10px; }");
        html.append("</style></head><body>");
        html.append("<div id='map'></div>");
        
        html.append("<script>");
        html.append("mapboxgl.accessToken = '" + MAPBOX_TOKEN + "';");
        
        html.append("const map = new mapboxgl.Map({");
        html.append("  container: 'map',");
        html.append("  style: 'mapbox://styles/mapbox/satellite-streets-v12',");
        html.append("  center: [" + centerLon + ", " + centerLat + "],");
        html.append("  zoom: " + zoom);
        html.append("});");
        
        html.append("map.addControl(new mapboxgl.NavigationControl());");
        html.append("map.addControl(new mapboxgl.FullscreenControl());");
        
        html.append("console.log('Mapbox map initialized');");
        html.append("map.on('load', function() { console.log('Mapbox map loaded'); });");
        html.append("map.on('error', function(e) { console.error('Mapbox error:', e); });");
        
        html.append("const markers = {};");
        
        html.append("function getMarkerColor(battery, connected) {");
        html.append("  if (!connected) return 'marker-gray';");
        html.append("  if (battery < 20) return 'marker-red';");
        html.append("  if (battery < 50) return 'marker-orange';");
        html.append("  return 'marker-green';");
        html.append("}");
        
        html.append("window.updateDeviceMarker = function(id, lat, lon, name, battery, connected) {");
        html.append("  if (markers[id]) {");
        html.append("    markers[id].marker.setLngLat([lon, lat]);");
        html.append("    markers[id].element.className = 'marker ' + getMarkerColor(battery, connected);");
        html.append("    markers[id].popup.setHTML(");
        html.append("      '<h3 style=\"margin:0 0 5px;font-size:14px;\">' + name + '</h3>' +");
        html.append("      '<div style=\"font-size:12px;color:#666;\">' +");
        html.append("      'Battery: ' + battery + '%<br>' +");
        html.append("      'Status: ' + (connected ? 'Connected' : 'Disconnected') + '<br>' +");
        html.append("      'Lat: ' + lat.toFixed(6) + '<br>' +");
        html.append("      'Lon: ' + lon.toFixed(6) +");
        html.append("      '</div>'");
        html.append("    );");
        html.append("  } else {");
        html.append("    const el = document.createElement('div');");
        html.append("    el.className = 'marker ' + getMarkerColor(battery, connected);");
        html.append("    const popup = new mapboxgl.Popup({ offset: 25 }).setHTML(");
        html.append("      '<h3 style=\"margin:0 0 5px;font-size:14px;\">' + name + '</h3>' +");
        html.append("      '<div style=\"font-size:12px;color:#666;\">' +");
        html.append("      'Battery: ' + battery + '%<br>' +");
        html.append("      'Status: ' + (connected ? 'Connected' : 'Disconnected') + '<br>' +");
        html.append("      'Lat: ' + lat.toFixed(6) + '<br>' +");
        html.append("      'Lon: ' + lon.toFixed(6) +");
        html.append("      '</div>'");
        html.append("    );");
        html.append("    const marker = new mapboxgl.Marker(el)");
        html.append("      .setLngLat([lon, lat])");
        html.append("      .setPopup(popup)");
        html.append("      .addTo(map);");
        html.append("    markers[id] = { marker: marker, element: el, popup: popup };");
        html.append("  }");
        html.append("};");
        
        html.append("window.updateAllMarkers = function(devices) {");
        html.append("  devices.forEach(dev => {");
        html.append("    window.updateDeviceMarker(dev.id, dev.lat, dev.lon, dev.name, dev.battery, dev.connected);");
        html.append("  });");
        html.append("};");
        
        html.append("window.clearMarkers = function() {");
        html.append("  Object.keys(markers).forEach(id => {");
        html.append("    markers[id].marker.remove();");
        html.append("  });");
        html.append("  markers = {};");
        html.append("};");
        
        html.append("</script></body></html>");
        
        return html.toString();
    }
    
    /**
     * Update single device marker
     */
    private void updateDeviceMarker(String deviceId, double lat, double lon, String name, int battery, boolean connected) {
        String escapedName = name.replace("'", "\\'").replace("\"", "\\\"");
        String script = String.format(
            "if (typeof window.updateDeviceMarker === 'function') { " +
            "  window.updateDeviceMarker('%s', %f, %f, '%s', %d, %s); " +
            "}",
            deviceId, lat, lon, escapedName, battery, connected
        );
        
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript(script);
            } catch (Exception e) {
                System.err.println("Marker update error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Update all device markers
     */
    private void updateAllMarkers() {
        if (deviceMarkers.isEmpty()) return;
        
        StringBuilder script = new StringBuilder("if (typeof window.updateAllMarkers === 'function') { window.updateAllMarkers([");
        
        int index = 0;
        for (Map.Entry<String, DeviceMarker> entry : deviceMarkers.entrySet()) {
            if (index > 0) script.append(", ");
            
            DeviceMarker marker = entry.getValue();
            String escapedName = marker.name.replace("'", "\\'").replace("\"", "\\\"");
            
            script.append(String.format(
                "{id: '%s', lat: %f, lon: %f, name: '%s', battery: %d, connected: %s}",
                entry.getKey(), marker.latitude, marker.longitude, 
                escapedName, marker.batteryLevel, marker.isConnected
            ));
            index++;
        }
        
        script.append("]); }");
        
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript(script.toString());
            } catch (Exception e) {
                System.err.println("Markers update error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Clear all markers
     */
    public void clearMarkers() {
        deviceMarkers.clear();
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript("if (typeof window.clearMarkers === 'function') { window.clearMarkers(); }");
            } catch (Exception e) {
                // Ignore
            }
        });
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (webView != null) {
            webView.resize(getWidth(), getHeight());
            Platform.runLater(() -> {
                try {
                    webView.getEngine().executeScript("if (window.map) { window.map.resize(); }");
                } catch (Exception e) {
                    // Ignore
                }
            });
        }
    }
}
