package edu.harvard.iq.dataverse.validation;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.ok;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.omnifaces.cdi.Eager;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.geonames.GeoNameDataFinder;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.validators.FieldValidatorBase;

@Eager
@ApplicationScoped
public class GeoNameValidator extends FieldValidatorBase {

    private GeoNameDataFinder finder;
    
    public GeoNameValidator() {
    }
    
    @Inject
    public GeoNameValidator(final GeoNameDataFinder finder) {
        this.finder = finder;
    }
    
    @Override
    public String getName() {
        return "geonames_validator";
    }

    @Override
    public FieldValidationResult validate(final ValidatableField field, 
            final Map<String, Object> params, 
            final Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        
        return this.finder.findById(field.getSingleValue()).isPresent()
                 ? ok() 
                 : invalid(field, "geonames.notValidId");
    }
}
