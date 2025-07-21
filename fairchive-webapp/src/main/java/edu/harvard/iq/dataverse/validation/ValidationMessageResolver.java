package edu.harvard.iq.dataverse.validation;

import java.util.Optional;

import javax.ejb.Stateless;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

/**
 * Service responsible for resolving validation errors messages
 */
@Stateless
public class ValidationMessageResolver {

    /**
     * Returns validation message for the given {@link FieldValidationResult}.
     */
    public String resolveValidationMessage(FieldValidationResult result) {
        String fieldTypeName = result.getField().getDatasetFieldType().getName();
        String metadataBlockName = result.getField().getDatasetFieldType().getMetadataBlock().getName();

        String key = "datasetfieldtype." + fieldTypeName + "." + result.getErrorCode();

        return Optional.of(BundleUtil.getStringFromNonDefaultBundle(key, metadataBlockName, result.getErrorArgs()))
            .filter(StringUtils::isNotBlank)
            .orElse(BundleUtil.getStringFromBundle(result.getErrorCode(), result.getErrorArgs()));

    }
}
