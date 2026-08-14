package edu.harvard.iq.dataverse.persistence.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Timestamp;

@SuppressWarnings("serial")
@Entity
@NamedQueries({
        @NamedQuery(name = "ApiToken.findByTokenString", query = "SELECT t FROM ApiToken t WHERE t.tokenString = :tokenString"),
        @NamedQuery(name = "ApiToken.findByUser", query = "SELECT t FROM ApiToken t WHERE t.authenticatedUser = :user"),
        @NamedQuery(name = "ApiToken.deleteByIds", query = "DELETE FROM ApiToken WHERE t.id IN :ids")
})
@Table(indexes = {@Index(columnList = "authenticateduser_id")})
public class ApiToken implements Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String tokenString;

    @NotNull
    @JoinColumn(nullable = false)
    @ManyToOne
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    boolean disabled;

    @Column(nullable = false)
    private Timestamp createTime;

    @Column(nullable = false)
    private Timestamp expireTime;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public String getTokenString() {
        return this.tokenString;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public Timestamp getCreateTime() {
        return this.createTime;
    }

    public Timestamp getExpireTime() {
        return this.expireTime;
    }

    // -------------------- SETTERS --------------------

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTokenString(final String token) {
        this.tokenString = token;
    }

    public void setAuthenticatedUser(final AuthenticatedUser user) {
        this.authenticatedUser = user;
    }

    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
    }

    public void setCreateTime(final Timestamp createTime) {
        this.createTime = createTime;
    }

    public void setExpireTime(final Timestamp expireTime) {
        this.expireTime = expireTime;
    }
}
