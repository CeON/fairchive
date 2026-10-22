package edu.harvard.iq.dataverse.harvest.server.xoai;

import static org.dspace.xoai.xml.XmlWriter.defaultContext;

import java.io.IOException;
import java.io.OutputStream;

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.xml.XmlWriter;

import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ExporterType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

/**
 * @author Leonid Andreev
 * <p>
 * This is the Dataverse extension of XOAI Record,
 * optimized to directly output a pre-exported metadata record to the
 * output stream, thus by-passing expensive parsing and writing by
 * an XML writer, as in the original XOAI implementation.
 */

public class Xrecord extends Record {
    private static final byte[] METADATA_FIELD = "metadata".getBytes();
    private static final byte[] METADATA_START_ELEMENT = "<metadata>".getBytes();
    private static final byte[] METADATA_END_ELEMENT = "</metadata>".getBytes();
    private static final String HEADER_FIELD = "header";
    private static final String STATUS_ATTRIBUTE = "status";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String DATESTAMP_FIELD = "datestamp";
    private static final String SETSPEC_FIELD = "setSpec";
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final byte[] DATAVERSE_EXTENDED_METADATA_FORMAT_BYTES = "dataverse_json".getBytes();
    private static final byte[] DATAVERSE_EXTENDED_METADATA_API = "/api/datasets/export".getBytes();
    private static final byte[] DIRECT_API_CALL = " directApiCall=\"".getBytes();
    private static final byte[] EXPORTER = "?exporter=".getBytes();
    private static final byte[] PERSISTENT_ID = "&amp;persistentId=".getBytes();
    
    private static final byte LT = '<';
    private static final byte GT = '>';
    private static final byte SLASH = '/';

    protected Dataset dataset;
    protected String formatName;


    public Dataset getDataset() {
        return dataset;
    }

    public Xrecord withDataset(Dataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public String getFormatName() {
        return formatName;
    }

    public Xrecord withFormatName(String formatName) {
        this.formatName = formatName;
        return this;
    }

    public void writeToStream(final OutputStream out, final ExportService exportService, 
    		final String dataverseUrl) throws IOException {
        writeItemHeader(out);

        // header.getStatus() is only non-null when it's indicating "deleted".
        if (header.getStatus() == null) { // Deleted records should not show metadata
            if (!isExtendedDataverseMetadataMode(formatName)) {
                out.write(METADATA_START_ELEMENT);

                if (dataset != null && formatName != null) {
                    try {
                        final String exportedDataset = exportService.exportToString(
                                dataset.getReleasedVersion(),
                                ExporterType.fromPrefix(formatName).get());
                        out.write(exportedDataset.getBytes());
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                out.write(METADATA_END_ELEMENT);
            } else {
            	writeCustomMetadataExtensionRef(out, this.dataset.getGlobalId().toString(), 
            			dataverseUrl);
            }
        }
        out.flush();
    }

    private void writeItemHeader(final OutputStream out) throws IOException {
        try {
            final XmlWriter writer = new XmlWriter(out, defaultContext());

            writer.writeStartElement(HEADER_FIELD);

            if (this.header.getStatus() != null) {
                writer.writeAttribute(STATUS_ATTRIBUTE, this.header.getStatus().value());
            }
            writer.writeElement(IDENTIFIER_FIELD, this.header.getIdentifier());
            writer.writeElement(DATESTAMP_FIELD, this.header.getDatestamp());
            for (final String setSpec : this.header.getSetSpecs()) {
                writer.writeElement(SETSPEC_FIELD, setSpec);
            }
            writer.writeEndElement(); // header
            writer.flush();
            writer.close();

        } catch (final Exception e) {
        	throw new IOException("Xrecord: failed to stream item header.", e);
        }
    }
    
    private void writeCustomMetadataExtensionRef(final OutputStream out, 
    		final String identifier, final String dataverseUrl) throws IOException {
    	
    	final byte DOUBLE_QUOTE = '"';
    	
    	 out.write(LT);
    	 out.write(METADATA_FIELD);
    	 out.write(DIRECT_API_CALL);
    	 out.write(dataverseUrl.getBytes());
    	 out.write(DATAVERSE_EXTENDED_METADATA_API);
    	 out.write(EXPORTER);
    	 out.write(DATAVERSE_EXTENDED_METADATA_FORMAT_BYTES);
    	 out.write(PERSISTENT_ID);
    	 out.write(identifier.getBytes());
    	 out.write(DOUBLE_QUOTE);
    	 out.write(SLASH);
    	 out.write(GT);
    }

    private boolean isExtendedDataverseMetadataMode(String formatName) {
        return DATAVERSE_EXTENDED_METADATA_FORMAT.equals(formatName);
    }
}
