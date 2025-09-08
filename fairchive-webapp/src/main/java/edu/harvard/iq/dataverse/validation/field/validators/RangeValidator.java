package edu.harvard.iq.dataverse.validation.field.validators;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringUtils;
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

    public static final String MIN_PARAM = "min";
    public static final String MAX_PARAM = "max";
    public static final String NOW_YEAR_MACRO = "#NOW_YEAR";

    @Override
    public String getName() {
        return "range_validator";
    }

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params,
                                          Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        String value = field.getSingleValue();
        long valueLong = Long.parseLong(value);

        String minParam = (String) params.get(MIN_PARAM);
        String maxParam = (String) params.get(MAX_PARAM);

        if (StringUtils.isBlank(minParam) && StringUtils.isBlank(maxParam)) {
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

    private long parseValue(String value) {
        String macroResolvedValue = value.replace(NOW_YEAR_MACRO, Year.now(Clock.systemUTC()).toString());
        return Long.parseLong(macroResolvedValue);
    }

    private FieldValidationResult buildValidationResult(ValidatableField field, Long min, Long max) {
        if (min == null && max == null) {
            throw new IllegalArgumentException("Both min and max cannot be null");
        }

        String displayName = field.getDatasetFieldType().getDisplayName();
        if (min == null) {
            return FieldValidationResult.invalid(field, "isGreaterThanValue", displayName, max.toString());
        }
        if (max == null) {
            return FieldValidationResult.invalid(field, "isLessThanValue", displayName, min.toString());
        }

        return FieldValidationResult.invalid(field, "isNotBetweenValues", displayName, min.toString(), max.toString());
    }
}
