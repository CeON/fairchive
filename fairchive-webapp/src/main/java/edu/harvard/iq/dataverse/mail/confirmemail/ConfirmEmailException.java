package edu.harvard.iq.dataverse.mail.confirmemail;

/**
 * @author bsilverstein
 */
@SuppressWarnings("serial")
public class ConfirmEmailException extends Exception {

    public ConfirmEmailException(String message) {
        super(message);
    }

    public ConfirmEmailException(String message, Throwable cause) {
        super(message, cause);
    }

}
