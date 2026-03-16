package edu.harvard.iq.dataverse.api;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.OutputStream;

import edu.harvard.iq.dataverse.dataaccess.DataFileZipper;

public class ZipperWrapper {
    private DataFileZipper zipper;
    private String manifest = EMPTY;

    public ZipperWrapper init(OutputStream outputStream) {
        if (this.isEmpty()) {
            zipper = new DataFileZipper(outputStream);
            zipper.setFileManifest(manifest);
        }
        return this;
    }

    public boolean isEmpty() {
        return zipper == null;
    }

    public void addToManifest(String text) {
        if (this.isEmpty()) {
            manifest = manifest + text;
        } else {
            zipper.addToManifest(text);
        }
    }

    public DataFileZipper getZipper() {
        return zipper;
    }
}
