package edu.harvard.iq.dataverse.validation.field.validators;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.ok;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.periodo.PeriodoDataFinder;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

@Eager
@ApplicationScoped
public class PeriodoFieldValidator extends MultiValueValidatorBase {

    private final static String prefix = "http://n2t.net/ark:/99152/p";
    
    @Inject
    private PeriodoDataFinder periods;

    @Override
    public String getName() {
        return "periodo_validator";
    }

    @Override
    public FieldValidationResult validateValue(final String periodoUrl,
            final ValidatableField field, final Map<String, Object> params,
            final Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        
        if (isNotBlank(periodoUrl) 
                && periodoUrl.startsWith(prefix)
                && periodoUrl.substring(prefix.length()).matches("[a-z0-9]{10}")) {
               return  this.periods.getByUrl(periodoUrl).isPresent()
                       ? ok()
                       : invalid(field, "periodo.invalidId");
        } else {
                return invalid(field, "periodo.invalid.format");
        }
    }
}
