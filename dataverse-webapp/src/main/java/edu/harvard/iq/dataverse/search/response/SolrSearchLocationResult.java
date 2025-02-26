package edu.harvard.iq.dataverse.search.response;

import java.util.Map;

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
                                    GeoPoint pointA,
                                    GeoPoint pointB,
                                    Map<String, String> customData) {
        this.name = name;
        this.doi = doi;
        this.draft = draft;
        this.marker = calculateCenter(pointA, pointB);
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
    private GeoPoint calculateCenter(GeoPoint pointA, GeoPoint pointB) {
        if (pointA == null || pointB == null) {
            throw new IllegalArgumentException("To determine a center we need two points.");
        }

        double centerLatitude = (pointA.getLatitude() + pointB.getLatitude()) / 2;
        double centerLongitude = (pointA.getLongitude() + pointB.getLongitude()) / 2;

        return new GeoPoint(centerLatitude, centerLongitude);
    }
}
