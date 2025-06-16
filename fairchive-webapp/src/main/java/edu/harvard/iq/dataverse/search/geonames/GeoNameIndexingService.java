package edu.harvard.iq.dataverse.search.geonames;

import static java.lang.System.currentTimeMillis;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.search.GeoNameSolrClient;

/**
 * Service dedicate for indexing GeoNames data.
 */
@Stateless
public class GeoNameIndexingService {

    private static final Logger log = getLogger(GeoNameIndexingService.class);
    private static final int BATCH_SIZE = 1000;

    @Inject
    @GeoNameSolrClient
    private SolrClient solr;

    public GeoNameIndexingService() {
    }

    public GeoNameIndexingService(final SolrClient solr) {
        this.solr = solr;
    }

    public void importNames(final InputStream in) throws Exception {

        final Iterator<GeoName> it = GeoNamesImporter.readNames(in);
        final ArrayList<GeoName> list = new ArrayList<>(BATCH_SIZE);

        log.info("Storing geo names.");

        int count = 0;
        final long begin = currentTimeMillis();
        while (fetch(it, list)) {
            this.solr.addBeans(list);
            this.solr.commit();
            count += BATCH_SIZE;
            log.info("Stored " + count);
        }
        log.info("Geo names stored in Solr in {}  seconds.",
                (currentTimeMillis() - begin) / 1000);
    }

    public void clear() throws Exception {
        this.solr.deleteByQuery("*:*");
        this.solr.commit();
    }

    private boolean fetch(final Iterator<GeoName> iterator, final List<GeoName> list) {

        list.clear();
        for (int count = 0; count < BATCH_SIZE & iterator.hasNext(); ++count) {
            list.add(iterator.next());
        }
        return list.size() > 0;
    }

}
