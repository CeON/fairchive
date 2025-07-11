package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.dataaccess.DataAccess.dataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccessOption.READ_ACCESS;
import static edu.harvard.iq.dataverse.dataaccess.FileAccessIO.DATASET_STORAGE_PREFIX;
import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.arquillian.transaction.api.annotation.TransactionMode.ROLLBACK;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.file.Path;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

@ExtendWith(MockitoExtension.class)
@Transactional(ROLLBACK)
public class OcrServiceIT extends WebappArquillianDeployment {

    private final static String fileName = "example_text.png";
    private final static String fileNameNoText = "example_no_text.jpg";
    private final String executable = "C:\\bin\\Tesseract-OCR\\tesseract.exe stdin stdout";

    private String originalFilesDir;
    @TempDir
    Path dir;
    private OcrService ocrSerivice;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;
    @Inject
    private DataverseSession dataverseSession;
    @Inject
    private DatasetRepository datasets;
    @Inject
    private DataFileRepository datafiles;
    @Inject
    private DataFileCreator fileCreator;
    @Inject
    private IngestServiceBean ingestService;
    @Inject
    private DatasetVersionServiceBean datasetVersionService;
    @Mock
    private SettingsServiceBean settings;

    private Dataset set;

    @BeforeEach
    void setUp() throws Exception {
        this.dataverseSession.logIn(this.authenticationServiceBean.getAdminUser());
        this.set = this.datasets.findById(72L).get();

        this.originalFilesDir = System.getProperty(FILES_DIRECTORY);
        System.setProperty(FILES_DIRECTORY, this.dir.toString());      
        final Path setDir = this.dir.resolve(this.set.getStorageIdentifier()
                .substring(DATASET_STORAGE_PREFIX.length()));
        createDirectories(setDir);
        copyResource("/images/" + fileName, setDir.resolve(fileName));
        copyResource("/images/" + fileNameNoText, setDir.resolve(fileNameNoText));

        this.ocrSerivice = new OcrService(this.fileCreator, this.ingestService,
                this.datasetVersionService, this.datafiles, this.settings);
    }

    @AfterEach
    void tearDown() {
        if (this.originalFilesDir != null) {
            System.setProperty(FILES_DIRECTORY, originalFilesDir);
        }
    }

    @Test
    void orc_forProperConfiguration_producesTextFile() throws Exception {
        // this is temporary so that this tests doesn't break the build in linux
        assumeTrue(isWindows());

        when(this.settings.getValueForKey(eq(Key.OcrCommand))).thenReturn(executable);

        DataFile textFile = this.ocrSerivice.ocr(createImageFileObject(fileName));

        assertThat(textFile.getDisplayName()).isEqualTo(fileName + ".txt");
        assertThat(contentOf(textFile)).startsWith("Actors:");

        assertThat(this.set.getLatestVersion().getFileMetadatas())
                .anyMatch(fm -> fm.getDataFile().getDisplayName()
                        .equals(textFile.getDisplayName()));

        // verify that state has been saved to DB
        Dataset ds = this.datasets.findById(72L).get();
        assertThat(ds.getLatestVersion().getFileMetadatas())
                .anyMatch(fm -> fm.getDataFile().getDisplayName()
                        .equals(textFile.getDisplayName()));

        DataFile savedFile = this.datafiles.getById(textFile.getId());
        assertThat(savedFile.getDisplayName()).isEqualTo(textFile.getDisplayName());
        assertThat(savedFile.getOwner().getId()).isEqualTo(this.set.getId());
    }

    @Test
    void ocr_forMissingConfiguration_doesNothing() throws Exception {
        // this is temporary so that this tests doesn't break the build in linux
        assumeTrue(isWindows());

        when(this.settings.getValueForKey(eq(Key.OcrCommand))).thenReturn("");

        DataFile textFile = this.ocrSerivice.ocr(createImageFileObject(fileName));

        assertThat(textFile).isNull();
    }
    
    @Test
    void ocr_forFileThtContainsNoText_producesEmptyTextFile() throws Exception {
        // this is temporary so that this tests doesn't break the build in linux
        assumeTrue(isWindows());

        when(this.settings.getValueForKey(eq(Key.OcrCommand))).thenReturn(executable);

        DataFile textFile = this.ocrSerivice.ocr(createImageFileObject(fileNameNoText));

        assertThat(textFile.getDisplayName()).isEqualTo(fileNameNoText + ".txt");
        assertThat(contentOf(textFile)).isEmpty();

        assertThat(this.set.getLatestVersion().getFileMetadatas())
                .anyMatch(fm -> fm.getDataFile().getDisplayName()
                        .equals(textFile.getDisplayName()));

        // verify that state has been saved to DB
        Dataset ds = this.datasets.findById(72L).get();
        assertThat(ds.getLatestVersion().getFileMetadatas())
                .anyMatch(fm -> fm.getDataFile().getDisplayName()
                        .equals(textFile.getDisplayName()));

        DataFile savedFile = this.datafiles.getById(textFile.getId());
        assertThat(savedFile.getDisplayName()).isEqualTo(textFile.getDisplayName());
        assertThat(savedFile.getOwner().getId()).isEqualTo(this.set.getId());
    }

    private String contentOf(final DataFile file) throws Exception {
        try (final StorageIO<DataFile> inputStorage = dataAccess()
                .getStorageIO(file)) {
            inputStorage.open(READ_ACCESS);
            return IOUtils.toString(inputStorage.getInputStream(), defaultCharset());
        }
    }

    private void copyResource(final String resourceName, final Path target)
            throws Exception {
        try (final InputStream image = getClass().getResourceAsStream(resourceName)) {
            copy(image, target);
        }
    }

    private DataFile createImageFileObject(final String name) {
        DataFile file = new DataFile();
        FileMetadata meta = new FileMetadata();
        meta.setLabel(name);
        file.setFileMetadatas(asList(meta));
        file.setStorageIdentifier(name);
        file.setOwner(this.set);
        return file;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
