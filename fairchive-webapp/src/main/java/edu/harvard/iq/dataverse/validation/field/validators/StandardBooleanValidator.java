package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardBooleanValidator extends MultiValueValidatorBase {
    public static final String ACCEPTABLE_VALUE_KEY = "acceptableValue";

    @Override
    public String getName() {
        return "standard_boolean";
    }

    @Override
    public FieldValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                               Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        Object acceptableValue = params.getOrDefault(ACCEPTABLE_VALUE_KEY, "true");
        if (!(acceptableValue instanceof String)) {
            return FieldValidationResult.ok();
        }
        boolean valueToValidateParsed = parseToBoolean(value);
        boolean acceptableValueParsed = parseToBoolean((String) acceptableValue);

        if (valueToValidateParsed == acceptableValueParsed) {
            return FieldValidationResult.ok();
        } else {
            return FieldValidationResult.invalid(field, "isNotAcceptable", value);
        }
    }

    public boolean parseToBoolean(String input) {
        return input != null && (
                input.equalsIgnoreCase("yes") ||
                        input.equalsIgnoreCase("true") ||
                        input.equalsIgnoreCase("y") ||
                        input.equals("1")
        );
    }
}
