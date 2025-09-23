package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.RarDataLineBeforeResultDelimiter;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.RarDataUtilCommand;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.RarDataUtilOpts;
import static java.nio.file.Files.newInputStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;

import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ExternalRarDataUtil;

@ApplicationScoped
public class ArchiveUncompressedSizeCalculator {

    private static final Logger logger = getLogger(
            ArchiveUncompressedSizeCalculator.class);

    private SettingsServiceBean settings;

    // -------------------- CONSTRUCTORS --------------------

    public ArchiveUncompressedSizeCalculator() {
    }

    @Inject
    public ArchiveUncompressedSizeCalculator(final SettingsServiceBean settings) {
        this.settings = settings;
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns summary size of files that are inside of an archive. Returns 0 if
     * file is not an archive or there was some problems in calculating.
     */
    public long calculateUncompressedSize(final Path path, final String mimeType, 
            final String fileName) {
        try {
            switch (mimeType) {
            case "application/zip":
                return uncompressedSizeForZip(path);
            case "application/vnd.rar":
                return uncompressedSizeForRar(path, fileName);
            case "application/x-7z-compressed":
                return uncompressedSizeFor7Zip(path, fileName);
            case "application/gzip":
            case "application/x-compressed-tar":
                return uncompressecSizeForGzip(path, fileName);
            default:
                return 0L;
            }
        } catch (final Exception e) {
            logger.warn("Exception while trying to uncompress file: ".concat(fileName),
                    e);
            return 0;
        }
    }

    private long uncompressecSizeForGzip(final Path path, final String fileName)
            throws IOException {
        try (final GZIPInputStream gzip = new GZIPInputStream(newInputStream(path))) {
            long size = 0;
            while (gzip.read() != -1) {
                ++size;
            }
            return size;
        }
    }

    private long uncompressedSizeFor7Zip(final Path path, final String fileName) 
            throws IOException {
        try (final SevenZFile archive = SevenZFile.builder().setPath(path)
                .get()) {
            long size = 0L;
            SevenZArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    size += entry.getSize();
                }
            }
            return size;
        }
    }

    private long uncompressedSizeForRar(final Path path, final String fileName) 
            throws RarException, IOException {
        try {
            return Junrar.getContentsDescription(path.toFile())
                    .stream()
                    .mapToLong(d -> d.size)
                    .sum();
        } catch (final UnsupportedRarV5Exception r5e) {
            return new ExternalRarDataUtil(
                    this.settings.getValueForKey(RarDataUtilCommand),
                    this.settings.getValueForKey(RarDataUtilOpts),
                    this.settings.getValueForKey(RarDataLineBeforeResultDelimiter))
                    .checkRarExternally(path, fileName);
        }
    }

    private long uncompressedSizeForZip(final Path path) throws IOException {
        try (final ZipFile zip = ZipFile.builder().setPath(path).get()) {
            long size = 0;
            final Enumeration<ZipArchiveEntry> zipEntries = zip.getEntries();
            while (zipEntries.hasMoreElements()) {
                final ZipArchiveEntry zipEntry = zipEntries.nextElement();
                if (!zipEntry.isDirectory()) {
                    size += zipEntry.getSize();
                }
            }
            return size;
        }
    }
}
