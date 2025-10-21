package edu.harvard.iq.dataverse.validation;

public class ValidationResult {

    private final static ValidationResult OK = new ValidationResult(true, null);

    private final boolean ok;

    private final String errorCode;

    // -------------------- CONSTRUCTORS --------------------

    protected ValidationResult(final boolean ok, final String errorCode) {
        this.ok = ok;
        this.errorCode = errorCode;
    }

    // -------------------- GETTERS --------------------

    public String getErrorCode() {
        return this.errorCode;
    }

    // -------------------- LOGIC --------------------

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult invalid(final String errorCode) {
        return new ValidationResult(false, errorCode);
    }

    public boolean isOk() {
        return this.ok;
    }
}
