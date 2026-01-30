/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.user;

import static java.util.regex.Pattern.matches;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author sarahferry
 * Modeled after PasswordValidator and EMailValidator
 */

public class UserNameValidator implements ConstraintValidator<ValidateUserName, String> {
    @Override
    public void initialize(final ValidateUserName constraintAnnotation) {

    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return isUserNameValid(value, context);
    }

    /**
     * Here we will validate the username
     *
     * @param username
     * @return boolean
     */
    public static boolean isUserNameValid(final String username, 
    		final ConstraintValidatorContext context) {
        return username != null 
        		? matches("[a-zA-Z0-9\\_\\-\\.]{2,60}", username) 
        		: false;
    }
}



