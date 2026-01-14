package edu.harvard.iq.dataverse.util;


import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.IOUtils.copy;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Convenience class to create a zip file, used by ShapefileHandler
 */
public final class ZipFileBuilder implements Closeable {

    private final ZipOutputStream zipStream;

    // -------------------- CONSTRUCTOR --------------------

    public ZipFileBuilder(final Path outputZipFilename) throws IOException {
        this.zipStream = new ZipOutputStream(newOutputStream(outputZipFilename));
    }

    // -------------------- LOGIC --------------------

    public void addToZipFile(final Path filePath) throws IOException {
        try(final InputStream in = newInputStream(filePath)) {
            this.zipStream.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
            copy(in, this.zipStream);
            this.zipStream.closeEntry();
        }
    }

    @Override
    public void close() throws IOException {
       this.zipStream.close();
    }
}
