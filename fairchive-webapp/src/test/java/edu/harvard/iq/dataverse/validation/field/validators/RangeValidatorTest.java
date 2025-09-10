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

public class RangeValidatorTest {

    private RangeValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new RangeValidator();
    }

    @Test
    public void validate__field_is_less_than_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "2000");

        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "1999");

        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_greater_than_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "1998");

        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isGreaterThanValue");
        assertThat(result.getErrorArgs()).containsExactly("FROM", "1998");
    }

    @Test
    public void validate__field_is_less_than_current_year_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).minusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_current_year_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_greater_than_current_year_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).plusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isGreaterThanValue");
        assertThat(result.getErrorArgs()).containsExactly("FROM", Year.now(Clock.systemUTC()).toString());
    }

    @Test
    public void validate__field_is_greater_than_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");

        DatasetField field = buildSimpleField("from", "2000");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");

        DatasetField field = buildSimpleField("from", "1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_less_than_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");

        DatasetField field = buildSimpleField("from", "1998");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isLessThanValue");
        assertThat(result.getErrorArgs()).containsExactly("FROM", "1999");
    }

    @Test
    public void validate__field_is_greater_than_current_year_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).plusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_equal_to_current_year_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_less_than_current_year_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField("from", Year.now(Clock.systemUTC()).minusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isLessThanValue");
        assertThat(result.getErrorArgs()).containsExactly("FROM", Year.now(Clock.systemUTC()).toString());
    }

    @Test
    public void validate__field_is_within_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");
        params.put(RangeValidator.MAX_PARAM, "2001");

        DatasetField field = buildSimpleField("from", "2000");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__field_is_above_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");
        params.put(RangeValidator.MAX_PARAM, "2001");

        DatasetField field = buildSimpleField("from", "2002");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValues");
        assertThat(result.getErrorArgs()).containsExactly("FROM", "1999", "2001");
    }

    @Test
    public void validate__field_is_below_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");
        params.put(RangeValidator.MAX_PARAM, "2001");

        DatasetField field = buildSimpleField("from", "1998");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValues");
        assertThat(result.getErrorArgs()).containsExactly("FROM", "1999", "2001");
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
