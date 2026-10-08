package edu.harvard.iq.dataverse.persistence.dataverse;

import static java.util.Objects.requireNonNull;
import static javax.persistence.GenerationType.IDENTITY;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

//@NamedQueries({
//        @NamedQuery(name = "DataverseFacet.removeByOwnerId",
//                query = "DELETE FROM DataverseFacet f WHERE f.dataverse.id=:ownerId"),
//        @NamedQuery(name = "DataverseFacet.findByDataverseId",
//                query = "select f from DataverseFacet f where f.dataverse.id = :dataverseId order by f.displayOrder")
//})

@Entity
public class DataverseFacet implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DataverseVersion version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "datasetfieldtype_id", nullable = false)
    private DatasetFieldType datasetFieldType;

    @Column
    private int displayOrder = 0;
    
    protected DataverseFacet() {}
    
    DataverseFacet(final DataverseVersion version, 
    			   final DatasetFieldType datasetFieldType) {
    	this.version = requireNonNull(version);
    	this.datasetFieldType = requireNonNull(datasetFieldType);
    }
    
    public DataverseFacet createNewFor(final DataverseVersion version) {
    	final DataverseFacet result = new DataverseFacet(version, this.datasetFieldType);
    	result.displayOrder = this.displayOrder;
    	return result;
    }
    
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public DataverseVersion getVersion() {
        return this.version;
    }

    public DatasetFieldType getDatasetFieldType() {
        return this.datasetFieldType;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(final int order) {
        this.displayOrder = order;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
    	if(object != null && getClass().equals(object.getClass())) {
            DataverseFacet other = (DataverseFacet) object;
            return Objects.equals(this.id, other.id);
    	} else {
    		return false;
    	}
    }

    @Override
    public String toString() {
        return "DataverseFacet[ id=" + id + " ]";
    }

}

