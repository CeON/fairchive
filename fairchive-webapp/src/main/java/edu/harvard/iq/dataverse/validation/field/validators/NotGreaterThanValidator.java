package edu.harvard.iq.dataverse.validation.field.validators;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * Validator that checks whether a field is not greater than dependant field.
 * 
 * @author Krzysztof Mądry
 * @author Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class NotGreaterThanValidator extends DependantFieldValidator {

    @Override
    public String getName() {
        return "not_greater_than_validator";
    }

    @Override
    protected FieldValidationResult validateWithDependantField(ValidatableField field, ValidatableField dependantField,
            Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        
        String value = field.getSingleValue();
        long valueLong = Long.parseLong(value);
        
        String compareToValue = dependantField.getSingleValue();
        if (StringUtils.isBlank(compareToValue)) {
            return FieldValidationResult.ok();
        }
        long compareToValueLong = Long.parseLong(compareToValue);

        if (valueLong > compareToValueLong) {
            return FieldValidationResult.invalid(field, "isGreaterThanField",
                    field.getDatasetFieldType().getDisplayName(),
                    dependantField.getDatasetFieldType().getDisplayName());
        }

        return FieldValidationResult.ok();
    }

}
