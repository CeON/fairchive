package edu.harvard.iq.dataverse.validation.field.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

public class NotGreaterThanValueValidatorTest {

    private NotGreaterThanValueValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new NotGreaterThanValueValidator();
    }

    @Test
    public void validate__field_is_less_than_value() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "2000");
        
        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_value() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "1999");
        
        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_greater_than_value() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "1998");
        
        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getMessage()).contains("FROM");
        assertThat(result.getMessage()).contains("1998");
    }

    @Test
    public void validate__field_is_less_than_current_year() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).minusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_current_year() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_greater_than_current_year() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(NotGreaterThanValueValidator.VALUE_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).plusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getMessage()).contains("FROM");
        assertThat(result.getMessage()).contains(Year.now(Clock.systemUTC()).toString());
    }

    private DatasetField buildSimpleField(String typeName, String value) {
        DatasetField datasetField = new DatasetField();
        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setName(typeName);
        datasetFieldType.setTitle(typeName.toUpperCase());
        datasetField.setDatasetFieldType(datasetFieldType);
        datasetField.setValue(value);
        return datasetField;
    }
}
