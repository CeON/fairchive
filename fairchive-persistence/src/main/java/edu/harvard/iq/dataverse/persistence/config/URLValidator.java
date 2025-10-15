package edu.harvard.iq.dataverse.persistence.config;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author skraffmi
 */
public class URLValidator implements ConstraintValidator<ValidateURL, String> {

    @Override
    public void initialize(ValidateURL constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        boolean valid = isURLValid(value);
        if (context != null && !valid) {
            context.buildConstraintViolationWithTemplate(value + "  " + 
        getStringFromBundle("url.invalid")).addConstraintViolation();
        }
        return valid;
    }

    public static boolean isURLValid(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

}
