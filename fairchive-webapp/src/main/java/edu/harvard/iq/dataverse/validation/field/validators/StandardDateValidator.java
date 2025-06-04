package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Eager
@ApplicationScoped
public class StandardDateValidator extends MultiValueValidatorBase {
    private static final String YYYY_MM_DD_FORMAT = "yyyy-MM-dd";
    private static final String YYYY_MM_FORMAT = "yyyy-MM";
    private static final String YYYY_FORMAT = "yyyy";

    private static final List<DateTimeFormatter> ALL_PARSERS = Initializer.initializeAllParsers();
    private static final DateTimeFormatter YEAR_ONLY_PARSER = Initializer.initializeYearOnlyParsers();

    private static final String RESTRICT_TO_YEAR_ONLY_KEY = "restrictToYearOnly";

    @Override
    public String getName() {
        return "standard_date";
    }

    @Override
    public FieldValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                               Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        value = value.startsWith("-") ? value.substring(1) : value;

        Object yearOnlyRestriction = params.getOrDefault(RESTRICT_TO_YEAR_ONLY_KEY, false);

        boolean restrictToYearOnly = Boolean.parseBoolean(yearOnlyRestriction.toString());
        if (restrictToYearOnly) {
            return validateOnlyYear(value, field);
        } else {
            return validateAllDateFormats(value, field);
        }
    }

    private FieldValidationResult validateAllDateFormats(String value, ValidatableField field) {
        for (DateTimeFormatter parser : ALL_PARSERS) {
            if (isValidDate(value, parser)) {
                return FieldValidationResult.ok();
            }
        }
        return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidDate",
                field.getDatasetFieldType().getDisplayName(), YYYY_MM_DD_FORMAT, YYYY_MM_FORMAT, YYYY_FORMAT));
    }

    private FieldValidationResult validateOnlyYear(String value, ValidatableField field) {
        if (isValidDate(value, YEAR_ONLY_PARSER)) {
            return FieldValidationResult.ok();
        }
        return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidYear",
                field.getDatasetFieldType().getDisplayName(), YYYY_FORMAT));
    }

    // -------------------- LOGIC --------------------

    private boolean isValidDate(String value, DateTimeFormatter parser) {
        try {
            TemporalAccessor parsed = parser.parse(value);
            if (parsed.isSupported(ChronoField.YEAR_OF_ERA)) {
                int year = parsed.get(ChronoField.YEAR_OF_ERA);
                if (year > 9999) {
                    return false;
                }
            }
        } catch (DateTimeException dte) {
            return false;
        }
        return true;
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        static List<DateTimeFormatter> initializeAllParsers() {
            return Stream.of(YYYY_MM_DD_FORMAT, YYYY_MM_FORMAT, YYYY_FORMAT)
                    .map(p -> new DateTimeFormatterBuilder()
                            .appendPattern(p)
                        // following defaults are required to parse or throw exceptions
                        // in the consistent way
                            .parseDefaulting(ChronoField.ERA, 1)
                            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                            .toFormatter()
                            .withResolverStyle(ResolverStyle.STRICT))
                    .collect(Collectors.toList());
        }

        static DateTimeFormatter initializeYearOnlyParsers() {
            return new DateTimeFormatterBuilder()
                    .appendPattern(YYYY_FORMAT)
                    .parseDefaulting(ChronoField.ERA, 1)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
        }
    }
}
