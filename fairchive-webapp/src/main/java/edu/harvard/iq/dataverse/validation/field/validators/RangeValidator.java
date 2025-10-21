package edu.harvard.iq.dataverse.validation.field.validators;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static java.lang.Long.parseLong;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * Validator that checks whether a field value is within the range defined
 * in validator's configuration.<br>
 * Validator accepts configuration parameters `min` and `max`. Values of these
 * parameters must contain a string that is parseable to `long`. Both range ends
 * are optional, and if none is provided, the validator will accept any value<br>
 * Both `min` and `max` additionally support the macro `#NOW_YEAR` that will
 * validate the field value against the current year.
 *
 * For example:
 * <pre>[{"name":"range_validator", parameters:{"min": 199, "max": "200"}}]</pre>
 * will allow values: 199, 200 and will not allow values: 198, 201<br>
 * <br>
 * <pre>[{"name":"range_validator", parameters:{"max": "#NOW_YEAR"}}]</pre>
 * will allow value less than or equal to the current year and will not allow
 * years greater than the current year<br>
 * <br>
 *
 * @author Krzysztof Mądry
 * @author Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class RangeValidator extends FieldValidatorBase {

    static final String MIN_PARAM = "min";
    static final String MAX_PARAM = "max";
    private static final String NOW_YEAR_MACRO = "#NOW_YEAR";

    @Override
    public String getName() {
        return "range_validator";
    }

    @Override
    public FieldValidationResult validate(final ValidatableField field, 
            final Map<String, Object> params,
            final Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        String value = field.getSingleValue();
        long valueLong = parseLong(value);

        String minParam = (String) params.get(MIN_PARAM);
        String maxParam = (String) params.get(MAX_PARAM);

        if (isBlank(minParam) && isBlank(maxParam)) {
            return FieldValidationResult.ok();
        }

        Long minValue = minParam != null ? parseValue(minParam) : null;
        Long maxValue = maxParam != null ? parseValue(maxParam) : null;

        boolean belowMin = minValue != null && valueLong < minValue;
        boolean aboveMax = maxValue != null && valueLong > maxValue;

        if (belowMin || aboveMax) {
            return buildValidationResult(field, minValue, maxValue);
        }

        return FieldValidationResult.ok();
    }

    private long parseValue(final String value) {
        return parseLong(value.replace(NOW_YEAR_MACRO, 
                Year.now(Clock.systemUTC()).toString()));
    }

    private FieldValidationResult buildValidationResult(final ValidatableField field, 
            final Long min, final Long max) {
        if (min == null && max == null) {
            throw new IllegalArgumentException("Both min and max cannot be null");
        }

        final String displayName = field.getDatasetFieldType().getDisplayName();
        if (min == null) {
            return invalid(field, "isGreaterThanValue", displayName, max.toString());
        }
        if (max == null) {
            return invalid(field, "isLessThanValue", displayName, min.toString());
        }

        return invalid(field, "isNotBetweenValues", displayName, min.toString(), 
                max.toString());
    }
}
