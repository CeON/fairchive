package edu.harvard.iq.dataverse.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetLocationSolrFieldsTest {

    // -------------------- TESTS --------------------

    @Test
    public void construct_dataset_location() {

        // when
        DatasetLocationSolrFields locationSolrFields = new DatasetLocationSolrFields();

        // then
        assertThat(locationSolrFields.getEast()).isEqualTo("dsf_txt_eastLongitude");
        assertThat(locationSolrFields.getNorth()).isEqualTo("dsf_txt_northLongitude");
        assertThat(locationSolrFields.getWest()).isEqualTo("dsf_txt_westLongitude");
        assertThat(locationSolrFields.getSouth()).isEqualTo("dsf_txt_southLongitude");
    }
}