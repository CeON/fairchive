/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.dataset;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

@Entity
@Table(indexes = {@Index(columnList = "name")})
public class ForeignMetadataFormatMapping implements Serializable, JpaEntity<Long> {
	
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "foreignMetadataFormatMapping", cascade = {REMOVE, MERGE, PERSIST})
    private List<ForeignMetadataFieldMapping> foreignMetadataFieldMappings;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String displayName;
    private String schemaLocation;
    private String startElement;

    /* getters/setters: */
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public List<ForeignMetadataFieldMapping> getDatasetFieldTypes() {
        return this.foreignMetadataFieldMappings;
    }

    public void setDatasetFieldTypes(final List<ForeignMetadataFieldMapping> fieldTypes) {
        this.foreignMetadataFieldMappings = fieldTypes;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(final String name) {
        this.displayName = name;
    }

    public String getSchemaLocation() {
        return this.schemaLocation;
    }

    public void setSchemaLocation(final String location) {
        this.schemaLocation = location;
    }

    public String getStartElement() {
        return this.startElement;
    }

    public void setStartElement(final String element) {
        this.startElement = element;
    }

    /* overrides: */

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object object) {
    	if(object != null && getClass().equals(object.getClass())) {
    		final ForeignMetadataFormatMapping other = (ForeignMetadataFormatMapping) object;
    		return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    	} else {
    		return false;
    	}
    }


    @Override
    public String toString() {
        return "ForeignMetadataFormatMapping: " + this.name;
    }

}

