package edu.harvard.iq.dataverse.export;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.dateOfDeposit;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.description;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.descriptionText;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static edu.harvard.iq.dataverse.export.ExporterType.OAI_PMH;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DCTERMS_DEFAULT_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DCTERMS_XML_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DC_XML_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.DEFAULT_XML_VERSION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.OAI_DC_XML_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.OAI_DC_XML_SCHEMALOCATION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.QDC_NAMESPACE;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.QDC_SCHEMALOCATION;
import static edu.harvard.iq.dataverse.export.dublincore.DublinCoreExportUtil.XSI_NAMESPACE;
import static edu.harvard.iq.dataverse.util.FileUtil.getPublicDownloadUrl;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.util.SystemConfig;
/*
 * This exporter will be used to provide metadata from archeological repository 
 * to PBI platform being developed by PSNC, so it is made to fulfill 
 * the requirements of the PBI. 
 * It combines the DC and DCTERMS namespaces. For this reason we cannot 
 * use (or even extend) our standard oai_dc exporter. Moreover, we would like to 
 * preserve the compatibility of oai_dc with other Dataverse installations 
 * (PBI requirements unfortunatelly have some non-obvious quirks).
 */
@ApplicationScoped
public class DCTermsPBIExporter implements Exporter {

    private final SystemConfig config;
    
    @Inject
    public DCTermsPBIExporter(final SystemConfig config) {
        this.config = config;
    }
    
    @Override
    public String exportDataset(final DatasetVersion version) throws ExportException {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            final XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(stream);
            writeDocument(version, xmlw);
            return stream.toString(UTF_8.name());
        } catch (final XMLStreamException | IOException e) {
            throw new ExportException( "There was a problem with exporting datasetVersion: "
                            + version.toString(), e);
        }
    }
    
    
    private void writeDocument(final DatasetVersion version, final XMLStreamWriter xmlw) 
        throws ExportException, XMLStreamException {
        xmlw.writeStartDocument();
        xmlw.writeStartElement("metadata");
        xmlw.writeAttribute("xmlns:xsi", XSI_NAMESPACE);
        xmlw.writeAttribute("xmlns:qdc", QDC_NAMESPACE);
        xmlw.writeAttribute("xmlns:dc", DC_XML_NAMESPACE);
        xmlw.writeAttribute("xmlns:dcterms", DCTERMS_XML_NAMESPACE);
        xmlw.writeDefaultNamespace(DCTERMS_DEFAULT_NAMESPACE);
        xmlw.writeAttribute("xsi:schemaLocation", QDC_NAMESPACE + " " + QDC_SCHEMALOCATION);
        xmlw.writeStartElement("qdc:qualifieddc");
        {
            writeElement(xmlw, "dc:title", version.extractFieldValues(title).get(0));
            String tag = "dc:creator";
            for(final Map<String, DatasetField> creator : version.extractFieldsWithSubfields(author, authorName)) {
                writeElement(xmlw, tag, creator.get(authorName).getValue());
                tag = "dc:contributor";
            }
            writeElement(xmlw, "dc:identifier", version.getDataset().getGlobalId().toURL().toString());
            for(final Map<String, DatasetField> creator : version.extractFieldsWithSubfields(description, descriptionText)) {
                writeElement(xmlw, "dc:description", creator.get(descriptionText).getValue());
            }
            final String siteUrl = this.config.getDataverseSiteUrl();
            for(final FileMetadata fileMetadata : version.getFileMetadatas()) {
                writeElement(xmlw, "dc:relation", getPublicDownloadUrl(siteUrl, 
                        null, fileMetadata.getDataFile().getId()));
            }
            if(version.getFileMetadatas().size() > 0)
            if(version.hasSameTermsOfUseForAllFiles()) {
                writeElement(xmlw, "dcterms:license",  version.getFileMetadatas().get(0).getTermsOfUse().getDisplayText());
            } else {
                writeElement(xmlw, "dcterms:license", "Different licenses for individual files");
            }
            
            writeElement(xmlw, "dcterms:created", version.extractFieldValues(dateOfDeposit).get(0));
            writeElement(xmlw, "dcterms:issued", version.getDataset().getPublicationDateFormattedYYYYMMDD());
        }
        xmlw.writeEndElement(); 
        xmlw.writeEndElement(); // <metadata> or <oai_dc:dc>
        xmlw.flush();
        
    }
    
    private static void writeElement(final XMLStreamWriter xmlw,
            final String name, final String value) throws XMLStreamException {
        xmlw.writeStartElement(name);
        xmlw.writeCharacters(value);
        xmlw.writeEndElement();
    }
    
    @Override
    public ExporterType getExporterType() {
        return OAI_PMH;
    }

    @Override
    public String getDisplayName() {
        return getStringFromBundle("dataset.exportBtn.itemLabel.oai_pmh");
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
