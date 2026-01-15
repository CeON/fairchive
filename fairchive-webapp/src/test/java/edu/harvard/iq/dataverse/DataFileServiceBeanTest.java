package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
