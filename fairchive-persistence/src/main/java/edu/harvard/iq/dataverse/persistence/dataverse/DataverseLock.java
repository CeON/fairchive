/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.persistence.dataverse;

import static java.util.Objects.requireNonNull;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

@Entity
@Table(indexes = {@Index(columnList = "user_id"), @Index(columnList = "dataverse_id")})
public class DataverseLock implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;
    
    public enum Reason {
        InReview;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataverse dataverse;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column
    private String info;

    /**
     * Constructing a lock for the given reason.
     *
     * @param reason Why the dataset gets locked.  Cannot be {@code null}.
     * @param user   The user causing the lock. Cannot be {@code null}.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *                                  because JPA would throw an exception later anyway.
     */
    public DataverseLock(final Reason reason, final AuthenticatedUser user) {
        this(reason, user, null);
    }

    /**
     * Constructing a lock for the given reason, with the specified descriptive info message.
     *
     * @param reason     Why the dataset gets locked.  Cannot be {@code null}.
     * @param aUser       The user causing the lock. Cannot be {@code null}.
     * @param infoMessage Descriptive message.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *                                  because JPA would throw an exception later anyway.
     */
    public DataverseLock(final Reason reason, final AuthenticatedUser user, 
    		final String infoMessage) {
    	
        this.reason = requireNonNull(reason, "Cannot lock a dataset for a null reason");
        this.user = requireNonNull(user, "Cannot lock a dataset for a null user");
        this.startTime = new Date();
        this.info = infoMessage;

    }
    
	public DataverseLock(final Dataverse dataverse, final Reason reason, 
			final AuthenticatedUser user) {

		this.dataverse = requireNonNull(dataverse, "Dataset cannot be null");
		this.reason = requireNonNull(reason, "Cannot lock a dataset for a null reason");
		this.user = requireNonNull(user, "Cannot lock a dataset for a null user");
		this.startTime = new Date();
	}

    /**
     * JPA no-args constructor. Client code should use the public constructor
     * and not this one.
     *
     * @see #DatasetLock(edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason)
     */
    protected DataverseLock() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public AuthenticatedUser getUser() {
        return this.user;
    }

    public void setUser(final AuthenticatedUser user) {
        this.user = user;
    }

    public String getInfo() {
        return this.info;
    }

    public void setInfo(final String info) {
        this.info = info;
    }

    public Reason getReason() {
        return this.reason;
    }

    public void setReason(final Reason reason) {
        this.reason = reason;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object other) {
    	return other != null && getClass().equals(other.getClass()) 
    		? Objects.equals(this.id, ((DataverseLock)other).id)
    	    : false;
    }

    @Override
    public String toString() {
        return "DatasetLock[ id=" + id + " ]";
    }
}
