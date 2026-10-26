package edu.harvard.iq.dataverse.dataverse;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class DataverseMetaBlockOptions implements Serializable {

    private final Map<Long, MetadataBlockViewOptions> mdbViewOptions = new HashMap<>();
    private final Map<Long, DatasetFieldViewOptions> datasetFieldViewOptions = new HashMap<>();
    private boolean inheritMetaBlocksFromParent = true;

    // -------------------- GETTERS --------------------

    /**
     * Retrives metadata block view options.
     */
    public Map<Long, MetadataBlockViewOptions> getMdbViewOptions() {
        return this.mdbViewOptions;
    }

    /**
     * Retrives dataset fields view options.
     */
    public Map<Long, DatasetFieldViewOptions> getDatasetFieldViewOptions() {
        return this.datasetFieldViewOptions;
    }

    /**
     * Indicates if metadata blocks are inherited from parent dataverse or if they are being edited by user.
     */
    public boolean isInheritMetaBlocksFromParent() {
        return this.inheritMetaBlocksFromParent;
    }
    // -------------------- LOGIC --------------------

    /**
     * Indicates if dataset field should be shown.
     *
     * @return true/false or false if null.
     */
    public boolean isShowDatasetFieldTypes(final Long mdbId) {

        return this.mdbViewOptions.containsKey(mdbId) 
        		&& this.mdbViewOptions.get(mdbId).isShowDatasetFieldTypes();
    }

    /**
     * Indicates if dataset field should be editable.
     *
     * @return true/false or false if null.
     */
    public boolean isEditableDatasetFieldTypes(final Long mdbId) {
        return this.mdbViewOptions.containsKey(mdbId) 
        		&& this.mdbViewOptions.get(mdbId).isEditableDatasetFieldTypes();
    }

    /**
     * Indicates if dataset field is included (required/optional).
     *
     * @return true/false or false if null.
     */
    public boolean isDsftIncludedField(final Long dsftId) {
        return this.datasetFieldViewOptions.containsKey(dsftId) 
        		&& this.datasetFieldViewOptions.get(dsftId).isIncluded();
    }

    /**
     * Indicates if metadata block is selected.
     *
     * @return true/false or false if null.
     */
    public boolean isMetaBlockSelected(final Long blockId) {
        return this.mdbViewOptions.containsKey(blockId) 
        		&& this.mdbViewOptions.get(blockId).isSelected();
    }

    /**
     * Makes a deep copy using serialization/deserialization.
     * If top performance is really required, it would be advisable to implement different deep copy mechanism.
     */
    public DataverseMetaBlockOptions deepCopy() {
        return SerializationUtils.clone(this);
    }

    // -------------------- SETTERS --------------------

    public void setInheritMetaBlocksFromParent(final boolean value) {
        this.inheritMetaBlocksFromParent = value;
    }
}
