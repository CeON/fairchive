package edu.harvard.iq.dataverse.datafile;

import static edu.harvard.iq.dataverse.dataaccess.DataAccess.dataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccessOption.READ_ACCESS;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

@Stateless
public class OcrService {

    private final Logger log = getLogger(OcrService.class);
    
    private DataFileCreator fileCreator;
    private IngestServiceBean ingestService;
    private DatasetVersionServiceBean datasetVersionService;
    private DataFileRepository datafiles;
    private SettingsServiceBean settings;

    public OcrService() {
    }

    @Inject
    public OcrService(final DataFileCreator fileCreator,
            final IngestServiceBean ingestService,
            final DatasetVersionServiceBean datasetVersionService,
            final DataFileRepository datafiles,
            SettingsServiceBean settings) {
        this.fileCreator = fileCreator;
        this.ingestService = ingestService;
        this.datasetVersionService = datasetVersionService;
        this.datafiles = datafiles;
        this.settings = settings;
    }

    public void ocr(final DataFile file) throws Exception {

        try (final StorageIO<DataFile> storage = dataAccess()
                .getStorageIO(file)) {
            storage.open(READ_ACCESS);

            final String command = this.settings.getValueForKey(Key.OcrCommand);
            if (isNotBlank(command)) {
                final ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
                final ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
                final ByteArrayOutputStream err = new ByteArrayOutputStream();

                try (final InputStream image = storage.getInputStream()) {
                    final Process process = builder.start();

                    copy(image, process.getOutputStream());
                    process.getOutputStream().close();

                    copy(process.getInputStream(), out);
                    copy(process.getErrorStream(), err);

                    if (process.waitFor() == 0) {
                        try (final OutputStream ocr = storage.openAuxOutput("ocr")) {
                            out.writeTo(ocr);
                        }
                    } else {
                        log.warn(err.toString());
                        throw new RuntimeException(
                                "OCR failed for ".concat(file.getDisplayName()));
                    }
                }
            } 
        }
    }
}
