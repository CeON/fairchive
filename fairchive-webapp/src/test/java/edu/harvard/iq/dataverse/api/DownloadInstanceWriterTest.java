package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.UnitTestUtils.copyFileFromClasspath;
import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.file.Files.createDirectories;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Collections;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
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
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.rserve.RemoteDataFrameService;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class DownloadInstanceWriterTest {

    private static final String DATASET_STORAGE_ID = "file://10.1010/FK2/ABCD";
    private static final String DATAFILE_STORAGE_ID = "datafilestorageid";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
        // prepare file system
        System.setProperty(FILES_DIRECTORY, this.dir.toString());
        createDirectories(getDatasetDir());
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

        writeToOutput();

        assertThatOutputStartsWith("PK");
    }

    @Test
    void writingInexistentFile_throwsException() throws Exception {
        // do not prepare file
        assertThrows(NotFoundException.class, () -> writeToOutput(),
                "Datafile 1: Failed to locate and/or open physical file.");
    }
    
    @Test
    void writingTabularFile_withNoVarHeader_works() throws Exception {
        prepareFile("tabular/example.xlsx");
        this.downloadInstance.setConversionParam("noVarHeader");
        this.dataFile.setDataTable(new DataTable());
        
        writeToOutput();

        assertThatOutputStartsWith("PK");
    }
    
    @Test
    void writingTabularInOriginalFormat_works() throws Exception {
        prepareFile("tabular/example.xlsx");
        prepareAuxFile("tabular/example.xlsx", ".orig");
        this.downloadInstance.setConversionParam("format");
        this.downloadInstance.setConversionParamValue("original");
        this.dataFile.setDataTable(new DataTable());
        this.dataFile.getDataTable().setOriginalFileFormat(XLSX);
        
        writeToOutput();

        assertThatOutputStartsWith("PK");
    }
    
    @Test
    void writingTabularInTabFormat_works() throws Exception {
        prepareFile("tabular/example.tab");
        this.downloadInstance.setConversionParam("format");
        this.downloadInstance.setConversionParamValue("tab");
        this.dataFile.setDataTable(new DataTable());
        this.dataFile.setContentType("text/tab-separated-values");
        
        writeToOutput();

        assertThatOutputStartsWith("r1");
    }
    
    @Test
    void writingSubsetTabularFile_works() throws Exception {
        prepareFile("tabular/example.tab");
        this.downloadInstance.setConversionParam("subset");
        this.dataFile.setDataTable(new DataTable());
        this.dataFile.getDataTable().setDataFile(this.dataFile);
        this.downloadInstance.setExtraArguments(
                singletonList(new DataVariable(1, this.dataFile.getDataTable())));
        this.dataFile.getDataTable().setCaseQuantity(10L);
        

        writeToOutput();

        assertThatOutputStartsWith("null\nPADS_ID");
    }

    @Test
    void writingImageThumbnails_works() throws Exception {
        prepareFile("images/coffeeshop.png");
        this.downloadInstance.setConversionParam("imageThumb");
        this.dataFile.setContentType("image/png");

        writeToOutput();

        assertThatOutputIsPNGFile();
    }
    
    @Test
    void writingImageThumbnailsWithWrongSize_throwsException() throws Exception {
        prepareFile("images/coffeeshop.png");
        this.downloadInstance.setConversionParam("imageThumb");
        this.downloadInstance.setConversionParamValue("0");
        this.dataFile.setContentType("image/png");

        assertThrows(WebApplicationException.class, () -> writeToOutput());
        assertThat(this.output.size()).isZero();
    }

    // --------------------------------------------------------------------------
    private void assertThatOutputStartsWith(final String s) {
        assertThat(this.output.toString()).startsWith(s);
    }
    
    private void assertThatOutputIsPNGFile() {
        assertThat(this.output.toByteArray())
                .startsWith(137, 80, 78, 71, 13, 10, 26, 10);
    }

    private void writeToOutput() throws Exception {
        this.writer.writeTo(this.downloadInstance, null, null, null, null,
                this.httpHeaders, this.output);
    }

    private void prepareFile(final String name) throws Exception {
        copyFileFromClasspath(name, getDatasetDir().resolve(DATAFILE_STORAGE_ID));
    }
    
    private void prepareAuxFile(final String name, final String suffix) 
            throws Exception {
        copyFileFromClasspath(name, getDatasetDir().resolve(DATAFILE_STORAGE_ID.concat(suffix)));
    }
    
    private Path getDatasetDir() {
        return this.dir.resolve("10.1010").resolve("FK2").resolve("ABCD");
    }
}
