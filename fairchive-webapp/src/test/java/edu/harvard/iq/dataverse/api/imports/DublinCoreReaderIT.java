package edu.harvard.iq.dataverse.api.imports;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.language;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

public class DublinCoreReaderIT extends WebappArquillianDeployment {

	@Inject 
	DublinCoreReader reader;
	
	private final static HarvestingClient client;
	private final static String harvestId = "id1";
	
	static {
		client = new HarvestingClient();
		client.setDataverse(new Dataverse());
		client.getDataverse().setId(123L);
	}
	
	@Test
	void properFile_withDOI_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withDoi.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			List<DatasetField> fields = dataset.getLatestVersion().getDatasetFields();
			
			assertThat(dataset.getHarvestedFrom()).isSameAs(client);
			assertThat(dataset.getHarvestIdentifier()).isEqualTo(harvestId);
			assertThat(dataset.getGlobalId().toString()).isEqualTo("doi:10.18150/04M8OI");
			
			assertThat(fields.get(0).getTypeName()).isEqualTo(title);
			assertThat(fields.get(0).getValue()).isEqualTo("Who complies with the restrictions");
			assertThat(fields.get(1).getTypeName()).isEqualTo(author);
			assertThat(fields.get(1).getChildByName(authorName).
					map(DatasetField::getValue)).contains("Kozakiewicz, Zuzanna");
			assertThat(fields.get(4).getTypeName()).isEqualTo(author);
			assertThat(fields.get(4).getChildByName(authorName).
					map(DatasetField::getValue)).contains("Jonason, Peter");
			assertThat(fields.get(5).getTypeName()).isEqualTo(language);
			assertThat(fields.get(5).getValue()).isEqualTo("en");
			
//			System.out.println("==============================================");
//			System.out.println("GlobalId: " + dataset.getGlobalId());
//			
//			System.out.println("==============================================");
//			
//			dataset.getLatestVersion().getDatasetFields().
//				forEach(field -> System.out.println(field.getTypeName() + 
//						" -> " + field.getValue()));
//			
//			System.out.println("==============================================");
		}
	}
		
	@Test
	void properFile_withHDL_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:123/456");
		}
	}
	
	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
	
}
