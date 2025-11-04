package edu.harvard.iq.dataverse.search.periodo;

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

import edu.harvard.iq.dataverse.search.PeriodoSolrClient;

/**
 * Service dedicate for indexing GeoNames data.
 */
@Stateless
public class PeriodoIndexingService {

    private static final Logger log = getLogger(PeriodoIndexingService.class);
    private static final int BATCH_SIZE = 1000;

    @Inject
    @PeriodoSolrClient
    private SolrClient solr;

    public PeriodoIndexingService() {
    }

    public PeriodoIndexingService(final SolrClient solr) {
        this.solr = solr;
    }

    public void importNames(final InputStream in) throws Exception {
        store(PeriodoImporter.readPeriods(in));
    }
    
    public void importNames(final InputStream json, final InputStream tsv) 
            throws Exception {
        store(PeriodoImporter.readPeriods(json, tsv));
    }
    
    public void clear() throws Exception {
        this.solr.deleteByQuery("*:*");
        this.solr.commit();
    }
    
    private void store(final Iterator<Period> it) throws Exception {
        final ArrayList<Period> list = new ArrayList<>(BATCH_SIZE);

        log.info("Storing Perio.do.");

        int count = 0;
        final long begin = currentTimeMillis();
        while (fetch(it, list)) {
            this.solr.addBeans(list);
            this.solr.commit();
            count += list.size();
            log.info("Stored " + count);
        }
        log.info("Perio.do stored in Solr in {}  seconds.",
                (currentTimeMillis() - begin) / 1000);
    }

    private static boolean fetch(final Iterator<Period> iterator, final List<Period> list) {

        list.clear();
        for (int count = 0; count < BATCH_SIZE & iterator.hasNext(); ++count) {
            list.add(iterator.next());
        }
        return list.size() > 0;
    }

}
