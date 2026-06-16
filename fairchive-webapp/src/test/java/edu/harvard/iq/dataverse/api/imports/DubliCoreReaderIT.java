package edu.harvard.iq.dataverse.api.imports;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

public class DubliCoreReaderIT extends WebappArquillianDeployment {

	@Inject 
	DublinCoreReader reader;
	
	private final static HarvestingClient client;
	
	static {
		client = new HarvestingClient();
		client.setDataverse(new Dataverse());
		client.getDataverse().setId(123L);
	}
	
	@Test
	void test1() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore1.xml")) {
			Dataset dataset = this.reader.read(client, xml);
			
			assertThat(dataset.getLatestVersion()).isNotNull();
		}
	}
	
	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
	
}
