package edu.harvard.iq.dataverse.persistence.group;

import static java.util.Objects.requireNonNull;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.User;

/**
 * A group that explicitly lists {@link RoleAssignee}s that belong to it. Implementation-wise,
 * there are three cases here: {@link AuthenticatedUser}s, other {@link ExplicitGroup}s, and all the rest.
 * AuthenticatedUsers and ExplicitGroups go in tables of their own. The rest are kept via their identifier.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(columnList = "owner_id"),
        @Index(columnList = "groupaliasinowner")})
public class ExplicitGroup implements Group, Serializable, JpaEntity<Long> {

    public final static String GROUP_TYPE = "explicit";
    
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    /**
     * Authenticated users directly added to the group.
     */
    @ManyToMany
    private Set<AuthenticatedUser> containedAuthenticatedUsers = new HashSet<>();

    /**
     * Explicit groups that belong to {@code this} explicit gorups.
     */
    @ManyToMany
    @JoinTable(name = "explicitgroup_explicitgroup",
            joinColumns = @JoinColumn(name = "explicitgroup_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "containedexplicitgroups_id", referencedColumnName = "id"))
    Set<ExplicitGroup> containedExplicitGroups = new HashSet<>();

    /**
     * All the role assignees that belong to this group
     * and are not {@link authenticatedUser}s or {@ExplicitGroup}s, are stored
     * here via their identifiers.
     *
     * @see RoleAssignee#getIdentifier()
     */
    @ElementCollection
    private Set<String> containedRoleAssignees = new HashSet<>();

    @Column(length = 1024)
    private String description;

    @NotBlank
    private String displayName;

    /**
     * The DvObject under which this group is defined.
     */
    @ManyToOne
    DvObject owner;

    /**
     * Given alias of the group, e.g by the user that created it. Unique in the owner.
     */
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}")
    private String groupAliasInOwner;

    /**
     * Alias of the group. Calculated from the group's name and its owner id. Unique in the table.
     */
    @Column(unique = true)
    private String groupAlias;

    public Set<AuthenticatedUser> getContainedAuthenticatedUsers() {
        return this.containedAuthenticatedUsers;
    }

    public Set<ExplicitGroup> getContainedExplicitGroups() {
        return this.containedExplicitGroups;
    }

    public void add(final User user) {
    	requireNonNull(user, "Cannot add a null user to an explicit group.");
        if (user instanceof AuthenticatedUser) {
            this.containedAuthenticatedUsers.add((AuthenticatedUser) user);
        } else {
            this.containedRoleAssignees.add(user.getIdentifier());
        }
    }

    /**
     * Adds the {@link RoleAssignee} to {@code this} group.
     *
     * @param assignee the role assignee to be added to this group.
     * @throws GroupException if {@code ra} is a group, and is either an ancestor of {@code this},
     *                        or is defined in a dataverse that is not an ancestor of {@code this.owner}.
     */
    public void add(final RoleAssignee assignee)  {
        if (assignee.equals(this)) {
            throw new GroupException(this, "A group cannot be added to itself.");
        }
        if (assignee instanceof User) {
            add((User) assignee);
        } else {
            if (assignee instanceof ExplicitGroup) {
                // validate no circular deps
                final ExplicitGroup group = (ExplicitGroup) assignee;
                if (group.structuralContains(this)) {
                    throw new GroupException(this, "A group cannot be added to one of its childs.");
                }
                if (group.owner.isAncestorOf(this.owner)) {
                    this.containedExplicitGroups.add(group);
                } else {
                    throw new GroupException(this, "Cannot add " + group + 
                    		", as it is not defined in " + this.owner + 
                    		" or one of its ancestors.");
                }
            } else {
                this.containedRoleAssignees.add(assignee.getIdentifier());
            }
        }
    }

    public void remove(final RoleAssignee roleAssignee) {
        removeByRoleAssgineeIdentifier(roleAssignee.getIdentifier());
    }

    /**
     * Returns all the role assignee identifiers in this group. <br>
     * <b>Note</b> some of the identifiers may be stale (i.e. group deleted but
     * identifiers lingered for a while).
     *
     * @return A list of the role assignee identifiers.
     */
    public Set<String> getContainedRoleAssgineeIdentifiers() {
        final Set<String> result = new TreeSet<>();
        result.addAll(this.containedRoleAssignees);
        for (final ExplicitGroup subg : getContainedExplicitGroups()) {
            result.add(subg.getIdentifier());
        }
        for (final AuthenticatedUser au : this.containedAuthenticatedUsers) {
            result.add(au.getIdentifier());
        }
        return result;
    }

    public void removeByRoleAssgineeIdentifier(final String identifier) {
        if (this.containedRoleAssignees.contains(identifier)) {
            this.containedRoleAssignees.remove(identifier);
        } else {
            for (final AuthenticatedUser au : this.containedAuthenticatedUsers) {
                if (au.getIdentifier().equals(identifier)) {
                    this.containedAuthenticatedUsers.remove(au);
                    return;
                }
            }
            for (final ExplicitGroup eg : this.containedExplicitGroups) {
                if (eg.getIdentifier().equals(identifier)) {
                    this.containedExplicitGroups.remove(eg);
                    return;
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Looks at structural containment: whether {@code ra} is part of the
     * group's structure. It mostly the same as {@link #contains(edu.harvard.iq.dataverse.engine.command.DataverseRequest)},
     * except for logical containment. So if an ExplicitGroup contains {@link AuthenticatedUsers} but not
     * a specific {@link AuthenticatedUser} {@code u}, {@code structuralContains(u)}
     * would return {@code false} while {@code contains( request(u, ...) )} would return true;
     *
     * @param assignee
     * @return {@code true} iff the role assignee is structurally a part of the group.
     */
    public boolean structuralContains(final RoleAssignee assignee) {
        // direct containment
        if (assignee instanceof AuthenticatedUser &&
        		this.containedAuthenticatedUsers.contains(assignee)) {
        	return true;
        } else if (assignee instanceof ExplicitGroup &&
        		this.containedExplicitGroups.contains(assignee)) {
        	return true;
        } else if (this.containedRoleAssignees.contains(assignee.getIdentifier())) {
            return true;
        } else {
        	// no direct containment. Recurse.
        	return this.containedExplicitGroups.stream().
        			anyMatch(group -> group.structuralContains(assignee));
        }
    }

    /**
     * Updates the alias of the group. Call this after setting the owner or the
     * groupAliasInOwner fields. JPA-related activities call this automatically.
     */
	public void updateAlias() {
		this.groupAlias = getOwner() != null 
				? getOwner().getId() + "-" + getGroupAliasInOwner()
				: getGroupAliasInOwner(); 
	}

    @PrePersist
    void prepersist() {
        updateAlias();
    }

    @PostLoad
    void postload() {
        updateAlias();
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + GROUP_TYPE
                + Group.PATH_SEPARATOR + getAlias();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(getDisplayName(), null);
    }

    public String getGroupAliasInOwner() {
        return this.groupAliasInOwner;
    }

    public void setGroupAliasInOwner(final String groupAliasInOwner) {
        this.groupAliasInOwner = groupAliasInOwner;
    }

    @Override
    public String getAlias() {
        return this.groupAlias;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public DvObject getOwner() {
        return this.owner;
    }

    public void setOwner(final DvObject owner) {
        this.owner = owner;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return 53 * Objects.hashCode(this.id) + Objects.hashCode(this.groupAliasInOwner);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExplicitGroup)) {
            return false;
        }
        final ExplicitGroup other = (ExplicitGroup) obj;
        if (this.id != null && other.getId() != null) {
            return Objects.equals(this.id, other.getId());
        } else {
            return Objects.equals(this.groupAliasInOwner, other.groupAliasInOwner)
                    && Objects.equals(this.owner, other.owner);
        }
    }

    /**
     * Low-level call to return the role assignee identifier strings. Note that
     * the role assignees themselves might be stale, which is why this call is here -
     * to allow the {@link ExplicitGroupServiceBean} to clean up this collection.
     *
     * @return the strings of the role assignees in this group.
     */
    public Set<String> getContainedRoleAssignees() {
        return this.containedRoleAssignees;
    }

    @Override
    public String toString() {
        return "[ExplicitGroup " + groupAlias + "]";
    }

}
