package edu.harvard.iq.dataverse.api.imports;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributor;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributorName;
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
	void properFile_withHttpDOI_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpDoi.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			List<DatasetField> fields = dataset.getLatestVersion().getDatasetFields();
			
			assertThat(dataset.getHarvestedFrom()).isSameAs(client);
			assertThat(dataset.getHarvestIdentifier()).isEqualTo(harvestId);
			assertThat(dataset.getGlobalId().toString()).isEqualTo("doi:10.18150/04M8OI");
			assertThat(dataset.getPublicationDate().toLocalDateTime().getYear()).isEqualTo(2020);
			
			assertThat(fields.get(0).getTypeName()).isEqualTo(title);
			assertThat(fields.get(0).getValue()).isEqualTo("Who complies with the restrictions");
			assertThat(fields.get(1).getTypeName()).isEqualTo(author);
			assertThat(fields.get(1).getChildByName(authorName).
					map(DatasetField::getValue)).contains("Kozakiewicz, Zuzanna");
			assertThat(fields.get(4).getTypeName()).isEqualTo(author);
			assertThat(fields.get(4).getChildByName(authorName).
					map(DatasetField::getValue)).contains("Jonason, Peter");
			assertThat(fields.get(5).getTypeName()).isEqualTo(contributor);
			assertThat(fields.get(5).getChildByName(contributorName).
					map(DatasetField::getValue)).contains("Pilarczyk, Zbigniew");
			assertThat(fields.get(6).getTypeName()).isEqualTo(language);
			assertThat(fields.get(6).getValue()).isEqualTo("en");
			
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
	void properFile_withHttpsDOI_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpsDoi.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getGlobalId().toString()).isEqualTo("doi:10.18150/04M8OI");
		}
	}
		
	@Test
	void properFile_withHttpHDL_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/22946");
		}
	}
	
	@Test
	void properFile_withHttpsHDL_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpsHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/22946");
		}
	}
	
	@Test
	void properFile_withUrl_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withUrl.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("url:https://repozytorium.uw.edu.pl//handle/item/124996");
		}
	}
	
	@Test
	void properFile_withDOI_noAuthor_contributorOnly() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withDoi_noAuthor_contributorOnly.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			List<DatasetField> fields = dataset.getLatestVersion().getDatasetFields();
			
			assertThat(dataset.getHarvestedFrom()).isSameAs(client);
			assertThat(dataset.getHarvestIdentifier()).isEqualTo(harvestId);
			assertThat(dataset.getGlobalId().toString()).isEqualTo("doi:10.18150/04M8OI");
			
			assertThat(fields.get(0).getTypeName()).isEqualTo(title);
			assertThat(fields.get(0).getValue()).isEqualTo("Who complies with the restrictions");
			assertThat(fields.get(1).getTypeName()).isEqualTo(contributor);
			assertThat(fields.get(1).getChildByName(contributorName).
					map(DatasetField::getValue)).contains("Pilarczyk, Zbigniew");
			assertThat(fields.get(2).getTypeName()).isEqualTo(language);
			assertThat(fields.get(2).getValue()).isEqualTo("en");
		}
	}
	
	@Test
	void fileWithPublicationDate_containingOnlyYearAndMoth() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_publication_year_and_month_only.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			assertThat(dataset.getPublicationDate().toLocalDateTime().getYear()).isEqualTo(2018);
			assertThat(dataset.getPublicationDate().toLocalDateTime().getMonthValue()).isEqualTo(9);
			assertThat(dataset.getPublicationDate().toLocalDateTime().getDayOfMonth()).isEqualTo(1);
		}
	}
	
	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
	
}
