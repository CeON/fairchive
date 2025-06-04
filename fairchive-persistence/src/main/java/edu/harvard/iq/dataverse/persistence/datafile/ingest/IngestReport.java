/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.datafile.ingest;

import static edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError.UNKNOWN_ERROR;
import static java.util.Arrays.asList;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

/**
 * @author Leonid Andreev
 */
@Entity
@Table(indexes = {@Index(columnList = "datafile_id")})
public class IngestReport implements JpaEntity<Long>, Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static int INGEST_TYPE_TABULAR = 1;
    public static int INGEST_TYPE_METADATA = 2;

    public static int INGEST_STATUS_INPROGRESS = 1;
    public static int INGEST_STATUS_SUCCESS = 2;
    public static int INGEST_STATUS_FAILURE = 3;

    @ManyToOne
    @JoinColumn(nullable = false)
    private DataFile dataFile;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestError errorKey = UNKNOWN_ERROR;

    @ElementCollection
    @OrderColumn
    private List<String> errorArguments = new ArrayList<>();

    private int type;

    private int status;

    @Temporal(value = TIMESTAMP)
    private Date startTime;

    @Temporal(value = TIMESTAMP)
    private Date endTime;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isFailure() {
        return status == INGEST_STATUS_FAILURE;
    }

    public void setFailure() {
        this.status = INGEST_STATUS_FAILURE;
    }

    public IngestError getErrorKey() {
        return errorKey;
    }

    public void setErrorKey(final IngestError errorKey) {
        this.errorKey = errorKey != null ? errorKey : UNKNOWN_ERROR;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<String> getErrorArguments() {
        return errorArguments;
    }

    // -------------------- LOGIC --------------------

    public String getIngestReportMessage() {
        return this.errorKey.getErrorMessage(this.errorArguments);
    }

    public static IngestReport createIngestFailureReport(DataFile dataFile,
            IngestException ingestException) {
        return createIngestFailureReport(dataFile,
                ingestException.getErrorKey(),
                ingestException.getErrorArguments());

    }

    public static IngestReport createIngestFailureReport(DataFile dataFile,
            IngestError errorKey, String... errorArguments) {
        return createIngestFailureReport(dataFile, errorKey, asList(errorArguments));
    }

    public static IngestReport createIngestFailureReport(DataFile dataFile,
            IngestError errorKey, List<String> errorArguments) {

        final IngestReport errorReport = new IngestReport();
        errorReport.setFailure();
        errorReport.setErrorKey(errorKey);
        errorReport.setDataFile(dataFile);
        errorReport.getErrorArguments().addAll(errorArguments);

        return errorReport;
    }

    @Override
    public int hashCode() {
        return this.id != null ? this.id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof IngestReport)) {
            return false;
        }
        IngestReport other = (IngestReport) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "IngestReport[ id=" + id + " ]";
    }

}
