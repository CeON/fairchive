package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.search.response.GeoPoint;

import java.util.List;
import java.util.stream.Collectors;

public class PolygonToSolrConverter {
    private static final String POLY_TEMPLATE = "POLYGON((%s))";
    private static final String POINT_TEMPLATE = "POINT(%s)";
    private static final String LINE_TEMPLATE = "LINESTRING(%s)";

    // -------------------- LOGIC --------------------

    /**
     * Converts geo point to Solr shape: POINT, LINESTRING or POLYGON.
     */
    public String toSolrPolygon(List<GeoPoint> points) {
        if (points.isEmpty()) {
            throw new IllegalStateException("Must have at lest one point");
        }

        if (points.size() == 1) {
            return String.format(POINT_TEMPLATE, points.get(0).formatLngLat());
        } else if (points.size() == 2) {
            return String.format(LINE_TEMPLATE, points.stream().map(GeoPoint::formatLngLat).collect(Collectors.joining(",")));
        } else {
            return String.format(POLY_TEMPLATE, points.stream().map(GeoPoint::formatLngLat).collect(Collectors.joining(",")));
        }
    }

}
