package edu.harvard.iq.dataverse.dataaccess;

import static edu.harvard.iq.dataverse.UnitTestUtils.copyFileFromClasspath;
import static edu.harvard.iq.dataverse.UnitTestUtils.readFileToByteArray;
import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.file.Files.createDirectories;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        this.dataFile.setContentType("image/png");
        final FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("image.png");
        this.dataFile.setFileMetadatas(singletonList(fileMetadata));
    }

    // -------------------- TESTS --------------------
    @Test
    void isThumbnailAvaileble_throwsNullPointer_whenGivenNull()
            throws Exception {
        assertThrows(NullPointerException.class,
                () -> this.converter.isThumbnailAvailable(null));
    }

    @Test
    void getImageThumbnailAsInputStream_throwsNullPointer_whenGivenNull()
            throws Exception {
        assertThrows(NullPointerException.class,
                () -> this.converter.getImageThumbnailAsInputStream(null, 48));
    }

    @Test
    void isThumbNailAvailable_and_getImageThumbnailAsInputStream_canBeCalledAlternately()
            throws Exception {
        prepareFile("images/coffeeshop.png");
        InputStreamIO streamIO = null;

        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 48)).isTrue();

        streamIO = this.converter.getImageThumbnailAsInputStream(this.dataFile, 48);

        assertThat(streamIO.getInputStream()).hasBinaryContent(
                readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
        assertThat(streamIO.getSize()).isEqualTo(4779);
        assertThat(streamIO.getMimeType()).isEqualTo("image/png");
        assertThat(streamIO.getFileName()).isEqualTo("image.png");

        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 48))
                .startsWith("data:image/png;base64,");

        // calling these methods again returns the same results
        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 48)).isTrue();

        streamIO = this.converter.getImageThumbnailAsInputStream(this.dataFile, 48);

        assertThat(streamIO.getInputStream()).hasBinaryContent(
                readFileToByteArray("images/coffeeshop_thumbnail_48.png"));
        assertThat(streamIO.getSize()).isEqualTo(4779);
        assertThat(streamIO.getMimeType()).isEqualTo("image/png");
        assertThat(streamIO.getFileName()).isEqualTo("image.png");

        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 48))
                .startsWith("data:image/png;base64,");

        // existence of thumbnails of different size does not impact other sizes
        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 64)).isTrue();

        streamIO = this.converter.getImageThumbnailAsInputStream(this.dataFile, 64);

        assertThat(streamIO.getInputStream()).hasBinaryContent(
                readFileToByteArray("images/coffeeshop_thumbnail_64.png"));
        assertThat(streamIO.getSize()).isEqualTo(8307);
        assertThat(streamIO.getMimeType()).isEqualTo("image/png");
        assertThat(streamIO.getFileName()).isEqualTo("image.png");

        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 64))
                .startsWith("data:image/png;base64,");
    }

    @Test
    void getImageThumbnailAsInputStream_canBeCalledBefore_isThumbnailAvailable()
            throws Exception {
        prepareFile("images/coffeeshop.png");
        InputStreamIO streamIO = null;

        streamIO = this.converter.getImageThumbnailAsInputStream(this.dataFile, 64);

        assertThat(streamIO.getInputStream()).hasBinaryContent(
                readFileToByteArray("images/coffeeshop_thumbnail_64.png"));
        assertThat(streamIO.getSize()).isEqualTo(8307);
        assertThat(streamIO.getMimeType()).isEqualTo("image/png");
        assertThat(streamIO.getFileName()).isEqualTo("image.png");

        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 64))
                .startsWith("data:image/png;base64,");

        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 64)).isTrue();
    }

    @Test
    void thumbnails_areNotAvaileble_forTooBigImages()
            throws Exception {
        this.dataFile.setFilesize(543938);
        when(this.config.getThumbnailSizeLimitImage()).thenReturn(100L);
        prepareFile("images/coffeeshop.png");

        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 64)).isFalse();
        assertThat(this.converter.getImageThumbnailAsInputStream(this.dataFile, 64))
                .isNull();
        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 64))
                .isNull();
    }

    @Test
    void thumbnails_areNotAvaileble_forNonImageFiles()
            throws Exception {
        this.dataFile.setContentType("text/json");
        prepareFile("txt/util/jsondata.txt");

        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 64)).isFalse();
        assertThat(this.converter.getImageThumbnailAsInputStream(this.dataFile, 64))
                .isNull();
        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 64))
                .isNull();
    }

    @Test
    void thumbnails_areNotAvaileble_forZeroSizes()
            throws Exception {
        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 0)).isFalse();
        assertThat(this.converter.getImageThumbnailAsInputStream(this.dataFile, 0))
                .isNull();
        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 0))
                .isNull();
    }

    @Test
    void thumbnails_areNotAvaileble_forNegativeSizes() throws Exception {
        assertThat(this.converter.isThumbnailAvailable(this.dataFile, -48)).isFalse();
        assertThat(this.converter.getImageThumbnailAsInputStream(this.dataFile, -64))
                .isNull();
        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, -64))
                .isNull();
    }
    
    @Test
    void thumbnails_areNotAvaileble_whenDisabledInConfig()
            throws Exception {
        prepareFile("images/coffeeshop.png");
        when(this.config.isThumbnailGenerationDisabledForImages()).thenReturn(true);
        
        assertThat(this.converter.isThumbnailAvailable(this.dataFile, 48)).isFalse();
        assertThat(this.converter.getImageThumbnailAsInputStream(this.dataFile, 48))
                .isNull();
        assertThat(this.converter.getImageThumbnailAsBase64(this.dataFile, 48))
                .isNull();
    }

    @Test
    void getImageAsBase64FromFile_throwsNullPointer_whenGivenNull() {
        assertThrows(NullPointerException.class,
                () -> this.converter.getImageAsBase64FromFile(null));
    }

    @Test
    void getImageAsBase64FromFile_returnsNull_whenGivenInexistentFile()
            throws IOException {
        assertThat(this.converter.getImageAsBase64FromFile(new File("inexistent")))
                .isNull();
    }

