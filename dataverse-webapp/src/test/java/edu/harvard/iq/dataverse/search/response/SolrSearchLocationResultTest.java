package edu.harvard.iq.dataverse.search.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolrSearchLocationResultTest {



    @Test
    public void calculateCenter__no_points() {
        // given
        List<GeoPoint> coordinates = new ArrayList<>();

        //when & then
        assertThrows(IllegalArgumentException.class, () ->
                new SolrSearchLocationResult(
                        "test",
                        "doi",
                        false,
                        coordinates,
                        null
                ));
    }

    @Test
    public void calculateCenter_one_point() {
        // given & when
        List<GeoPoint> coordinates = new ArrayList<>();
        coordinates.add(new GeoPoint(1,2));
        SolrSearchLocationResult result = new SolrSearchLocationResult(
                "test",
                "doi",
                false,
                coordinates,
                null
        );

        // then
        assertThat(result.getMarker()).isEqualTo(new GeoPoint(1, 2));
    }

    @Test
    public void calculateCenter_two_points() {
        // given & when
        List<GeoPoint> coordinates = new ArrayList<>();
        coordinates.add(new GeoPoint(1,2));
        coordinates.add(new GeoPoint(5,8));
        SolrSearchLocationResult result = new SolrSearchLocationResult(
                "test",
                "doi",
                false,
                coordinates,
                null
        );

        // then
        assertThat(result.getMarker()).isEqualTo(new GeoPoint(3, 5));
    }

    @Test
    public void calculateCenter_multiple_points() {
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
                coordinates,
                null
        );

        // then
        assertThat(result.getMarker()).isEqualTo(new GeoPoint(35.840585,36.19439));
    }
}