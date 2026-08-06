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

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * @author Leonid Andreev
 * based on the DVN 3 implementation,
 * @author Ellen Kraffmiller
 */
@Entity
public class OAISet implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_SET_SPEC_NAME = "";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    @Size(max = 30, message = "{setspec.maxLength}")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*",
    						message = "{dataverse.nameIllegalCharacters}")})
    private String spec;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String definition;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;


    private boolean updateInProgress;

    private boolean deleted;

    // -------------------- CONSTRUCTORS --------------------

    public OAISet() {
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public Long getVersion() {
        return this.version;
    }

    public String getName() {
        return this.name;
    }

    public String getSpec() {
        return this.spec;
    }

    public String getDefinition() {
        return this.definition;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isUpdateInProgress() {
        return this.updateInProgress;
    }

    public boolean isDeleteInProgress() {
        return this.deleted;
    }

    // -------------------- LOGIC --------------------

    public boolean isDefaultSet() {
        return DEFAULT_SET_SPEC_NAME.equals(this.spec);
    }

    // -------------------- PRIVATE --------------------

    public void setId(final Long id) {
        this.id = id;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSpec(final String spec) {
        this.spec = spec;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setUpdateInProgress(final boolean inProgress) {
        this.updateInProgress = inProgress;
    }

    public void setDeleteInProgress(final boolean inProgress) {
        this.deleted = inProgress;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OAISet)) {
            return false;
        }
        final OAISet other = (OAISet) object;
        return (this.id != null || other.id == null) 
        		&& (this.id == null || this.id.equals(other.id));
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "OaiSet[ id=" + this.id + " ]";
    }

}
