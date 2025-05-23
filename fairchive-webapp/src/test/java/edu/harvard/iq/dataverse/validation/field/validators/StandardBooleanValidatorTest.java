package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StandardBooleanValidatorTest {

    private StandardBooleanValidator validator = new StandardBooleanValidator();

    @ParameterizedTest
    @CsvSource({
            "yes, yes, true",
            "yes, true, true",
            "true, true, true",
            "1, yes, true",
            "0, yes, false",
            "no, no, true",
            "false, false, true",
            "true, false, false",
            "unknown, true, false",
            "yes, , true",
            "true, , true",
            "1, , true",
            "0, , false",
            "no, , false",
            "false, , false"
    })
    void validate(String value, String acceptableValue, boolean expectedResult) {
        // given
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(value);

        Map<String, Object> params = acceptableValue != null
                ? Collections.singletonMap(StandardBooleanValidator.ACCEPTABLE_VALUE_KEY, acceptableValue)
                : Collections.emptyMap();

        // when
        FieldValidationResult result = validator.validate(datasetField, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
    }
}
