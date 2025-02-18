package edu.harvard.iq.dataverse.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetLocationSolrFieldsTest {

    // -------------------- TESTS --------------------

    @Test
    public void construct_dataset_location__empty_custom_prefix() {

        // when
        DatasetLocationSolrFields locationSolrFields = new DatasetLocationSolrFields(null);

        // then
        assertThat(locationSolrFields.getEast()).isEqualTo("dsf_txt_eastLongitude");
        assertThat(locationSolrFields.getNorth()).isEqualTo("dsf_txt_northLongitude");
        assertThat(locationSolrFields.getWest()).isEqualTo("dsf_txt_westLongitude");
        assertThat(locationSolrFields.getSouth()).isEqualTo("dsf_txt_southLongitude");
    }

    @Test
    public void construct_dataset_location__custom_prefix() {

        // when
        DatasetLocationSolrFields locationSolrFields = new DatasetLocationSolrFields("custom_prefix");

        // then
        assertThat(locationSolrFields.getEast()).isEqualTo("dsf_txt_custom_prefix_eastLongitude");
        assertThat(locationSolrFields.getNorth()).isEqualTo("dsf_txt_custom_prefix_northLongitude");
        assertThat(locationSolrFields.getWest()).isEqualTo("dsf_txt_custom_prefix_westLongitude");
        assertThat(locationSolrFields.getSouth()).isEqualTo("dsf_txt_custom_prefix_southLongitude");
    }
}