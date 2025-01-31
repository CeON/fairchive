package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

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

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromNonDefaultBundle;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author skraffmiller
 */
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

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    @OneToMany(mappedBy = "metadataBlock", cascade = {REMOVE, MERGE, PERSIST})
    @OrderBy("displayOrder")
    private List<DatasetFieldType> datasetFieldTypes;

    public List<DatasetFieldType> getDatasetFieldTypes() {
        return datasetFieldTypes;
    }

    public void setDatasetFieldTypes(List<DatasetFieldType> datasetFieldTypes) {
        this.datasetFieldTypes = datasetFieldTypes;
    }

    public boolean isDisplayOnCreate() {
        return this.datasetFieldTypes.stream()
                .anyMatch(DatasetFieldType::isDisplayOnCreate);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCitationMetaBlock() {
        // eventually this will be dynamic, for now only citation is required
        return "citation".equals(name);
    }

    @OneToOne
    @JoinColumn(name = "owner_id", unique = false, nullable = true, 
                insertable = true, updatable = true)
    private Dataverse owner;

    public Dataverse getOwner() {
        return owner;
    }

    public void setOwner(Dataverse owner) {
        this.owner = owner;
    }

    @Transient
    private boolean empty;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    @Transient
    private boolean selected;

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Transient
    private boolean hasRequired;

    public void setHasRequired(boolean hasRequired) {
        this.hasRequired = hasRequired;
    }

    public boolean isHasRequired() {
        return hasRequired;
    }

    public String getIdString() {
        return id.toString();
    }
    
    public boolean isVisibleThroughAnonymizedUrl() {
        return this.datasetFieldTypes.stream()
                .anyMatch(DatasetFieldType::isVisibleThroughAnonymizedUrl);
    }

    @Transient
    private boolean showDatasetFieldTypes;

    public void setShowDatasetFieldTypes(boolean showDatasetFieldTypes) {
        this.showDatasetFieldTypes = showDatasetFieldTypes;
    }

    public boolean isShowDatasetFieldTypes() {
        return showDatasetFieldTypes;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MetadataBlock)) {
            return false;
        }
        MetadataBlock other = (MetadataBlock) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "MetadataBlock[ id=" + id + " ]";
    }

    public String getLocaleDisplayName() {
        String localeDisplayName =  getStringFromNonDefaultBundle("metadatablock.displayName", getName());
        return localeDisplayName.isEmpty() ? displayName : localeDisplayName;
    }
}
