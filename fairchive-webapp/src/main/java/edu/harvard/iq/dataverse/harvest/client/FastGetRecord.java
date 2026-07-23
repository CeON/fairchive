/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.harvest.client;

import static java.lang.Math.min;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
import static javax.xml.stream.XMLStreamConstants.SPACE;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.xml.sax.SAXException;

/*
 * This is an optimized implementation of OAIPMH GetRecord method.
 * Some code is borrowed from the OCLC implementation.
 * It handles the retrieval of the record in a drastically different manner:
 * It parses and validates the top, "administrative" portion of the record using
 * an event-driven parser. Once it reaches the "payload", the actual metadata
 * record enclosed in <metadata>...</metadata> tags, it just reads it line by
 * line without parsing and saves it in a temp file. (The record will be parsed
 * and validated in the next step, when we attempt to import it).
 * On a very large record, for example, a DDI of a Dataset with a large number
 * of associated data variables, even event-driven XML parsing can end up
 * being rather expensive.
 * This optimized version was originally written for DVN 3.*.
 * Added in Dataverse 4: custom protocol extension for sending the metadata
 * record as a pre-declared numbe of bytes.
 * @author Leonid Andreev
 *
 */

public class FastGetRecord implements AutoCloseable {

    private static final String DATAVERSE_EXTENDED_METADATA = "dataverse_json";
    
    private String errorMessage = null;
    private final XMLInputFactory xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
    private boolean recordDeleted = false;
    private final StringBuilder buffer;
    private String content;
    private long timeout = 0;
    private final Logger logger;
    
    private FastGetRecord(final StringBuilder buffer, final Logger logger) {
    	this.buffer = buffer;
    	this.logger = logger;
    }

