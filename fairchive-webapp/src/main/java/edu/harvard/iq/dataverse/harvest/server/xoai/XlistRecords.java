package edu.harvard.iq.dataverse.harvest.server.xoai;

import static org.dspace.xoai.model.oaipmh.Granularity.Second;
import static org.dspace.xoai.xml.XmlWriter.defaultContext;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.dspace.xoai.model.oaipmh.ListRecords;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.model.oaipmh.ResumptionToken;
import org.dspace.xoai.xml.XmlWriter;

import com.lyncode.xml.exceptions.XmlWriteException;

import edu.harvard.iq.dataverse.export.ExportService;

/**
 * @author Leonid Andreev
 * <p>
 * This is the Dataverse extension of XOAI ListRecords,
 * optimized to stream individual records using fast dumping
 * of pre-exported metadata fragments (and by-passing expensive
 * XML parsing and writing).
 */
public class XlistRecords extends ListRecords {
    private static final byte[] RECORD_START_ELEMENT = "<record>".getBytes();
    private static final byte[] RECORD_CLOSE_ELEMENT = "</record>".getBytes();
    private static final String RESUMPTION_TOKEN_FIELD = "resumptionToken";
    private static final String EXPIRATION_DATE_ATTRIBUTE = "expirationDate";
    private static final String COMPLETE_LIST_SIZE_ATTRIBUTE = "completeListSize";
    private static final String CURSOR_ATTRIBUTE = "cursor";

    public void writeToStream(final OutputStream out, final ExportService exportService,
    		final String dataverseUrl) throws IOException {
        for (final Record record : this.records) {
            out.write(RECORD_START_ELEMENT);

            ((Xrecord) record).writeToStream(out, exportService, dataverseUrl);

            out.write(RECORD_CLOSE_ELEMENT);
        }
        writeResumptionToken(out);
    }

    private void writeResumptionToken(final OutputStream out) 
    		throws IOException {
        try {
        	
        	final ResumptionToken token = this.resumptionToken;
        	if(token != null) {
	            final XmlWriter writer = new XmlWriter(out, defaultContext());
	            writer.writeStartElement(RESUMPTION_TOKEN_FIELD);
	
	            if (token.getExpirationDate() != null) {
	                writer.writeAttribute(EXPIRATION_DATE_ATTRIBUTE, 
	                		token.getExpirationDate(), Second);
	            }
	            if (token.getCompleteListSize() != null) {
	                writer.writeAttribute(COMPLETE_LIST_SIZE_ATTRIBUTE, 
	                		token.getCompleteListSize().toString());
	            }
	            if (token.getCursor() != null) {
	                writer.writeAttribute(CURSOR_ATTRIBUTE, 
	                		token.getCursor().toString());
	            }
	            if (token.getValue() != null) {
	                writer.write(token.getValue());
	            }
	
	            writer.writeEndElement(); // resumptionToken;
	            writer.flush();
	            writer.close();
        	}
        } catch (final XMLStreamException | XmlWriteException e) {
        	throw new IOException("XlistRecords: failed to output resumption token", e);
        }
    }
}
