package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.control.Option;

import java.util.List;
import java.util.Map;

/**
 * Base class for validators on dependant fields. Implementing validators are
 * expected to be configured with an `dependantField` parameter indicating for
 * which field it should be applied.<br>
 * Configuration for implementing validators can contain `dependantIsSibling`
 * parameter. If value for the parameter is true (default) then dependant field
 * must be a sibling of the field (have the same parent). In this case parent
 * field can be multivalued but dependant field can not be multivalued. If the
 * value for the parameter is false then dependant field can be any field.
 * There is a limitation in such case that dependant field and its parents
 * can not be a multivalued fields.
 * 
 * @author Filipe Dias Lewandowski
 * @author Krzysztof Mądry
 * @author Sylwester Niewczas
 */
public abstract class DependantFieldValidator extends FieldValidatorBase {

    public static final String DEPENDANT_FIELD_PARAM = "dependantField";
    public static final String DEPENDANT_FIELD_IS_SIBLING = "dependantIsSibling";

    // -------------------- LOGIC --------------------

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params,
                                   Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        Object dependantTypeName = params.get(DEPENDANT_FIELD_PARAM);
        if (!(dependantTypeName instanceof String)) {
            return FieldValidationResult.ok();
        }
        Object dependantIsSibling = params.getOrDefault(DEPENDANT_FIELD_IS_SIBLING, true);
        if (!(dependantIsSibling instanceof Boolean)) {
            return FieldValidationResult.ok();
        }

        return findDependantField(field, (String) dependantTypeName, (boolean) dependantIsSibling, fieldIndex)
                .map(dependantField -> validateWithDependantField(field, dependantField, params, fieldIndex))
                .getOrElse(FieldValidationResult.ok());
    }

    protected abstract FieldValidationResult validateWithDependantField(ValidatableField field, ValidatableField dependantField, Map<String, Object> params,
                                                                        Map<String, ? extends List<? extends ValidatableField>> fieldIndex);

    // -------------------- PRIVATE ---------------------

    private static Option<ValidatableField> findDependantField(ValidatableField field, String dependantTypeName, boolean dependantIsSibling,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        if (dependantIsSibling) {
            return findSiblingDependantField(field, dependantTypeName);
        } else {
            return findNonSiblingDependantField(field, dependantTypeName, fieldIndex);
        }
    }

    private static Option<ValidatableField> findNonSiblingDependantField(ValidatableField field, String dependantTypeName,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        List<? extends ValidatableField> dependantField = fieldIndex.get(dependantTypeName);
        if (dependantField == null) {
            return Option.none();
        }
        return Option.ofOptional(dependantField.stream().findFirst());
    }

    private static Option<ValidatableField> findSiblingDependantField(ValidatableField field, String dependantTypeName) {
        Option<? extends ValidatableField> parent = field.getParent();
        if (parent.isEmpty()) {
            return Option.none();
        }

        Option<? extends ValidatableField> dependantType = parent.flatMap(p -> Option.ofOptional(p.getChildren().stream()
                .filter(c -> c.getDatasetFieldType().getName().equals(dependantTypeName))
                .findFirst()));

        if (dependantType.isEmpty()) {
            return findSiblingDependantField(parent.get(), dependantTypeName);
        }

        return Option.narrow(dependantType);
    }
}
