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
package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * @author Leonid Andreev
 * based on the DVN implementation of "HarvestStudy" by
 * @author Gustavo Durand
 */
@Entity
public class OAIRecord implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String setName;

    private String globalId;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdateTime;

    private boolean removed;

    // -------------------- CONSTRUCTORS --------------------

    protected OAIRecord() {
    }

    public OAIRecord(final String setName, final String globalId, 
    		final Date lastUpdateTime) {
        this.setName = setName;
        this.globalId = globalId;
        this.lastUpdateTime = lastUpdateTime;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public String getSetName() {
        return this.setName;
    }

    public String getGlobalId() {
        return this.globalId;
    }

    public Date getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    // -------------------- SETTERS --------------------

    public void setSetName(final String name) {
        this.setName = name;
    }

    public void setGlobalId(final String id) {
        this.globalId = id;
    }

    public void setLastUpdateTime(final Date time) {
        this.lastUpdateTime = time;
    }

    public void setRemoved(final boolean removed) {
        this.removed = removed;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OAIRecord)) {
            return false;
        }
        final OAIRecord other = (OAIRecord) object;
        return (this.id != null || other.id == null) && 
        		(this.id == null || this.id.equals(other.id));
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "OAIRecord[ id=" + this.id + " ]";
    }
}
