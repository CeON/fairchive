package edu.harvard.iq.dataverse.persistence.group;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

/**
 * Persistence for Saml groups.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "samlgroup")
public class SamlGroup implements JpaEntity<Long>, Group, Serializable {

    public final static String GROUP_TYPE = "saml";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the group that will be displayed to the end user.
     */
    @Column(nullable = false)
    private String name;

    /**
     * EntityId of the identity provider.
     */
    @Column(name = "entityid", nullable = false)
    private String entityId;

    // -------------------- CONSTRUCTORS --------------------

    public SamlGroup() { }

    public SamlGroup(final String name, final String entityId) {
        this.name = name;
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "SamlGroup{" + "id=" + id + ", name=" + name + ", entityId=" + entityId + '}';
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getEntityId() {
        return this.entityId;
    }

    @Override
    public String getAlias() {
        return GROUP_TYPE + Group.PATH_SEPARATOR + getId().toString();
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEditable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * i.e. &shib/1
     */
    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + GROUP_TYPE + Group.PATH_SEPARATOR + getId();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(getName(), null);
    }

    // -------------------- equals & hashCode --------------------

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final SamlGroup samlGroup = (SamlGroup) other;
        return Objects.equals(this.id, samlGroup.id)
                && this.name.equals(samlGroup.name)
                && this.entityId.equals(samlGroup.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.entityId);
    }
}
