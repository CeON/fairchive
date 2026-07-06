package edu.harvard.iq.dataverse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

/**
 * @author rmp553
 */
public class GlobalIdTest {
	
	@Test
	public void testContruction() {
		
		 assertEquals("doi:10.5072/FK2/BYM3IW", new GlobalId("doi:10.5072/FK2/BYM3IW").toString());
		 assertThrows(NullPointerException.class, () -> new GlobalId((String)null));
	}

    @Test
    public void testValidDOI() {
        GlobalId instance = new GlobalId("doi:10.5072/FK2/BYM3IW");

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
        assertEquals("doi:10.5072/FK2/BYM3IW", instance.toString());
        assertEquals("https://doi.org/10.5072/FK2/BYM3IW", instance.toURL().toString());
    }

    @Test
    public void testValidHandle() {
        GlobalId instance = new GlobalId("hdl:1902.1/111012");

        assertEquals("hdl", instance.getProtocol());
        assertEquals("1902.1", instance.getAuthority());
        assertEquals("111012", instance.getIdentifier());
        assertEquals("hdl:1902.1/111012", instance.toString());
        assertEquals("https://hdl.handle.net/1902.1/111012", instance.toURL().toString());
    }
    
    @Test
    public void testValidUrl() {
        GlobalId instance = new GlobalId("https:https://google.com/id1");

        assertEquals("https", instance.getProtocol());
        assertEquals("google.com", instance.getAuthority());
        assertEquals("/id1", instance.getIdentifier());
        assertEquals("https:https://google.com/id1", instance.toString());
        assertEquals("https://google.com/id1", instance.toURL().toString());
    }

    @Test
    public void testContructFromDataset() {
        Dataset testDS = new Dataset();

        testDS.setProtocol("doi");
        testDS.setAuthority("10.5072");
        testDS.setIdentifier("FK2/BYM3IW");

        GlobalId instance = new GlobalId(testDS);

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
    }

    @Test
    public void testInject() {

        String badProtocol = "hdl:'Select value from datasetfieldvalue';/ha";

        GlobalId instance = new GlobalId(badProtocol);

        assertEquals("hdl", instance.getProtocol());
        assertEquals("Selectvaluefromdatasetfieldvalue", instance.getAuthority());
        assertEquals("ha", instance.getIdentifier());
    }

    @Test
    public void testUnknownProtocol() {

        String badProtocol = "doy:10.5072/FK2/BYM3IW";

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> new GlobalId(badProtocol));
        assertEquals("Failed to parse identifier: " + badProtocol, thrown.getMessage());
    }

    @Test
    public void testBadIdentifierOnePart() {

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> new GlobalId("1part"));
        assertEquals("Failed to parse identifier: 1part", thrown.getMessage());
    }

    @Test
    public void testBadIdentifierTwoParts() {

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> new GlobalId("doi:2part/blah"));
        assertEquals("Failed to parse identifier: doi:2part/blah", thrown.getMessage());
    }

    @Test
    public void testIsComplete() {
        assertFalse(new GlobalId("doi", "10.123", null).isComplete());
        assertFalse(new GlobalId("doi", null, "123").isComplete());
        assertFalse(new GlobalId(null, "10.123", "123").isComplete());
        assertTrue(new GlobalId("doi", "10.123", "123").isComplete());
    }

    @Test
    public void testVerifyImportCharacters() {
        assertTrue(GlobalId.verifyImportCharacters("-"));
        assertTrue(GlobalId.verifyImportCharacters("qwertyQWERTY"));
        assertFalse(GlobalId.verifyImportCharacters("Hällochen"));
        assertFalse(GlobalId.verifyImportCharacters("*"));
    }
}
