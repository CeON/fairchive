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
    
    private static final String FIELD_TYPE_NAME = "foo";
    private static final String FIELD_TYPE_TITLE = "FOO";

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

        DatasetField field = buildSimpleField("1999");

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

        DatasetField field = buildSimpleField("1999");

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

        DatasetField field = buildSimpleField("1999");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotLessThanOrEqualToValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1998");
    }

    @Test
    public void validate__field_is_less_than_current_year_max() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).minusYears(1).toString());

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

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).toString());

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

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).plusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotLessThanOrEqualToValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, Year.now(Clock.systemUTC()).toString());
    }

    @Test
    public void validate__field_is_greater_than_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");

        DatasetField field = buildSimpleField("2000");

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

        DatasetField field = buildSimpleField("1999");

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

        DatasetField field = buildSimpleField("1998");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotGreaterThanOrEqualToValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1999");
    }

    @Test
    public void validate__field_is_greater_than_current_year_min() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "#NOW_YEAR");

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).plusYears(1).toString());

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

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).toString());

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

        DatasetField field = buildSimpleField(Year.now(Clock.systemUTC()).minusYears(1).toString());

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotGreaterThanOrEqualToValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, Year.now(Clock.systemUTC()).toString());
    }

    @Test
    public void validate__field_is_within_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");
        params.put(RangeValidator.MAX_PARAM, "2001");

        DatasetField field = buildSimpleField("2000");

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

        DatasetField field = buildSimpleField("2002");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValuesBothBoundsInclusive");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1999", "2001");
    }

    @Test
    public void validate__field_is_below_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1999");
        params.put(RangeValidator.MAX_PARAM, "2001");

        DatasetField field = buildSimpleField("1998");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValuesBothBoundsInclusive");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1999", "2001");
    }

    @Test
    public void validate__decimal_within_inclusive_range() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "0.5");
        params.put(RangeValidator.MAX_PARAM, "10.5");

        DatasetField field = buildSimpleField("3.14");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__decimal_equal_to_min_inclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1.23");

        DatasetField field = buildSimpleField("1.23");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__decimal_equal_to_max_inclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "4.56");

        DatasetField field = buildSimpleField("4.56");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__equal_to_min_when_min_exclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "5.0");
        params.put("min_exclusive", true);

        DatasetField field = buildSimpleField("5.0");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotGreaterThanValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "5.0");
    }

    @Test
    public void validate__equal_to_max_when_max_exclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MAX_PARAM, "7.7");
        params.put("max_exclusive", true);

        DatasetField field = buildSimpleField("7.7");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotLessThanValue");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "7.7");
    }

    @Test
    public void validate__equal_to_bounds_when_both_exclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1.0");
        params.put(RangeValidator.MAX_PARAM, "2.0");
        params.put("min_exclusive", true);
        params.put("max_exclusive", true);

        DatasetField field = buildSimpleField("1.0");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValuesBothBoundsExclusive");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1.0", "2.0");
    }

    @Test
    public void validate__not_in_range_max_exclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1.0");
        params.put(RangeValidator.MAX_PARAM, "2.0");
        params.put("min_exclusive", false);
        params.put("max_exclusive", true);

        DatasetField field = buildSimpleField("2.1");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValuesLowerBoundInclusive");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1.0", "2.0");
    }

    @Test
    public void validate__not_in_range_min_exclusive() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(RangeValidator.MIN_PARAM, "1.0");
        params.put(RangeValidator.MAX_PARAM, "2.0");
        params.put("min_exclusive", true);
        params.put("max_exclusive", false);

        DatasetField field = buildSimpleField("2.1");

        // when
        FieldValidationResult result = validator.validate(field, params, null);

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("isNotBetweenValuesUpperBoundInclusive");
        assertThat(result.getErrorArgs()).containsExactly(FIELD_TYPE_TITLE, "1.0", "2.0");
    }
    
    private DatasetField buildSimpleField(String value) {
        DatasetField datasetField = new DatasetField();
        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setName(FIELD_TYPE_NAME);
        datasetFieldType.setTitle(FIELD_TYPE_TITLE);
        datasetField.setDatasetFieldType(datasetFieldType);
        datasetField.setValue(value);
        return datasetField;
    }
}
