package edu.harvard.iq.dataverse.export;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.export.ExporterType.DCTERMS;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DCTERMS_XML_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DCTERMS_XML_SCHEMALOCATION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DC_FLAVOR_DCTERMS;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DEFAULT_XML_VERSION;
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
public class DCTermsExporter extends ExporterBase {

    @Inject
    DCTermsExporter(final SettingsServiceBean settingsService, 
            final CitationFactory citationFactory) {
        super(citationFactory, settingsService);
    }
    
    @Override
    public String exportDataset(final DatasetVersion version) throws ExportException {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            datasetJson2dublincore(createDTO(version), stream, DC_FLAVOR_DCTERMS);
            return stream.toString(UTF_8.name());
        } catch (final XMLStreamException | IOException e) {
            throw new ExportException("There was a problem with exporting datasetVersion: "
                            + version.toString(), e);
        }
    }

    @Override
    public ExporterType getExporterType() {
        return DCTERMS;
    }

    @Override
    public String getDisplayName() {
        final String name = getStringFromBundle("dataset.exportBtn.itemLabel.dublinCore");
        return  name != null ? name: "Dublin Core (DCTERMS)";
    }

    @Override
    public Boolean isXMLFormat() {
        return true;
    }

    @Override
    public Boolean isHarvestable() {
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() {
        return DCTERMS_XML_NAMESPACE;
    }

    @Override
    public String getXMLSchemaLocation() {
        return DCTERMS_XML_SCHEMALOCATION;
    }

    @Override
    public String getXMLSchemaVersion() {
        return DEFAULT_XML_VERSION;
    }
}
