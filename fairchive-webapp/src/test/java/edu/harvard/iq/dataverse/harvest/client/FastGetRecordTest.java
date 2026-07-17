package edu.harvard.iq.dataverse.harvest.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

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
	
	private Reader open(final String fileName) {
		return new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8);
	}
}
