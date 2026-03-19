package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.common.BundleUtil.getCurrentLocale;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundleWithLocale;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetField.NA_VALUE;
import static java.util.Comparator.comparingInt;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.StringUtils.stripAccents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(columnList = "datasetfieldtype_id"), @Index(columnList = "displayorder")})
public class ControlledVocabularyValue implements Serializable {

    public static final Comparator<ControlledVocabularyValue> DisplayOrder
            = comparingInt(ControlledVocabularyValue::getDisplayOrder);

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(columnDefinition = "TEXT", nullable = false)
    private String strValue;

    private String identifier;

    private int displayOrder;

    @Column(name = "suggestion_details", columnDefinition = "TEXT")
    private String suggestionDetails;

    @ManyToOne
    private DatasetFieldType datasetFieldType;

    @OneToMany(mappedBy = "controlledVocabularyValue", cascade = {REMOVE, MERGE, PERSIST})
    private Collection<ControlledVocabAlternate> controlledVocabAlternates = new ArrayList<>();

    @Column
    private String displayGroup;

    // -------------------- CONSTRUCTORS --------------------

    public ControlledVocabularyValue() { }

    public ControlledVocabularyValue(final Long id, final String strValue, 
            final DatasetFieldType fieldType) {
        this.id = id;
        this.strValue = strValue;
        this.datasetFieldType = fieldType;
    }

    // -------------------- GETTERS --------------------

    public String getStrValue() {
        return this.strValue;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * Method used in suggestionInputField.xhtml for var=suggestion
     * ControlledVocabularyValue and Suggestion must have getValue property to match definition in xhtml
     * used in itemLabel, itemValue of p:autoComplete
     */
    public String getValue() {
        return this.strValue;
    }

    public DatasetFieldType getDatasetFieldType() {
        return this.datasetFieldType;
    }

    public Collection<ControlledVocabAlternate> getControlledVocabAlternates() {
        return this.controlledVocabAlternates;
    }

    public String getDisplayGroup() {
        return this.displayGroup;
    }

    public String getSuggestionDetails() {
        return this.suggestionDetails;
    }

    // -------------------- LOGIC --------------------

    public String getLocaleStrValue() {
        return getLocaleStrValue(getCurrentLocale());
    }

    public String getLocaleStrValue(final Locale locale) {
    	if(NA_VALUE.equals(this.strValue) || this.datasetFieldType == null || 
    			this.datasetFieldType.getMetadataBlock() == null) {
    		return this.strValue;
    	} else {
            final String value = getStringFromNonDefaultBundleWithLocale(getKey(),
                    getBundleName(), locale);
            return value.isEmpty() ? this.strValue : value;
    	}
    }
    
    private String getKey() {
    	return "controlledvocabulary." + this.datasetFieldType.getName() + '.' + 
    			stripAccents(this.strValue.toLowerCase().replace(' ', '_'));
    }
    
    private String getBundleName() {
    	return getDatasetFieldType().getMetadataBlock().getName();
    }

    // -------------------- SETTERS --------------------

    public void setStrValue(final String value) {
        this.strValue = value;

    }

    public void setIdentifier(final String value) {
        this.identifier = value;
    }

    public void setDisplayOrder(final int order) {
        this.displayOrder = order;
    }

    public void setDatasetFieldType(final DatasetFieldType type) {
        this.datasetFieldType = type;
    }

    public void setControlledVocabAlternates(
    		final Collection<ControlledVocabAlternate> alternates) {
        this.controlledVocabAlternates = alternates;
    }

    public void setDisplayGroup(final String group) {
        this.displayGroup = group;
    }

    public void setSuggestionDetails(final String details) {
        this.suggestionDetails = details;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

	@Override
	public boolean equals(final Object object) {
		return object instanceof ControlledVocabularyValue
				? Objects.equals(this.id, ((ControlledVocabularyValue) object).id)
				: false;
	}

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return this.strValue;
    }
}
