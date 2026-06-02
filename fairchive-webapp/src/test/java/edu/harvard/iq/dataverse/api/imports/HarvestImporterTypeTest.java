package edu.harvard.iq.dataverse.api.imports;

import static edu.harvard.iq.dataverse.api.imports.HarvestImporterType.filterSupported;
import static edu.harvard.iq.dataverse.api.imports.HarvestImporterType.resolve;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.export.ExporterType;

public class HarvestImporterTypeTest {
	
	@Test
	void resolveImporterType_throwsNullPointer_froNullArgument() {
		
		assertThrows(NullPointerException.class, () -> resolve(null));
	}

	@Test
	void resolve_returns_DDI_forDDIMetadata() {
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("ddi")
				.withMetadataNamespace("ddi:codebook:2_5")
				.withSchema("https://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd"))).
			contains(HarvestImporterType.DDI);
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("oai_ddi")
				.withMetadataNamespace("ddi:codebook:2_5")
				.withSchema("https://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd"))).
			contains(HarvestImporterType.DDI);
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("oai_ddi.abc")
				.withMetadataNamespace("ddi:codebook:2_5")
				.withSchema("https://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd"))).
			contains(HarvestImporterType.DDI);
	}
	
	@Test
	void resolveImporterType_returns_DUBLIN_CORE_forDublinCodeMetadata() {
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("dc")
				.withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
				.withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"))).
			contains(HarvestImporterType.DUBLIN_CORE);
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("oai_dc")
				.withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
				.withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"))).
			contains(HarvestImporterType.DUBLIN_CORE);
	}
	
	@Test
	void resolveImporterType_returns_DATAVERSE_JSON_forJsonMetadata() {
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("dataverse_json")
				.withMetadataNamespace("")
				.withSchema(""))).
			contains(HarvestImporterType.DATAVERSE_JSON);
	}
	
	@Test
	void resolveImporterType_returns_EmptyOptional_forUnknownMetadata() {
		
		assertThat(resolve(
				new MetadataFormat()
				.withMetadataPrefix("oai_datacite")
				.withMetadataNamespace("http://datacite.org/schema/kernel-3")
                .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd"))).
			isEmpty();
	}
	
    @Test
    void filterSupported_fthrowsNullPointer_forNullParameter() {
    	
    	assertThrows(NullPointerException.class, () -> filterSupported(null));
    }
    
    @Test
    void filterSupported_returnsEmptyList_forEmptyInput() {

        assertThat(filterSupported(emptyList())).isEmpty();
    }
	
    @Test
    void filterSupportedFormats() {

        List<MetadataFormat> allFormats = stream(ExporterType.values())
                .map(ExporterType::getPrefix)
                .map(prefix -> new MetadataFormat()
                        .withMetadataPrefix(prefix)
                        .withSchema(prefix + "_schema.xsd")
                        .withMetadataNamespace(prefix + "_namespace"))
                .collect(toList());

        List<MetadataFormat> filtered = filterSupported(allFormats);

        assertThat(filtered.stream().map(MetadataFormat::getMetadataPrefix).collect(toList()))
                .containsExactlyInAnyOrder("oai_dc", "DDI", "oai_ddi", "dataverse_json");
    }

    @Test
    public void filterSupportedFormats__none_match() {

        List<MetadataFormat> input = singletonList((new MetadataFormat()
                .withMetadataPrefix("oai_datacite")
                .withMetadataNamespace("http://datacite.org/schema/kernel-3")
                .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd")));

        assertThat(filterSupported(input)).isEmpty();
    }
}