//--
    @Test
    void getImageAsBase64FromFile_works_whenGivenImageFile()
            throws Exception {
        prepareFile("images/coffeeshop.png");

        assertThat(this.converter.getImageAsBase64FromFile(getFilePath()))
                .startsWith("data:image/png;base64,");
    }

    @Test
    void generateImageThumbnailFromFileAsBase64_throwsNullPointer_whenGivenNull() {
        assertThrows(NullPointerException.class,
                () -> this.converter.generateImageThumbnailFromFileAsBase64(null, 48));
    }

    @Test
    void generateImageThumbnailFromFileAsBase64_returnsNull_whenGivenInexistentFile()
            throws IOException {
        assertThat(this.converter
                .generateImageThumbnailFromFileAsBase64(new File("inexistent"), 48))
                .isNull();
    }

    @Test
    void generateImageThumbnailFromFileAsBase64_returnsNull_whenGivenZeroSize()
            throws IOException {
        assertThat(this.converter.generateImageThumbnailFromFileAsBase64(getFilePath(), 0))
                .isNull();
    }

    @Test
    void generateImageThumbnailFromFileAsBase64_returnsNull_whenGivenNegativeSize()
            throws IOException {
        assertThat(this.converter.generateImageThumbnailFromFileAsBase64(getFilePath(), -1))
                .isNull();
    }

    @Test
    void generateImageThumbnailFromFileAsBase64_works_whenGivenImageFile()
            throws Exception {
        prepareFile("images/coffeeshop.png");

        assertThat(this.converter.generateImageThumbnailFromFileAsBase64(getFilePath(), 48))
                .startsWith("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAA");
    }

    // --------------------------------------------------------------------------
    private Path getDatasetDir() {
        return this.dir.resolve("10.1010").resolve("FK2").resolve("ABCD");
    }

    private void prepareFile(final String name) throws Exception {
        copyFileFromClasspath(name, getDatasetDir().resolve(DATAFILE_STORAGE_ID));
    }

    private File getFilePath() {
        return getDatasetDir().resolve(DATAFILE_STORAGE_ID).toFile();
    }
}
