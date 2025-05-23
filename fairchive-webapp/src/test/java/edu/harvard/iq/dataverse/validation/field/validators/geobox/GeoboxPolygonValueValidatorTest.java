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
        assertThat(result.getMessage()).isEqualTo("Each line should have only two numbers");
    }

    @Test
    void validate__not_numerical_longitude() {
        // given
        String value = "abc 54.401347\n22.531644 xyz";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("abc is not a valid number.");
    }

    @Test
    void validate__not_numerical_latitude() {
        // given
        String value = "22.531644 xyz";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("xyz is not a valid number.");
    }

    @Test
    void validate__out_of_range_longitude() {
        // given
        String value = "190.000000 54.401347";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("The longitude must be a number between -180 and 180.");
    }

    @Test
    void validate__longitude_span() {
        // given
        String value = "-20.000000 54.401347\n170.531644 12.213134";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("The longitude cannot span more than 180 degrees.");
    }

    @Test
    void validate__out_of_range_latitude() {
        // given
        String value = "22.531644 95.000000";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getMessage()).isEqualTo("The latitude must be a number between -90 and 90.");
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