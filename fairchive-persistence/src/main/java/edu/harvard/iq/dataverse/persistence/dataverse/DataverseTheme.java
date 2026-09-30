package edu.harvard.iq.dataverse.persistence.dataverse;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
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
	
    public static final String DEFAULT_LOGO_BACKGROUND_COLOR = "FFFFFF";
    public static final String DEFAULT_BACKGROUND_COLOR = "FFFFFF";
    public static final String DEFAULT_LINK_COLOR = "428BCA";
    public static final String DEFAULT_TEXT_COLOR = "888888";

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
    private ImageFormat logoFormat = ImageFormat.SQUARE;
    @Enumerated(EnumType.STRING)
    private Alignment logoAlignment = Alignment.LEFT;
    @Column
    private String logoBackgroundColor = DEFAULT_BACKGROUND_COLOR;
    @Column
    private String logo;
    @Column
    private String tagline;
    @Column
    private String linkUrl;
    @Column
    private String linkColor = DEFAULT_LINK_COLOR;
    @Column
    private String textColor = DEFAULT_TEXT_COLOR;
    @Column
    private String backgroundColor = DEFAULT_BACKGROUND_COLOR;
    @OneToOne
    @JoinColumn(name = "version_id")
    private DataverseVersion version;
    
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

    public DataverseVersion getVersion() {
		return this.version;
	}

	public void setVersion(final DataverseVersion version) {
		this.version = version;
	}

	@Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
    	if(object != null && getClass().equals(object.getClass())) {
    		final DataverseTheme other = (DataverseTheme) object;
    		return Objects.equals(this.id, other.id);
    	} else {
    		return false;
    	}
    }

    @Override
    public String toString() {
        return "DataverseTheme[ id=" + id + " ]";
    }
}
