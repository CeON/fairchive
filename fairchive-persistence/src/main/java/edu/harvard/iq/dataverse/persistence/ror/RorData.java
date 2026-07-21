package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
@Entity
public class RorData implements JpaEntity<Long>, Serializable {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(length = 9, unique = true)
    private String rorId;

    @Column
    private String name;

    @Column
    private String countryName;

    @Column(length = 16)
    private String countryCode;

    @Column
    private String website;

    @Column
    private String city;

    @ElementCollection
    @CollectionTable(name = "rordata_namealias", joinColumns = @JoinColumn(name = "rordata_id"))
    @Column(name = "namealias")
    private Set<String> nameAliases = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "rordata_acronym", joinColumns = @JoinColumn(name = "rordata_id"))
    @Column(name = "acronym")
    private Set<String> acronyms = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "rordata_label", joinColumns = @JoinColumn(name = "rordata_id"))
    private Set<RorLabel> labels = new HashSet<>();

    // -------------------- CONSTRUCTORS --------------------

    public RorData() { }

    public RorData(final String rorId, final String name, final String countryName, 
                   final String countryCode, final String website, final String city,
                   final Set<String> nameAliases, final Set<String> acronyms, 
                   final Set<RorLabel> labels) {
        this.rorId = rorId;
        this.name = name;
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.website = website;
        this.city = city;
        this.nameAliases.addAll(nameAliases);
        this.acronyms.addAll(acronyms);
        this.labels.addAll(labels);
    }

    // -------------------- GETTERS --------------------
    @Override
    public Long getId() {
        return this.id;
    }

    public String getRorId() {
        return this.rorId;
    }

    public String getName() {
        return this.name;
    }

    public String getCountryName() {
        return this.countryName;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public Set<String> getNameAliases() {
        return this.nameAliases;
    }

    public Set<String> getAcronyms() {
        return this.acronyms;
    }

    public Set<RorLabel> getLabels() {
        return this.labels;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getCity() {
        return this.city;
    }

    // -------------------- SETTERS --------------------

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRorId(final String rorId) {
        this.rorId = rorId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCountryName(final String name) {
        this.countryName = name;
    }

    public void setCountryCode(final String code) {
        this.countryCode = code;
    }

    public void setWebsite(final String website) {
        this.website = website;
    }

    public void setCity(final String city) {
        this.city = city;
    }
}
