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
    
    public Optional<Dataverse> findByAlias(String anAlias) {
        return getSingleResult(
                this.em.createQuery(
                        "SELECT dv FROM Dataverse dv WHERE LOWER(dv.alias)=:alias",
                        Dataverse.class)
                        .setParameter("alias", anAlias.toLowerCase()));
    }
    
}
