package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.UnitTestUtils.copyFileFromClasspath;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GzipMaxInputFileSizeInBytes;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GzipMaxOutputFileSizeInBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
public class ArchiveUncompressedSizeCalculatorTest {

    @InjectMocks
    private ArchiveUncompressedSizeCalculator caclulator;

    @Mock
    private SettingsServiceBean settings;

    @TempDir
    Path tempDir;

    // -------------------- TESTS --------------------

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForZipFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.zip");
        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/zip", "archive.zip");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForRarFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.rar");
        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/vnd.rar", "archive.rar");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeFor7zFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.7z");
        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/x-7z-compressed", "archive.7z");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void createDataFiles_shouldComputeUncompressedSizeForGzFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/dummy.pdf.gz");

        when(this.settings.getValueForKeyAsLong(GzipMaxInputFileSizeInBytes))
                .thenReturn(1024 * 1024L);
        when(this.settings.getValueForKeyAsLong(GzipMaxOutputFileSizeInBytes))
                .thenReturn(1024 * 1024L);

        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(13264L);
    }

    @Test
    void createDataFiles_shouldNotComputeUncompressedSizeForGzFileIfOutputFileIsTooBig()
            throws IOException {
        // given
        Path filePath = copyResource("jhove/dummy.pdf.gz");

        when(this.settings.getValueForKeyAsLong(GzipMaxInputFileSizeInBytes))
                .thenReturn(1024 * 1024L);
        when(this.settings.getValueForKeyAsLong(GzipMaxOutputFileSizeInBytes))
                .thenReturn(1L);

        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(0L);
    }

    @Test
    void createDataFiles_shouldNotComputeUncompressedSizeForGzFileIfItIsTooBig()
            throws IOException {
        // given
        Path filePath = copyResource("jhove/dummy.pdf.gz");

        when(this.settings.getValueForKeyAsLong(GzipMaxInputFileSizeInBytes))
                .thenReturn(1L);
        when(this.settings.getValueForKeyAsLong(GzipMaxOutputFileSizeInBytes))
                .thenReturn(1024 * 1024L);

        // when
        Long uncompressedSize = this.caclulator.calculateUncompressedSize(filePath,
                "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(0L);
    }
    
    private Path copyResource(final String name) throws IOException {
        return copyFileFromClasspath(name,this.tempDir.resolve("file"));
    }

}
