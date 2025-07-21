package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;
import static org.assertj.core.api.Assertions.assertThat;

class GeoboxRectangleValueValidatorTest {

    private FieldValidator validator = new GeoboxRectangleValueValidator();

    @Test
    void validate__dataset_field() {
        // given
        String value = "22.531644 54.401347\n18.103104 51.289406";
        DatasetField datasetField = createField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result).isEqualTo(FieldValidationResult.ok());
    }

    @Test
    void validate__search_field() {
        // given
        String value = "20.8514 52.0976\n21.2715 52.0976\n21.2715 52.3700\n20.8514 52.3700";
        GeoboxCoordSearchField datasetField = createSearchField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result).isEqualTo(FieldValidationResult.ok());
    }

    @Test
    void validate__wrong_coordinates_count() {
        // given
        String value = "20.8514 52.0976\n21.2715 52.0976\n21.2715 52.3700";
        GeoboxCoordSearchField datasetField = createSearchField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.polygon.invalid.coordiantes.length");
    }

    @Test
    void validate__wrong_coordinates_count_duplicates() {
        // given
        String value = "20.8514 52.0976\n21.2715 52.0976\n21.2715 52.3700\n21.2715 52.3700";
        GeoboxCoordSearchField datasetField = createSearchField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.polygon.invalid.coordiantes.length");
    }

    @Test
    void validate__not_axis_aligned() {
        // given
        String value = "21.0 52.2707\n21.0707 52.2\n21.0 52.1293\n20.9293  52.2";
        GeoboxCoordSearchField datasetField = createSearchField(value);

        // when
        FieldValidationResult result = validator.validate(datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.getErrorCode()).isEqualTo("geobox.polygon.invalid.coordiantes.rectangle");
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

    private GeoboxCoordSearchField createSearchField(String value) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(
                geographicCoordinates, FieldType.TEXT, true);
        datasetFieldType.setTitle("geo");
        GeoboxCoordSearchField searchField = new GeoboxCoordSearchField(datasetFieldType);
        searchField.setFieldValue(value);
        return searchField;
    }
}