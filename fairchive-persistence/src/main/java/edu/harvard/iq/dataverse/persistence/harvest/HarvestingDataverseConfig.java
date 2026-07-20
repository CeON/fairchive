/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.harvest;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

/**
 * @author Leonid Andreev
 */
@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")
        , @Index(columnList = "harvesttype")
        , @Index(columnList = "harveststyle")
        , @Index(columnList = "harvestingurl")})
public class HarvestingDataverseConfig implements JpaEntity<Long>, Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String HARVEST_TYPE_OAI = "oai";
    public static final String HARVEST_TYPE_NESSTAR = "nesstar";

    public static final String HARVEST_STYLE_DATAVERSE = "dataverse";
    // pre-4.0 remote Dataverse:
    public static final String HARVEST_STYLE_VDC = "vdc";
    public static final String HARVEST_STYLE_ICPSR = "icpsr";
    public static final String HARVEST_STYLE_NESSTAR = "nesstar";
    public static final String HARVEST_STYLE_ROPER = "roper";
    public static final String HARVEST_STYLE_HGL = "hgl";
    public static final String HARVEST_STYLE_DEFAULT = "default";

    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATAVERSE = "dataverse";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_DATASET = "dataset";
    public static final String REMOTE_ARCHIVE_URL_LEVEL_FILE = "file";
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @OneToOne(cascade = {REMOVE, MERGE, PERSIST})
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;
    private String harvestType = HARVEST_TYPE_OAI; 
    private String harvestStyle = HARVEST_STYLE_DATAVERSE; 
    private String harvestingUrl;
    private String archiveUrl;
    @Column(columnDefinition = "TEXT")
    private String archiveDescription;
    private String harvestingSet;
    
    @Override
    public Long getId() {
        return id;
    }
    
    public void setId(final Long id) {
        this.id = id;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public String getHarvestType() {
        return this.harvestType;
    }

    public void setHarvestType(final String type) {
        this.harvestType = type;
    }

    public String getHarvestStyle() {
        return this.harvestStyle;
    }

    public void setHarvestStyle(final String style) {
        this.harvestStyle = style;
    }

    public String getHarvestingUrl() {
        return this.harvestingUrl;
    }

    public void setHarvestingUrl(final String url) {
        this.harvestingUrl = url.trim();
    }

    public String getArchiveUrl() {
        return this.archiveUrl;
    }

    public void setArchiveUrl(final String url) {
        this.archiveUrl = url;
    }

    public String getArchiveDescription() {
        return this.archiveDescription;
    }

    public void setArchiveDescription(final String description) {
        this.archiveDescription = description;
    }

    public String getHarvestingSet() {
        return this.harvestingSet;
    }

    public void setHarvestingSet(final String set) {
        this.harvestingSet = set;
    }

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof HarvestingDataverseConfig)) {
            return false;
        }
        final HarvestingDataverseConfig other = (HarvestingDataverseConfig) object;
        return (this.id != null || other.id == null) 
        		&& (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "HarvestingDataverse[ id=" + id + " ]";
    }
}
