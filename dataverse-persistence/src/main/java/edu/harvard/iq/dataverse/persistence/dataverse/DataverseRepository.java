package edu.harvard.iq.dataverse.persistence.dataverse;

import java.util.List;
import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DataverseRepository extends JpaRepository<Long, Dataverse> {

    // -------------------- CONSTRUCTORS --------------------

    public DataverseRepository() {
        super(Dataverse.class);
    }

    // -------------------- LOGIC --------------------

    public List<Dataverse> findPublishedByOwnerId(final Long ownerId) {
        return this.em.createQuery(
                "select d from Dataverse d where d.owner.id =:ownerId and d.publicationDate is not null order by d.name",
                Dataverse.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Dataverse> findByOwnerId(final Long ownerId) {
        return this.em.createQuery(
                "select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name",
                Dataverse.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public Dataverse findRoot() {
        return this.em.createQuery(
                "SELECT d FROM Dataverse d where d.owner.id=null",
                Dataverse.class)
                .getSingleResult();
    }

    public Long countRoots() {
        return this.em.createQuery(
                "SELECT count(dv) FROM Dataverse dv WHERE dv.owner.id=null",
                Long.class)
                .getSingleResult();
    }

    public List<Long> findAllIDs() {
        return this.em.createQuery("SELECT o.id FROM Dataverse o ORDER BY o.id",
                Long.class)
                .getResultList();
    }

    public List<Long> findAllUnindexedIDs() {
        return this.em.createQuery(
                "SELECT o.id FROM Dataverse o WHERE o.indexTime IS null ORDER BY o.id",
                Long.class)
                .getResultList();
    }

    public List<Long> findIDsByOwnerID(final Long ownerId) {
        return this.em.createQuery(
                "select o.id from Dataverse as o where o.owner.id =:ownerId order by o.id",
                Long.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public Optional<Dataverse> findByAlias(String alias) {
        return getSingleResult(
                this.em.createQuery(
                        "SELECT dv FROM Dataverse dv WHERE LOWER(dv.alias)=:alias",
                        Dataverse.class)
                        .setParameter("alias", alias.toLowerCase()));
    }

    public List<Dataverse> findByAliasOrName(final String alias, final String name) {
        return this.em.createQuery(
                "SELECT dv FROM Dataverse dv " +
                "WHERE (LOWER(dv.alias) LIKE :alias) OR (LOWER(dv.name) LIKE :name) "+
                "order by dv.alias",
                Dataverse.class)
                .setParameter("alias", "%" + alias.toLowerCase() + "%")
                .setParameter("name", "%" + name.toLowerCase() + "%")
                .getResultList();
    }

    public List<Dataverse> findByAliasOrNameOrAffiliation(final String alias,
            final String name, final String affiliation) {
        return this.em.createQuery(
                "SELECT dv FROM Dataverse dv "+
                "WHERE (LOWER(dv.alias) LIKE :alias) OR (LOWER(dv.name) LIKE :name) OR (LOWER(dv.affiliation) LIKE :affiliation) " +
                 "order by dv.alias",
                Dataverse.class)
                .setParameter("alias", alias.toLowerCase() + "%")
                .setParameter("name", "%" + name.toLowerCase() + "%")
                .setParameter("affiliation", "%" + affiliation.toLowerCase() + "%")
                .getResultList();
    }

    public Long countDataversesWithParent(final Long parentId) {
        return (Long) this.em.createNativeQuery(
                "SELECT count(1) FROM dvobject WHERE dtype='Dataverse' AND owner_id = ?1")
                .setParameter(1, parentId)
                .getSingleResult();
    }

    public List<Object[]> getParentAliasesForIds(final List<Long> ids) {
        return this.em.createQuery(
                "SELECT o.id, dv.alias FROM Dataverse dv, DvObject o " +
                        "WHERE dv.id = o.owner.id AND o.id IN :ids",
                Object[].class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public Long countChildrenOf(final Dataverse datavserse) {
        return this.em.createQuery(
                "SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id",
                Long.class)
                .setParameter("id", datavserse.getId())
                .getSingleResult();
    }
}
