package edu.harvard.iq.dataverse.persistence.user;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author bsilverstein
 */
@SuppressWarnings("serial")
@Table(indexes = {
        @Index(columnList = "token"),
        @Index(columnList = "authenticateduser_id")})
@NamedQueries({
        @NamedQuery(name = "ConfirmEmailData.findAll",
                query = "SELECT prd FROM ConfirmEmailData prd"),
        @NamedQuery(name = "ConfirmEmailData.findByUser",
                query = "SELECT prd FROM ConfirmEmailData prd WHERE prd.authenticatedUser = :user"),
        @NamedQuery(name = "ConfirmEmailData.findByToken",
                query = "SELECT prd FROM ConfirmEmailData prd WHERE prd.token = :token")
})
@Entity
public class ConfirmEmailData implements Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String token;

    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private Timestamp expires;

    public ConfirmEmailData(final AuthenticatedUser user, 
    		final long minutesUntilConfirmEmailTokenExpires) {
        this.authenticatedUser = user;
        this.token = randomUUID().toString();
        final Instant now = Instant.now();
        this.created = Timestamp.from(now);
        this.expires = Timestamp.from(now.plus(minutesUntilConfirmEmailTokenExpires, MINUTES));
    }

    public boolean isExpired() {
        return this.expires !=  null ? this.expires.before(new Date()) : true;
    }

    public String getToken() {
        return this.token;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    public Timestamp getCreated() {
        return this.created;
    }

    public Timestamp getExpires() {
        return this.expires;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This is only here because it has to be: "The class should have a no-arg,
     * public or protected constructor." Please use the constructor that takes
     * arguments.
     */
    @Deprecated
    public ConfirmEmailData() {
    }

    public void setAuthenticatedUser(final AuthenticatedUser user) {
        this.authenticatedUser = user;
    }

}
