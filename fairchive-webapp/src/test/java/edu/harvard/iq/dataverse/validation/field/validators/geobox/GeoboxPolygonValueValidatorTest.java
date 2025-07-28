package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;

class GeoboxPolygonValueValidatorTest {

    private FieldValidator validator = new GeoboxPolygonValueValidator();

    @Test
    void validate() {
        // given
        String value = "22.531644 54.401347\n18.103104 51.289406";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result).isEqualTo(FieldValidationResult.ok());
    }

    @Test
    void validate__one_geo_point() {
        // given
        String value = "22.531644";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.polygon.invalid.geo.point");
    }

    @Test
    void validate__not_numerical_longitude() {
        // given
        String value = "abc 54.401347\n22.531644 xyz";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("isNotValidNumber");
        assertThat(result.getErrorArgs()).containsExactly("abc");
    }

    @Test
    void validate__not_numerical_latitude() {
        // given
        String value = "22.531644 xyz";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("isNotValidNumber");
        assertThat(result.getErrorArgs()).containsExactly("xyz");
    }

    @Test
    void validate__out_of_range_longitude() {
        // given
        String value = "190.000000 54.401347";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.invalid.longitude");
    }

    @Test
    void validate__longitude_span() {
        // given
        String value = "-20.000000 54.401347\n170.531644 12.213134";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.invalid.longitude.span");
    }

    @Test
    void validate__out_of_range_latitude() {
        // given
        String value = "22.531644 95.000000";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.invalid.latitude");
    }

    @Test
    void validate__intersecting_sides() {
        // given
        String value = "20.821362 49.053845\n" +
                "21.859114 48.793973\n" +
                "21.414363 48.485439\n" +
                "20.695075 48.481797\n" +
                "21.606539 49.186865\n" +
                "20.821362 49.053845";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("Self-intersecting polygons are not allowed.");
    }

    private DatasetField createField(String value) {
        DatasetField datasetField = new DatasetField();
        datasetField.setValue(value);

        DatasetFieldType datasetFieldType = new DatasetFieldType(
                geographicCoordinates, FieldType.TEXT, true);
        datasetFieldType.setTitle("geo");
        datasetField.setDatasetFieldType(datasetFieldType);
        return  datasetField;
    }
}