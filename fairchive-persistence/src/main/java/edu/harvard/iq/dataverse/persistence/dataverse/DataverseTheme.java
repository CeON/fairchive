package edu.harvard.iq.dataverse.persistence.dataverse;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * @author ellenk
 */

@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")})
public class DataverseTheme implements Serializable, JpaEntity<Long> {

    public enum ImageFormat {
        SQUARE, RECTANGLE
    }
    
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ImageFormat logoFormat;
    @Enumerated(EnumType.STRING)
    private Alignment logoAlignment;
    private String logoBackgroundColor;
    private String logo;
    private String tagline;
    private String linkUrl;
    private String linkColor;
    private String textColor;
    private String backgroundColor;
    @OneToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;
    
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public ImageFormat getLogoFormat() {
        return this.logoFormat;
    }

    public void setLogoFormat(final ImageFormat format) {
        this.logoFormat = format;
    }

    public Alignment getLogoAlignment() {
        return this.logoAlignment;
    }

    public void setLogoAlignment(final Alignment alignment) {
        this.logoAlignment = alignment;
    }

    public String getLogoBackgroundColor() {
        return this.logoBackgroundColor;
    }

    public void setLogoBackgroundColor(final String color) {
        this.logoBackgroundColor = color;
    }

    public String getLogo() {
        return this.logo;
    }

    public void setLogo(final String logo) {
        this.logo = logo;
    }

    public String getTagline() {
        return this.tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(final String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkColor() {
        return this.linkColor;
    }

    public void setLinkColor(final String color) {
        this.linkColor = color;
    }

    public String getTextColor() {
        return this.textColor;
    }

    public void setTextColor(final String color) {
        this.textColor = color;
    }

    public String getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(final String color) {
        this.backgroundColor = color;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataverseTheme)) {
            return false;
        }
        DataverseTheme other = (DataverseTheme) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "DataverseTheme[ id=" + id + " ]";
    }

}
