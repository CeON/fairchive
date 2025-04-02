package edu.harvard.iq.dataverse.search.response;

import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class SolrSearchLocationResult {
    private final String name;
    private final String doi;
    private final boolean draft;
    private final GeoPoint marker;
    private final Map<String, String> customData;

    // -------------------- CONSTRUCTORS --------------------

    public SolrSearchLocationResult(String name,
                                    String doi,
                                    boolean draft,
                                    List<GeoPoint> coordinates,
                                    Map<String, String> customData) {
        this.name = name;
        this.doi = doi;
        this.draft = draft;
        this.marker = calculateCenter(coordinates);
        this.customData = customData;
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getDoi() {
        return doi;
    }

    public GeoPoint getMarker() {
        return marker;
    }

    public String getDatasetUrl() {
        return "/dataset.xhtml?persistentId=" + doi + (draft ? "&version=DRAFT" : "");
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    // -------------------- PRIVATE --------------------

    // Calculate center from the first two polygon points (representing a rectangle)
    private GeoPoint calculateCenter(List<GeoPoint> geoPoints) {
        if (geoPoints.isEmpty()) {
            throw new IllegalArgumentException("At lest one point is needed.");
        } else if (geoPoints.size() == 1) {
            return  geoPoints.get(0);
        } else if (geoPoints.size() == 2) {
            GeoPoint pointA = geoPoints.get(0);
            GeoPoint pointB = geoPoints.get(1);
            double centerLatitude = (pointA.getLatitude() + pointB.getLatitude()) / 2;
            double centerLongitude = (pointA.getLongitude() + pointB.getLongitude()) / 2;

            return new GeoPoint(centerLatitude, centerLongitude);
        } else {
            Coordinate[] coords = geoPoints.stream()
                    .map(g -> new Coordinate(g.getLongitude(), g.getLatitude()))
                    .toArray(Coordinate[]::new);

            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon polygon = geometryFactory.createPolygon(coords);
            Point center = polygon.getCentroid();
            return new GeoPoint(center.getY(), center.getX());
        }
    }
}
