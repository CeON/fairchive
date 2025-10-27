package edu.harvard.iq.dataverse.validation.field.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class NotGreaterThanValidatorTest {

    private NotGreaterThanValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new NotGreaterThanValidator();
    }

    @ParameterizedTest
    @CsvSource({
        "1999, 2000",
        "2000.01, 2000.02",
    })
    public void validate__field_is_less_than_dependant_field(String fieldValue, String dependantValue) {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(DependantFieldValidator.DEPENDANT_FIELD_PARAM, "to");
        params.put(DependantFieldValidator.DEPENDANT_FIELD_IS_SIBLING, false);
        
        DatasetField field = buildSimpleField("from", fieldValue);
        DatasetField dependantField = buildSimpleField("to", dependantValue);

        Map<String, List<? extends ValidatableField>> fieldIndex = new HashMap<>();
        fieldIndex.put("from", Collections.singletonList(field));
        fieldIndex.put("to", Collections.singletonList(dependantField));

        // when
        FieldValidationResult result = validator.validate(field, params, fieldIndex);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "1999, 1999",
        "2000.01, 2000.01",
    })
    public void validate__field_is_equal_to_dependant_field(String fieldValue, String dependantValue) {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(DependantFieldValidator.DEPENDANT_FIELD_PARAM, "to");
        params.put(DependantFieldValidator.DEPENDANT_FIELD_IS_SIBLING, false);
        
        DatasetField field = buildSimpleField("from", fieldValue);
        DatasetField dependantField = buildSimpleField("to", dependantValue);

        Map<String, List<? extends ValidatableField>> fieldIndex = new HashMap<>();
        fieldIndex.put("from", Collections.singletonList(field));
        fieldIndex.put("to", Collections.singletonList(dependantField));

        // when
        FieldValidationResult result = validator.validate(field, params, fieldIndex);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__empty_dependant_field() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(DependantFieldValidator.DEPENDANT_FIELD_PARAM, "to");
        params.put(DependantFieldValidator.DEPENDANT_FIELD_IS_SIBLING, false);
        
        DatasetField field = buildSimpleField("from", "1999");
        DatasetField dependantField = buildSimpleField("to", "");

        Map<String, List<? extends ValidatableField>> fieldIndex = new HashMap<>();
        fieldIndex.put("from", Collections.singletonList(field));
        fieldIndex.put("to", Collections.singletonList(dependantField));

        // when
        FieldValidationResult result = validator.validate(field, params, fieldIndex);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void validate__no_dependant_field() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(DependantFieldValidator.DEPENDANT_FIELD_PARAM, "to");
        params.put(DependantFieldValidator.DEPENDANT_FIELD_IS_SIBLING, false);
        
        DatasetField field = buildSimpleField("from", "1999");

        Map<String, List<? extends ValidatableField>> fieldIndex = new HashMap<>();
        fieldIndex.put("from", Collections.singletonList(field));

        // when
        FieldValidationResult result = validator.validate(field, params, fieldIndex);

        // then
        assertThat(result.isOk()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "2000, 1999",
        "2000.02, 2000.01",
    })
    public void validate__field_is_greater_than_dependant_field(String fieldValue, String dependantValue) {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put(DependantFieldValidator.DEPENDANT_FIELD_PARAM, "to");
        params.put(DependantFieldValidator.DEPENDANT_FIELD_IS_SIBLING, false);
        
        DatasetField field = buildSimpleField("from", fieldValue);
        DatasetField dependantField = buildSimpleField("to", dependantValue);

        Map<String, List<? extends ValidatableField>> fieldIndex = new HashMap<>();
        fieldIndex.put("from", Collections.singletonList(field));
        fieldIndex.put("to", Collections.singletonList(dependantField));

        // when
        FieldValidationResult result = validator.validate(field, params, fieldIndex);

        // then
        assertThat(result.isOk()).isFalse();
    }

    private DatasetField buildSimpleField(String typeName, String value) {
        DatasetField datasetField = new DatasetField();
        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setName(typeName);
        datasetField.setDatasetFieldType(datasetFieldType);
        datasetField.setValue(value);
        return datasetField;
    }
}
