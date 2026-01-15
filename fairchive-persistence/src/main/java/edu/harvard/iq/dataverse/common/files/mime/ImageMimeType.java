package edu.harvard.iq.dataverse.common.files.mime;

public enum ImageMimeType implements MimeType {

    FITSIMAGE("image/fits");

    private final String mimeValue;

    ImageMimeType(final String mimeType) {
        this.mimeValue = mimeType;
    }
    @Override
    public String getMimeValue() {
        return this.mimeValue;
    }
}
