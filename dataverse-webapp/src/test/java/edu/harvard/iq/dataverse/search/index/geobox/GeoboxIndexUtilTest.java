package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxTestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoboxIndexUtilTest {
    private GeoboxIndexUtil geoboxIndexUtil = new GeoboxIndexUtil();
    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

    @Test
    void geoboxFieldToSolr__noCut() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("1", "2", "3", "4");

        // when
        List<String> solrData = geoboxIndexUtil.geoboxFieldToSolr(field);

        // then
        assertThat(solrData).containsExactlyInAnyOrder("POLYGON((1 2,3 2,3 4,1 4,1 2))");
    }

    @Test
    void geoboxFieldToSolr__cut() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("-180", "-1", "180", "1");

        // when
        List<String> solrData = geoboxIndexUtil.geoboxFieldToSolr(field);

        // then
        assertThat(solrData).containsExactlyInAnyOrder(
                "POLYGON((-180 -1,0 -1,0 1,-180 1,-180 -1))",
                "POLYGON((0 -1,180 -1,180 1,0 1,0 -1))");
    }

    @Test
    void geoboxPolygonFieldToSolr__missing_dataset_field() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("-180", "-1", "180", "1");

        // when & then
        assertThatThrownBy(() -> geoboxIndexUtil.geoboxPolygonFieldToSolr(field))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing polygon dataset field");
    }

    @Test
    void geoboxPolygonFieldToSolr__one_point() {
        // given
        DatasetField field = geoboxUtil.buildPolygonGeobox("23.123123 12.321321");

        // when
        List<String> result = geoboxIndexUtil.geoboxPolygonFieldToSolr(field);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("POINT(23.123123 12.321321)");
    }

    @Test
    void geoboxPolygonFieldToSolr__line() {
        // given
        DatasetField field = geoboxUtil.buildPolygonGeobox("23.123123 12.321321\n 33.123123 44.123123");

        // when
        List<String> result = geoboxIndexUtil.geoboxPolygonFieldToSolr(field);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("LINESTRING(23.123123 12.321321,33.123123 44.123123)");
    }

    @Test
    void geoboxPolygonFieldToSolr__polygon() {
        // given
        DatasetField field = geoboxUtil.buildPolygonGeobox("23.123123 12.321321\n 33.123123 44.123123\n 55.123123 66.123123");

        // when
        List<String> result = geoboxIndexUtil.geoboxPolygonFieldToSolr(field);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("POLYGON((23.123123 12.321321,33.123123 44.123123,55.123123 66.123123))");
    }

    // We do not check another cases as they're covered by tests of geobox validators
    @Test
    void isIndexable__rejectInconsistentField() {
        // given
        DatasetField field = geoboxUtil.buildGeobox("1", "-11", "20", "11");
        field.getDatasetFieldsChildren().remove(0); // Now not all coordinates are present

        // when
        boolean result = geoboxIndexUtil.isIndexable(field);

        // then
        assertThat(result).isFalse();
    }
}