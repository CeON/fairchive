package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.ValidationResult;

public class FieldValidationResult extends ValidationResult {

    private final static FieldValidationResult OK = new FieldValidationResult(true, null, null);

    private final ValidatableField field;
    private final Object[] errorArgs;

    // -------------------- CONSTRUCTORS --------------------

    private FieldValidationResult(final boolean ok, final String errorCode, 
            final ValidatableField field, final Object...errorArgs) {
        super(ok, errorCode);
        this.field = field;
        this.errorArgs = errorArgs;
    }

    // -------------------- GETTERS --------------------

    public ValidatableField getField() {
        return this.field;
    }

    public Object[] getErrorArgs() {
        return this.errorArgs;
    }

    // -------------------- LOGIC --------------------

    public static FieldValidationResult ok() {
        return OK;
    }

    public static FieldValidationResult invalid(final ValidatableField field, 
            final String errorCode, final Object... errorArgs) {
        return new FieldValidationResult(false, errorCode, field, errorArgs);
    }

}
