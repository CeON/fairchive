package edu.harvard.iq.dataverse.persistence.geonames;

import static java.util.Collections.emptyList;

import java.util.List;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class GeoNameRepository extends JpaRepository<Integer, GeoName> {

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
}