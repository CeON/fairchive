package edu.harvard.iq.dataverse.validation.field.validators;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.ok;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.PeriodoValidator;
import edu.harvard.iq.dataverse.validation.ValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

@Eager
@ApplicationScoped
public class PeriodoFieldValidator extends MultiValueValidatorBase {

    @Inject
    private PeriodoValidator validator;

    @Override
    public String getName() {
        return "periodo_validator";
    }

    @Override
    public FieldValidationResult validateValue(final String periodoUrl,
            final ValidatableField field, final Map<String, Object> params,
            final Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        
        final String fieldName = field.getDatasetFieldType().getDisplayName();
        final ValidationResult result = this.validator.validate(periodoUrl);
        return result.isOk()
                ? ok()
                : invalid(field, getStringFromBundle(result.getErrorCode(), fieldName));
    }
}
