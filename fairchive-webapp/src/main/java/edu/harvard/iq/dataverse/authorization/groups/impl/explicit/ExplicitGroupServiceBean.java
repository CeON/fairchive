package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroupRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;

/**
 * A bean providing the {@link ExplicitGroupProvider}s with container services,
 * such as database connectivity.
 *
 * @author michael
 */
@Stateless
public class ExplicitGroupServiceBean {

    @EJB
    protected RoleAssigneeServiceBean roleAssigneeService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;
    
    @EJB
    private GroupServiceBean groupService;
    
    @EJB
    private ExplicitGroupRepository repository;

    /**
     * A PostgreSQL-specific query that returns a group and all the groups
     * that contain it, and their parents too (-> recourse up teh containment
     * hierarchy of the explicit groups). Takes the group id as a parameter.
     */
    private static final String FIND_ALL_PARENTS_QUERY_TEMPLATE = "WITH RECURSIVE\n" +
            "explicit_group_graph AS (\n" +
            "  SELECT\n" +
            "     eg.id as id,\n" +
            "     ee.explicitgroup_id as parent_group_id\n" +
            "  FROM explicitgroup eg \n" +
            "    LEFT JOIN explicitgroup_explicitgroup ee \n" +
            "      ON eg.id=ee.containedexplicitgroups_id\n" +
            "),\n" +
            "parents AS (\n" +
            "  SELECT * FROM explicit_group_graph\n" +
            "  WHERE \n" +
            "    id IN (@IDS)\n" +
            "  UNION ALL\n" +
            "  SELECT egg.*\n" +
            "  FROM explicit_group_graph egg, parents\n" +
            "  WHERE parents.parent_group_id = egg.id\n" +
            ") SELECT * from explicitgroup \n" +
            "WHERE id IN (SELECT distinct id FROM parents);";

    public ExplicitGroupProvider getProvider() {
        return this.groupService.getExplicitGroupProvider();
    }

    public ExplicitGroup persist(ExplicitGroup g) {
        if (g.getId() == null) {
            em.persist(g);
            return g;
        } else {
            // clean stale data once in a while
            if (Math.random() >= 0.5) {
                Set<String> stale = new TreeSet<>();
                for (String idtf : g.getContainedRoleAssignees()) {
                    if (roleAssigneeService.getRoleAssignee(idtf) == null) {
                        stale.add(idtf);
                    }
                }
                if (!stale.isEmpty()) {
                    g.getContainedRoleAssignees().removeAll(stale);
                }
            }

            return em.merge(g);
        }
    }

    public List<ExplicitGroup> findByOwnerId(final Long dvObjectId) {
        return this.repository.findByOwnerId(dvObjectId);
    }

    public ExplicitGroup findByAlias(final String groupAlias) {
        return this.repository.findByAlias(groupAlias).orElse(null);
    }

    public ExplicitGroup findByOwnerIdAndAlias(final Long ownerId, final String groupAliasInOwner) {
        return this.repository.findByOwnerIdAndAlias(ownerId, groupAliasInOwner).orElse(null);
    }

    public void removeGroup(final ExplicitGroup group) {
    	this.repository.delete(group);
    }

    /**
     * Returns all the explicit groups that are available in the context of the passed DvObject.
     *
     * @param owner The DvObject where the groups are queried
     * @return All the explicit groups defined at {@code d} and its ancestors.
     */
    public Set<ExplicitGroup> findAvailableFor(DvObject owner) {
        final Set<ExplicitGroup> result = new HashSet<>();
        while (owner != null) {
            result.addAll(findByOwnerId(owner.getId()));
            owner = owner.getOwner();
        }
        return result;
    }

    /**
     * Finds all the explicit groups {@code ra} is <b>directly</b> a member of.
     * To find all these groups and the groups the contain them (recursively upwards),
     * consider using {@link #findGroups(edu.harvard.iq.dataverse.persistence.user.RoleAssignee)}
     *
     * @param assignee the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra} directly.
     * @see #findGroups(edu.harvard.iq.dataverse.persistence.user.RoleAssignee)
     */
    public Set<ExplicitGroup> findDirectlyContainingGroups(final RoleAssignee assignee) {
        if (assignee instanceof AuthenticatedUser) {
            return new HashSet<>(this.repository.
            		findByAuthUserIdentifier(assignee.getIdentifier().substring(1)));
        } else if (assignee instanceof ExplicitGroup) {
            return new HashSet<>(this.repository.
            		findByContainedExplicitGroupId(((ExplicitGroup) assignee).getId()));
        } else {
            return new HashSet<>(this.repository.
            		findByRoleAssgineeIdentifier(assignee.getIdentifier()));
        }
    }


    /**
     * Finds all the explicit groups {@code ra} is a member of.
     *
     * @param ra the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra}.
     */
    public Set<ExplicitGroup> findGroups(final RoleAssignee assignee) {
        return findClosure(findDirectlyContainingGroups(assignee));
    }

    /**
     * Finds all the groups {@code ra} is a member of, in the context of {@code o}.
     * This includes both direct and indirect memberships.
     *
     * @param rassignee The role assignee whose memberships we seek.
     * @param owner  The {@link DvObject} whose context we search.
     * @return All the groups in {@code o}'s context that {@code ra} is a member of.
     */
    public Set<ExplicitGroup> findGroups(final RoleAssignee rassignee, final DvObject owner) {
        return findGroups(rassignee).stream()
                .filter(g -> g.getOwner().isAncestorOf(owner))
                .collect(toSet());
    }

    /**
     * Finds all the groups that contain the groups in {@code seed} (including {@code seed}), and the
     * groups that contain these groups, an so on.
     *
     * @param seed the initial set of groups.
     * @return Transitive closure (based on group  containment) of the groups in {@code seed}.
     */
    @SuppressWarnings("unchecked")
    protected Set<ExplicitGroup> findClosure(Set<ExplicitGroup> seed) {

        if (seed.isEmpty()) {
            return Collections.emptySet();
        }

        String ids = seed.stream().map(eg -> Long.toString(eg.getId())).collect(joining(","));

        // PSQL driver has issues with arrays and collections as parameters, so we're using 
        // string manipulation to create the query here. Not ideal, but seems to be
        // the only solution at the Java Persistence level (i.e. without downcasting to org.postgresql.*)
        String sqlCode = FIND_ALL_PARENTS_QUERY_TEMPLATE.replace("@IDS", ids);
        return new HashSet<>(em.createNativeQuery(sqlCode, ExplicitGroup.class)
                                     .getResultList());
    }

    /**
     * Fully strips the assignee of membership in all the explicit groups.
     *
     * @param assignee User or Group
     */
    public void revokeAllGroupsFor(final AuthenticatedUser user) {
    	this.repository.revokeAllGroupForAuthUser(user.getId());
    }

    /**
     * Returns a set of all direct members of the group, including
     * logical role assignees.
     *
     * @return members of the group.
     */
    public Set<RoleAssignee> getDirectMembers(ExplicitGroup group) {
        final Set<RoleAssignee> result = new HashSet<>();

        result.addAll(group.getContainedExplicitGroups());
        result.addAll(group.getContainedAuthenticatedUsers());
        group.getContainedRoleAssignees().stream()
        	.map(this.roleAssigneeService::getRoleAssignee)
        	.filter(Objects::nonNull)
        	.forEach(result::add);
        
        return result;
    }
}
