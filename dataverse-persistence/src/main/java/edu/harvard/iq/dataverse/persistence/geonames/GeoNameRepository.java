package edu.harvard.iq.dataverse.persistence.geonames;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.ejb.Singleton;
import javax.transaction.Transactional;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class GeoNameRepository extends JpaRepository<Integer, GeoName> {

    private static final Logger log = Logger.getLogger(GeoNameRepository.class.getName());
    
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
                    "OR LOWER(gn.alternateNames) LIKE LOWER(CONCAT('%', :text, '%')) ",
                    GeoName.class)
                    .setParameter("text", trimmedText)
                    .getResultList();
        }
    }

    public void deleteAll() {
        this.em.createNativeQuery("TRUNCATE TABLE geoname CONTINUE IDENTITY RESTRICT")
                .executeUpdate();
    }

    public void importNames(final InputStream in) throws Exception {

        final Stream<GeoName> stream = GeoNamesImporter.readNames(in);
        
        final long begin = currentTimeMillis();
        stream.forEach(this::store);
        log.info("Geo names stored in DB in " + (currentTimeMillis() - begin) / 1000 + " seconds.");
    }

    @Transactional
    private void store(final GeoName gn) {
        save(gn);
    }
}