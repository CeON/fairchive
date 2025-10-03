package edu.harvard.iq.dataverse.validation;

import javax.ejb.Stateless;

import static edu.harvard.iq.dataverse.validation.ValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.ValidationResult.ok;
import static java.lang.Long.parseLong;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class RorValidator {
    
    public static final String INVALID_FORMAT = "ror.invalid.format";
    public static final String INVALID_CHECKSUM = "ror.invalid.checksum";
    private static final Map<String, Integer> SYMBOL_VALUES = symbolValues();
    private static final Pattern FORMAT_PATTERN = 
            compile("https://ror\\.org/0[a-hjkmnp-tv-z0-9]{6}[0-9]{2}");

    // -------------------- LOGIC --------------------

    public ValidationResult validate(final String fullRor) {
        if (isBlank(fullRor)) {
            return invalid(INVALID_FORMAT);
        }
        final Matcher matcher = FORMAT_PATTERN.matcher(fullRor);
        if (!matcher.matches()) {
            return invalid(INVALID_FORMAT);
        }
        final String value = fullRor.substring(fullRor.lastIndexOf("/") + 1);
        final String encoded = value.substring(0, 7);
        final long checksum = parseLong(value.substring(7));
        return checksum == computeChecksum(encoded)
                ? ok()
                : invalid(INVALID_CHECKSUM);
    }

    // -------------------- PRIVATE --------------------

    private static long computeChecksum(final String encoded) {
        long decoded = stream(encoded.split(""))
                .mapToLong(SYMBOL_VALUES::get)
                .reduce(0L, (accumulated, element) -> 32L * accumulated + element);
        return 98L - ((decoded * 100L) % 97);
    }

    private static Map<String, Integer> symbolValues() {
        final String symbols = "0123456789abcdefghjkmnpqrstvwxyz";
        return unmodifiableMap(
                stream(symbols.split(""))
                        .collect(toMap(Function.identity(), symbols::indexOf)));
    }
}
