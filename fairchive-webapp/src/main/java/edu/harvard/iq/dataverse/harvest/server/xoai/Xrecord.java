package edu.harvard.iq.dataverse.harvest.server.xoai;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.services.impl.UTCDateProvider;

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
    private static final byte[] HEADER_FIELD = "header".getBytes();
    private static final byte[] STATUS_ATTRIBUTE = "status".getBytes();
    private static final byte[] IDENTIFIER_FIELD = "identifier".getBytes();
    private static final byte[] DATESTAMP_FIELD = "datestamp".getBytes();
    private static final byte[] SETSPEC_FIELD = "setSpec".getBytes();
    private static final String DATAVERSE_EXTENDED_METADATA_FORMAT = "dataverse_json";
    private static final byte[] DATAVERSE_EXTENDED_METADATA_FORMAT_BYTES = "dataverse_json".getBytes();
    private static final byte[] DATAVERSE_EXTENDED_METADATA_API = "/api/datasets/export".getBytes();
    private static final byte[] DIRECT_API_CALL = " directApiCall=\"".getBytes();
    private static final byte[] EXPORTER = "?exporter=".getBytes();
    private static final byte[] PERSISTENT_ID = "&amp;persistentId=".getBytes();
    
    private static final byte LT = '<';
    private static final byte GT = '>';
    private static final byte SLASH = '/';
	private static final byte DOUBLE_QUOTE = '"';
    private static final byte SPACE = ' ';
    private static final byte EQUALS = '=';
    
    private final static UTCDateProvider dateProvider = new UTCDateProvider();

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
                    	final Writer writer = new OutputStreamWriter(out, UTF_8);
                        exportService.export(writer,dataset.getReleasedVersion(),
                                ExporterType.fromPrefix(formatName).get());                       
                        writer.flush();
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
    	
    	out.write(LT);
    	out.write(HEADER_FIELD);
        if (this.header.getStatus() != null) {
        	out.write(SPACE);
        	out.write(STATUS_ATTRIBUTE);
        	out.write(EQUALS);
        	out.write(DOUBLE_QUOTE);
        	out.write(this.header.getStatus().value().getBytes());
        	out.write(DOUBLE_QUOTE);
        }
    	out.write(GT);
    	{
	    	out.write(LT);
	    	out.write(IDENTIFIER_FIELD);
	    	out.write(GT);
	    	out.write(this.header.getIdentifier().getBytes());
	    	out.write(LT);
	    	out.write(SLASH);
	    	out.write(IDENTIFIER_FIELD);
	    	out.write(GT);
    	}
    	{
	    	out.write(LT);
	    	out.write(DATESTAMP_FIELD);
	    	out.write(GT);
	    	out.write(dateProvider.format(this.header.getDatestamp()).getBytes());
	    	out.write(LT);
	    	out.write(SLASH);
	    	out.write(DATESTAMP_FIELD);
	    	out.write(GT);
    	}
    	{
    		for (final String setSpec : this.header.getSetSpecs()) {
    	    	out.write(LT);
    	    	out.write(SETSPEC_FIELD);
    	    	out.write(GT);
    	    	out.write(setSpec.getBytes());
    	    	out.write(LT);
    	    	out.write(SLASH);
    	    	out.write(SETSPEC_FIELD);
    	    	out.write(GT);
    		}
    	}
    	out.write(LT);
    	out.write(SLASH);
    	out.write(HEADER_FIELD);
    	out.write(GT);
    }
    
    private void writeCustomMetadataExtensionRef(final OutputStream out, 
    		final String identifier, final String dataverseUrl) throws IOException {
    	
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
