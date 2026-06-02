package edu.harvard.iq.dataverse.persistence.dataset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Leonid Andreev
 */
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"foreignMetadataFormatMapping_id", "foreignFieldXpath"})
        , indexes = {@Index(columnList = "foreignmetadataformatmapping_id")
        , @Index(columnList = "foreignfieldxpath")
        , @Index(columnList = "parentfieldmapping_id")})
@NamedQueries({
        @NamedQuery(name = "ForeignMetadataFieldMapping.findByPath",
                query = "SELECT fmfm FROM ForeignMetadataFieldMapping fmfm WHERE fmfm.foreignMetadataFormatMapping.name=:formatName AND fmfm.foreignFieldXPath=:xPath")
})
@Entity
public class ForeignMetadataFieldMapping implements Serializable, JpaEntity<Long> {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @ManyToOne(cascade = MERGE)
    private ForeignMetadataFormatMapping foreignMetadataFormatMapping;

    @Column(name = "foreignFieldXPath", columnDefinition = "TEXT")
    private String foreignFieldXPath;

    @Column(name = "datasetfieldName", columnDefinition = "TEXT")
    private String datasetfieldName;

    @OneToMany(mappedBy = "parentFieldMapping", cascade = {REMOVE, MERGE, PERSIST})
    private Collection<ForeignMetadataFieldMapping> childFieldMappings;

    @ManyToOne(cascade = MERGE)
    private ForeignMetadataFieldMapping parentFieldMapping;

    private boolean isAttribute;

    public ForeignMetadataFormatMapping getForeignMetadataFormatMapping() {
        return this.foreignMetadataFormatMapping;
    }

    public void setForeignMetadataFormatMapping(final ForeignMetadataFormatMapping mapping) {
        this.foreignMetadataFormatMapping = mapping;
    }

    public String getForeignFieldXPath() {
        return this.foreignFieldXPath;
    }

    public void setForeignFieldXPath(final String foreignFieldXPath) {
        this.foreignFieldXPath = foreignFieldXPath;
    }

    public String getDatasetfieldName() {
        return this.datasetfieldName;
    }

    public void setDatasetfieldName(final String name) {
        this.datasetfieldName = name;
    }

    public Collection<ForeignMetadataFieldMapping> getChildFieldMappings() {
        return this.childFieldMappings;
    }

    public void setChildFieldMappings(final Collection<ForeignMetadataFieldMapping> mappings) {
        this.childFieldMappings = mappings;
    }

    public ForeignMetadataFieldMapping getParentFieldMapping() {
        return this.parentFieldMapping;
    }

    public void setParentFieldMapping(final ForeignMetadataFieldMapping mapping) {
        this.parentFieldMapping = mapping;
    }

    public boolean isAttribute() {
        return this.isAttribute;
    }

    public void setIsAttribute(final boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public boolean isChild() {
        return this.parentFieldMapping != null;
    }

    public boolean HasChildren() {
        return !this.childFieldMappings.isEmpty();
    }

    public boolean HasParent() {
        return this.parentFieldMapping != null;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ForeignMetadataFieldMapping)) {
            return false;
        }
        ForeignMetadataFieldMapping other = (ForeignMetadataFieldMapping) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return this.datasetfieldName;
    }

}
