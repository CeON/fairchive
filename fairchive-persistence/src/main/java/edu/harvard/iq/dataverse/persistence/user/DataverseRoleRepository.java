package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class DataverseRoleRepository extends JpaRepository<Long, DataverseRole> {

    public DataverseRoleRepository() {
        super(DataverseRole.class);
    }

	public List<DataverseRole> findByOwnerId(final Long id) {
		return createQuery("SELECT r FROM DataverseRole r" 
				+ " WHERE r.owner.id=:ownerId ORDER BY r.name")
				.setParameter("ownerId", id)
				.getResultList();
	}
    
    public List<DataverseRole> findWithoutOwner() {
        return createQuery("SELECT r FROM DataverseRole r"
        		+ " WHERE r.owner is null ORDER BY r.name")
                .getResultList();
    }

    public Optional<DataverseRole> findByAlias(final String alias) {
        return getSingleResult(
                createQuery("SELECT r FROM DataverseRole r WHERE r.alias=:alias")
                 .setParameter("alias", alias));
    }
}