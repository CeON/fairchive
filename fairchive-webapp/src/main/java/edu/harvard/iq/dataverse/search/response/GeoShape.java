package edu.harvard.iq.dataverse.search.response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Objects;

public class GeoShape {
    private final List<GeoPoint> points;
    private final GeoPoint center;

    // -------------------- CONSTRUCTORS --------------------

    public GeoShape(List<GeoPoint> points) {
        this.points = points;
        this.center = calculateCenter(points);
    }

    public static GeoShape of(List<GeoPoint> points) {
        return new GeoShape(points);
    }


    // -------------------- GETTERS --------------------

    public List<GeoPoint> getPoints() {
        return points;
    }

    public GeoPoint getCenter() {
        return center;
    }

    // -------------------- LOGIC --------------------

    public GeoPoint calculateCenter(List<GeoPoint> geoPoints) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoShape geoShape = (GeoShape) o;
        return Objects.equals(points, geoShape.points) &&
                Objects.equals(center, geoShape.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points, center);
    }

    @Override
    public String toString() {
        return "GeoShape{" +
                "points=" + points +
                ", center=" + center +
                '}';
    }
}
