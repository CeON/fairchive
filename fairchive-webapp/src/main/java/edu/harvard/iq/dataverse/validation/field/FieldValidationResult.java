package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.ValidationResult;

public class FieldValidationResult extends ValidationResult {

    public static FieldValidationResult OK = new FieldValidationResult(true, null, null);

    private final ValidatableField field;
    private final Object[] errorArgs;

    // -------------------- CONSTRUCTORS --------------------

    private FieldValidationResult(boolean ok, String errorCode, ValidatableField field, Object...errorArgs) {
        super(ok, errorCode);
        this.field = field;
        this.errorArgs = errorArgs;
    }

    // -------------------- GETTERS --------------------

    public ValidatableField getField() {
        return field;
    }

    public Object[] getErrorArgs() {
        return errorArgs;
    }

    // -------------------- LOGIC --------------------

    public static FieldValidationResult ok() {
        return OK;
    }

    public static FieldValidationResult invalid(ValidatableField field, String errorCode, Object... errorArgs) {
        return new FieldValidationResult(false, errorCode, field, errorArgs);
    }

}