    private FastGetRecord(final String baseURL, final String identifier, 
    		final String metadataPrefix, final StringBuilder buffer, final Logger logger)
            throws IOException, ParserConfigurationException, SAXException,
            TransformerException {
    	
    	this(buffer, logger);
        harvestRecord(baseURL, identifier, metadataPrefix);
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    
    public String getContent() throws IOException {	
    	return this.content;
    }

    public boolean isDeleted() {
        return this.recordDeleted;
    }


    public void harvestRecord(String baseURL, String identifier, String metadataPrefix) throws IOException,
            ParserConfigurationException, SAXException, TransformerException {

    	this.timeout += 30000;
        final String requestURL = getRequestURL(baseURL, identifier, metadataPrefix);

        int responseCode = 0;

        final HttpURLConnection con = (HttpURLConnection) new URL(requestURL).openConnection();
        con.setRequestProperty("User-Agent", "DataverseHarvester/3.0");
        con.setRequestProperty("Accept-Encoding",
                               "compress, gzip, identify");
        con.setRequestProperty("Accept-Charset", "utf-8");
        
        try {
            responseCode = con.getResponseCode();
        } catch (FileNotFoundException e) {
            responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
        }

        if (responseCode == 200) {
    		final Charset charset = ContentType.parse(con.getHeaderField("Content-Type")).getCharset();
    		
    		try(final BufferedReader rd = new BufferedReader(
    				new InputStreamReader(openInputStream(con), charset))) {
    			parseContent(metadataPrefix, rd);
    		}
        } else if(responseCode == 429) { // too many requests
        	this.logger.info("Too many requests.");
        	pause();
        	harvestRecord(baseURL, identifier, metadataPrefix);
        } else {
            this.errorMessage = "GetRecord request failed. HTTP error code " + responseCode;
        }
    }
    
    private void pause() {
    	try {
    		final Object lock = new Object();
    		synchronized(lock) {
    		this.logger.info("Waiting " + this.timeout + " ms.");
			lock.wait(this.timeout);
    		}
		} catch (final InterruptedException e) {}
    }

	void parseContent(String metadataPrefix, final BufferedReader rd)
			throws IOException, FileNotFoundException, UnsupportedEncodingException {
		
		IOUtils.copy(rd, this.buffer);
		
		final int metadataBlockIndex = this.buffer.indexOf("<metadata");
		if(metadataBlockIndex == -1) {
			this.errorMessage = "<metadata> tag not found.";
			return;
		}
		
		final String metadataEntryPrefix = "<".concat(metadataPrefix);
		final int metadataEntryIndex = this.buffer.indexOf(metadataEntryPrefix, metadataBlockIndex);
		if(metadataEntryIndex == -1) {
			this.errorMessage = "Entry tag with prefix '" + metadataPrefix + 
					"' not found.";
			return;
		}
		
		final String metadataExitPrefix = "</".concat(metadataPrefix);
		int metadataExitIndex = this.buffer.indexOf(metadataExitPrefix, 
				metadataEntryIndex);
		if(metadataExitIndex == -1) {
			this.errorMessage = "Exit tag with prefix '" + metadataPrefix + 
					"' not found.";
			return;
		}
		
		while(this.buffer.charAt(metadataExitIndex) != '>') {
			++metadataExitIndex;
		}
		
		this.content = this.buffer.substring(metadataEntryIndex, metadataExitIndex + 1);
		
		try {
			final XMLStreamReader xmlr = xmlInputFactory.createXMLStreamReader(new StringBuilderReader(this.buffer));
		    processOAIheader(xmlr, metadataPrefix.equals(DATAVERSE_EXTENDED_METADATA));

		} catch (XMLStreamException ex) {
		    if (this.errorMessage == null) {
		        this.errorMessage = "Malformed GetRecord response: " + this.buffer;
		    }
		}
	}

	private InputStream openInputStream(final HttpURLConnection con) 
			throws IOException {
		
		switch(defaultString(con.getHeaderField("Content-Encoding"))) {
		case "compress":
		    final ZipInputStream zip = new ZipInputStream(con.getInputStream());
		    zip.getNextEntry();
		    return zip;
		case "gzip":
		    return new GZIPInputStream(con.getInputStream());
		case "deflate":
		    return new InflaterInputStream(con.getInputStream());
		default:
		    return con.getInputStream();
		}
	}

    /**
     * Construct the query portion of the http request
     * (borrowed from OCLC implementation)
     *
     * @return a String containing the query portion of the http request
     */
    private static String getRequestURL(String baseURL,
                                        String identifier,
                                        String metadataPrefix) {

        StringBuilder requestURL = new StringBuilder(baseURL);
        requestURL.append("?verb=GetRecord");
        requestURL.append("&identifier=").append(identifier);
        requestURL.append("&metadataPrefix=").append(metadataPrefix);

        return requestURL.toString();
    }

    private void processOAIheader(XMLStreamReader xmlr, boolean extensionMode) throws XMLStreamException, IOException {

        // is this really a GetRecord response?
        xmlr.nextTag();
        xmlr.require(START_ELEMENT, null, "OAI-PMH");
        processOAIPMH(xmlr, extensionMode);

    }

    private void processOAIPMH(XMLStreamReader xmlr, boolean extensionMode) 
    		throws XMLStreamException, IOException {

        for (int event = xmlr.next(); event != END_DOCUMENT; event = xmlr.next()) {
            if (event == START_ELEMENT) {
                // TODO:
                // process all the fields currently skipped -- ? -- L.A.
                if (xmlr.getLocalName().equals("responseDate")) {
                } else if (xmlr.getLocalName().equals("request")) {
                } else if (xmlr.getLocalName().equals("error")) {
                    final String errorCode = xmlr.getAttributeValue(null, "code");
                    final String errorMessageText = getElementText(xmlr);

                    if (errorCode != null) {
                        this.errorMessage = "GetRecord error code: " + errorCode + "; ";
                    }

                    if (errorCode != null) {
                        this.errorMessage = this.errorMessage + "GetRecord error message: " 
                        		+ errorMessageText + "; ";
                    }
                    throw new XMLStreamException(this.errorMessage);

                } else if (xmlr.getLocalName().equals("GetRecord")) {
                    processGetRecordSection(xmlr, extensionMode);
                }
            } else if (event == END_ELEMENT) {
                if (xmlr.getLocalName().equals("OAI-PMH")) {
                    return;
                }
            }
        }
    }

    private void processGetRecordSection(XMLStreamReader xmlr, boolean extensionMode) 
    		throws XMLStreamException, IOException {
        for (int event = xmlr.next(); event != END_DOCUMENT; event = xmlr.next()) {
            if (event == START_ELEMENT) {
                if (xmlr.getLocalName().equals("record")) {
                    processRecord(xmlr, extensionMode);
                }
            } else if (event == END_ELEMENT) {
                if (xmlr.getLocalName().equals("GetRecord")) {
                    return;
                }
            }
        }

    }

    private void processRecord(XMLStreamReader xmlr, boolean extensionMode) 
    		throws XMLStreamException, IOException {
        for (int event = xmlr.next(); event != END_DOCUMENT; event = xmlr.next()) {
            if (event == START_ELEMENT) {
                if (xmlr.getLocalName().equals("header")) {
                    if ("deleted".equals(xmlr.getAttributeValue(null, "status"))) {
                        this.recordDeleted = true;
                    }
                    processHeader(xmlr);
                } else if (xmlr.getLocalName().equals("metadata")) {
                    if (extensionMode) {
                        String extendedMetadataApiUrl = xmlr.getAttributeValue(null, "directApiCall");
                        processMetadataExtended(extendedMetadataApiUrl);
                    }
                }
            } else if (event == END_ELEMENT) {
                if (xmlr.getLocalName().equals("record")) {
                    return;
                }
            }
        }
    }

    private void processHeader(XMLStreamReader xmlr) throws XMLStreamException {
        for (int event = xmlr.next(); event != END_DOCUMENT; event = xmlr.next()) {
            if (event == START_ELEMENT) {
                if (xmlr.getLocalName().equals("identifier")) {/*do nothing*/} else if (xmlr.getLocalName().equals("datestamp")) {/*do nothing -- ?*/} else if (xmlr.getLocalName().equals("setSpec")) {/*do nothing*/}


            } else if (event == END_ELEMENT) {
                if (xmlr.getLocalName().equals("header")) {
                    return;
                }
            }
        }
    }

    private void processMetadataExtended(String extendedApiUrl) throws IOException {
        int responseCode = 0;
        HttpURLConnection con = null;


        try {
            URL url = new URL(extendedApiUrl.replaceAll("&amp;", "&")); // is this necessary?

            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "DataverseHarvester/3.0");
            con.setRequestProperty("Accept-Charset", "utf-8");
            responseCode = con.getResponseCode();
        } catch (MalformedURLException mue) {
            throw new IOException("Bad API URL: " + extendedApiUrl);
        } catch (FileNotFoundException e) {
            responseCode = HTTP_UNAVAILABLE;
        }


        if (responseCode == 200) {
        	final Charset charset = ContentType.parse(con.getHeaderField("Content-Type")).getCharset();
            try(final InputStream in = openInputStream(con)) {
            	this.content = IOUtils.toString(in, charset);
            }
        }

        throw new IOException("Failed to download extended metadata.");
    }


