package edu.harvard.iq.dataverse.search.response;

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
}
