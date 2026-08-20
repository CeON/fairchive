package edu.harvard.iq.dataverse.mocks;

import java.time.Clock;
import java.util.Optional;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.qualifiers.TestBean;

/**
 * @author madunlap
 */
@TestBean
public class MockAuthenticationServiceBean extends AuthenticationServiceBean {

    public MockAuthenticationServiceBean() {
        super();
    }

    public MockAuthenticationServiceBean(Clock clock) {
        super(clock);
    }

    @Override
    public AuthenticatedUser getAuthenticatedUser(String identifier) {
        return new MockAuthenticatedUser();
    }

    @Override
    public AuthenticatedUser getAuthenticatedUserByEmail(String email) {
        return new MockAuthenticatedUser();
    }

    @Override
    public Optional<ApiToken> findApiTokenByUser(AuthenticatedUser au) {
        return Optional.of(generateApiToken(au));
    }
}