    // (from Gustavo's ddiServiceBean -- L.A.)
    //
    /* We had to add this method because the ref getElementText has a bug where it
     * would append a null before the text, if there was an escaped apostrophe; it appears
     * that the code finds an null ENTITY_REFERENCE in this case which seems like a bug;
     * the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
    private String getElementText(XMLStreamReader xmlr) throws XMLStreamException {
        if (xmlr.getEventType() != START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", 
            		xmlr.getLocation());
        }
        int eventType = xmlr.next();
        final StringBuilder content = new StringBuilder();
        while (eventType != END_ELEMENT) {
            if (eventType == CHARACTERS
                    || eventType == CDATA
                    || eventType == SPACE
                /* || eventType == XMLStreamConstants.ENTITY_REFERENCE*/) {
                content.append(xmlr.getText());
            } else if (eventType == PROCESSING_INSTRUCTION
                    || eventType == COMMENT
                    || eventType == ENTITY_REFERENCE) {
                // skipping
            } else if (eventType == END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if (eventType == START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", 
                		xmlr.getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type " + 
                		eventType, xmlr.getLocation());
            }
            eventType = xmlr.next();
        }
        return content.toString();
    }

	@Override
	public void close() throws Exception {
	}
	
	public static Factory newFactory() {
		return new Factory();
	}
	
	public static class Factory {
		
		private final StringBuilder buffer = new StringBuilder(20000);
		
	    public FastGetRecord build(final String baseURL, final String identifier,
	    		final String metadataPrefix, final Logger logger)
	            throws IOException, ParserConfigurationException, SAXException,
	            TransformerException {
	    	
	    	this.buffer.setLength(0);
	    	return new FastGetRecord(baseURL, identifier, metadataPrefix, 
	    			this.buffer, logger);
	    }
	    
	    public FastGetRecord build()
	            throws IOException, ParserConfigurationException, SAXException,
	            TransformerException {
	    	
	    	this.buffer.setLength(0);
	    	return new FastGetRecord(this.buffer, Logger.getLogger(FastGetRecord.class.getName()));
	    }
	}
	
	private static class StringBuilderReader extends Reader {
	    private final StringBuilder builder;
	    private int pos = 0;

	    private StringBuilderReader(final StringBuilder builder) {
	        this.builder = builder;
	    }

	    @Override
	    public int read(final char[] cbuf, final int off, final int len) {
	        if (this.pos >= this.builder.length()) {
	            return -1;
	        }

	        int n = min(len, this.builder.length() - this.pos);
	        this.builder.getChars(this.pos, this.pos + n, cbuf, off);
	        this.pos += n;
	        return n;
	    }

	    @Override
	    public void close() {
	        // nothing to do
	    }
	}
}
