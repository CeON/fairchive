package edu.harvard.iq.dataverse.search.periodo;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;

import java.io.IOException;
import java.util.AbstractList;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import edu.harvard.iq.dataverse.search.PeriodoSolrClient;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;

/**
 * Solr data finder dedicated for GeoNames collection
 */
@Stateless
public class PeriodoDataFinder {

    @Inject
    @PeriodoSolrClient
    private SolrClient solr;
    @Inject
    private SolrQuerySanitizer sanitizer;

    public PeriodoDataFinder() {
    }

    public PeriodoDataFinder(final SolrClient solr,
            final SolrQuerySanitizer sanitizer) {
        this.solr = solr;
        this.sanitizer = sanitizer;
    }

    public List<Period> find(final String phrase, final int maxResultsCount) {
        try {
            if (isNotBlank(phrase)) {
                final StringBuilder builder = buildQueryString(phrase);
                final SolrQuery query = new SolrQuery(builder.toString())
                        .setRows(maxResultsCount);
                final List<Period> result = this.solr.query(query)
                        .getBeans(Period.class);
                if (result.isEmpty()) {
                    try {
                        // It may fail if there is more than one word in the phrase.
                        findById(phrase.trim()).ifPresent(result::add);
                    } catch (final Exception e) {
                        return emptyList(); // It's ok to return nothing in this case.
                    }
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
            if (word.length() > 1) {
                if (builder.length() > 0) {
                    builder.append(" AND ");
                }
                if (isFeatureCode(word)) {
                    final String code = word.substring(1).trim();
                    if (code.length() > 1) {
                        builder.append("featureCode:").append(code.toUpperCase())
                                .append('*');
                    }
                } else {
                    builder.append("(label:").append(word).append("*)");
                        //    .append("* OR alternateNames:*").append(word).append("*)");
                }
            }
        }

        return builder;
    }
    
    private boolean isFeatureCode(final String word) {
        return word.charAt(0) == '#';
    }

    public Optional<Period> findById(final String id) {
        try {
            if (isNotBlank(id)) {
                return this.solr.query(new SolrQuery("id:".concat(id)))
                        .getBeans(Period.class)
                        .stream()
                        .findAny();
            } else {
                return Optional.empty();
            }
        } catch (final SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Optional<Period> getByUrl(final String url) {
        return url.startsWith(Period.base)
            ? findById(url.substring(Period.base.length()))
            : Optional.empty();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getAllLocations() throws Exception {
        final ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("terms", "true");
        params.set("terms.fl", "locations");
        params.set("terms.limit", "-1");

        final QueryRequest req = new QueryRequest(params);
        req.setPath("/terms"); 
        final NamedList<Object> terms = (NamedList<Object>)this.solr.request(req).get("terms");
        return new LocationsList((NamedList<Object>)terms.get("locations"));
    }
    
    private static final class LocationsList extends AbstractList<String> {
        
        private final NamedList<Object> source;
        
        private LocationsList(NamedList<Object> source) {
            this.source = source;
        }

        @Override
        public String get(final int index) {
            return this.source.getName(index);
        }

        @Override
        public int size() {
            return this.source.size();
        }
    }
}
