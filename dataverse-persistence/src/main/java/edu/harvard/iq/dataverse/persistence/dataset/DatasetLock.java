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

package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.Objects.requireNonNull;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

/**
 * Holds the reason a dataset is locked, and possibly the user that created the
 * lock.
 *
 * @author Leonid Andreev
 * @author Michael Bar-Sinai
 */
@Entity
@Table(indexes = { @Index(columnList = "user_id"), @Index(columnList = "dataset_id") })
@NamedQueries({
        @NamedQuery(name = "DatasetLock.getLocksByAuthenticatedUserId", query = "SELECT lock FROM DatasetLock lock WHERE lock.user.id=:authenticatedUserId")
})
public class DatasetLock implements Serializable, JpaEntity<Long> {

    public enum Reason {
        /**
         * Data being ingested
         */
        Ingest,

        /**
         * Waits for a {@link edu.harvard.iq.dataverse.persistence.workflow.Workflow
         * Workflow} to end
         */
        Workflow,

        /**
         * Waiting for a curator to approve/send back to author
         */
        InReview,

        /**
         * DCM (rsync) upload in progress
         */
        DcmUpload,

        // ** Registering PIDs for DS and DFs
        pidRegister
    }

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Temporal(value = TIMESTAMP)
    private Date startTime;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    private String info;

    /**
     * Constructing a lock for the given reason.
     *
     * @param reason Why the dataset gets locked. Cannot be {@code null}.
     * @param user   The user causing the lock. Cannot be {@code null}.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *                                  because JPA would throw an exception later
     *                                  anyway.
     */
    public DatasetLock(final Reason reason, final AuthenticatedUser user) {
        this(reason, null, user, null);
    }
    
    /**
     * Constructing a lock for the given reason, with the specified descriptive info
     * message.
     *
     * @param reason      Why the dataset gets locked. Cannot be {@code null}.
     * @param dataset     Dataset to be locked
     * @param user        The user causing the lock. Cannot be {@code null}.
     * @param infoMessage Descriptive message.
     * @throws IllegalArgumentException if any of the parameters are null. That's
     *                                  because JPA would throw an exception later
     *                                  anyway.
     */
    public DatasetLock(final Reason reason, final Dataset dataset, final AuthenticatedUser user,
            final String infoMessage) {
        requireNonNull(reason, "Cannot lock a dataset for a null reason");
        this.reason = reason;
        this.dataset = dataset;
        this.startTime = new Date();
        user.getDatasetLocks().add(this);
        this.user = user;
        this.info = infoMessage;
    }

    protected DatasetLock() {
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

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(final Dataset dataset) {
        this.dataset = dataset;
    }

    public AuthenticatedUser getUser() {
        return this.user;
    }

    public void setUser(final AuthenticatedUser user) {
        user.getDatasetLocks().add(this);
        this.user = user;
    }

    public String getInfo() {
        return this.info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Reason getReason() {
        return this.reason;
    }

    public void setReason(final Reason reason) {
        this.reason = reason;
    }
    
    public void removeFromDataset() {
        getDataset().removeLock(this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof DatasetLock) {
            final DatasetLock other = (DatasetLock) object;
            return (id == null && other.id == null)
                    || (id != null && id.equals(other.getId()));
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "DatasetLock[ id=" + id + " ]";
    }

}
