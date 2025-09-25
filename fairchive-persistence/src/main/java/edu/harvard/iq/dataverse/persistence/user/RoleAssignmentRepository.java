package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.ejb.Singleton;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.List;

@Singleton
public class RoleAssignmentRepository extends JpaRepository<Long, RoleAssignment> {

    // -------------------- CONSTRUCTORS --------------------

    public RoleAssignmentRepository() {
        super(RoleAssignment.class);
    }

    // -------------------- LOGIC --------------------

    public List<RoleAssignment> findByDefinitionPointId(long definitionPointId) {
        return em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", definitionPointId)
                .getResultList();
    }

    public List<RoleAssignment> findByDefinitionPointIds(List<Long> definitionPointIds) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.definitionPoint.id IN :definitionPointIds", RoleAssignment.class)
                .setParameter("definitionPointIds", definitionPointIds)
                .getResultList();
    }

    public List<RoleAssignment> findByAssigneeIdentifier(String assigneeIdentifier) {
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier", RoleAssignment.class)
                .setParameter("assigneeIdentifier", assigneeIdentifier)
                .getResultList();
    }

    public List<RoleAssignment> findByRoleId(long roleId) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.role.id=:roleId", RoleAssignment.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

    public List<RoleAssignment> findByAssigneeIdentifiersAndDefinitionPointIds(List<String> assigneeIdentifiers,
                                                                               List<Long> definitionPointIds) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE "
                                + " r.assigneeIdentifier in :assigneeIdentifiers AND "
                                + " r.definitionPoint.id in :definitionPointIds", RoleAssignment.class)
                .setParameter("assigneeIdentifiers", assigneeIdentifiers)
                .setParameter("definitionPointIds", definitionPointIds)
                .getResultList();
    }

    public int deleteAllByAssigneeIdentifier(String identifier) {
        return em.createNamedQuery("RoleAssignment.deleteAllByAssigneeIdentifier")
                .setParameter("assigneeIdentifier", identifier)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findDataversesWithUserPermitted(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyList();
        }
        String query = "SELECT id FROM dvobject WHERE dtype = 'Dataverse' " +
                "and id in (select definitionpoint_id from roleassignment " +
                "where assigneeidentifier in ("
                + identifiers.stream().map(i -> "'" + i + "'").collect(joining(",")) + "));";
        Query nativeQuery = em.createNativeQuery(query);
        return (List<Integer>) nativeQuery.getResultList();
    }
    
    /**
     * @return A RoleAssignment or null.
     * @todo This might be a good place for Optional.
     */
    public RoleAssignment getRoleAssignmentFromPrivateUrlToken(
            final String privateUrlToken) {
        if (privateUrlToken == null) {
            return null;
        } else {
            try {
                return this.em
                        .createNamedQuery("RoleAssignment.listByPrivateUrlToken",
                                RoleAssignment.class)
                        .setParameter("privateUrlToken", privateUrlToken)
                        .getSingleResult();
            } catch (final NoResultException | NonUniqueResultException ex) {
                return null;
            }
        }
    }

    /**
     * @param dataset A non-null dataset;
     * @return A role assignment for a Private URL, if found, or null.
     * @todo This might be a good place for Optional.
     */
    public RoleAssignment getPrivateUrlRoleAssignmentFromDataset(
            final Dataset dataset, final boolean anonymized) {
        if (dataset == null) {
            return null;
        } else {
            try {
                return this.em.createNamedQuery(
                        "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId",
                        RoleAssignment.class)
                        .setParameter("assigneeIdentifier",
                                new PrivateUrlUser(dataset.getId()).getIdentifier())
                        .setParameter("definitionPointId", dataset.getId())
                        .setParameter("anonymized", anonymized)
                        .getSingleResult();
            } catch (final NoResultException | NonUniqueResultException ex) {
                return null;
            }
        }
    }
}
