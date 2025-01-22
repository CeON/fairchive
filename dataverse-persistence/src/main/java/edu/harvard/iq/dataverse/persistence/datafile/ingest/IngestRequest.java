/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.datafile.ingest;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

/**
 * @author Leonid Andreev
 */
@Entity
@Table(indexes = {@Index(columnList = "datafile_id")})
public class IngestRequest implements Serializable {
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

    @OneToOne(cascade = {MERGE, PERSIST})
    @JoinColumn(name = "datafile_id")
    private DataFile dataFile;

    private String textEncoding;

    private String controlCard;

    private String labelsFile;

    private Boolean forceTypeCheck;

    public IngestRequest() {
    }

    public IngestRequest(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public String getTextEncoding() {
        return textEncoding;
    }

    public void setTextEncoding(String textEncoding) {
        this.textEncoding = textEncoding;
    }

    public String getControlCard() {
        return controlCard;
    }

    public void setControlCard(String controlCard) {
        this.controlCard = controlCard;
    }

    public String getLabelsFile() {
        return labelsFile;
    }

    public void setLabelsFile(String labelsFile) {
        this.labelsFile = labelsFile;
    }

    public void setForceTypeCheck(boolean forceTypeCheck) {
        this.forceTypeCheck = forceTypeCheck;
    }

    public boolean isForceTypeCheck() {
        return forceTypeCheck != null ? forceTypeCheck : false;
    }

    @Override
    public int hashCode() {
        return this.id != null ? this.id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof IngestRequest)) {
            return false;
        }
        IngestRequest other = (IngestRequest) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "IngestRequest[ id=" + id + " ]";
    }

}
