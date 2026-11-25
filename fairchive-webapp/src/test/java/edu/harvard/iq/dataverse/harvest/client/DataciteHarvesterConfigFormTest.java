package edu.harvard.iq.dataverse.harvest.client;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class DataciteHarvesterConfigFormTest {

    // -------------------- TESTS --------------------

    @Test
    public void constructor__fromConfig() {
        // given
        DataciteHarvesterConfig config = givenConfig();

        // when
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm(config);

        // then
        assertThat(form.getQuery()).isEqualTo("creators.affiliation.name:AGH");
        assertThat(form.getAdditionalParams()).isEqualTo("affiliation-country=PL\nclient-id=cern.zenodo");
        assertThat(form.getDois()).isEqualTo("10.5072/ABC\n10.5072/DEF");
        assertThat(form.getMaxRecords()).isEqualTo(20);
    }

    @Test
    public void constructor__fromConfigWithoutMaxRecords() {
        // given
        DataciteHarvesterConfig config = givenConfig();
        config.setMaxRecords(null);

        // when
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm(config);

        // then
        assertThat(form.getMaxRecords()).isNull();
    }

    @Test
    public void toConfig__queryOnly() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("  publicationYear:2023  ");

        // when
        DataciteHarvesterConfig config = form.toConfig();

        // then
        assertThat(config.getQueryParams()).containsExactly(entry("query", "publicationYear:2023"));
        assertThat(config.getDois()).isEmpty();
        assertThat(config.getConfiguredMaxRecords()).isNull();
    }

    @Test
    public void toConfig__queryAndAdditionalParams() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("publicationYear:2023");
        form.setAdditionalParams(" affiliation-country = PL \n\nquery-like=a=b\n");

        // when
        DataciteHarvesterConfig config = form.toConfig();

        // then
        assertThat(config.getQueryParams()).containsExactly(
                entry("query", "publicationYear:2023"),
                entry("affiliation-country", "PL"),
                entry("query-like", "a=b"));
    }

    @Test
    public void toConfig__doisOnly() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setDois("10.5072/ABC\r\n\r\n  https://doi.org/10.5072/DEF  \n"
                + "http://dx.doi.org/10.5072/GHI\ndoi:10.5072/JKL");

        // when
        DataciteHarvesterConfig config = form.toConfig();

        // then
        assertThat(config.getDois()).containsExactly("10.5072/ABC", "10.5072/DEF", "10.5072/GHI", "10.5072/JKL");
        assertThat(config.getQueryParams()).isEmpty();
    }

    @Test
    public void toConfig__maxRecords() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("publicationYear:2023");
        form.setMaxRecords(50);

        // when
        DataciteHarvesterConfig config = form.toConfig();

        // then
        assertThat(config.getConfiguredMaxRecords()).isEqualTo(50);
    }

    @Test
    public void toConfig__additionalParamWithoutValueSeparator() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("publicationYear:2023");
        form.setAdditionalParams("affiliation-country PL");

        // when & then
        assertThatThrownBy(form::toConfig)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Additional parameter must be given as name=value: affiliation-country PL");
    }

    @Test
    public void toConfig__additionalParamWithoutName() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("publicationYear:2023");
        form.setAdditionalParams(" =PL");

        // when & then
        assertThatThrownBy(form::toConfig)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Additional parameter must be given as name=value: =PL");
    }

    @Test
    public void toConfig__additionalParamGivenTwice() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setAdditionalParams("affiliation-country=PL\naffiliation-country=DE");

        // when & then
        assertThatThrownBy(form::toConfig)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Additional parameter given more than once: affiliation-country");
    }

    @Test
    public void toConfig__queryAmongAdditionalParams() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setAdditionalParams("query=publicationYear:2023");

        // when & then
        assertThatThrownBy(form::toConfig)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The query has its own field, it is not an additional parameter");
    }

    @Test
    public void toConfig__nothingToHarvest() {
        // given
        DataciteHarvesterConfigForm form = new DataciteHarvesterConfigForm();
        form.setQuery("  ");
        form.setDois("\n\n");

        // when & then
        assertThatThrownBy(form::toConfig)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing DOI's or query parameters");
    }

    // -------------------- PRIVATE --------------------

    private DataciteHarvesterConfig givenConfig() {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("query", "creators.affiliation.name:AGH");
        queryParams.put("affiliation-country", "PL");
        queryParams.put("client-id", "cern.zenodo");
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(queryParams);
        config.setDois(asList("10.5072/ABC", "10.5072/DEF"));
        config.setMaxRecords(20);
        return config;
    }
}
