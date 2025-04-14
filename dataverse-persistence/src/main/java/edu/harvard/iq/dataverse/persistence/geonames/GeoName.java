package edu.harvard.iq.dataverse.persistence.geonames;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.persistence.Entity;
import javax.persistence.Id;

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
    
    public String getDetails(final String separator) {
        final StringBuilder result = new StringBuilder(80);
        result.append("Identyfikator: ").append(this.id).append(separator);
        result.append("Nazwa: ").append(this.name).append(separator);
        if(isNotBlank(this.alternateNames)) {
            result.append("Nazwy alternatywne: ").append(this.alternateNames);
        }
        return result.toString();
    }

    public String getDetails() {
        return getDetails(" ");
    }
    
    @Override
    public String toString() {
        return this.id.toString();
    }
    
}
