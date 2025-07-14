package edu.harvard.iq.dataverse.search.response;

import java.util.List;
import java.util.Map;

public class SolrSearchLocationResult {
    private final String name;
    private final String doi;
    private final boolean draft;
    private final GeoPoint marker;
    private final List<GeoPoint> coordinates;
    private final Map<String, String> customData;

    // -------------------- CONSTRUCTORS --------------------

    public SolrSearchLocationResult(String name,
                                    String doi,
                                    boolean draft,
                                    GeoShape shape,
                                    Map<String, String> customData) {
        this.name = name;
        this.doi = doi;
        this.draft = draft;
        this.coordinates = shape.getPoints();
        this.marker = shape.getCenter();
        this.customData = customData;
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return name;
    }

    public String getDoi() {
        return doi;
    }

    public List<GeoPoint> getCoordinates() {
        return coordinates;
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
}
