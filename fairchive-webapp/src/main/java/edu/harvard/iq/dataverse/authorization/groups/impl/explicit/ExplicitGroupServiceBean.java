package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    protected RoleAssigneeServiceBean roleAssigneeSvc;

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
        return groupService.getExplicitGroupProvider();
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
                    if (roleAssigneeSvc.getRoleAssignee(idtf) == null) {
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

    public void removeGroup(ExplicitGroup explicitGroup) {
        em.remove(explicitGroup);
    }

    /**
     * Returns all the explicit groups that are available in the context of the passed DvObject.
     *
     * @param d The DvObject where the groups are queried
     * @return All the explicit groups defined at {@code d} and its ancestors.
     */
    public Set<ExplicitGroup> findAvailableFor(DvObject d) {
        Set<ExplicitGroup> egs = new HashSet<>();
        while (d != null) {
            egs.addAll(findByOwnerId(d.getId()));
            d = d.getOwner();
        }
        return egs;
    }

    /**
     * Finds all the explicit groups {@code ra} is <b>directly</b> a member of.
     * To find all these groups and the groups the contain them (recursively upwards),
     * consider using {@link #findGroups(edu.harvard.iq.dataverse.persistence.user.RoleAssignee)}
     *
     * @param ra the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra} directly.
     * @see #findGroups(edu.harvard.iq.dataverse.persistence.user.RoleAssignee)
     */
    public Set<ExplicitGroup> findDirectlyContainingGroups(RoleAssignee ra) {
        if (ra instanceof AuthenticatedUser) {
            return new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByAuthenticatedUserIdentifier", ExplicitGroup.class)
                                    .setParameter("authenticatedUserIdentifier", ra.getIdentifier().substring(1))
                                    .getResultList()
                    );
        } else if (ra instanceof ExplicitGroup) {
            return new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByContainedExplicitGroupId", ExplicitGroup.class)
                                    .setParameter("containedExplicitGroupId", ((ExplicitGroup) ra).getId())
                                    .getResultList()
                    );
        } else {
            return new HashSet<>(
                            em.createNamedQuery("ExplicitGroup.findByRoleAssgineeIdentifier", ExplicitGroup.class)
                                    .setParameter("roleAssigneeIdentifier", ra.getIdentifier())
                                    .getResultList()
                    );
        }
    }


    /**
     * Finds all the explicit groups {@code ra} is a member of.
     *
     * @param ra the role assignee whose membership list we seek
     * @return set of the explicit groups that contain {@code ra}.
     */
    public Set<ExplicitGroup> findGroups(RoleAssignee ra) {
        return findClosure(findDirectlyContainingGroups(ra));
    }

    /**
     * Finds all the groups {@code ra} is a member of, in the context of {@code o}.
     * This includes both direct and indirect memberships.
     *
     * @param ra The role assignee whose memberships we seek.
     * @param o  The {@link DvObject} whose context we search.
     * @return All the groups in {@code o}'s context that {@code ra} is a member of.
     */
    public Set<ExplicitGroup> findGroups(RoleAssignee ra, DvObject o) {
        return findGroups(ra).stream()
                .filter(g -> g.getOwner().isAncestorOf(o))
                .collect(Collectors.toSet());
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
    public void revokeAllGroupsForAssignee(RoleAssignee assignee) {
        if (assignee instanceof AuthenticatedUser) {
            em.createNativeQuery("DELETE FROM explicitgroup_authenticateduser WHERE containedauthenticatedusers_id=" + ((AuthenticatedUser) assignee).getId()).executeUpdate();
        } else if (assignee instanceof ExplicitGroup) {
            em.createNativeQuery("DELETE FROM explicitgroup_explicitgroup WHERE containedexplicitgroups_id=" + ((ExplicitGroup) assignee).getId()).executeUpdate();
        }
    }

    /**
     * Returns a set of all direct members of the group, including
     * logical role assignees.
     *
     * @return members of the group.
     */
    public Set<RoleAssignee> getDirectMembers(ExplicitGroup explicitGroup) {
        Set<RoleAssignee> res = new HashSet<>();

        res.addAll(explicitGroup.getContainedExplicitGroups());
        res.addAll(explicitGroup.getContainedAuthenticatedUsers());
        for (String idtf : explicitGroup.getContainedRoleAssignees()) {
            RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
            if (ra != null) {
                res.add(ra);
            }
        }

        return res;
    }
}
