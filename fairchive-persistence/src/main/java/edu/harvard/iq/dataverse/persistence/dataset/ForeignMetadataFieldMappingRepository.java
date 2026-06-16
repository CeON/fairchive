package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class ForeignMetadataFieldMappingRepository extends JpaRepository<Long, ForeignMetadataFieldMapping> {

    public ForeignMetadataFieldMappingRepository() {
        super(ForeignMetadataFieldMapping.class);
    }
    
    public Optional<ForeignMetadataFieldMapping> find(final String formatName, final String pathName) {
        return getSingleResult(createQuery(
        		"SELECT m FROM ForeignMetadataFieldMapping m" + 
        		" WHERE m.foreignMetadataFormatMapping.name=:formatName" + 
        				" AND m.foreignFieldXPath=:xPath")
                .setParameter("formatName", formatName)
                .setParameter("xPath", pathName));
    }
}
