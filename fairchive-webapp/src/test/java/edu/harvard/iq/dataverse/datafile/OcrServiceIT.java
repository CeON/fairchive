package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.dataaccess.DataAccess.dataAccess;
import static edu.harvard.iq.dataverse.dataaccess.FileAccessIO.DATASET_STORAGE_PREFIX;
import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_DIRECTORY;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.arquillian.transaction.api.annotation.TransactionMode.ROLLBACK;

import java.io.InputStream;
import java.nio.file.Path;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
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
    private final static String fileNamePdf = "example_text_images.pdf";

    private String originalFilesDir;
    private String originalOcrCommand;
    @TempDir
    Path dir;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;
    @Inject
    private DataverseSession dataverseSession;
    @Inject
    private DatasetRepository datasets;
    @Inject
    private SettingsServiceBean settings;
    @Inject
    private OcrService ocrSerivice;

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
        copyResource("/images/" + fileNamePdf, setDir.resolve(fileNamePdf));
        
        this.originalOcrCommand = this.settings.getValueForKey(Key.OcrCommand);
        this.settings.setValueForKey(Key.OcrCommand, "tesseract stdin stdout");
    }

    @AfterEach
    void tearDown() {
        if (this.originalFilesDir != null) {
            System.setProperty(FILES_DIRECTORY, originalFilesDir);
        }
        
        this.settings.setValueForKey(Key.OcrCommand, this.originalOcrCommand);
    }

    @Disabled
    @Test
    @Tag("OCR")
    void orc_forProperConfiguration_producesTextFile() throws Exception {
        DataFile image = createImageFileObject(fileName);
        
        this.ocrSerivice.ocr(image);
        
        assertThat(contentOf(image, "ocr")).startsWith("Actors:");
    }
    
    @Disabled
    @Test
    @Tag("OCR")
    void ocr_forMissingConfiguration_doesNothing() throws Exception {
        this.settings.setValueForKey(Key.OcrCommand, "");

        DataFile image = createImageFileObject(fileName);
        
        this.ocrSerivice.ocr(image);
        
        assertThat(isPresent(image, "ocr")).isFalse();
    }
    
    @Disabled  
    @Test
    @Tag("OCR")
    void ocr_forImageFileThatContainsNoText_producesEmptyTextFile() throws Exception {
        DataFile image = createImageFileObject(fileNameNoText);
        
        this.ocrSerivice.ocr(image);

        assertThat(contentOf(image, "ocr")).isEmpty();
    }
    
    @Disabled
    @Test
    @Tag("OCR")
    void orc_forPdf_producesTextFile() throws Exception {
        DataFile pdf = createPdfFileObject(fileNamePdf);
        
        this.ocrSerivice.ocr(pdf);
        
        String text = contentOf(pdf, "ocr");
        assertThat(text).startsWith("Offprint from"); // page 1
        assertThat(text).contains("THE TREPHINED"); // page 2
        assertThat(text).contains("90*"); // page 3
        assertThat(text).contains("91*"); // page 4
        assertThat(text).contains("92*"); // page 5
        assertThat(text).contains("Zias J. & Pomeranz S."); // page 6
    }
    
    private String contentOf(final DataFile file, final String tag) throws Exception {
        try (final InputStream in = dataAccess().getStorageIO(file)
                .getAuxFileAsInputStream(tag)) {
            return IOUtils.toString(in, defaultCharset());
        }
    }
    
    private boolean isPresent(final DataFile file, final String tag) throws Exception {
        return dataAccess().getStorageIO(file).isAuxObjectCached(tag);
    }

    private void copyResource(final String resourceName, final Path target)
            throws Exception {
        try (final InputStream image = getClass().getResourceAsStream(resourceName)) {
            copy(image, target);
        }
    }

    private DataFile createImageFileObject(final String name) {
        DataFile file = createIFileObject(name);
        file.setContentType("image/png");
        return file;
    }
    
    private DataFile createPdfFileObject(final String name) {
        DataFile file = createIFileObject(name);
        file.setContentType("application/pdf");
        return file;
    }
    
    private DataFile createIFileObject(final String name) {
        DataFile file = new DataFile();
        FileMetadata meta = new FileMetadata();
        meta.setLabel(name);
        file.setFileMetadatas(asList(meta));
        file.setStorageIdentifier(name);
        file.setOwner(this.set);
        return file;
    }
}
