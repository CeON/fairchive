package edu.harvard.iq.dataverse.persistence.user;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Table(indexes = {@Index(columnList = "token")
        , @Index(columnList = "builtinuser_id")})
@NamedQueries({
        @NamedQuery(name = "PasswordResetData.findAll",
                query = "SELECT prd FROM PasswordResetData prd"),
        @NamedQuery(name = "PasswordResetData.findByUser",
                query = "SELECT prd FROM PasswordResetData prd WHERE prd.builtinUser = :user"),
        @NamedQuery(name = "PasswordResetData.findByToken",
                query = "SELECT prd FROM PasswordResetData prd WHERE prd.token = :token")
})
@Entity
public class PasswordResetData implements Serializable {

    public enum Reason {
        FORGOT_PASSWORD,
        NON_COMPLIANT_PASSWORD,
        UPGRADE_REQUIRED
    }

    // TODO cleaup: can remove the (unused) id field, and use the token field as an id instead.
    // This will prevent duplicate tokens (ok, not a likely poroblem) and would
    // make the token lookup much faster.

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String token;

    /**
     * @todo Is there an annotation to help enforce that a given DataverseUser
     * can only have one token at a time?
     */
    @OneToOne
    @JoinColumn(nullable = false)
    private BuiltinUser builtinUser;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private Timestamp expires;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    /**
     * This is only here because it has to be: "The class should have a no-arg,
     * public or protected constructor." Please use the constructor that takes
     * arguments.
     */
    @Deprecated
    public PasswordResetData() {
    }

    public PasswordResetData(final BuiltinUser user, final Reason reason,
    		final long minutesUntilPasswordResetDataExpires) {
        this.builtinUser = user;
        this.reason = reason;
        this.token = randomUUID().toString();
        final Instant now = Instant.now();
        this.created = Timestamp.from(now);
        this.expires = Timestamp.from(now.plus(minutesUntilPasswordResetDataExpires, MINUTES));
    }

    public boolean isExpired() {
    	return this.expires.before(new Date());
    }

    public String getToken() {
        return this.token;
    }

    public BuiltinUser getBuiltinUser() {
        return this.builtinUser;
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

    public void setId(final Long id) {
        this.id = id;
    }

    public Reason getReason() {
        return this.reason;
    }

    public void setReason(final Reason reason) {
        this.reason = reason;
    }

    public void setExpires(final Timestamp expires) {
        this.expires = expires;
    }
}
