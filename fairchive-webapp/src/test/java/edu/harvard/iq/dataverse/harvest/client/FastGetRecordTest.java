package edu.harvard.iq.dataverse.harvest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

public class FastGetRecordTest {
	
	private final static FastGetRecord.Factory factory = FastGetRecord.newFactory();
	
	private static WireMockServer wireMockServer;
	
	private FastGetRecord record;
	
	@BeforeAll
	public static void startServer() {
		wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
		wireMockServer.start();
	}
	
	@AfterAll
	public static void stopServer() {
		wireMockServer.stop();
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		wireMockServer.resetAll();
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
	public void parseContent__deletedRecord() throws Exception {
		// given
		final BufferedReader response = getRecordResponse(
				"<record><header status=\"deleted\">"
				+ "<identifier>doi:10.18150/17LFME</identifier>"
				+ "<datestamp>2024-01-06T02:00:03Z</datestamp>"
				+ "</header></record>");

		// when
		this.record.parseContent("oai_dc", response);

		// then
		assertThat(this.record.isDeleted()).isTrue();
		assertThat(this.record.getErrorMessage()).isNull();
		assertThat(this.record.getContent()).isNull();
	}

	@Test
	public void parseContent__missingMetadata() throws Exception {
		// given
		final BufferedReader response = getRecordResponse(
				"<record><header>"
				+ "<identifier>doi:10.18150/00QUKH</identifier>"
				+ "</header></record>");

		// when
		this.record.parseContent("oai_dc", response);

		// then
		assertThat(this.record.isDeleted()).isFalse();
		assertThat(this.record.getErrorMessage()).isEqualTo("<metadata> tag not found.");
	}

	@Test
	public void parseContent__oaiError() throws Exception {
		// given
		final BufferedReader response = oaiResponse(
				"<error code=\"idDoesNotExist\">No such record</error>");

		// when
		this.record.parseContent("oai_dc", response);

		// then
		assertThat(this.record.getErrorMessage())
				.contains("idDoesNotExist")
				.contains("No such record");
	}

	@Test
	public void parseContent__dataverseJson() throws Exception {
		// given
		final String json = "{\"identifier\":\"00QUKH\",\"author\":\"Przemys\u0142aw\"}";
		wireMockServer.stubFor(get(urlPathEqualTo("/api/datasets/export"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(json.getBytes(UTF_8))));

		// when
		this.record.parseContent("dataverse_json", dataverseJsonResponse());

		// then
		assertThat(this.record.getErrorMessage()).isNull();
		assertThat(this.record.isDeleted()).isFalse();
		assertThat(this.record.getContent()).isEqualTo(json);
	}

	@Test
	public void parseContent__dataverseJsonDownloadFailed() throws Exception {
		// given
		wireMockServer.stubFor(get(urlPathEqualTo("/api/datasets/export"))
				.willReturn(aResponse().withStatus(500)));
		final BufferedReader response = dataverseJsonResponse();

		// when & then
		assertThatThrownBy(() -> this.record.parseContent("dataverse_json", response))
				.isInstanceOf(IOException.class)
				.hasMessage("Failed to download extended metadata.");
	}

	@Test
	public void parseContent__dataverseJsonDeletedRecord() throws Exception {
		// given
		final BufferedReader response = getRecordResponse(
				"<record><header status=\"deleted\">"
				+ "<identifier>doi:10.18150/17LFME</identifier>"
				+ "</header></record>");

		// when
		this.record.parseContent("dataverse_json", response);

		// then
		assertThat(this.record.isDeleted()).isTrue();
		assertThat(this.record.getErrorMessage()).isNull();
	}

	/**
	 * A record the way Fairchive serves the "dataverse_json" format: an empty
	 * metadata element pointing to the place to download the record from.
	 */
	private BufferedReader dataverseJsonResponse() {
		return getRecordResponse("<record><header>"
				+ "<identifier>doi:10.18150/00QUKH</identifier>"
				+ "</header><metadata directApiCall=\"http://localhost:"
				+ wireMockServer.port()
				+ "/api/datasets/export?exporter=dataverse_json&amp;"
				+ "persistentId=doi:10.18150/00QUKH\"/></record>");
	}

	private BufferedReader getRecordResponse(final String record) {
		return oaiResponse("<GetRecord>" + record + "</GetRecord>");
	}

	private BufferedReader oaiResponse(final String body) {
		return new BufferedReader(new StringReader(
				"<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\">"
				+ "<responseDate>2026-10-12T06:37:05Z</responseDate>"
				+ "<request verb=\"GetRecord\">https://repod.icm.edu.pl/oai</request>"
				+ body + "</OAI-PMH>"));
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
		this.record.parseContent("datacite", metadataResponse(resource));

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
		this.record.parseContent("oai_ddi", metadataResponse(codeBook));

		// then
		assertThat(this.record.getErrorMessage()).isNull();
		assertThat(this.record.getContent()).isEqualTo(codeBook);
	}

	@Test
	public void parseContent__emptyMetadata() throws Exception {
		// when
		this.record.parseContent("datacite", metadataResponse(""));

		// then
		assertThat(this.record.getErrorMessage())
			.isEqualTo("No record of format 'datacite' found in <metadata>.");
		assertThat(this.record.getContent()).isNull();
	}

	private BufferedReader metadataResponse(final String metadata) {
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
