package edu.harvard.iq.dataverse.harvest.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

public class DataciteHarvesterConfigTest {

    @Test
    public void fromJson__dois() {
        // given
        String json = "{ \"dois\": [ \"10.5072/FK2/BYM3IW\", \"1902.1/111012\" ] }";

        // when
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(json);

        // then
        assertThat(config.getDois()).containsExactly("10.5072/FK2/BYM3IW", "1902.1/111012");
    }

    @Test
    public void fromJson__empty() {
        // when
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson("{}");

        // then
        assertThat(config.getDois()).isEmpty();
    }

    @Test
    public void fromJson__blank() {
        // when & then
        assertThatThrownBy(() -> DataciteHarvesterConfig.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing DataCite harvester configuration");
    }

    @Test
    public void fromJson__null() {
        // when & then
        assertThatThrownBy(() -> DataciteHarvesterConfig.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing DataCite harvester configuration");
    }

    @Test
    public void fromJson__malformed() {
        // when & then
        assertThatThrownBy(() -> DataciteHarvesterConfig.fromJson("{ \"dois\": \"10.5072/a\" }"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Malformed DataCite harvester configuration: ");
    }

    @Test
    public void toJson__readBack() {
        // given
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setDois(singletonList("10.5072/a"));
        config.setQueryParams(singletonMap("affiliation-country", "PL"));
        config.setMaxRecords(5);

        // when
        String json = config.toJson();

        // then
        DataciteHarvesterConfig readBack = DataciteHarvesterConfig.fromJson(json);
        assertThat(readBack.getDois()).containsExactly("10.5072/a");
        assertThat(readBack.getQueryParams()).containsExactly(entry("affiliation-country", "PL"));
        assertThat(readBack.getMaxRecords()).isEqualTo(5);
    }

    @Test
    public void getDoiValues() {
        // given
        DataciteHarvesterConfig config = parse("{ \"dois\": [ \"10.5072/FK2/ABC\" ] }");

        // when
        List<DataciteHarvesterConfig.DOIValue> doiValues = config.getDoiValues();

        // then
        assertThat(doiValues).extracting(DataciteHarvesterConfig.DOIValue::getAuthority, DataciteHarvesterConfig.DOIValue::getId)
                .containsExactly(tuple("10.5072", "FK2/ABC"));
    }

    @Test
    public void validate__invalidDoi() {
        // given
        DataciteHarvesterConfig config = parse("{ \"dois\": [ \"10.5072\" ] }");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid DOI: 10.5072");
    }

    @Test
    public void fromJson__queryParams() {
        // given
        String json = "{ \"queryParams\": { \"affiliation-country\": \"PL\", "
                + "\"query\": \"publicationYear:2026 AND NOT types.resourceTypeGeneral:Dataset\" }, \"maxRecords\": 50 }";

        // when
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(json);

        // then
        assertThat(config.getQueryParams())
                .containsEntry("affiliation-country", "PL")
                .containsEntry("query", "publicationYear:2026 AND NOT types.resourceTypeGeneral:Dataset")
                .hasSize(2);
        assertThat(config.getMaxRecords()).isEqualTo(50);
    }

    @Test
    public void fromJson__noQueryParams() {
        // given
        String json = "{}";

        // when
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(json);

        // then
        assertThat(config.getQueryParams()).isEmpty();
        assertThat(config.getMaxRecords()).isEqualTo(1000);
    }

    @Test
    public void validate__supportedParams() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\", "
                + "\"resource-type-id\": \"dataset\" }, \"maxRecords\": 1 }");

        // when & then
        assertThatCode(config::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__doisOnly() {
        // given
        DataciteHarvesterConfig config = parse("{ \"dois\": [ \"10.5061/a\" ] }");

        // when & then
        assertThatCode(config::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__nothingToHarvest() {
        // given
        DataciteHarvesterConfig config = parse("{}");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing DOI's or query parameters");
    }

    @Test
    public void validate__maxRecordsWithoutQuery() {
        // given
        DataciteHarvesterConfig config = parse("{ \"dois\": [ \"10.5061/a\" ], "
                + "\"maxRecords\": 1 }");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRecords applies only to queryParams, which are missing");
    }

    @Test
    public void validate__anyParamName() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"affiliation-name\": \"AGH\" } }");

        // when & then
        assertThatCode(config::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__pagingParam() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"page[size]\": \"10\" } }");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DataCite paging parameter not allowed, the harvester pages itself: page[size]");
    }

    @Test
    public void validate__blankValue() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"query\": \" \" } }");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empty value of DataCite query parameter: query");
    }

    @Test
    public void validate__maxRecordsBelowOne() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\" }, \"maxRecords\": 0 }");

        // when & then
        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRecords must be at least 1, was: 0");
    }

    @Test
    public void getSearchParams__datasetsByDefault() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\" } }");

        // when
        Map<String, String> searchParams = config.getSearchParams();

        // then
        assertThat(searchParams)
                .containsEntry("affiliation-country", "PL")
                .containsEntry("resource-type-id", "dataset")
                .hasSize(2);
        assertThat(config.getQueryParams()).hasSize(1);
    }

    @Test
    public void getSearchParams__ownResourceType() {
        // given
        DataciteHarvesterConfig config = parse("{ \"queryParams\": { \"resource-type-id\": \"software\" } }");

        // when
        Map<String, String> searchParams = config.getSearchParams();

        // then
        assertThat(searchParams).containsExactly(entry("resource-type-id", "software"));
    }

    @Test
    public void doiValue__identifierWithSlash() {
        // when
        DataciteHarvesterConfig.DOIValue doi = new DataciteHarvesterConfig.DOIValue("10.5072/FK2/ABC");

        // then
        assertThat(doi.getAuthority()).isEqualTo("10.5072");
        assertThat(doi.getId()).isEqualTo("FK2/ABC");
    }

    @Test
    public void doiValue__noSlash() {
        // when & then
        assertThatThrownBy(() -> new DataciteHarvesterConfig.DOIValue("10.5072"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid DOI: 10.5072");
    }

    private DataciteHarvesterConfig parse(String json) {
        return DataciteHarvesterConfig.fromJson(json);
    }
}
