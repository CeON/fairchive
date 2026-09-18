package edu.harvard.iq.dataverse.dataverse;

import java.io.Serializable;

@SuppressWarnings("serial")
public class MetadataBlockViewOptions implements Serializable {

    private boolean showDatasetFieldTypes;
    private boolean editableDatasetFieldTypes;
    private boolean selected;

    // -------------------- CONSTRUCTORS --------------------

    private MetadataBlockViewOptions(final Builder builder) {
        this.showDatasetFieldTypes = builder.showDatasetFieldTypes;
        this.editableDatasetFieldTypes = builder.editableDatasetFieldTypes;
        this.selected = builder.selected;
    }

    // -------------------- GETTERS --------------------

    /**
     * Indicates if the metadata fields should be visible (used when creating/editing dataverse).
     */
    public boolean isShowDatasetFieldTypes() {
        return this.showDatasetFieldTypes;
    }

    /**
     * Indicates if you are able to edit metadata blocks fields to be (Optional/Required/Hidden).
     */
    public boolean isEditableDatasetFieldTypes() {
        return this.editableDatasetFieldTypes;
    }

    /**
     * Indicates if this metadata block is selected ( in order to mark the checkbox).
     */
    public boolean isSelected() {
        return this.selected;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    // -------------------- LOGIC --------------------

    public static final class Builder {
        private boolean showDatasetFieldTypes;
        private boolean editableDatasetFieldTypes;
        private boolean selected;

        private Builder() {
        }

        public Builder showDatasetFieldTypes(boolean value) {
            this.showDatasetFieldTypes = value;
            return this;
        }

        public Builder editableDatasetFieldTypes(final boolean value) {
            this.editableDatasetFieldTypes = value;
            return this;
        }

        public Builder selected(final boolean value) {
            this.selected = value;
            return this;
        }

        public MetadataBlockViewOptions build() {
            return new MetadataBlockViewOptions(this);
        }
    }

    // -------------------- SETTERS --------------------

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
}
