package edu.harvard.iq.dataverse.dataset;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

/**
 * Class intended to be overridden via extensions.
 * It modifies dataset fields before showing them for the end user on UI.
 * <br/>
 * Extensions can override this class to modify values of existing dataset
 * fields or add new dataset fields that are not part of dataset fields in DB.
 * <br/>
 * Extensions should extend this class and mark them with {@code @Specializes}
 * and {@code @ApplicationScoped} to use this feature.
 * 
 * @author Krzysztof Mądry, Sylwester Niewczas
 */
@ApplicationScoped
public class DatasetFieldsForViewTransformer {

    /**
     * Transforms dataset fields before presenting them on the UI
     * 
     * @param datasetFields - fields of a dataset or a template
     * @param isTemplate - if true then dataset fields comes from a template
     *   otherwise it comes from a dataset
     */
    public void transformDatasetFields(List<DatasetField> datasetFields, boolean isTemplate) {
        // Empty on purpose, method intended to be overridden in extensions
    }
}
