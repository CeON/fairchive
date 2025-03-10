package edu.harvard.iq.dataverse.export;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.export.ExporterType.DUBLINCORE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DC_FLAVOR_OAI;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DEFAULT_XML_VERSION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.OAI_DC_XML_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.OAI_DC_XML_SCHEMALOCATION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.datasetJson2dublincore;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ApplicationScoped
public class DublinCoreExporter extends ExporterBase {

    // -------------------- CONSTRUCTORS --------------------
    
    @Inject
    public DublinCoreExporter(final SettingsServiceBean settingsService, 
            final CitationFactory citationFactory) {
        super(citationFactory, settingsService);
    }
    
    @Override
    public String exportDataset(final DatasetVersion version) throws ExportException {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            datasetJson2dublincore(createDTO(version), stream, DC_FLAVOR_OAI);
            return stream.toString(UTF_8.name());
        } catch (final XMLStreamException | IOException e) {
            throw new ExportException( "There was a problem with exporting datasetVersion: "
                            + version.toString(), e);
        }
    }

    @Override
    public ExporterType getExporterType() {
        return DUBLINCORE;
    }

    @Override
    public String getDisplayName() {
        return getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore");
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
    }

    @Override
    public Boolean isHarvestable() {
        return true;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return false;
    }

    @Override
    public String getXMLNameSpace() {
        return OAI_DC_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return OAI_DC_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DEFAULT_XML_VERSION;
    }
}
