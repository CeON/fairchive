package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class ForeignMetadataFormatMappingRepository extends JpaRepository<Long, ForeignMetadataFormatMapping> {

    public ForeignMetadataFormatMappingRepository() {
        super(ForeignMetadataFormatMapping.class);
    }
    
    public Optional<ForeignMetadataFormatMapping> findByName(final String name) {
        return getSingleResult(createQuery(
        		"SELECT f FROM ForeignMetadataFormatMapping f WHERE f.name=:name")
                .setParameter("name", name));
    }
}
