package edu.harvard.iq.dataverse.search;


public class DatasetLocationSolrFields {
    private static final String NORTH = "northLongitude";
    private static final String SOUTH = "southLongitude";
    private static final String WEST = "westLongitude";
    private static final String EAST = "eastLongitude";

    private final String prefix = "dsf_txt";

    // -------------------- GETTERS --------------------

    public String getNorth() {
        return String.format("%s_%s", this.prefix, NORTH);
    }

    public String getSouth() {
        return String.format("%s_%s", this.prefix, SOUTH);
    }

    public String getWest() {
        return String.format("%s_%s", this.prefix, WEST);
    }

    public String getEast() {
        return String.format("%s_%s", this.prefix, EAST);
    }
}
