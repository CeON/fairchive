package edu.harvard.iq.dataverse.validation.field.validators;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorIdType;
import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.ok;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * Base class for author identifier validators. Implementing validators are
 * expected to be configured with an `authorIdentifierScheme` parameter
 * indicating for which type of identifier it should be applied. Validation is
 * omitted for other types.
 */
public abstract class AuthorIdentifierValidator extends MultiValueValidatorBase {

    public static final String IDENTIFIER_SCHEME_PARAM = "authorIdentifierScheme";
        public static final String AUTHOR_TYPE_FIELD = "authorTypeField";

    // -------------------- LOGIC --------------------

    @Override
    public FieldValidationResult validateValue(String value, ValidatableField field,
            Map<String, Object> params,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        if (isSelectedIdentifierScheme(field, params)) {
            return validateIdentifier(value, field, params, fieldIndex);
        } else {
            return ok();
        }
    }

    protected abstract FieldValidationResult validateIdentifier(String identifier,
            ValidatableField field,
            Map<String, Object> params,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex);

    // -------------------- PRIVATE ---------------------

    private static boolean isSelectedIdentifierScheme(final ValidatableField field,
            final Map<String, Object> params) {

        final Object idType = params.get(IDENTIFIER_SCHEME_PARAM);
        final String authorTypeField = (String)params.getOrDefault(AUTHOR_TYPE_FIELD, authorIdType);

        return field.getFirstSiblingOfType(authorTypeField)
                .map(ValidatableField::getSingleValue)
                .filter(Objects::nonNull)
                .filter(value -> value.equals(idType))
                .isDefined();
    }
}
