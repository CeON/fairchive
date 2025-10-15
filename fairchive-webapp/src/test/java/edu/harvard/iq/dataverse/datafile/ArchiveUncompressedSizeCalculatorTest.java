package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.UnitTestUtils.copyFileFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

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
    private ArchiveUncompressedSizeCalculator calculator;

    @Mock
    private SettingsServiceBean settings;

    @TempDir
    Path tempDir;

    // -------------------- TESTS --------------------

    @Test
    void uncompressedSizeForZipFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.zip");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/zip", "archive.zip");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }
    
    @Test
    void uncompressedSizeForZipFile_forBrokenZip_returnsZero() throws IOException {
        // given
        Path filePath = copyResource("jhove/empty");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/zip", "archive.zip");
        // then
        assertThat(uncompressedSize).isZero();
    }

    @Test
    void uncompressedSizeForRarFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.rar");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/vnd.rar", "archive.rar");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }

    @Test
    void uncompressedSizeFor7zFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/archive.7z");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/x-7z-compressed", "archive.7z");
        // then
        assertThat(uncompressedSize).isEqualTo(4L);
    }
    
    @Test
    void uncompressedSizeFor7zFile_forBrokenFile_returnsZero() throws IOException {
        // given
        Path filePath = copyResource("jhove/empty");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/x-7z-compressed", "archive.7z");
        // then
        assertThat(uncompressedSize).isZero();
    }

    @Test
    void uncompressedSizeForGzFile() throws IOException {
        // given
        Path filePath = copyResource("jhove/dummy.pdf.gz");

        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/gzip", "dummy.pdf.gz");
        // then
        assertThat(uncompressedSize).isEqualTo(13264L);
    }
    
    @Test
    void uncompressedSizeForNonArchiveFile_returnsZero() throws IOException {
        // given
        Path filePath = copyResource("jhove/dummy.pdf");
        // when
        Long uncompressedSize = this.calculator.calculateUncompressedSize(filePath,
                "application/pdf", "dummy.pdf");
        // then
        assertThat(uncompressedSize).isZero();
    }
    
    private Path copyResource(final String name) throws IOException {
        return copyFileFromClasspath(name,this.tempDir.resolve("file"));
    }

}
