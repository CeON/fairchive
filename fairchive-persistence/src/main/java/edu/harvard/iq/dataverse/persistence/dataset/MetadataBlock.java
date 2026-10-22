package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundle;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Table(indexes = {@Index(columnList = "name") , @Index(columnList = "owner_id")})
@Entity
public class MetadataBlock implements JpaEntity<Long>, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String displayName;
    @Column(name = "namespaceuri", columnDefinition = "TEXT")
    private String namespaceUri;
    @Column(nullable = false)
    private int displayOrder;
    @OneToMany(mappedBy = "metadataBlock", cascade = {REMOVE, MERGE, PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetFieldType> datasetFieldTypes = new ArrayList<>();
    @OneToOne
    @JoinColumn(name = "owner_id", unique = false, nullable = true, 
                insertable = true, updatable = true)
    private Dataverse owner;
    @Transient
    private boolean showDatasetFieldTypes;
    @Transient
    private boolean empty;
    @Transient
    private boolean selected;
    @Transient
    private boolean hasRequired;

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    public void setNamespaceUri(final String uri) {
        this.namespaceUri = uri;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<DatasetFieldType> getDatasetFieldTypes() {
        return this.datasetFieldTypes;
    }

    public void setDatasetFieldTypes(final List<DatasetFieldType> types) {
        this.datasetFieldTypes = types;
    }

    public boolean isDisplayOnCreate() {
        return this.datasetFieldTypes.stream()
                .anyMatch(DatasetFieldType::isDisplayOnCreate);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(final String name) {
        this.displayName = name;
    }

    public boolean isCitationMetaBlock() {
        // eventually this will be dynamic, for now only citation is required
        return "citation".equals(this.name);
    }

    public Dataverse getOwner() {
        return this.owner;
    }

    public void setOwner(final Dataverse owner) {
        this.owner = owner;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public void setEmpty(final boolean empty) {
        this.empty = empty;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setHasRequired(final boolean hasRequired) {
        this.hasRequired = hasRequired;
    }

    public boolean isHasRequired() {
        return this.hasRequired;
    }

    public String getIdString() {
        return this.id.toString();
    }
    
    public boolean isVisibleThroughAnonymizedUrl() {
        return this.datasetFieldTypes.stream()
                .anyMatch(DatasetFieldType::isVisibleThroughAnonymizedUrl);
    }

    public void setShowDatasetFieldTypes(final boolean showDatasetFieldTypes) {
        this.showDatasetFieldTypes = showDatasetFieldTypes;
    }

    public boolean isShowDatasetFieldTypes() {
        return this.showDatasetFieldTypes;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
    	if(object != null && getClass().equals(object.getClass())) {
    		final MetadataBlock other = (MetadataBlock) object;
    		return Objects.equals(this.id, other.id);
    	} else {
    		return false;
    	}
    }

    @Override
    public String toString() {
        return "MetadataBlock[ id=" + id + " ]";
    }

    public String getLocaleDisplayName() {
        final String localeDisplayName =  getStringFromNonDefaultBundle(
        		"metadatablock.displayName", getName());
        return localeDisplayName.isEmpty() ? this.displayName : localeDisplayName;
    }
}
