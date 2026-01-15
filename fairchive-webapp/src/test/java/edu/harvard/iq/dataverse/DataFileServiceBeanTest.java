package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test that the DataFileServiceBean classifies DataFiles correctly.
 *
 * @author bencomp
 */
public class DataFileServiceBeanTest {

    public DataFileServiceBeanTest() {
    }

    /**
     * A DataFile without content type.
     */
    private DataFile fileWoContentType = null;
    /**
     * A DataFile with bogus content type "foo/bar".
     */
    private DataFile fileWithBogusContentType = null;
    /**
     * The Bean Under Test.
     */
    private DataFileServiceBean dataFileServiceBean;


    @BeforeEach
    public void setUp() {
        fileWoContentType = createDataFile(null);
        fileWithBogusContentType = createDataFile("foo/bar");
        dataFileServiceBean = new DataFileServiceBean();
    }
    
    /**
     * Expect that files without content type or with a bogus content type are
     * classed as "other". Note that the file classes are not coded as constants!
     *
     * @throws Exception when the test is in error.
     */
    @Test
    public void testGetFileClass() throws Exception {
    	assertEquals("image", getFileClass("image/png"));
    	
    	assertEquals("video", getFileClass("video/mp4"));	
    	
    	assertEquals("audio", getFileClass("audio/wav"));
    	
    	assertEquals("code", getFileClass("application/x-r-syntax"));
    	assertEquals("code", getFileClass("text/x-stata-syntax"));
    	assertEquals("code", getFileClass("text/x-sas-syntax"));
    	assertEquals("code", getFileClass("text/x-spss-syntax"));
    	
    	assertEquals("document", getFileClass("text/plain"));
    	assertEquals("document", getFileClass("application/pdf"));
    	assertEquals("document", getFileClass("application/msword"));
    	assertEquals("document", getFileClass("application/vnd.ms-excel"));
    	assertEquals("document", getFileClass("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    	assertEquals("document", getFileClass("text/plain;something"));
    	
    	assertEquals("astro", getFileClass("application/fits"));
    	assertEquals("astro", getFileClass("image/fits"));
    	
    	assertEquals("network", getFileClass("text/xml-graphml"));
    	
    	assertEquals("geodata", getFileClass("application/zipped-shapefile"));
    	
    	assertEquals("tabular", getFileClass("text/tsv"));
    	assertEquals("tabular", getFileClass("text/x-fixed-field"));
    	assertEquals("tabular", getFileClass("application/x-sas-transport"));
    	assertEquals("tabular", getFileClass("application/x-sas-system"));
    	
    	assertEquals("tabular", getFileClass("application/x-stata"));
    	assertEquals("tabular", getFileClass("application/x-stata-13"));
    	assertEquals("tabular", getFileClass("application/x-stata-14"));
    	assertEquals("tabular", getFileClass("application/x-stata-15"));
    	assertEquals("tabular", getFileClass("application/x-rlang-transport"));
    	assertEquals("tabular", getFileClass("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    	assertEquals("tabular", getFileClass("application/x-spss-sav"));
    	assertEquals("tabular", getFileClass("application/x-spss-por"));
    	assertEquals("tabular", getFileClass("text/csv"));
    	assertEquals("tabular", getFileClass("text/comma-separated-values"));
    	assertEquals("tabular", getFileClass("text/tsv"));
    	assertEquals("tabular", getFileClass("text/tab-separated-values"));
    	
    	DataFile tabular = createDataFile("");
    	tabular.setDataTable(new DataTable());
    	assertEquals("tabular", dataFileServiceBean.getFileClass(tabular));
    	
    	assertEquals("package", getFileClass("application/vnd.dataverse.file-package"));
    	
        assertEquals("other", getFileClass(""));
        assertEquals("other", getFileClass("foo/bar"));
    }
    
    private String getFileClass(final String mime) {
    	return dataFileServiceBean.getFileClass(createDataFile(mime));
    }

    
    
    
    /**
     * Create a DataFile with properties.
     *
     * @param contentType       the content media type as a string
     * @param storageIdentifier an identifier that signifies the location of the
     *                          file in storage. Must not be null, but may be empty.
     * @return a DataFile with the given content type and storage identifier
     * @see #createDataFile(java.lang.String)
     */
    private DataFile createDataFile(String contentType, String storageIdentifier) {
        DataFile file = new DataFile(contentType);
        file.setStorageIdentifier(storageIdentifier);
        return file;
    }

    /**
     * Create a DataFile with the given content type and empty storage identifier.
     *
     * @param contentType the content type of the DataFile (may be {@code null})
     * @return a DataFile with content type and empty storage identifier
     * @see #createDataFile(java.lang.String, java.lang.String)
     */
    private DataFile createDataFile(String contentType) {
        return createDataFile(contentType, "");
    }

    @Test
    public void testfindMostRecentVersionFileIsIn() {
        assertEquals(null, dataFileServiceBean.findMostRecentVersionFileIsIn(null));
    }

}
