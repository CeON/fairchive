package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

class GeoboxFillValidator implements FieldValidator {

    @Override
    public String getName() {
        return StringUtils.EMPTY;
    }

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (field.hasNonUniqueValue()) {
            return FieldValidationResult.invalid(field, "validation.nonunique");
        }

        return FieldValidationResult.ok();
    }
}
