package edu.harvard.iq.dataverse.persistence.datafile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DataFileTagTest {

    @Test
    public void listTags_works() {

        assertThat(DataFileTag.listTags()).containsExactlyInAnyOrder("Survey", "Time Series", "Panel",
                "Event", "Genomics", "Network", "Geospatial");
    }
}
