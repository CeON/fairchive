package edu.harvard.iq.dataverse.dataaccess;

import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class ImageThumbConverterTest {
    
    private static final String DATASET_STORAGE_ID = "file://10.1010/FK2/ABCD";
    private static final String DATAFILE_STORAGE_ID = "datafilestorageid";

    @TempDir
    Path dir;
    @InjectMocks
    private ImageThumbConverter converter;
    @Mock
    private SystemConfig config;
    
    private final DataFile dataFile = new DataFile();

    @BeforeEach
    void beforeEach() throws IOException {
        System.setProperty(FILES_DIRECTORY, this.dir.toString());
        createDirectories(getDatasetDir());

        final Dataset dataset = new Dataset();
        dataset.setStorageIdentifier(DATASET_STORAGE_ID);

        this.dataFile.setOwner(dataset);
        this.dataFile.setStorageIdentifier(DATAFILE_STORAGE_ID);
    }

    // -------------------- TESTS --------------------

    @Test
    void isThumbnailAvailable() throws IOException {
        // given
        this.dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", getDataFilePath());

        // when
        boolean thumbnailAvailable = this.converter.isThumbnailAvailable(this.dataFile);

        // then
        assertThat(thumbnailAvailable).isTrue();
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb64")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_64.png"));
    }

    @Test
    void isThumbnailAvailable_different_size() throws IOException {
        // given
        this.dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", getDataFilePath());

        // when
        boolean thumbnailAvailable = this.converter.isThumbnailAvailable(this.dataFile, 48);

        // then
        assertThat(thumbnailAvailable).isTrue();
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    @Test
    void isThumbnailAvailable__image_too_big() throws IOException {
        // given
        this.dataFile.setContentType("image/png");
        this.dataFile.setFilesize(543938);
        copyFromClasspath("images/coffeeshop.png", getDataFilePath());
        when(this.config.getThumbnailSizeLimitImage()).thenReturn(100L);

        // when
        boolean thumbnailAvailable = this.converter.isThumbnailAvailable(this.dataFile);

        // then
        assertThat(thumbnailAvailable).isFalse();
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb64")).doesNotExist();
    }

    @Test
    void isThumbnailAvailable__not_supported_content_type() throws IOException {
        // given
        this.dataFile.setContentType("text/plain");
        copyFromClasspath("images/sample.txt", getDataFilePath());

        // when
        boolean thumbnailAvailable = this.converter.isThumbnailAvailable(this.dataFile);

        // then
        assertThat(thumbnailAvailable).isFalse();
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb64")).doesNotExist();
    }

    @Test
    void getImageThumbnailAsInputStream() throws IOException {
        // given
        this.dataFile.setContentType("image/png");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("image.png");
        this.dataFile.setFileMetadatas(Lists.newArrayList(fileMetadata));

        copyFromClasspath("images/coffeeshop.png", getDataFilePath());

        // when
        InputStreamIO thumbnailStreamIO = this.converter.getImageThumbnailAsInputStream(this.dataFile, 48);

        // then
        assertThat(thumbnailStreamIO.getInputStream())
            .hasBinaryContent(UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
        assertThat(thumbnailStreamIO.getFileName()).isEqualTo("image.png");
        assertThat(thumbnailStreamIO.getSize()).isEqualTo(4779);
        assertThat(thumbnailStreamIO.getMimeType()).isEqualTo("image/png");
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    @Test
    void getImageThumbnailAsBase64() throws IOException {
        // given
        this.dataFile.setContentType("image/png");
        copyFromClasspath("images/coffeeshop.png", getDataFilePath());

        // when
        String thumbnailBase64 = this.converter.getImageThumbnailAsBase64(this.dataFile, 48);

        // then
        assertThat(thumbnailBase64)
            .startsWith("data:image/png;base64,")
            .hasSizeGreaterThan("data:image/png;base64,".length());
        assertThat(getDatasetDir().resolve("datafilestorageid.thumb48")).hasBinaryContent(
                UnitTestUtils.readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
    }

    // -------------------- PRIVATE --------------------

    private void copyFromClasspath(final String classpath, final Path target) 
            throws IOException {
        Files.write(target, UnitTestUtils.readFileToByteArray(classpath));
    }
    
    private Path getDatasetDir() {
        return this.dir.resolve("10.1010").resolve("FK2").resolve("ABCD");
    }
    
    private Path getDataFilePath() {
        return getDatasetDir().resolve(DATAFILE_STORAGE_ID);
    }
}
