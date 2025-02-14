package edu.harvard.iq.dataverse.search.response;

import java.util.List;

public class SolrSearchLocationResult {
    private final String name;
    private final GeoPoint marker;

    // -------------------- CONSTRUCTORS --------------------

    public SolrSearchLocationResult(String name, GeoPoint pointA, GeoPoint pointB) {
        this.name = name;
        this.marker = calculateCenter(pointA, pointB);
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public GeoPoint getMarker() {
        return marker;
    }

    // -------------------- PRIVATE --------------------

    // Calculate center from the first two polygon points (representing a rectangle)
    private GeoPoint calculateCenter(GeoPoint pointA, GeoPoint pointB) {
        if (pointA == null || pointB == null) {
            throw new IllegalArgumentException("To determine a center we need two points.");
        }

        double centerLatitude = (pointA.getLatitude() + pointB.getLatitude()) / 2;
        double centerLongitude = (pointA.getLongitude() + pointB.getLongitude()) / 2;

        return new GeoPoint(centerLatitude, centerLongitude);
    }
}
