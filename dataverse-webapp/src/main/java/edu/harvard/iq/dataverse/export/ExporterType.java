package edu.harvard.iq.dataverse.export;

import static java.util.Arrays.stream;

import java.util.Optional;

public enum ExporterType {
    DDI("DDI"),
    DATACITE("DATACITE"),
    DCTERMS("DCTERMS"),
    DUBLINCORE("oai_dc"),
    JSON("dataverse_json"),
    OAIDDI("oai_ddi"),
    OAIORE("OAIORE"),
    SCHEMADOTORG("SCHEMADOTORG"),
    OPENAIRE("oai_datacite"),
    DDI_HTML("ddi_html"),
    DCTERMS_PBI("DCTERMS_PBI");

    private final String prefix;

    // -------------------- CONSTRUCTORS --------------------

    ExporterType(final String prefix) {
        this.prefix = prefix;
    }
    // -------------------- GETTERS --------------------

    public String getPrefix() {
        return this.prefix;
    }

    // -------------------- LOGIC --------------------

    /**
     * @return ExporterConstant if present or Optional.empty if the enum with given string doesnt exist.
     */
    public static Optional<ExporterType> fromPrefix(final String prefix) {
        return stream(values())
                .filter(v -> v.getPrefix().equals(prefix))
                .findFirst();
    }
}
