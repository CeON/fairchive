package edu.harvard.iq.dataverse.common.files.mime;

import static java.util.Arrays.stream;

public enum TextMimeType implements MimeType {

    TSV("text/tsv"),
    TSV_ALT("text/tab-separated-values"),
    CSV("text/csv"),
    CSV_ALT("text/comma-separated-values"),
    PLAIN_TEXT("text/plain"),
    FIXED_FIELD("text/x-fixed-field"),
    NETWORK_GRAPHML("text/xml-graphml"),
    STATA_SYNTAX("text/x-stata-syntax"),
    SPSS_CCARD("text/x-spss-syntax"),
    SAS_SYNTAX("text/x-sas-syntax");

    private final static TextMimeType[] ingestable = {CSV, CSV_ALT, TSV, TSV_ALT};
    
    private final String mimeValue;

    TextMimeType(final String mimeType) {
        this.mimeValue = mimeType;
    }

    @Override
    public String getMimeValue() {
        return this.mimeValue;
    }
    
    public static boolean isIngestable(final String mimeType) {
        return stream(ingestable).anyMatch(type -> type.mimeValue.equals(mimeType));
    }
}
