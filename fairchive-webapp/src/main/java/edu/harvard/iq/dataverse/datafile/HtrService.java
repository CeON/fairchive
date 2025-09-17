package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static edu.harvard.iq.dataverse.dataaccess.DataAccess.dataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccessOption.READ_ACCESS;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.pdfbox.rendering.ImageType.RGB;
import static org.apache.pdfbox.tools.imageio.ImageIOUtil.writeImage;
import static org.slf4j.LoggerFactory.getLogger;

@Stateless
public class HtrService {

    private static final Logger log = getLogger(HtrService.class);

    private SettingsServiceBean settings;

    public HtrService() {
    }

    @Inject
    public HtrService(final SettingsServiceBean settings) {
        this.settings = settings;
    }

    public void htr(final DataFile file) throws Exception {
        try (final StorageIO<DataFile> storage = dataAccess()
                .getStorageIO(file)) {
            storage.open(READ_ACCESS);

            final String command = this.settings.getValueForKey(Key.HtrCommand);
            if (isNotBlank(command)) {
                final ProcessBuilder builder = new ProcessBuilder(
                        command.split("\\s"));
                final ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
                final ByteArrayOutputStream err = new ByteArrayOutputStream();

                try (final InputStream image = storage.getInputStream()) {
                    final Process process = builder.start();

                    copy(image, process.getOutputStream());
                    process.getOutputStream().close();

                    copy(process.getInputStream(), out);
                    copy(process.getErrorStream(), err);

                    if (process.waitFor() == 0) {
                        try (final OutputStream ocr = storage.openAuxOutput("htr")) {
                            out.writeTo(ocr);
                        }
                    } else {
                        log.warn(err.toString());
                        throw new RuntimeException(
                                "HTR failed for ".concat(file.getDisplayName()));
                    }
                }
            }
        }
    }
}
