package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxTestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeoboxIndexUtilTest {
    private GeoboxIndexUtil geoboxIndexUtil = new GeoboxIndexUtil();
    private GeoboxTestUtil geoboxUtil = new GeoboxTestUtil();

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
}