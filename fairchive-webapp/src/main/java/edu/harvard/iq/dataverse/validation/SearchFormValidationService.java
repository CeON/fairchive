package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.validation.field.SearchFormValidationDispatcherFactory;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Stateless
public class SearchFormValidationService {

    private SearchFormValidationDispatcherFactory dispatcherFactory;

    // -------------------- CONSTRUCTORS --------------------

    public SearchFormValidationService() { }

    @Inject
    public SearchFormValidationService(SearchFormValidationDispatcherFactory dispatcherFactory) {
        this.dispatcherFactory = dispatcherFactory;
    }

    // -------------------- LOGIC --------------------

    public List<FieldValidationResult> validateSearchForm(Map<String, SearchField> searchFields,
                                                          Map<String, SearchField> nonSearchFields) {
        searchFields.forEach((k, v) -> v.setValidationMessage(null));
        List<FieldValidationResult> fieldValidationResults = dispatcherFactory.create(searchFields, nonSearchFields)
                .executeValidations();
        fieldValidationResults.forEach(r -> {
            ValidatableField field = r.getField();
            field.setValidationMessage(resolveValidationMessage(r));
        });
        return fieldValidationResults;
    }

    private String resolveValidationMessage(FieldValidationResult result) {
        String fieldTypeName = result.getField().getDatasetFieldType().getName();
        String metadataBlockName = result.getField().getDatasetFieldType().getMetadataBlock().getName();

        String key = "datasetfieldtype." + fieldTypeName + "." + result.getErrorCode();

        return Optional.of(BundleUtil.getStringFromNonDefaultBundle(key, metadataBlockName, result.getErrorArgs()))
            .filter(StringUtils::isNotBlank)
            .orElse(BundleUtil.getStringFromBundle(key, result.getErrorArgs()));

    }
}
