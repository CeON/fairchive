package edu.harvard.iq.dataverse.dataverse;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class DatasetFieldViewOptions implements Serializable {

    private boolean requiredField;
    private boolean included;
    private List<SelectItem> selectedDatasetFields = new ArrayList<>();

    public DatasetFieldViewOptions(final boolean requiredField, final boolean included) {
        this.requiredField = requiredField;
        this.included = included;
    }

    // -------------------- GETTERS --------------------

    /**
     * Indicates if this field is required, otherwise it is 'optional'.
     */
    public boolean isRequiredField() {
        return this.requiredField;
    }

    /**
     * Indicates if this field is included, otherwise it is 'hidden'.
     */
    public boolean isIncluded() {
        return this.included;
    }

    /**
     * Returns boolean list of (Required/Optional) or (Hidden).
     */
    public List<SelectItem> getSelectedDatasetFields() {
        return this.selectedDatasetFields;
    }

    // -------------------- SETTERS --------------------


    public void setRequiredField(final boolean requiredField) {
        this.requiredField = requiredField;
    }

    public void setIncluded(final boolean included) {
        this.included = included;
    }

    public void setSelectedDatasetFields(final List<SelectItem> fields) {
        this.selectedDatasetFields = fields;
    }
}
