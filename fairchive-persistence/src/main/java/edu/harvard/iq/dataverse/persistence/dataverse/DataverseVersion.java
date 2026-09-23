package edu.harvard.iq.dataverse.persistence.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.MarkupChecker.sanitizeBasicHTML;
import static edu.harvard.iq.dataverse.common.MarkupChecker.stripAllTags;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType;

@Entity
public class DataverseVersion implements JpaEntity<Long> {
	
    public enum State {
        DRAFT, RELEASED;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Temporal(value = TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;
    
    @NotBlank(message = "{dataverse.name}")
    @Column(nullable = false)
    private String name = EMPTY;
    
    @NotBlank(message = "{dataverse.alias}")
    @Column(nullable = false, unique = true)
    @Size(max = 100, message = "{dataverse.aliasLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]+", message = "{dataverse.nameIllegalCharacters}"),
            @Pattern(regexp = ".*\\D.*", message = "{dataverse.aliasNotnumber}")})
    private String alias = EMPTY;

    @Column
    private String description;

    @Column
    private String additionalDescription;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{dataverse.category}")
    @Column(nullable = false)
    private DataverseType dataverseType;
    
    
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private State state = State.DRAFT;
	
	@Column
    private String affiliation;
    
    @OneToMany(mappedBy = "version", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @OrderBy("displayOrder")
    @NotEmpty(message = "At least one contact is required.")
    private List<DataverseContact> contacts = new ArrayList<>();
    
    @Column
    private boolean messagesBannersAllowed;
    
    @OneToMany(mappedBy = "version", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<DataverseFacet> facets = new ArrayList<>();
    
    @OneToMany(mappedBy = "version", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    private List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels = new ArrayList<>();
  
    @ManyToOne(optional = false)
    @JoinColumn(name = "dataverse_id", nullable = false)
    private Dataverse dataverse;
    
    protected DataverseVersion() {}
    
    public DataverseVersion(final Dataverse dataverse) {
    	this(dataverse, State.RELEASED);
    }
    
    public DataverseVersion(final Dataverse dataverse, final State state) {
    	this.dataverse = requireNonNull(dataverse);
    	this.createTime = new Date();
    	this.state = state;
    	addContact(EMPTY);
    }
    
    @Override
    public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}
	
	public Date getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(final Date createTime) {
		this.createTime = createTime;
	}

	public DataverseType getDataverseType() {
        return this.dataverseType;
    }

    public void setDataverseType(final DataverseType type) {
        this.dataverseType = type;
    }

    public String getFriendlyTypeName() {  	
    	return this.dataverseType != null 
    			? getStringFromBundle(this.dataverseType.i18nKey)
    			: EMPTY;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getAdditionalDescription() {
        return this.additionalDescription;
    }

    public void setAdditionalDescription(final String additionalDescription) {
        this.additionalDescription = additionalDescription;
    }
    
    public State getState() {
		return this.state;
	}
    
    public boolean isDraft() {
    	return State.DRAFT.equals(this.state);
    }
    
    public boolean isReleased() {
    	return State.RELEASED.equals(this.state);
    }

	public void setState(final State state) {
		this.state = requireNonNull(state);
	}

	public String getAffiliation() {
		return this.affiliation;
	}

	public void setAffiliation(final String affiliation) {
		this.affiliation = affiliation;
	}

	public List<DataverseContact> getContacts() {
		return unmodifiableList(this.contacts);
	}
	
	public DataverseContact addContact(final String email) {
		final DataverseContact contact = new DataverseContact(this, email);
		this.contacts.add(contact);
		return contact;
	}
	
	public void removeContact(final int index) {
	    if (this.contacts.size() <= 1) {
	        throw new IllegalStateException("A DataverseVersion must have at least one contact.");
	    }
		this.contacts.remove(index);
	}
	
	public List<DataverseFacet> getFacets() {
		return unmodifiableList(this.facets);
	}
	
	public DataverseFacet addFacet(final DatasetFieldType type) {
		final DataverseFacet facet = new DataverseFacet(this, type);
		this.facets.add(facet);
		return facet;
	}
	
	public void removeFacet(final int index) {
		this.contacts.remove(index);
	}
	
	public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
		return unmodifiableList(this.dataverseFieldTypeInputLevels);
	}
	
	public void addDataverseFieldTypeInputLevel(final DataverseFieldTypeInputLevel level) {
		this.dataverseFieldTypeInputLevels.add(level);
		level.setVersion(this);
	}
	
	public void removeDataverseFieldTypeInputLevel(final int index) {
		this.dataverseFieldTypeInputLevels.remove(index);
	}

	public boolean isMessagesBannersAllowed() {
		return this.messagesBannersAllowed;
	}

	public void setMessagesBannersAllowed(final boolean allowed) {
		this.messagesBannersAllowed = allowed;
	}

	public Dataverse getDataverse() {
		return this.dataverse;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(this.id);
	}

	@Override
    public boolean equals(final Object object) {
        if(object != null && getClass().equals(object.getClass())) {
            final DataverseVersion other = (DataverseVersion) object;
            return Objects.equals(this.id, other.id);
        } else {
        	return false;
        }
    }

    public String getDisplayName() {
        return stripAllTags(sanitizeBasicHTML(getName()));
    }
}
