package edu.harvard.iq.dataverse.persistence;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

@Stateless
public class DvObjectRepository extends JpaRepository<Long, DvObject> {

    public DvObjectRepository() {
        super(DvObject.class);
    }
    
    public List<DvObject> findByOwnerId(final Long id) {
        return createQuery("SELECT o FROM DvObject o WHERE o.owner.id=:id")
                .setParameter("id", id)
                .getResultList();
    }
    
    public boolean hasData(final Long id) {
        return this.em.createQuery(
        		"SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id", 
        		Long.class)
                .setParameter("id", id)
                .getSingleResult() > 0;
    }
    
    public List<DvObject> findByAuthenticatedUserId(final Long id) {
        return createQuery(
        		"SELECT o FROM DvObject o WHERE o.creator.id=:id or o.releaseUser.id=:id")
                .setParameter("id", id)
                .getResultList();
    }
    
    public Optional<DvObject> findByGlobalId(final String protocol, 
    		final String authority, final String identifier) {
        return getSingleResult(createQuery(
        		"SELECT o FROM DvObject o " + 
        		"WHERE o.identifier=:identifier and o.authority=:authority and o.protocol=:protocol")
                .setParameter("protocol", protocol)
                .setParameter("authority", authority)
                .setParameter("identifier", identifier));
    }
}
