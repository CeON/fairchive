package edu.harvard.iq.dataverse.search.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolrSearchLocationResultTest {

    @Test
    public void solrSearchLocationResultTest__geo_shape() {
        // given & when
        List<GeoPoint> coordinates = new ArrayList<>();
        coordinates.add(new GeoPoint(21.82144,50.17739));
        coordinates.add(new GeoPoint(21.82144,22.21139));
        coordinates.add(new GeoPoint(49.85973,22.21139));
        coordinates.add(new GeoPoint(49.85973,50.17739));
        coordinates.add(new GeoPoint(21.82144,50.17739));
        SolrSearchLocationResult result = new SolrSearchLocationResult(
                "test",
                "doi",
                false,
                GeoShape.of(coordinates),
                null
        );

        // then
        assertThat(result.getCoordinates()).hasSize(5);
        assertThat(result.getMarker()).isEqualTo(new GeoPoint(35.840585,36.19439));
    }
}