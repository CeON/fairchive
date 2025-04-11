package edu.harvard.iq.dataverse.persistence.geonames;

import static java.util.Collections.emptyList;

import java.util.List;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class GeoNameRepository  extends JpaRepository<Integer, GeoName> {

    public GeoNameRepository() {
        super(GeoName.class);
    }
    
    public List<GeoName> find(final String text) {
        final String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return emptyList();
        } else {
            return this.em.createQuery("SELECT gn FROM GeoName gn " +
                    "WHERE LOWER(gn.name) LIKE LOWER(CONCAT('%', :text, '%')) " +
                    "OR LOWER(gn.alternateNames) LIKE LOWER(CONCAT('%', :text, '%'))",
                    GeoName.class)
                    .setParameter("text", trimmedText)
                    .getResultList();
        }
    }

    @Override
    public GeoName save(final GeoName entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeoName saveAndFlush(final GeoName entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeoName saveFlushAndClear(final GeoName entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveAll(final Iterable<GeoName> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(final Integer id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final GeoName entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mergeAndDelete(final GeoName entity) {
        throw new UnsupportedOperationException();
    }
}