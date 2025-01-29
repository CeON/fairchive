package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

@Entity
public class DataverseLocalizedBanner {

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
    
    private BufferedImage getBufferedImage() throws IOException {
        return ImageIO.read(new ByteArrayInputStream(this.image));
    }
    
    public boolean isImageWithin(final int maxWidth, final int maxHeight)
            throws IOException {
        final BufferedImage img = getBufferedImage();
        if(img != null) {
            return img.getWidth() > maxWidth || img.getHeight() > maxHeight;
        } else {
            throw new IOException("Unsupported image format");
        }
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

    public Optional<String> getImageLink() {
        return Optional.ofNullable(this.imageLink);
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
