package edu.harvard.iq.dataverse.persistence.group;

import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class IpGroupRepository extends JpaRepository<Long, IpGroup>{

    public IpGroupRepository() {
        super(IpGroup.class);
    }
    
    public Optional<IpGroup> getByAlias(final String alias) {
    	return getSingleResult(createQuery(
    			"SELECT g FROM IpGroup g WHERE g.persistedGroupAlias=:alias")
    			.setParameter("alias", alias));
    }
}
