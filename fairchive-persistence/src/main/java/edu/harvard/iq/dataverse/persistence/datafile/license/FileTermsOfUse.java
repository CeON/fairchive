package edu.harvard.iq.dataverse.persistence.datafile.license;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * Entity describing on what terms
 * file can be used by app users.
 *
 * @author madryk
 */
@Entity
public class FileTermsOfUse implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;

    public enum TermsOfUseType {
        LICENSE_BASED,
        ALL_RIGHTS_RESERVED,
        RESTRICTED,
        TERMS_UNKNOWN;
    }

    public enum RestrictType {
        ACADEMIC_PURPOSE,
        NOT_FOR_REDISTRIBUTION,
        ACADEMIC_PURPOSE_AND_NOT_FOR_REDISTRIBUTION,
        CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private License license;

    private boolean allRightsReserved;

    @Enumerated(EnumType.STRING)
    private RestrictType restrictType;

    @Column(columnDefinition = "TEXT")
    private String restrictCustomText;


    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    /**
     * Returns license describing terms
     * of use if {@link #getTermsOfUseType()} is
     * equal to {@link TermsOfUseType#LICENSE_BASED}
     */
    public License getLicense() {
        return license;
    }

    /**
     * Returns true if all rights are reserved
     * for associated file
     */
    public boolean isAllRightsReserved() {
        return allRightsReserved;
    }

    /**
     * Returns type of restriction if {@link #getTermsOfUseType()} is
     * equal to {@link TermsOfUseType#RESTRICTED}
     */
    public RestrictType getRestrictType() {
        return restrictType;
    }

    /**
     * Returns text describing on what terms
     * associated file is accessible
     */
    public String getRestrictCustomText() {
        return restrictCustomText;
    }
    
    // -------------------- LOGIC -------------------- 
    
    public String getDisplayText() {
        switch(getTermsOfUseType()) {
        case LICENSE_BASED : 
            return this.license.getName();
        case ALL_RIGHTS_RESERVED :
            return "All rights reserved";
        case RESTRICTED :
            return getRestrictedDisplayText(getRestrictType());
        case TERMS_UNKNOWN:
            return "Unknown";
        default:
            return "Unknown";
        }
    }

    private String getRestrictedDisplayText(RestrictType restrictType) {
        String restrictedAccess = "Restricted access";
        if (restrictType == null) {
            return restrictedAccess;
        }
        switch (restrictType) {
            case ACADEMIC_PURPOSE:
                return restrictedAccess + " - Academic purpose only";
            case NOT_FOR_REDISTRIBUTION:
                return restrictedAccess + " - Not for redistribution";
            case ACADEMIC_PURPOSE_AND_NOT_FOR_REDISTRIBUTION:
                return restrictedAccess + " - Academic purpose and not for redistribution";
            case CUSTOM:
                return StringUtils.isBlank(restrictCustomText) ?
                        restrictedAccess : restrictedAccess + " - " + restrictCustomText;
            default:
                return restrictedAccess;
        }
    }

    public boolean isSameAs(final FileTermsOfUse other) {
        final TermsOfUseType thisTerms = getTermsOfUseType();
        if (thisTerms != other.getTermsOfUseType()) {
            return false;
        } else if (thisTerms == TermsOfUseType.LICENSE_BASED) {
            return this.license.getId().equals(other.license.getId());
        } else if (thisTerms == TermsOfUseType.RESTRICTED) {
            return this.restrictType == other.restrictType &&
                    StringUtils.equals(this.restrictCustomText,
                            other.restrictCustomText);
        } else {
            return true;
        }
    }


    public TermsOfUseType getTermsOfUseType() {
        if (license != null) {
            return TermsOfUseType.LICENSE_BASED;
        }
        if (allRightsReserved) {
            return TermsOfUseType.ALL_RIGHTS_RESERVED;
        }
        if (restrictType != null) {
            return TermsOfUseType.RESTRICTED;
        }
        return TermsOfUseType.TERMS_UNKNOWN;
    }
    
    public Optional<LicenseIcon> getIcon() {
        return Optional.ofNullable(this.license).map(License::getIcon);
    }

    public FileTermsOfUse createCopy() {
        FileTermsOfUse copy = new FileTermsOfUse();
        copy.setLicense(getLicense());
        copy.setAllRightsReserved(isAllRightsReserved());
        copy.setRestrictType(getRestrictType());
        copy.setRestrictCustomText(getRestrictCustomText());
        return copy;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public void setAllRightsReserved(boolean allRightsReserved) {
        this.allRightsReserved = allRightsReserved;
    }

    public void setRestrictType(RestrictType restrictType) {
        this.restrictType = restrictType;
    }

    public void setRestrictCustomText(String restrictCustomText) {
        this.restrictCustomText = restrictCustomText;
    }


}
