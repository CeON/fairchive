package edu.harvard.iq.dataverse.persistence.geonames;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@ApplicationScoped
public class GeoNameRepository extends JpaRepository<Integer, GeoName> {

    private static final Logger log = Logger
            .getLogger(GeoNameRepository.class.getName());
    private static final int BATCH_SIZE = 100;
    @Inject
    private GeoNameBatchRepository batchRepo;

    public GeoNameRepository() {
        super(GeoName.class);
    }

    public List<GeoName> find(final String text) {
        // sanitize parameter by retainin only leters and numbers
        final String trimmedText = text.replaceAll("[^\\pL\\pN]", "").trim();
        if (trimmedText.isEmpty()) {
            return emptyList();
        } else {
            // native querries do not properly bind parameter when used inside
            // functions - that's why string concatenation
            final List<GeoName> result = this.em.createNativeQuery(
                    "SELECT * FROM geoname " + 
                    "WHERE fullText @@ plainto_tsquery('" + trimmedText + "')",
                    GeoName.class)
                    .getResultList();
            if(result.isEmpty()) {
                try {
                    findById(parseInt(trimmedText)).ifPresent(result::add);
                } catch(final NumberFormatException e) {
                    // if is wasn't a number, just ignore
                }
            }
            return result;
        }
    }

    public void deleteAll() {
        this.em.createNativeQuery("TRUNCATE TABLE geoname CONTINUE IDENTITY RESTRICT")
                .executeUpdate();
    }

    public void importNames(final InputStream in) throws Exception {

        final Iterator<GeoName> it = GeoNamesImporter.readNames(in).iterator();
        final ArrayList<GeoName> list = new ArrayList<>(BATCH_SIZE);

        log.info("Storing geo names.");

        final long begin = currentTimeMillis();
        while (fetch(it, list)) {
            this.batchRepo.store(list);
        }
        log.info("Geo names stored in DB in " + (currentTimeMillis() - begin) / 1000
                + " seconds.");
    }

    private boolean fetch(final Iterator<GeoName> iterator, final List<GeoName> list) {

        list.clear();
        for (int count = 0; count < BATCH_SIZE & iterator.hasNext(); ++count) {
            list.add(iterator.next());
        }
        return list.size() > 0;
    }
}