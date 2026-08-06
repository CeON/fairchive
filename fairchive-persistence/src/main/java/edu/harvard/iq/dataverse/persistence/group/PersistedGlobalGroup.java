package edu.harvard.iq.dataverse.persistence.group;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

/**
 * Convenience base class for implementing groups that apply to the entire Dataverse
 * installation, and are persisted to the DB.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(columnList = "dtype")})
public abstract class PersistedGlobalGroup implements Group, JpaEntity<Long>, Serializable {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * A unique alias within the Dataverse system installation.
     */
    @Column(unique = true)
    private String persistedGroupAlias;

    private String displayName;
    private String description;

    @Transient
    private String groupProviderAlias;

    // -------------------- CONSTRUCTORS --------------------

    public PersistedGlobalGroup() { }

    public PersistedGlobalGroup(String groupProviderAlias) {
        this.groupProviderAlias = groupProviderAlias;
    }

    // -------------------- GETTERS --------------------
    @Override
    public Long getId() {
        return this.id;
    }

    public String getPersistedGroupAlias() {
        return this.persistedGroupAlias;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + getAlias();
    }

    @Override
    public String getAlias() {
        return this.groupProviderAlias + Group.PATH_SEPARATOR + this.persistedGroupAlias;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(displayName, null);
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setPersistedGroupAlias(String alias) {
        this.persistedGroupAlias = alias;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[PersistedGlobalGroup " + getIdentifier() + "]";
    }
}
