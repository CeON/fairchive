package edu.harvard.iq.dataverse.validation;

import javax.ejb.Stateless;

import static edu.harvard.iq.dataverse.validation.ValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.ValidationResult.ok;
import static java.util.Arrays.binarySearch;
import static java.util.regex.Pattern.compile;

import java.util.regex.Pattern;

@Stateless
public class RorValidator {
    
    private static final char[] SYMBOL_VALUES = 
        {'0','1','2','3','4','5','6','7','8','9',
        'a','b','c','d','e','f','g','h','j','k','m','n','p','q','r','s','t','v','w','x','y','z'};
    private static final Pattern FORMAT_PATTERN = 
            compile("https://ror\\.org/0[a-hjkmnp-tv-z0-9]{6}[0-9]{2}");

    public ValidationResult validate(final String fullRor) {
        if (fullRor != null && FORMAT_PATTERN.matcher(fullRor).matches()) {
            return isChecksumValid(fullRor)
                    ? ok()
                    : invalid("ror.invalid.checksum");
        } else {
            return invalid("ror.invalid.format");
        }
    }

    private static boolean isChecksumValid(final String fullRor) {
        int index = fullRor.length() - 9;
        final int end = index + 7;
        long decoded = 0;
        while (index < end) {
            decoded = 32L * decoded + binarySearch(SYMBOL_VALUES, fullRor.charAt(index));
            ++index;
        }
        decoded = 98L - ((decoded * 100L) % 97);
        
        final long checksum = 10*(fullRor.charAt(index) - '0') +
                (fullRor.charAt(++index) - '0');
        return decoded == checksum;
    }
}
