package edu.harvard.iq.dataverse.search.geonames;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;

import edu.harvard.iq.dataverse.search.GeoNameSolrClient;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;

/**
 * Solr data finder dedicated for GeoNames collection
 */
@Stateless
public class GeoNameDataFinder {

    @Inject
    @GeoNameSolrClient
    private SolrClient solr;
    @Inject
    private SolrQuerySanitizer sanitizer;

    public GeoNameDataFinder() {
    }

    public GeoNameDataFinder(final SolrClient solr,
            final SolrQuerySanitizer sanitizer) {
        this.solr = solr;
        this.sanitizer = sanitizer;
    }

    public List<GeoName> find(final String phraze, final int maxResultsCount) {
        try {
            if (isNotBlank(phraze)) {
                final StringBuilder builder = buildQueryString(phraze);
                final SolrQuery query = new SolrQuery(builder.toString())
                        .setRows(maxResultsCount);
                final List<GeoName> result = this.solr.query(query)
                        .getBeans(GeoName.class);
                if (result.isEmpty()) {
                    findById(phraze.trim()).ifPresent(result::add);
                }
                return result;
            } else {
                return emptyList();
            }

        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private StringBuilder buildQueryString(final String phraze) {
        final StringBuilder builder = new StringBuilder();
        for (final String word : split(
                this.sanitizer.removeSolrSpecialChars(phraze))) {
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            builder.append("(hierarchy:*").append(word)
                    .append("* OR alternateNames:*").append(word).append("*)");
        }

        return builder;
    }

    public Optional<GeoName> findById(final String id) {
        try {
            if (isNotBlank(id)) {
                return this.solr.query(new SolrQuery("id:".concat(id)))
                        .getBeans(GeoName.class)
                        .stream()
                        .findAny();
            } else {
                return Optional.empty();
            }
        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
