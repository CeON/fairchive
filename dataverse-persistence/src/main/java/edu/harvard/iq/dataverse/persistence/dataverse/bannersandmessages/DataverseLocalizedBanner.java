package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

@Entity
public class DataverseLocalizedBanner implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    @Lob
    @Basic(fetch = LAZY)
    private byte[] image;

    private String contentType;

    private String imageName;

    private String imageLink;

    @ManyToOne(fetch = LAZY)
    private DataverseBanner dataverseBanner;

    public DataverseLocalizedBanner() {
    }

    public DataverseLocalizedBanner(final String locale) {
        this.locale = locale;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public byte[] getImage() {
        return this.image;
    }

    public InputStream getImageAsStream() {
        return new ByteArrayInputStream(this.image);
    }
    
    public boolean isImagePresent() {
        return this.image != null;
    }

    public void setImage(final byte[] image) {
        this.image = image;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public String getImageName() {
        return this.imageName;
    }

    public void setImageName(final String imageName) {
        this.imageName = imageName;
    }

    public String getImageLink() {
        return this.imageLink;
    }

    public void setImageLink(final String imageLink) {
        this.imageLink = imageLink;
    }

    public DataverseBanner getDataverseBanner() {
        return this.dataverseBanner;
    }

    public void setDataverseBanner(final DataverseBanner dataverseBanner) {
        this.dataverseBanner = dataverseBanner;
    }
}
