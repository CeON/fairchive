package edu.harvard.iq.dataverse.persistence.config;

import org.apache.commons.validator.routines.EmailValidator;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author skraffmi
 */
public class EMailValidator implements ConstraintValidator<ValidateEmail, String> {

    // -------------------- LOGIC --------------------

    @Override
    public void initialize(ValidateEmail constraintAnnotation) { }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isEmailValid(value, context);
    }

    public static boolean isEmailValid(String value, ConstraintValidatorContext context) {
        boolean isValid = EmailValidator.getInstance().isValid(value);
        if (!isValid && context != null) {
            context.buildConstraintViolationWithTemplate(getStringFromBundle("email.invalid", value))
                    .addConstraintViolation();
        }
        return isValid;
    }
}
