package edu.harvard.iq.dataverse.persistence.geonames;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

@Entity
public class GeoName implements JpaEntity<Integer> {    
    
    @Id
    private Integer id;
    private String name;
    private String alternateNames;
    private String featureCode;
    private String countryCode;
    private String admin1Code;
    private String admin2Code;
    private String admin3Code;
    private String admin4Code;
    private String hierarchy;
    private String fullText;

    @Override
    public Integer getId() {
        return this.id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getAlternateNames() {
        return this.alternateNames;
    }

    public void setAlternateNames(final String alternateNames) {
        this.alternateNames = alternateNames;
    }

    public String getFeatureCode() {
        return this.featureCode;
    }

    public void setFeatureCode(final String featureCode) {
        this.featureCode = featureCode;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAdmin1Code() {
        return this.admin1Code;
    }

    public void setAdmin1Code(final String admin1Code) {
        this.admin1Code = admin1Code;
    }

    public String getAdmin2Code() {
        return this.admin2Code;
    }

    public void setAdmin2Code(final String admin2Code) {
        this.admin2Code = admin2Code;
    }

    public String getAdmin3Code() {
        return this.admin3Code;
    }

    public void setAdmin3Code(final String admin3Code) {
        this.admin3Code = admin3Code;
    }

    public String getAdmin4Code() {
        return this.admin4Code;
    }

    public void setAdmin4Code(final String admin4Code) {
        this.admin4Code = admin4Code;
    }
    
    boolean isTier0() {
        return this.admin1Code == null;
    }
    
    boolean isTier1() {
        return this.admin1Code != null & this.admin2Code == null;
    }
    
    boolean isTier2() {
        return this.admin2Code != null & this.admin3Code == null;
    }
    
    boolean isTier3() {
        return this.admin3Code != null & this.admin4Code == null;
    }
    
    boolean isTier4() {
        return this.admin4Code != null;
    }
    
    boolean isAdm1() {
        return this.featureCode.startsWith("ADM1");
    }
    
    boolean isAdm2() {
        return this.featureCode.startsWith("ADM2");
    }
    
    boolean isAdm3() {
        return this.featureCode.startsWith("ADM3");
    }
    
    boolean isAdm4() {
        return this.featureCode.startsWith("ADM4");
    }
    
    boolean isAdm5() {
        return this.featureCode.startsWith("ADM5");
    }
    
    public String getHierarchy() {
        return this.hierarchy;
    }

    public void setHierarchy(final String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public String getFullText() {
        return this.fullText;
    }

    public void setFullText(final String fullText) {
        this.fullText = fullText;
    }

    public String getDetails(final String beginDecorator, final String endDecorator,
            final String separator) {
        final StringBuilder result = new StringBuilder(80);
        result.append(beginDecorator).append(getStringFromBundle("geoname.id"))
                .append(endDecorator).append(": ").append(this.id)
                .append(separator);
        result.append(beginDecorator).append(getStringFromBundle("geoname.name"))
                .append(endDecorator).append(": ")
                .append(this.name).append(separator);
        result.append(beginDecorator).append(getStringFromBundle("geoname.hierarchy"))
                .append(endDecorator).append(": ")
                .append(this.hierarchy).append(separator);
        if (isNotBlank(this.alternateNames)) {
            result.append(beginDecorator)
                    .append(getStringFromBundle("geoname.altnames"))
                    .append(endDecorator).append(": ")
                    .append(this.alternateNames)
                    .append(separator);
        }
        result.append(beginDecorator)
                .append(getStringFromBundle("geonames.featurecode"))
                .append(endDecorator).append(": ")
                .append(this.featureCode);
        return result.toString();
    }

    public String getDetails() {
        return getDetails(EMPTY, EMPTY, " ");
    }
    
    public String getDetailsHTML() {
        return getDetails("<b>", "</b>", " ");
    }
    
    @Override
    public String toString() {
        return this.id.toString();
    }
    
}
