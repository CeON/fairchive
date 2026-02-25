package edu.harvard.iq.dataverse.validation.field.validators;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.math.BigDecimal;
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
 * parameters must contain a string that is parseable to a number. Both range ends
 * are optional, and if none is provided, the validator will accept any value.<br>
 * Both `min` and `max` additionally support the macro `#NOW_YEAR` that will
 * validate the field value against the current year.
 * <br>
 * Additionally, parameters `min_exclusive` and `max_exclusive` allow configuring
 * exclusivity of the lower and upper bound respectively. If not provided, bounds
 * are inclusive by default.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>
 *         <pre>[{"name":"range_validator", parameters:{"min": 199, "max": "200"}}]</pre>
 *         will allow values: 199, 200 and will not allow values: 198, 201
 *     </li>
 *     <li>
 *         <pre>[{"name":"range_validator", parameters:{"min": 0, "max": "1", "min_exclusive": "true"}}]</pre>
 *         will allow values: 0.1, 1.0 and will not allow values: -0.1, 0, 1.1
 *     </li>
 *     <li>
 *         <pre>[{"name":"range_validator", parameters:{"max": "#NOW_YEAR"}}]</pre>
 *         will allow value less than or equal to the current year and will not
 *         allow years greater than the current year
 *     </li>
 *     <li>
 *         <pre>[{"name":"range_validator", parameters:{"max": "#NOW_YEAR", "max_exclusive": "true"}}]</pre>
 *         will allow value less than to the current year and will not allow
 *         years equal to or greater than the current year
 *     </li>
 * </ul>
 * </p>
 *
 * @author Krzysztof Mądry
 * @author Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class RangeValidator extends FieldValidatorBase {

    static final String MIN_PARAM = "min";
    static final String MAX_PARAM = "max";
    static final String MIN_EXCLUSIVE_PARAM = "min_exclusive";
    static final String MAX_EXCLUSIVE_PARAM = "max_exclusive";
    private static final String NOW_YEAR_MACRO = "#NOW_YEAR";

    @Override
    public String getName() {
        return "range_validator";
    }

    @Override
    public FieldValidationResult validate(final ValidatableField field, 
            final Map<String, Object> params,
            final Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        BigDecimal valueNumber = new BigDecimal(field.getSingleValue());

        Object minParamObj = params.get(MIN_PARAM);
        Object maxParamObj = params.get(MAX_PARAM);

        String minParam = minParamObj != null ? String.valueOf(minParamObj) : null;
        String maxParam = maxParamObj != null ? String.valueOf(maxParamObj) : null;

        if (isBlank(minParam) && isBlank(maxParam)) {
            return FieldValidationResult.ok();
        }

        BigDecimal minValue = minParam != null ? parseValue(minParam) : null;
        BigDecimal maxValue = maxParam != null ? parseValue(maxParam) : null;

        boolean minExclusive = parseBoolean(params.get(MIN_EXCLUSIVE_PARAM));
        boolean maxExclusive = parseBoolean(params.get(MAX_EXCLUSIVE_PARAM));

        boolean belowMin = false;
        if (minValue != null) {
            int cmp = valueNumber.compareTo(minValue);
            belowMin = minExclusive ? (cmp <= 0) : (cmp < 0);
        }

        boolean aboveMax = false;
        if (maxValue != null) {
            int cmp = valueNumber.compareTo(maxValue);
            aboveMax = maxExclusive ? (cmp >= 0) : (cmp > 0);
        }

        if (belowMin || aboveMax) {
            return buildValidationResult(field, minValue, maxValue, minExclusive, maxExclusive);
        }

        return FieldValidationResult.ok();
    }

    private BigDecimal parseValue(final String value) {
        return new BigDecimal(value.replace(NOW_YEAR_MACRO,
                                            Year.now(Clock.systemUTC()).toString()));
    }

    private boolean parseBoolean(final Object param) {
        return param != null && Boolean.parseBoolean(String.valueOf(param));
    }

    private FieldValidationResult buildValidationResult(final ValidatableField field, 
            final BigDecimal min, final BigDecimal max,
            final boolean minExclusive, final boolean maxExclusive) {
        if (min == null && max == null) {
            throw new IllegalArgumentException("Both min and max cannot be null");
        }

        final String displayName = field.getDatasetFieldType().getDisplayName();
        if (min == null) {
            final String errorCode = maxExclusive ? "isNotLessThanValue" : "isNotLessThanOrEqualToValue";
            return invalid(field, errorCode, displayName, max.toString());
        }
        if (max == null) {
            final String errorCode = minExclusive ? "isNotGreaterThanValue" : "isNotGreaterThanOrEqualToValue";
            return invalid(field, errorCode, displayName, min.toString());
        }

        final String errorCode = getErrorCodeForBothBounds(minExclusive, maxExclusive);
        return invalid(field, errorCode, displayName, min.toString(),
                max.toString());
    }

    private String getErrorCodeForBothBounds(final boolean minExclusive, final boolean maxExclusive) {
        if (minExclusive && maxExclusive) {
            return "isNotBetweenValuesBothBoundsExclusive";
        } else if (minExclusive) {
            return "isNotBetweenValuesUpperBoundInclusive";
        } else if (maxExclusive) {
            return "isNotBetweenValuesLowerBoundInclusive";
        } else {
            return "isNotBetweenValuesBothBoundsInclusive";
        }
    }
}
