package edu.harvard.iq.dataverse.persistence.group;

import java.util.List;
import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class ExplicitGroupRepository extends JpaRepository<Long, ExplicitGroup>{

    public ExplicitGroupRepository() {
        super(ExplicitGroup.class);
    }
    
	public List<ExplicitGroup> findByOwnerId(final Long ownerId) {
		return createQuery("SELECT eg FROM ExplicitGroup eg WHERE eg.owner.id=:id")
				.setParameter("id", ownerId)
				.getResultList();
	}
	
    public Optional<ExplicitGroup> findByAlias(final String alias) {
        return getSingleResult(createQuery(
        		"SELECT eg FROM ExplicitGroup eg WHERE eg.groupAlias=:alias")
                .setParameter("alias", alias));
    }
    
    public Optional<ExplicitGroup> findByOwnerIdAndAlias(final Long ownerId, 
    		final String groupAliasInOwner) {
         return getSingleResult(createQuery(
        		 "SELECT eg FROM ExplicitGroup eg" + 
                 " WHERE eg.owner.id=:ownerId AND eg.groupAliasInOwner=:alias")
                 .setParameter("alias", groupAliasInOwner)
                 .setParameter("ownerId", ownerId));
    }
    
    public void revokeAllGroupForAuthUser(final Long authUserId) {
    	this.em.createNativeQuery(
    			"DELETE FROM explicitgroup_authenticateduser" + 
    			" WHERE containedauthenticatedusers_id=?")
    			.setParameter(1, authUserId)
    			.executeUpdate();
    }
    
	public List<ExplicitGroup> findByAuthUserIdentifier(final String identifier) {
		return createQuery(
				"SELECT eg FROM ExplicitGroup eg JOIN eg.containedAuthenticatedUsers au" +
                " WHERE au.userIdentifier=:id")
				.setParameter("id", identifier)
				.getResultList();
	}
	
	public List<ExplicitGroup> findByContainedExplicitGroupId(final Long id) {
		return createQuery(
				"SELECT eg FROM ExplicitGroup eg join eg.containedExplicitGroups ceg" +
                 " WHERE ceg.id=:id")
				.setParameter("id", id)
				.getResultList();
	}
	
	public List<ExplicitGroup> findByRoleAssgineeIdentifier(final String identifier) {
		return createQuery(
				"SELECT eg FROM ExplicitGroup eg JOIN eg.containedRoleAssignees cra" +
                 " WHERE cra=:id")
				.setParameter("id", identifier)
				.getResultList();
	}
}
