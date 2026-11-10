package edu.harvard.iq.dataverse.persistence.workflow;

import static java.time.Instant.now;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

@SuppressWarnings("serial")
@Entity
public class WorkflowComment implements Serializable, JpaEntity<Long> {

    /*
    This release only supports Return to Author as a comment type
    More may be added in future releases,
    */
    public enum Type {
        RETURN_TO_AUTHOR
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DatasetVersion datasetVersion;
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private DataverseVersion dataverseVersion;

    /**
     * The WorkflowAction string has a namespace for the workflow followed by
     * the action.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    // This is nullable because when you submit for review, especially the first time, you don't have to enter a message.
    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    // This is nullable so the user can be deleted if necessary.
    @JoinColumn(nullable = true)
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    private Timestamp created;
    
    public WorkflowComment() { }

    public WorkflowComment(final DatasetVersion version, final WorkflowComment.Type type, 
    		final String message, final AuthenticatedUser user) {
    	this(type, message, user);	
        if (this.type.equals(Type.RETURN_TO_AUTHOR)) {
            this.datasetVersion = version;
        }
    }
    
    public WorkflowComment(final DataverseVersion version, final WorkflowComment.Type type, 
    		final String message, final AuthenticatedUser user) {
    	this(type, message, user);
        if (this.type.equals(Type.RETURN_TO_AUTHOR)) {
            this.dataverseVersion = version;
        }
    }
    
    private WorkflowComment(final WorkflowComment.Type type, final String message,
    		final AuthenticatedUser user) {
        this.type = type;
        this.message = message;
        this.authenticatedUser = user;
        this.created = Timestamp.from(now());
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }
    
    public void setDatasetVersion(final DatasetVersion version) {
        this.datasetVersion = version;
    }

    public DataverseVersion getDataverseVersion() {
		return this.dataverseVersion;
	}

	public void setDataverseVersion(final DataverseVersion version) {
		this.dataverseVersion = version;
	}

	public Type getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }

    public Timestamp getCreated() {
        return this.created;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return this.authenticatedUser;
    }
    
    public void setAuthenticatedUser(final AuthenticatedUser user){
        this.authenticatedUser = user;
    }

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj != null && getClass() == obj.getClass()) {
			final WorkflowComment other = (WorkflowComment) obj;
			return Objects.equals(id, other.id);
		} else {
			return false;
		}
	}
}
