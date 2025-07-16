package edu.harvard.iq.dataverse.validation.field.validators;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * Validator that checks whether field is not greater then the value defined
 * in validator configuration.<br>
 * Validator requires configuration parameter `value`. Value of the parameter
 * must contain a string that is parseable to number.<br>
 * Parameter `value` additionally supports macro `#NOW_YEAR` that will validate
 * the field value against the current year.
 * 
 * For example:
 * <pre>[{"name":"not_greater_than_value_validator", parameters:{"value": "200"}}]</pre>
 * will allow values: 199, 200 and do not allow value: 201<br>
 * <br>
 * <pre>[{"name":"not_greater_than_value_validator", parameters:{"value": "#NOW_YEAR"}}]</pre>
 * will allow value less then or equal to current year and will not allow years greater than current year
 * 
 * @author Krzysztof Mądry
 * @author Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class NotGreaterThanValueValidator extends FieldValidatorBase {

    public static final String VALUE_PARAM = "value";
    public static final String NOW_YEAR_MACRO = "#NOW_YEAR";

    @Override
    public String getName() {
        return "not_greater_than_value_validator";
    }

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        String value = field.getSingleValue();
        long valueLong = Long.parseLong(value);

        Object compareToValue = params.get(VALUE_PARAM);
        long compareToValueLong = parseValue((String) compareToValue);

        if (valueLong > compareToValueLong) {
            return FieldValidationResult.invalid(field, "isGreaterThanValue",
                    field.getDatasetFieldType().getDisplayName(),
                    String.valueOf(compareToValueLong));
        }

        return FieldValidationResult.ok();
    }

    private long parseValue(String value) {
        String macroResolvedValue = value.replace(NOW_YEAR_MACRO, Year.now(Clock.systemUTC()).toString());
        return Long.parseLong(macroResolvedValue);
    }
}
