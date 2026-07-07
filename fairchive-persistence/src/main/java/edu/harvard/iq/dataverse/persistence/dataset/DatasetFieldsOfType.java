package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Model class grouping dataset fields with the same type
 * and perform some actions on them
 * @author madryk
 */
public final class DatasetFieldsOfType implements Iterable<DatasetField> {

    private static FieldDefaultValueApplier defaultValueApplier = new FieldDefaultValueApplier();

	private final DatasetFieldType type;
	private final ArrayList<DatasetField> fields = new ArrayList<>();
    private boolean include = true;
    private FieldValueDivider divider;
    
    //--------------------------------------------------------------------------

    public DatasetFieldsOfType(final DatasetFieldType type) {
    	
    	this.type = type;
    }
    
    public DatasetFieldsOfType(final DatasetFieldType type, final List<DatasetField> fields) {
    	
        this.type = type;
        fields.forEach(this::add);
    }

	public boolean add(final DatasetField field) {
		
    	if(field.isOfType(this.type)) {
    		return this.fields.add(field);
    	} else {
    		throw new IllegalArgumentException(
    				"Field " + field + " is not of type " + this.type);
    	}
	}

	public DatasetFieldType getType() {
        return this.type;
    }

    public List<DatasetField> getFields() {
        return unmodifiableList(this.fields);
    }

    /**
     * Returns true if fields should be visible on create/edit metadata
     * form. If false then they should be hidden.
     * Note that only fields of type without parent can have this setting
     */
    public boolean isInclude() {
        return this.include;
    }
    
    public void setInclude(final boolean include) {
        this.include = include;
    }
    
    public boolean isVisibleThroughAnonymizedUrl() {
        return this.type.isVisibleThroughAnonymizedUrl();
    }

    // -------------------- LOGIC --------------------

    public DatasetField addEmpty(final int position) {
        final DatasetField field = DatasetField.createNewEmptyDatasetField(this.type, null);
        defaultValueApplier.applyDefaultValue(field);
        this.fields.add(position, field);
        return field;
    }
    
    public DatasetField addEmpty() {
        DatasetField field = DatasetField.createNewEmptyDatasetField(this.type, null);
        defaultValueApplier.applyDefaultValue(field);
        this.fields.add(field);
        return field;
    }
    
    public DatasetField clearAndAddEmpty() {
    	this.fields.clear();
        return addEmpty();
    }
    
    public DatasetField getLast() {
        return this.fields.isEmpty()? addEmpty() : this.fields.get(size() - 1);
    }

    public void divide(final int position, final String delimiter) {
        final List<DatasetField> divided = getDivider().divide(get(position), delimiter);
        if (size() > 1 || !divided.isEmpty()) { // remove field only if that won't left us with no fields
        	this.fields.remove(position);
        }
        this.fields.addAll(position, divided);
    }

    public void copyValues(final List<DatasetField> sources, final String sourceName, int position) {
    	
    	final int totalRequiredLength = position + sources.size();
    	while(totalRequiredLength > size()) {
    		add(DatasetField.createNewEmptyChildDatasetField(this.type, null));
    	}
    	
    	for (final DatasetField source : sources) {
    		this.fields.get(position++).copyChildValuesFrom(source);
    	}
    }

    public boolean areAllFieldsEmpty() {
        return this.fields.stream().allMatch(DatasetField::isEmpty);
    }
    
    public DatasetField get(final int index) {
    	return this.fields.get(index);
    }
    
    @Override
    public Iterator<DatasetField> iterator() {
    	return this.fields.iterator();
    }
    
    public Stream<DatasetField> stream() {
    	return this.fields.stream();
    }
    
    public int size() {
    	return this.fields.size();
    }
    
    public boolean isEmpty() {
    	return this.fields.isEmpty();
    }

    private FieldValueDivider getDivider() {
        if (this.divider == null) {
            this.divider = FieldValueDivider.create(this.type);
        }
        return this.divider;
    }
}
