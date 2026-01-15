package edu.harvard.iq.dataverse.common.files.mime;

public enum MimePrefix {

    AUDIO("audio"),
    CODE("code"),
    DOCUMENT("document"),
    ASTRO("astro"),
    IMAGE("image"),
    NETWORK("network"),
    GEO("geodata"),
    TABULAR("tabular"),
    VIDEO("video"),
    PACKAGE("package"),
    OTHER("other");

    private final String prefix;

    MimePrefix(final String prefix) {
        this.prefix = prefix;
    }

    public String getPrefixValue() {
        return this.prefix;
    }
}
