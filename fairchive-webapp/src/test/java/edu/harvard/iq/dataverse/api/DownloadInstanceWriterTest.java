package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.UnitTestUtils.copyFileFromClasspath;
import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.dataaccess.DataConverter;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datafile.page.WholeDatasetDownloadLogger;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.rserve.RemoteDataFrameService;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class DownloadInstanceWriterTest {

    private static final String DATASET_STORAGE_ID = "file://10.1010/FK2/ABCD";
    private static final String DATAFILE_STORAGE_ID = "datafilestorageid";

    @TempDir
    Path dir;
    @Mock
    private SystemConfig sysConfig;
    @Mock
    private WholeDatasetDownloadLogger datasetDownloadLogger;
    @Mock
    private RemoteDataFrameService remoteDataFrameService;
    @InjectMocks
    private ImageThumbConverter thumbConverter;
    @InjectMocks
    private DataConverter dataConverter;
    private final Dataset dataset = new Dataset();
    private final DataFile dataFile = new DataFile();
    private final DownloadInstance downloadInstance = new DownloadInstance();

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final MultivaluedMap<String, Object> httpHeaders = new MultivaluedHashMap<>();

    private DownloadInstanceWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        // create object under test
        this.writer = new DownloadInstanceWriter(this.dataConverter,
                this.datasetDownloadLogger, this.thumbConverter);
        // prepare helper objects
        this.dataset.setStorageIdentifier(DATASET_STORAGE_ID);
        this.dataset.setId(10L);

        this.dataFile.setOwner(this.dataset);
        this.dataFile.setId(1L);
        this.dataFile.setStorageIdentifier(DATAFILE_STORAGE_ID);

        this.downloadInstance.setDownloadInfo(new DownloadInfo(this.dataFile));
    };

    // --------------------------------------------------------------------------
    @Test
    void isWriteable_returnsTrue_onlyForDownloadInstanceClass() {
        assertThat(this.writer.isWriteable(DownloadInstance.class, null, null, null))
                .isTrue();

        assertThat(this.writer.isWriteable(null, null, null, null)).isFalse();
        assertThat(this.writer.isWriteable(String.class, null, null, null)).isFalse();
    }

    @Test
    void writingOriginalFile_works() throws Exception {
        prepareFile("tabular/example.xlsx");
        
        writeTo();
        
        assertThatOutputStartsWith("PK");
    }

    @Test
    void writingInexistentFile_throwsException() throws Exception {
        // do not prepare file
        assertThrows(NotFoundException.class, () -> writeTo(),
                "Datafile 1: Failed to locate and/or open physical file.");
    }

    // --------------------------------------------------------------------------
    private void assertThatOutputStartsWith(final String s) {
        assertThat(this.output.toString()).startsWith(s);
    }

    private void writeTo() throws Exception {
        this.writer.writeTo(this.downloadInstance, null, null, null, null,
                this.httpHeaders, this.output);
    }

    private void prepareFile(final String name) throws Exception {
        System.setProperty(FILES_DIRECTORY, this.dir.toString());
        final Path dir = this.dir.resolve("10.1010").resolve("FK2").resolve("ABCD");
        createDirectories(dir);
        copyFileFromClasspath(name, dir.resolve(DATAFILE_STORAGE_ID));
    }
}
