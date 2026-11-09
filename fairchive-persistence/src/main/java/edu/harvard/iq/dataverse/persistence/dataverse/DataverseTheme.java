package edu.harvard.iq.dataverse.persistence.dataverse;

import static javax.persistence.GenerationType.IDENTITY;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
    
    public DataverseTheme createNewFor(final DataverseVersion version) {
    	final DataverseTheme result = new DataverseTheme();
    	result.backgroundColor = this.backgroundColor;
    	result.linkColor = this.linkColor;
    	result.linkUrl = this.linkUrl;
    	result.logo = this.logo;
    	result.logoAlignment = this.logoAlignment;
    	result.logoBackgroundColor = this.logoBackgroundColor;
    	result.logoFormat = this.logoFormat;
    	result.tagline = this.tagline;
    	result.textColor = this.textColor;
    	result.version = version;
    	return result;
    }
    
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
        this.logoFormat = format != null ? format : ImageFormat.SQUARE;
    }

    public Alignment getLogoAlignment() {
        return this.logoAlignment;
    }

    public void setLogoAlignment(final Alignment alignment) {
        this.logoAlignment = alignment != null ? alignment : Alignment.LEFT;
    }

    public String getLogoBackgroundColor() {
        return this.logoBackgroundColor;
    }

    public void setLogoBackgroundColor(final String color) {
        this.logoBackgroundColor = isNoneBlank(color) ? color : DEFAULT_LOGO_BACKGROUND_COLOR;
    }

    public String getLogo() {
        return this.logo;
    }
    
    public boolean isLogoPresent() {
    	return isNotEmpty(this.logo);
    }
    
    public boolean isRectangularLogoPresent() {
    	return isLogoPresent() && this.logoFormat == ImageFormat.RECTANGLE;
    }
    
    public boolean isSquaredLogoPresent() {
    	return isLogoPresent() && this.logoFormat == ImageFormat.SQUARE;
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
        this.linkColor = isNoneBlank(color) ? color : DEFAULT_LINK_COLOR;
    }

    public String getTextColor() {
        return this.textColor;
    }

    public void setTextColor(final String color) {
        this.textColor = isNoneBlank(color) ? color : DEFAULT_TEXT_COLOR;
    }

    public String getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(final String color) {
        this.backgroundColor = isNoneBlank(color) ? color : DEFAULT_BACKGROUND_COLOR;
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
