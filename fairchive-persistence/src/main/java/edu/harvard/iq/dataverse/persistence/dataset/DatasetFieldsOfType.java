package edu.harvard.iq.dataverse.persistence.dataset;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Model class grouping dataset fields with the same type
 * and perform some actions on them
 * @author madryk
 */
public class DatasetFieldsOfType {

    private final DatasetFieldType type;

    private final List<DatasetField> datasetFields;

    // Currently this is the only action we perform on the fields.
    // If the number increases this should be made into more generic form, eg.
    // some kind of factory and collection of field actions.
    private FieldValueDivider divider;

    private FieldDefaultValueApplier defaultValueApplier = new FieldDefaultValueApplier();

    private boolean include = true;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldsOfType(final DatasetFieldType datasetFieldType, final List<DatasetField> datasetFields) {
        datasetFields.forEach(field -> Preconditions.checkArgument(field.getDatasetFieldType().equals(datasetFieldType)));

        this.type = datasetFieldType;
        this.datasetFields = datasetFields;
    }

    // -------------------- GETTERS --------------------

    public DatasetFieldType getType() {
        return this.type;
    }

    public List<DatasetField> getDatasetFields() {
        return this.datasetFields;
    }

    /**
     * Returns true if fields should be visible on create/edit metadata
     * form. If false then they should be hidden.
     * Note that only fields of type without parent can have this setting
     */
    public boolean isInclude() {
        return this.include;
    }
    
    public boolean isVisibleThroughAnonymizedUrl() {
        return this.type.isVisibleThroughAnonymizedUrl();
    }

    // -------------------- LOGIC --------------------

    public void addEmptyDatasetField(int position) {
        DatasetField newField = DatasetField.createNewEmptyDatasetField(this.type, null);
        applyDefaultValues(newField);
        this.datasetFields.add(position, newField);
    }
    
    public void addEmptyDatasetField() {
        DatasetField newField = DatasetField.createNewEmptyDatasetField(this.type, null);
        applyDefaultValues(newField);
        this.datasetFields.add(newField);
    }

    public DatasetField addAndReturnEmptyDatasetField(int position) {
        addEmptyDatasetField(position);
        return this.datasetFields.get(position);
    }

    public void divide(final int position, final String delimiter) {
        List<DatasetField> divided = getDivider().divide(this.datasetFields.get(position), delimiter);
        if (this.datasetFields.size() > 1 || !divided.isEmpty()) { // remove field only if that won't left us with no fields
            this.datasetFields.remove(position);
        }
        this.datasetFields.addAll(position, divided);
    }

    public void copyValues(final List<DatasetField> sources, final String sourceName, int position) {
    	
    	final int totalRequiredLength = position + sources.size();
    	while(totalRequiredLength > this.datasetFields.size()) {
    		this.datasetFields.add(DatasetField.createNewEmptyChildDatasetField(this.type, null));
    	}
    	
    	for (final DatasetField source : sources) {
    		this.datasetFields.get(position++).copyChildValuesFrom(source);
    	}
    }

    public void removeDatasetField(final int position) {
        this.datasetFields.remove(position);
    }

    public boolean areAllFieldsEmpty() {
        return this.datasetFields.stream().allMatch(DatasetField::isEmpty);
    }

    // -------------------- PRIVATE --------------------

    private FieldValueDivider getDivider() {
        if (this.divider == null) {
            this.divider = FieldValueDivider.create(this.type);
        }
        return this.divider;
    }

    private void applyDefaultValues(final DatasetField datasetField) {
        this.defaultValueApplier.applyDefaultValue(datasetField);
    }

    // -------------------- SETTERS --------------------

    public void setInclude(final boolean include) {
        this.include = include;
    }
}
