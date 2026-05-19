package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ControlledVocabularyValueTest {

	private static final DatasetFieldType fieldType = new DatasetFieldType();
	
	@BeforeAll
	static void setUp() {
		
		fieldType.setName("type1");
		fieldType.setMetadataBlock(new MetadataBlock());
		fieldType.getMetadataBlock().setName("block1");
	}
	
	@Test
	void getLocaleStrValue() {
		
		ControlledVocabularyValue value = new ControlledVocabularyValue();
		value.setId(1L);
		value.setStrValue("N/A");
		
		assertThat(value.getLocaleStrValue(ENGLISH)).isEqualTo("N/A");
		
		value.setStrValue("value1");
		
		assertThat(value.getLocaleStrValue(ENGLISH)).isEqualTo("value1");
		
		value.setDatasetFieldType(fieldType);
		
		assertThat(value.getLocaleStrValue(ENGLISH)).isEqualTo("value1");
	}
	
}
