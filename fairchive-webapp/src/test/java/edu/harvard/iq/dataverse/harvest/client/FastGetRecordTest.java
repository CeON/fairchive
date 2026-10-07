package edu.harvard.iq.dataverse.harvest.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FastGetRecordTest {
	
	private final static FastGetRecord.Factory factory = FastGetRecord.newFactory();
	
	private FastGetRecord record;
	
	@BeforeEach
	public void setUp() throws Exception {
		this.record = factory.build();
	}
	
	@AfterEach
	public void ceanlUp() throws Exception {
		this.record.close();
	}
	
	@Test
	public void parseContent() throws Exception {
		
		try(final Reader xml = open("/xml/imports/OAI-PMH_withDublinCore.xml")) {
			this.record.parseContent("oai_dc", new BufferedReader(xml));
			
			assertThat(this.record.getContent()).startsWith("<oai_dc:dc");
			assertThat(this.record.getContent()).endsWith("</oai_dc:dc>");
			
			assertThat(this.record.getErrorMessage()).isNull();
			assertThat(this.record.isDeleted()).isFalse();
			
			System.out.print("error message: ");
			System.out.println(this.record.getErrorMessage());
			System.out.print("content: ");
			System.out.println(this.record.getContent());
		}
	}
	
	@Test
	public void parseContent__rootElementNotNamedAfterPrefix() throws Exception {
		
		try(final Reader xml = open("/xml/imports/OAI-PMH_withDatacite.xml")) {
			// when
			this.record.parseContent("datacite", new BufferedReader(xml));
			
			// then
			assertThat(this.record.getErrorMessage()).isNull();
			assertThat(this.record.getContent()).startsWith("<resource xmlns=");
			assertThat(this.record.getContent()).endsWith("</descriptions>\n</resource>");
			assertThat(this.record.isDeleted()).isFalse();
		}
	}
	
	@Test
	public void parseContent__rootElementWithNamespacePrefixEqualToMetadataPrefix() throws Exception {
		// given
		final String resource = "<datacite:resource xmlns:datacite=\"http://datacite.org/schema/kernel-4\">"
				+ "<datacite:identifier identifierType=\"DOI\">10.5072/FK2/05NAR1</datacite:identifier>"
				+ "<datacite:titles><datacite:title>Title</datacite:title></datacite:titles>"
				+ "</datacite:resource>";

		// when
		this.record.parseContent("datacite", getRecordResponse(resource));

		// then
		assertThat(this.record.getErrorMessage()).isNull();
		assertThat(this.record.getContent()).isEqualTo(resource);
	}

	@Test
	public void parseContent__codeBookServedAsOaiDdi() throws Exception {
		// given
		final String codeBook = "<codeBook xmlns=\"ddi:codebook:2_5\">"
				+ "<stdyDscr><citation><titlStmt>"
				+ "<IDNo agency=\"DOI\">doi:10.5072/FK2/05NAR1</IDNo>"
				+ "</titlStmt></citation></stdyDscr>"
				+ "</codeBook>";

		// when
		this.record.parseContent("oai_ddi", getRecordResponse(codeBook));

		// then
		assertThat(this.record.getErrorMessage()).isNull();
		assertThat(this.record.getContent()).isEqualTo(codeBook);
	}

	@Test
	public void parseContent__emptyMetadata() throws Exception {
		// when
		this.record.parseContent("datacite", getRecordResponse(""));

		// then
		assertThat(this.record.getErrorMessage())
			.isEqualTo("No record of format 'datacite' found in <metadata>.");
		assertThat(this.record.getContent()).isNull();
	}

	private BufferedReader getRecordResponse(final String metadata) {
		return new BufferedReader(new StringReader(
				"<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\">"
				+ "<responseDate>2026-10-06T09:09:09Z</responseDate>"
				+ "<request verb=\"GetRecord\">https://example.org/oai</request>"
				+ "<GetRecord><record>"
				+ "<header><identifier>oai:example.org:1</identifier>"
				+ "<datestamp>2026-10-05T18:58:56Z</datestamp></header>"
				+ "<metadata>" + metadata + "</metadata>"
				+ "</record></GetRecord></OAI-PMH>"));
	}

	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
}
