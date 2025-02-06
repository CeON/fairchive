package edu.harvard.iq.dataverse.search.response;

import java.util.List;

public class SolrSearchLocationResult {
    private final String name;
    private final GeoPoint marker;
    private final List<GeoPoint> polygonPoints;

    // -------------------- CONSTRUCTORS --------------------

    public SolrSearchLocationResult(String name, List<GeoPoint> polygonPoints) {
        this.name = name;
        this.polygonPoints = polygonPoints;
        this.marker = calculateCenter(polygonPoints);
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public GeoPoint getMarker() {
        return marker;
    }

    public List<GeoPoint> getPolygonPoints() {
        return polygonPoints;
    }

    // -------------------- PRIVATE --------------------

    // Calculate center from the first two polygon points (representing a rectangle)
    private GeoPoint calculateCenter(List<GeoPoint> polygonPoints) {
        if (polygonPoints.size() != 2) {
            throw new IllegalArgumentException("Polygon must have at least two points to determine a center.");
        }

        GeoPoint p1 = polygonPoints.get(0);
        GeoPoint p2 = polygonPoints.get(1);

        double centerLatitude = (p1.getLatitude() + p2.getLatitude()) / 2;
        double centerLongitude = (p1.getLongitude() + p2.getLongitude()) / 2;

        return new GeoPoint(centerLatitude, centerLongitude);
    }
}
