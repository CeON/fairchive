package edu.harvard.iq.dataverse.persistence.user;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.config.LocaleConverter;
import edu.harvard.iq.dataverse.persistence.config.ValidateEmail;
import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;

/**
 * When adding an attribute to this class, be sure to update the following:
 * <p>
 * (1) UserServiceBean.getUserListCore() - native SQL query
 * (2) UserServiceBean.createAuthenticatedUserForView() - add values to a detached AuthenticatedUser object
 *
 * @author rmp553
 */
@SuppressWarnings("serial")
@Entity
public class AuthenticatedUser implements User, Serializable, JpaEntity<Long> {

    public static final String IDENTIFIER_PREFIX = "@";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    /**
     * @todo Shouldn't there be some constraints on what the userIdentifier is
     * allowed to be? It can't be as restrictive as the "userName" field on
     * BuiltinUser because we can't predict what Shibboleth Identity Providers
     * (IdPs) will send (typically in the "eppn" SAML assertion) but perhaps
     * spaces, for example, should be disallowed. Right now "elisah.da mota" can
     * be persisted as a userIdentifier per
     * https://github.com/IQSS/dataverse/issues/2945
     */
    @NotNull
    @Column(nullable = false, unique = true)
    private String userIdentifier;

    @ValidateEmail(message = "{user.invalidEmail}")
    @NotNull
    @Column(nullable = false, unique = true)
    private String email;
    private String orcid;
    private String affiliation;
    private String affiliationROR;
    private String position;

    @NotBlank(message = "{user.lastName}")
    private String lastName;

    @NotBlank(message = "{user.firstName}")
    private String firstName;

    @Column(nullable = true)
    private Timestamp emailConfirmed;

    @Column(nullable = false)
    private Timestamp createdTime;

    @Column(nullable = true)
    private Timestamp lastLoginTime;    // last user login timestamp

    @Column(nullable = true)
    private Timestamp lastApiUseTime;   // last API use with user's token

    @Column(nullable = false)
    @Convert(converter = LocaleConverter.class)
    private Locale notificationsLanguage = ENGLISH;

    @OneToMany(mappedBy = "user", cascade = ALL)
    private List<AcceptedConsent> acceptedConsents = new ArrayList<>();

    @OneToOne(mappedBy = "authenticatedUser")
    private AuthenticatedUserLookup authenticatedUserLookup;

    private boolean superuser;

    @Transient
    private String shibIdentityProvider;

    //For User List Admin dashboard
    @Transient
    private String roles;

    @OneToMany(mappedBy = "user", cascade = {REMOVE, MERGE, PERSIST})
    private List<DatasetLock> datasetLocks;

    // -------------------- GETTERS --------------------

    public List<DatasetLock> getDatasetLocks() {
        return this.datasetLocks;
    }

    public String getRoles() {
        return this.roles;
    }

    public Long getId() {
        return this.id;
    }

    public String getUserIdentifier() {
        return this.userIdentifier;
    }

    public String getEmail() {
        return this.email;
    }

    public String getOrcid() {
        return this.orcid;
    }

    public String getAffiliation() {
        return this.affiliation;
    }

    public String getAffiliationROR() {
        return this.affiliationROR;
    }

    public String getPosition() {
        return this.position;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public Timestamp getEmailConfirmed() {
        return this.emailConfirmed;
    }
    
    @Override
    public Locale getNotificationsLanguage() {
        return this.notificationsLanguage;
    }

    /**
     * Consents that were accepted by user.
     * This is history table so no element should be removed from this list.
     */
    public List<AcceptedConsent> getAcceptedConsents() {
        return this.acceptedConsents;
    }

    @Override
    public boolean isSuperuser() {
        return this.superuser;
    }

    public AuthenticatedUserLookup getAuthenticatedUserLookup() {
        return this.authenticatedUserLookup;
    }

    public String getShibIdentityProvider() {
        return this.shibIdentityProvider;
    }

    public Timestamp getLastLoginTime() {
        return this.lastLoginTime;
    }

    public Timestamp getCreatedTime() {
        return this.createdTime;
    }

    public Timestamp getLastApiUseTime() {
        return this.lastApiUseTime;
    }

    // -------------------- LOGIC --------------------

    @Override
    public String getIdentifier() {
        return IDENTIFIER_PREFIX + this.userIdentifier;
    }
    

    @Override
    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo(this.firstName, this.lastName, 
        		this.email, this.orcid, this.affiliation, this.affiliationROR, 
        		this.position);
    }

    /**
     * Takes the passed info object and updated the internal fields according to it.
     * @param inf the info from which we update the fields.
     */
    public void applyDisplayInfo(final AuthenticatedUserDisplayInfo inf) {
        setFirstName(inf.getFirstName());
        setLastName(inf.getLastName());
        if (isNotBlank(inf.getEmailAddress())) {
            setEmail(inf.getEmailAddress());
        }
        if (isNotBlank(inf.getAffiliation())) {
            setAffiliation(inf.getAffiliation());
        }
        if (isNotBlank(inf.getPosition())) {
            setPosition(inf.getPosition());
        }
        if (isNotBlank(inf.getOrcid())) {
            setOrcid(inf.getOrcid());
        }
        if (isNotBlank(inf.getAffiliationROR())) {
            setAffiliationROR(inf.getAffiliationROR());
        }
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    public String getName() {
        return this.firstName + SPACE + this.lastName;
    }

    public String getSortByString() {
        return format("%s %s %s", getLastName(), getFirstName(), getUserIdentifier());
    }

    // -------------------- SETTERS --------------------

    public void setDatasetLocks(final List<DatasetLock> locks) {
        this.datasetLocks = locks;
    }

    public void setRoles(final String roles) {
        this.roles = roles;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUserIdentifier(final String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    //Stripping spaces to continue support of #2945
    public void setEmail(final String email) {
        this.email = email.trim();
    }

    public void setOrcid(final String orcid) {
        this.orcid = orcid;
    }

    public void setAffiliation(final String affiliation) {
        this.affiliation = affiliation;
    }

    public void setAffiliationROR(final String affiliationROR) {
        this.affiliationROR = affiliationROR;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setEmailConfirmed(final Timestamp emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    public void setNotificationsLanguage(final Locale notificationsLanguage) {
        this.notificationsLanguage = notificationsLanguage;
    }

    public void setSuperuser(final boolean superuser) {
        this.superuser = superuser;
    }

    public void setAuthenticatedUserLookup(final AuthenticatedUserLookup authenticatedUserLookup) {
        this.authenticatedUserLookup = authenticatedUserLookup;
    }

    public void setShibIdentityProvider(final String shibIdentityProvider) {
        this.shibIdentityProvider = shibIdentityProvider;
    }

    public void setLastLoginTime(final Timestamp lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public void setCreatedTime(final Timestamp createdTime) {
        this.createdTime = createdTime;
    }

    public void setLastApiUseTime(final Timestamp lastApiUseTime) {
        this.lastApiUseTime = lastApiUseTime;
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[AuthenticatedUser identifier:" + getIdentifier() + ']';
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        return object instanceof AuthenticatedUser
                && Objects.equals(getId(), ((AuthenticatedUser) object).getId());
    }
}
