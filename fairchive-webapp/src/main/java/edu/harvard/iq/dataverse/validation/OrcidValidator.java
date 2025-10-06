package edu.harvard.iq.dataverse.validation;

import javax.ejb.Stateless;

import static edu.harvard.iq.dataverse.validation.ValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.ValidationResult.ok;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class OrcidValidator {

    public static final String INVALID_FORMAT = "orcid.invalid.format";
    public static final String INVALID_CHECKSUM = "orcid.invalid.checksum";

    private static final Pattern FORMAT_PATTERN = 
            compile("([0-9]{4})-([0-9]{4})-([0-9]{4})-([0-9]{3})([0-9X])");

    // -------------------- LOGIC --------------------

    public ValidationResult validate(final String orcid) {
        if (isBlank(orcid)) {
            return invalid(INVALID_FORMAT);
        }
        final Matcher matcher = FORMAT_PATTERN.matcher(orcid);
        if (!matcher.matches()) {
            return invalid(INVALID_FORMAT);
        }
        final String encoded = matcher.group(1) + matcher.group(2) + 
                               matcher.group(3) + matcher.group(4);
        final String checksum = matcher.group(5);
        return checksum.equals(computeChecksum(encoded))
                ? ok()
                : invalid(INVALID_CHECKSUM);
    }

    // -------------------- PRIVATE --------------------

    public String computeChecksum(final String baseDigits) {    
        final int total = stream(baseDigits.split(""))
                .mapToInt(Integer::parseInt)
                .reduce(0, (accumulated, digit) -> (accumulated + digit) * 2);
        final int result = (12 - total % 11) % 11;
        return result == 10 ? "X" : String.valueOf(result);
    }
}
