package edu.harvard.iq.dataverse.test.arquillian;

import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ArquillianGlassfishConfigurationParserTest {

    private ParametrizedGlassfishConfCreator confCreator = new ParametrizedGlassfishConfCreator();

    @Test
    public void shouldSuccessfullyCreateTemporaryFile() {
    	
        this.confCreator.createTempGlassfishResources();

        assertTrue(exists(this.confCreator.newResourcePath));
    }

    @AfterEach
    public void removeTempGlassfishResource() {
        this.confCreator.cleanTempGlassfishResource();
    }
}

