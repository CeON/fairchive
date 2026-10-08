package edu.harvard.iq.dataverse.api.imports;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorAffiliationIdentifier;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorIdType;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorIdValue;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.datasetContact;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.datasetContactAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.datasetContactName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.description;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.descriptionText;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumber;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumberAgency;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumberAgencyIdentifier;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.grantNumberValue;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producer;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producerAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.producerName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.relatedMaterial;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.relatedMaterialIDNumber;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.relatedMaterialIDType;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.relatedMaterialRelationType;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.relatedMaterialURL;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.title;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;

public class DataCiteReaderTest {

    private final DataCiteReader reader = new DataCiteReader();

    // -------------------- TESTS --------------------

    @Test
    void read__doiIdentifier() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        assertThat(dataset.getProtocol()).isEqualTo("doi");
        assertThat(dataset.getAuthority()).isEqualTo("10.5072");
        assertThat(dataset.getIdentifier()).isEqualTo("FK2/05NAR1");
    }

    @Test
    void read__handleIdentifier() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(dataset.getProtocol()).isEqualTo("hdl");
        assertThat(dataset.getAuthority()).isEqualTo("20.500.12345");
        assertThat(dataset.getIdentifier()).isEqualTo("678");
    }

    @Test
    void read__neitherDoiNorHandle() {
        // when & then
        assertThatThrownBy(() -> read("/xml/imports/datacite_withoutIdentifier.xml"))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("no DOI or Handle identifier");
    }

    @Test
    void read__publicationYear() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        assertThat(dataset.getPublicationDate()).isEqualTo("2019-01-01");
        assertThat(dataset.getDatasetVersion().getVersionState()).isEqualTo("RELEASED");
    }

    @Test
    void read__placeholderPublicationYear() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(dataset.getPublicationDate()).isNull();
    }

    @Test
    void read__title() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        assertThat(field(dataset, title).getSinglePrimitive()).isEqualTo("Export test");
    }

    @Test
    void read__titleWithTypePrecedingMainTitle() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(field(dataset, title).getSinglePrimitive()).isEqualTo("Main title");
    }

    @Test
    void read__creators() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        List<Map<String, String>> authors = compound(dataset, author);
        assertThat(authors).hasSize(2);
        assertThat(authors.get(0)).containsOnly(
                entry(authorName, "Kowalski, Jan"),
                entry(authorIdType, "ORCID"),
                entry(authorIdValue, "0000-0002-1825-0097"),
                entry(authorAffiliation, "University of Warsaw"),
                entry(authorAffiliationIdentifier, "https://ror.org/039bjqg32"));
        assertThat(authors.get(1)).containsOnly(entry(authorName, "Nowak, Anna"));
    }

    @Test
    void read__nameIdentifierGivenAsUrl() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(compound(dataset, author).get(0))
                .containsEntry(authorIdType, "ORCID")
                .containsEntry(authorIdValue, "0000-0002-1825-0097");
    }

    @Test
    void read__relatedIdentifiers() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        List<Map<String, String>> materials = compound(dataset, relatedMaterial);
        assertThat(materials).hasSize(2);
        assertThat(materials.get(0)).containsOnly(
                entry(relatedMaterialIDNumber, "10.1000/182"),
                entry(relatedMaterialIDType, "doi"),
                entry(relatedMaterialRelationType, "IsCitedBy"));
        assertThat(materials.get(1)).containsOnly(
                entry(relatedMaterialURL, "https://example.org/material"),
                entry(relatedMaterialIDType, "url"),
                entry(relatedMaterialRelationType, "References"));
    }

    @Test
    void read__relatedIdentifierWithoutValue() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(compound(dataset, relatedMaterial)).isEmpty();
    }

    @Test
    void read__description() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        assertThat(compound(dataset, description))
                .containsExactly(singletonMap(descriptionText, "EXPORT"));
    }

    @Test
    void read__contributors() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        List<Map<String, String>> contacts = compound(dataset, datasetContact);
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0)).containsOnly(
                entry(datasetContactName, "Admin, Dataverse"),
                entry(datasetContactAffiliation, "Dataverse.org"));
        List<Map<String, String>> producers = compound(dataset, producer);
        assertThat(producers).hasSize(1);
        assertThat(producers.get(0)).containsOnly(
                entry(producerName, "ICM"),
                entry(producerAffiliation, "University of Warsaw"));
    }

    @Test
    void read__fundingReference() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite.xml");

        // then
        List<Map<String, String>> grants = compound(dataset, grantNumber);
        assertThat(grants).hasSize(1);
        assertThat(grants.get(0)).containsOnly(
                entry(grantNumberAgency, "Grant Agency"),
                entry(grantNumberAgencyIdentifier, "https://ror.org/00e0c0q64"),
                entry(grantNumberValue, "grant-123"));
    }

    @Test
    void read__funderIdentifierOtherThanRor() throws Exception {
        // when
        DatasetDTO dataset = read("/xml/imports/datacite_minimal.xml");

        // then
        assertThat(compound(dataset, grantNumber))
                .containsExactly(singletonMap(grantNumberAgency, "Other Agency"));
    }

    @Test
    void read__rootIsNotResource() {
        // when & then
        assertThatThrownBy(() -> this.reader.read(new StringReader("<dc><title>Not DataCite</title></dc>")))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Root element is not 'resource'");
    }

    @Test
    void read__malformedXml() {
        // when & then
        assertThatThrownBy(() -> this.reader.read(new StringReader("<resource><titles></resource>")))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Failed to parse DataCite XML record");
    }

    // -------------------- PRIVATE --------------------

    private DatasetDTO read(String fileName) throws Exception {
        try (Reader xml = new InputStreamReader(getClass().getResourceAsStream(fileName), UTF_8)) {
            return this.reader.read(xml);
        }
    }

    private DatasetFieldDTO field(DatasetDTO dataset, String typeName) {
        return dataset.getDatasetVersion().getMetadataBlocks().get("citation").getFields().stream()
                .filter(field -> typeName.equals(field.getTypeName()))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, String>> compound(DatasetDTO dataset, String typeName) {
        DatasetFieldDTO field = field(dataset, typeName);
        if (field == null) {
            return emptyList();
        }
        return field.getMultipleCompound().stream()
                .map(children -> children.stream().collect(
                        Collectors.toMap(DatasetFieldDTO::getTypeName, DatasetFieldDTO::getSinglePrimitive)))
                .collect(Collectors.toList());
    }
}
