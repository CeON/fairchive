package edu.harvard.iq.dataverse.search.ror;

import edu.harvard.iq.dataverse.search.RorSolrClient;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.split;

import java.io.IOException;
import java.util.List;

/**
 * Solr data finder dedicated for ROR collection
 */
@Stateless
public class RorSolrDataFinder {

    @Inject
    @RorSolrClient
    private SolrClient solrClient;

    @Inject
    private SolrQuerySanitizer solrQuerySanitizer;

    public List<RorDto> findRorData(final String phrase, final int maxResultsCount) {
        try {
            return this.solrClient.query(new SolrQuery(buildQuery(phrase))
                    .setRows(maxResultsCount)).getBeans(RorDto.class);
        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQuery(final String phrase) {
        final StringBuilder builder = new StringBuilder();

        for (final String slice : split(sanitize(phrase))) {
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            builder.append(slice).append('*');
        }
        if (builder.length() == 0) {
            builder.append('*');
        }
        return builder.toString();
    }

    private String sanitize(final String s) {
        return this.solrQuerySanitizer.removeSolrSpecialChars(s);
    }
}
