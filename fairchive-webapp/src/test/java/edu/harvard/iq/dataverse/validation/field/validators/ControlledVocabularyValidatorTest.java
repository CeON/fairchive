package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledVocabularyValidatorTest {

    @Mock
    private ControlledVocabularyValueServiceBean vocabularyDao;

    @InjectMocks
    private ControlledVocabularyValidator validator;


    @Test
    void testValidateValue_success() {
        String testValue = "TestValue";
        ValidatableField testField = mock(ValidatableField.class);
        DatasetFieldType testFieldType = mock(DatasetFieldType.class);

        when(testField.getDatasetFieldType()).thenReturn(testFieldType);
        when(testFieldType.getName()).thenReturn("TestFieldType");

        ControlledVocabularyValue vocabularyValue = mock(ControlledVocabularyValue.class);
        when(vocabularyValue.getStrValue()).thenReturn(testValue);

        when(vocabularyDao.findByDatasetFieldTypeNameAndValueLike("TestFieldType", testValue, 1))
                .thenReturn(Collections.singletonList(vocabularyValue));

        FieldValidationResult result = validator.validateValue(
                testValue, testField, new HashMap<>(), new HashMap<>());

        assertThat(result.isOk()).isTrue();
    }

    @Test
    void testValidateValue_failure() {
        String testValue = "InvalidValue";
        ValidatableField testField = mock(ValidatableField.class);
        DatasetFieldType testFieldType = mock(DatasetFieldType.class);

        when(testField.getDatasetFieldType()).thenReturn(testFieldType);
        when(testFieldType.getName()).thenReturn("TestFieldType");

        when(vocabularyDao.findByDatasetFieldTypeNameAndValueLike("TestFieldType", testValue, 1))
                .thenReturn(Collections.emptyList());

        FieldValidationResult result = validator.validateValue(
                testValue, testField, new HashMap<>(), new HashMap<>());

        assertThat(result.isOk()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("validation.value.not.allowed.in.controlled.vocabulary");
        assertThat(result.getErrorArgs()).containsExactly("InvalidValue");
    }
}