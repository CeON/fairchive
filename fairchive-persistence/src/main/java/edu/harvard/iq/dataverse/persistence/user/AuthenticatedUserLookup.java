package edu.harvard.iq.dataverse.persistence.user;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

/**
 * A somewhat glorified key-value pair, persisted in the database.
 * The value is the {@link AuthenticatedUser}, the internal user representation pointed by the
 * IDP, and the key is a pair of the authentication provider's alias and the user's persistent id
 * within that authentication provider. These objects may be used both for storage (the full constructor)
 * and retrieval (the idp+id constructor, and then {@link #getLookupKey()}.
 *
 * @author pdurbin
 * @author michael
 */
@SuppressWarnings("serial")
@Table(
        uniqueConstraints =
        @UniqueConstraint(columnNames = {"persistentuserid", "authenticationproviderid"})
)
@NamedQueries({
        @NamedQuery(name = "AuthenticatedUserLookup.findByAuthPrvID_PersUserId",
                query = "SELECT au FROM AuthenticatedUserLookup au "
                        + "WHERE au.authenticationProviderId=:authPrvId "
                        + "  AND au.persistentUserId=:persUserId "),
        @NamedQuery(name = "AuthenticatedUserLookup.findByAuthUser",
                query = "SELECT au FROM AuthenticatedUserLookup au WHERE au.authenticatedUser=:authUser")
})
@Entity
public class AuthenticatedUserLookup implements Serializable {

    public static final String ORCID_PROVIDER_ID_PRODUCTION = "orcid";
    public static final String ORCID_PROVIDER_ID_SANDBOX = "orcid-sandbox";
    
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    private String authenticationProviderId;
    private String persistentUserId;

    @OneToOne(cascade = {PERSIST, MERGE})
    @JoinColumn(unique = true, nullable = false)
    private AuthenticatedUser authenticatedUser;

    public AuthenticatedUserLookup(final String persistentUserIdFromIdp, 
    		final String idp) {
        this(persistentUserIdFromIdp, idp, null);
    }

    public AuthenticatedUserLookup(final String persistentUserIdFromIdp, 
    		final String authPrvId, final AuthenticatedUser authenticatedUser) {
        this.persistentUserId = persistentUserIdFromIdp;
        this.authenticationProviderId = authPrvId;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Constructor for JPA
     */
    public AuthenticatedUserLookup() {
    }

    public long getId() {
        return this.id;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    public void setAuthenticatedUser(final AuthenticatedUser user) {
        this.authenticatedUser = user;
    }

    public String getAuthenticationProviderId() {
        return this.authenticationProviderId;
    }

    public void setAuthenticationProviderId(final String id) {
        this.authenticationProviderId = id;
    }

    public String getPersistentUserId() {
        return this.persistentUserId;
    }

    public void setPersistentUserId(final String id) {
        this.persistentUserId = id;
    }


}
