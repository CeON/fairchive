package edu.harvard.iq.dataverse.search.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GeoPoint {
    private final double latitude;
    private final double longitude;

    // -------------------- CONSTRUCTORS --------------------

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GeoPoint(String latitude, String longitude) {
        this.latitude = Double.parseDouble(latitude);
        this.longitude = Double.parseDouble(longitude);
    }

    // -------------------- GETTERS --------------------

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // -------------------- LOGIC --------------------

    public String formatLngLat() {
        return String.format("%.6f %.6f", longitude, latitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPoint geoPoint = (GeoPoint) o;
        return Double.compare(geoPoint.latitude, latitude) == 0 &&
                Double.compare(geoPoint.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "GeoPoint{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public static List<GeoPoint> fromCoordinateString(String coords) {
        List<GeoPoint> points = new ArrayList<>();
        if (coords == null || coords.trim().isEmpty()) {
            return points;
        }

        // Normalize line breaks, trim, and split by whitespace
        String[] geoValues = coords.trim().replace("\n", " ").replace("\r", " ").split("\\s+");

        if (geoValues.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid coordinate string, must be even number of values.");
        }

        for (int i = 0; i < geoValues.length; i += 2) {
            double lon = Double.parseDouble(geoValues[i]);
            double lat = Double.parseDouble(geoValues[i + 1]);
            points.add(new GeoPoint(lat, lon));
        }

        return points;
    }
}
