package edu.harvard.iq.dataverse.passwordreset;

/**
 * @todo do we really need a special exception at all?
 */
@SuppressWarnings("serial")
public class PasswordResetException extends Exception {

    public PasswordResetException(String message) {
        super(message);
    }

    public PasswordResetException(String message, Throwable cause) {
        super(message, cause);
    }

}
