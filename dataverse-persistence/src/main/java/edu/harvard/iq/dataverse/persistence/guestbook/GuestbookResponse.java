/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.guestbook;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbook_id"),
        @Index(columnList = "datafile_id"),
        @Index(columnList = "dataset_id")
})
@NamedQueries(
        @NamedQuery(name = "GuestbookResponse.findByAuthenticatedUserId",
                query = "SELECT gbr FROM GuestbookResponse gbr WHERE gbr.authenticatedUser.id=:authenticatedUserId")
)
public class GuestbookResponse implements Serializable, JpaEntity<Long> {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Guestbook guestbook;

    @ManyToOne
    @JoinColumn(nullable = false)
    private DataFile dataFile;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DatasetVersion datasetVersion;

    @ManyToOne
    @JoinColumn(nullable = true)
    private AuthenticatedUser authenticatedUser;

    @OneToMany(mappedBy = "guestbookResponse", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @OrderBy("id")
    private List<CustomQuestionResponse> customQuestionResponses;


    private String name;
    private String email;
    private String institution;
    private String position;
    /**
     * Possible values for downloadType include "Download", "Subset",
     * "WorldMap", or the displayName of an ExternalTool.
     * <p>
     * TODO: Types like "Download" and "Subset" and probably "WorldMap" should
     * be defined once as constants (likely an enum) rather than having these
     * strings duplicated in various places when setDownloadtype() is called.
     * (Some day it would be nice to convert WorldMap into an ExternalTool but
     * it's not worth the effort at this time.)
     */
    private String downloadtype;
    private String sessionId;

    @Temporal(value = TIMESTAMP)
    private Date responseTime;


    public GuestbookResponse() {

    }

    public GuestbookResponse(GuestbookResponse source) {
        //makes a clone of a response for adding of studyfiles in case of multiple downloads
        this.setName(source.getName());
        this.setEmail(source.getEmail());
        this.setInstitution(source.getInstitution());
        this.setPosition(source.getPosition());
        this.setResponseTime(source.getResponseTime());
        this.setDataset(source.getDataset());
        this.setDatasetVersion(source.getDatasetVersion());
        this.setAuthenticatedUser(source.getAuthenticatedUser());
        this.setSessionId(source.getSessionId());
        List<CustomQuestionResponse> customQuestionResponses = new ArrayList<>();
        if (!source.getCustomQuestionResponses().isEmpty()) {
            for (CustomQuestionResponse customQuestionResponse : source.getCustomQuestionResponses()) {
                CustomQuestionResponse customQuestionResponseAdd = new CustomQuestionResponse();
                customQuestionResponseAdd.setResponse(customQuestionResponse.getResponse());
                customQuestionResponseAdd.setCustomQuestion(customQuestionResponse.getCustomQuestion());
                customQuestionResponseAdd.setGuestbookResponse(this);
                customQuestionResponses.add(customQuestionResponseAdd);
            }
        }
        this.setCustomQuestionResponses(customQuestionResponses);
        this.setGuestbook(source.getGuestbook());
    }


    public String getEmail() {
        return this.email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public Guestbook getGuestbook() {
        return this.guestbook;
    }

    public void setGuestbook(final Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public String getInstitution() {
        return this.institution;
    }

    public void setInstitution(final String institution) {
        this.institution = institution;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPosition() {
        return this.position;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Date getResponseTime() {
        return this.responseTime;
    }

    public void setResponseTime(final Date time) {
        this.responseTime = time;
    }

    public String getResponseDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(this.responseTime);
    }

    public String getResponseDateForDisplay() {
        return null;
    }


    public List<CustomQuestionResponse> getCustomQuestionResponses() {
        return this.customQuestionResponses;
    }

    public void setCustomQuestionResponses(final List<CustomQuestionResponse> responses) {
        this.customQuestionResponses = responses;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(final Dataset dataset) {
        this.dataset = dataset;
    }

    public DataFile getDataFile() {
        return this.dataFile;
    }

    public void setDataFile(final DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }

    public void setDatasetVersion(final DatasetVersion version) {
        this.datasetVersion = version;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    public void setAuthenticatedUser(final AuthenticatedUser user) {
        this.authenticatedUser = user;
    }

    public String getDownloadtype() {
        return this.downloadtype;
    }

    public void setDownloadtype(final String type) {
        this.downloadtype = type;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(final String id) {
        this.sessionId = id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof GuestbookResponse)) {
            return false;
        }
        GuestbookResponse other = (GuestbookResponse) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "GuestbookResponse [id=" + id + ", guestbook=" + guestbook + ", dataFile=" + dataFile + ", dataset="
                + dataset + ", datasetVersion=" + datasetVersion + ", authenticatedUser=" + authenticatedUser
                + ", customQuestionResponses=" + customQuestionResponses + ", name=" + name + ", email=" + email
                + ", institution=" + institution + ", position=" + position + ", downloadtype=" + downloadtype
                + ", sessionId=" + sessionId + "]";
    }

}

