package edu.harvard.iq.dataverse.persistence.dataverse;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

/**
 * When dataverse is created/edited, the user can customize which fields are optional/required
 * and that data is stored here.
 *
 * @author skraffmiller
 */
@Table(name = "DataverseFieldTypeInputLevel"
        , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dataverse_id", "datasetfieldtype_id"})}
        , indexes = {@Index(columnList = "dataverse_id")
        , @Index(columnList = "datasetfieldtype_id")
        , @Index(columnList = "required")}
)
@Entity
public class DataverseFieldTypeInputLevel implements JpaEntity<Long>, Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "version_id", nullable = false)
    private DataverseVersion version;
    @ManyToOne
    @JoinColumn(name = "datasetfieldtype_id", nullable = false)
    private DatasetFieldType datasetFieldType;
    private boolean include;
    private boolean required;
    
    public DataverseFieldTypeInputLevel createNewFor(final DataverseVersion version) {
    	final DataverseFieldTypeInputLevel result = new DataverseFieldTypeInputLevel();
    	result.version = version;
    	result.datasetFieldType = this.datasetFieldType;
    	result.include = this.include;
    	result.required = this.required;
    	return result;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
    	return Objects.hash(this.id);
    }
    
    public DataverseVersion getVersion() {
        return this.version;
    }
    
    public void setVersion(final DataverseVersion version) {
		this.version = version;
	}

	public DatasetFieldType getDatasetFieldType() {
        return this.datasetFieldType;
    }

    public void setDatasetFieldType(final DatasetFieldType type) {
        this.datasetFieldType = type;
    }

    public boolean isInclude() {
        return this.include;
    }

    public void setInclude(final boolean include) {
        this.include = include;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    @Override
    public boolean equals(final Object object) {
    	if(object != null && getClass().equals(object.getClass())) {
    		 final DataverseFieldTypeInputLevel other = (DataverseFieldTypeInputLevel) object;
    		 return Objects.equals(this.id, other.id);
    	} else {
    		return false;
    	}
    }

    @Override
    public String toString() {
        return "DataverseFieldTypeInputLevel[ id=" + id + " ]";
    }

}
