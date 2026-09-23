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
import javax.validation.constraints.NotBlank;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.config.ValidateEmail;

@Entity
public class DataverseContact implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DataverseVersion version;
    
    @NotBlank(message = "{user.invalidEmail}")
    @ValidateEmail(message = "{user.invalidEmail}")
    @Column(nullable = false)
    private String email;
    
    @Column
    private int displayOrder = 0;
    
    protected DataverseContact() {}
    
    DataverseContact(final DataverseVersion version, final String email) {
    	this.version = requireNonNull(version);
    	this.email = email;
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

    public String getEmail() {
        return this.email;
    }

    public void setEmail(final String email) {
        this.email = email;
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
            final DataverseContact other = (DataverseContact) object;
            return Objects.equals(this.id, other.id);
    	} else {
    		return false;
    	}
    }

    @Override
    public String toString() {
        return "DataverseContact[ id=" + id + " ]";
    }
}
