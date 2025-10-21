package edu.harvard.iq.dataverse.passwordreset;

// based on http://howtodoinjava.com/2012/12/14/how-to-build-regex-based-password-validator-in-java/
/**
 * @todo perhaps replace with
 * https://code.google.com/p/vt-middleware/wiki/vtpassword
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator {

    private static PasswordValidator INSTANCE = new PasswordValidator();
    private static String pattern = null;

    /**
     * No one can make a direct instance
     */
    private PasswordValidator() {
        //do nothing
    }

    /**
     * Force the user to build a validator using this way only
     *
     * @param forceSpecialChar
     * @param forceCapitalLetter
     * @param forceNumber
     * @param minLength
     * @param maxLength
     * @return
     */
    public static PasswordValidator buildValidator(boolean forceSpecialChar,
                                                   boolean forceCapitalLetter,
                                                   boolean forceNumber,
                                                   int minLength,
                                                   int maxLength) {
        // [a-z] is why one letter is required
        final StringBuilder builder = new StringBuilder("((?=.*[a-z])");

        /**
         * @todo should probably allow additional special characters
         */
        if (forceSpecialChar) {
            builder.append("(?=.*[@#$%])");
        }

        if (forceCapitalLetter) {
            builder.append("(?=.*[A-Z])");
        }

        if (forceNumber) {
            builder.append("(?=.*\\d)");
        }

        builder.append(".{").append(minLength).append(',').append(maxLength).append("})");
        pattern = builder.toString();

        return INSTANCE;
    }

    /**
     * Here we will validate the password
     *
     * @param password
     */
    public boolean validatePassword(final String password) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(password);
        return m.matches();
    }

}
