package edu.harvard.iq.dataverse.persistence.user;

import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class ApiTokenRepository extends JpaRepository<Long, ApiToken> {

    // -------------------- CONSTRUCTORS --------------------

    public ApiTokenRepository() {
        super(ApiToken.class);
    }

    public Optional<ApiToken> findByUser(final AuthenticatedUser user) {
    	
    	return getSingleResult(createQuery(
    			"SELECT t FROM ApiToken t WHERE t.authenticatedUser = :user")
    			.setParameter("user", user));
    }
    
    public Optional<ApiToken> findByToken(final String token) {
    	
    	return getSingleResult(createQuery(
    			"SELECT t FROM ApiToken t WHERE t.tokenString = :token")
    			.setParameter("token", token));
    }
}
