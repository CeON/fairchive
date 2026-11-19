package edu.harvard.iq.dataverse.harvest.client;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class DataciteHarvesterParamsTest {

    @Test
    public void parseJson__empty() {
        // given
        String json = "{}";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).isEmpty();
    }

    @Test
    public void parseJson__emptyArray() {
        // given
        String json = "{ \"doiImport\": [] }";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).isEmpty();
    }

    @Test
    public void parseJson__array_of_doi_parts() {
        // given
        String json = "{ \"doiImport\": [ {\"authority\": \"10.5072\", \"id\": \"FK2/BYM3IW\"}, {\"authority\": \"1902.1\", \"id\": \"111012\"} ] }";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getDoiImport()).hasSize(2);
    }

    @Test
    public void parseJson__class() {
        // given
        String json = "{ \"doiImport\": [ {\"authority\": \"10.5072\", \"id\": \"FK2/BYM3IW\"}, {\"authority\": \"1902.1\", \"id\": \"111012\"} ] }";

        // when
        HarvesterParams params = new Gson().fromJson(json, getParams());

        // then
        assertThat(params).isInstanceOf(DataciteHarvesterParams.class);
    }

    @Test
    public void parseJson__queryParams() {
        // given
        String json = "{ \"queryParams\": { \"affiliation-country\": \"PL\", "
                + "\"query\": \"publicationYear:2026 AND NOT types.resourceTypeGeneral:Dataset\" }, \"maxRecords\": 50 }";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getQueryParams())
                .containsEntry("affiliation-country", "PL")
                .containsEntry("query", "publicationYear:2026 AND NOT types.resourceTypeGeneral:Dataset")
                .hasSize(2);
        assertThat(params.getMaxRecords()).isEqualTo(50);
    }

    @Test
    public void parseJson__noQueryParams() {
        // given
        String json = "{}";

        // when
        DataciteHarvesterParams params = new Gson().fromJson(json, DataciteHarvesterParams.class);

        // then
        assertThat(params.getQueryParams()).isEmpty();
        assertThat(params.getMaxRecords()).isEqualTo(1000);
    }

    @Test
    public void validate__supportedParams() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\", "
                + "\"resource-type-id\": \"dataset\" }, \"maxRecords\": 1 }");

        // when & then
        assertThatCode(params::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__doisOnly() {
        // given
        DataciteHarvesterParams params = parse("{ \"doiImport\": [ { \"authority\": \"10.5061\", \"id\": \"a\" } ] }");

        // when & then
        assertThatCode(params::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__nothingToHarvest() {
        // given
        DataciteHarvesterParams params = parse("{}");

        // when & then
        assertThatThrownBy(params::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing DOI's or query parameters");
    }

    @Test
    public void validate__maxRecordsWithoutQuery() {
        // given
        DataciteHarvesterParams params = parse("{ \"doiImport\": [ { \"authority\": \"10.5061\", \"id\": \"a\" } ], "
                + "\"maxRecords\": 1 }");

        // when & then
        assertThatThrownBy(params::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRecords applies only to queryParams, which are missing");
    }

    @Test
    public void validate__anyParamName() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"affiliation-name\": \"AGH\" } }");

        // when & then
        assertThatCode(params::validate).doesNotThrowAnyException();
    }

    @Test
    public void validate__pagingParam() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"page[size]\": \"10\" } }");

        // when & then
        assertThatThrownBy(params::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DataCite paging parameter not allowed, the harvester pages itself: page[size]");
    }

    @Test
    public void validate__blankValue() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"query\": \" \" } }");

        // when & then
        assertThatThrownBy(params::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empty value of DataCite query parameter: query");
    }

    @Test
    public void validate__maxRecordsBelowOne() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\" }, \"maxRecords\": 0 }");

        // when & then
        assertThatThrownBy(params::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxRecords must be at least 1, was: 0");
    }

    @Test
    public void getSearchParams__datasetsByDefault() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"affiliation-country\": \"PL\" } }");

        // when
        Map<String, String> searchParams = params.getSearchParams();

        // then
        assertThat(searchParams)
                .containsEntry("affiliation-country", "PL")
                .containsEntry("resource-type-id", "dataset")
                .hasSize(2);
        assertThat(params.getQueryParams()).hasSize(1);
    }

    @Test
    public void getSearchParams__ownResourceType() {
        // given
        DataciteHarvesterParams params = parse("{ \"queryParams\": { \"resource-type-id\": \"software\" } }");

        // when
        Map<String, String> searchParams = params.getSearchParams();

        // then
        assertThat(searchParams).containsExactly(entry("resource-type-id", "software"));
    }

    @Test
    public void doiValue__identifierWithSlash() {
        // when
        DataciteHarvesterParams.DOIValue doi = new DataciteHarvesterParams.DOIValue("10.5072/FK2/ABC");

        // then
        assertThat(doi.getAuthority()).isEqualTo("10.5072");
        assertThat(doi.getId()).isEqualTo("FK2/ABC");
    }

    @Test
    public void doiValue__noSlash() {
        // when & then
        assertThatThrownBy(() -> new DataciteHarvesterParams.DOIValue("10.5072"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid DOI: 10.5072");
    }

    private DataciteHarvesterParams parse(String json) {
        return new Gson().fromJson(json, DataciteHarvesterParams.class);
    }

    private Class<? extends HarvesterParams> getParams() {
        return getSpecific();
    }

    private Class<? extends HarvesterParams> getSpecific() {
        return DataciteHarvesterParams.class;
    }
}
