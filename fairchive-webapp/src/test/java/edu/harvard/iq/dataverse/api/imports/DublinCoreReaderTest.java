package edu.harvard.iq.dataverse.api.imports;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributor;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.contributorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.language;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

public class DublinCoreReaderTest {

	private DublinCoreReader reader = new DublinCoreReader(this::findTypeByName);
	
	private final static HarvestingClient client;
	private final static String harvestId = "id1";
	
	@TempDir
	Path dir;
	
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
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10.18150/04M8OI");
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
		}
	}
	
	@Test
	void properFile_withHttpsDOI_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpsDoi.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getGlobalId().toString()).isEqualTo("doi:10.18150/04M8OI");
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10.18150/04M8OI");
		}
	}
		
	@Test
	void properFile_withHttpHDL_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/22946");
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10593/22946");
		}
	}
	
	@Test
	void properFile_withHttpsHDL_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withHttpsHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/22946");
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10593/22946");
		}
	}
	
	@Test
	void properFile_withUrl_identifier() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withUrl.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);
			
			assertThat(dataset.getGlobalId().toString()).
				isEqualTo("https:https://repozytorium.uw.edu.pl//handle/item/124996");
			assertThat(dataset.getGlobalId().getStoragePath()).
				isEqualTo("repozytorium.uw.edu.pl/handle/item/124996");
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
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10.18150/04M8OI");
			
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
	
	
	@Test
	void fileWithBrokenDoi_butValidHandle() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withBrokenDoi_butValidHandle.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/25658");
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10593/25658");
		}
	}
	
	@Test
	void fileWithBrokenDoiAndHandle_butValidUrl() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withBrokenDoiAndHandle_butValidUrl.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getGlobalId().toString()).
				isEqualTo("https:https://repozytorium.uw.edu.pl//handle/item/124996");
			assertThat(dataset.getGlobalId().getStoragePath()).
				isEqualTo("repozytorium.uw.edu.pl/handle/item/124996");
		}
	}
	
	@Test
	void fileWithMissingLanguage() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withoutLanguageTag.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getLatestVersion().streamDatasetFieldsByTypeName(language)).isEmpty();
		}
	}
	
	@Test
	void fileWithBrokenDoi_butValidHandle2() throws Exception {
		
		try(final Reader xml = open("/xml/imports/dublinCore_withBrokenDoi_butValidHandle2.xml")) {
			Dataset dataset = this.reader.read(client, harvestId, xml);

			assertThat(dataset.getGlobalId().toString()).isEqualTo("hdl:10593/22153");
			assertThat(dataset.getGlobalId().getStoragePath()).isEqualTo("10593/22153");
		}
	}
	
	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
	
	private Optional<DatasetFieldType> findTypeByName(final String name) {
		return Optional.of(new DatasetFieldType(name, TEXT, false));
	}
}
