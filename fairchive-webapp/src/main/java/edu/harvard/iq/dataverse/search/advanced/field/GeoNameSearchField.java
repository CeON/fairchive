package edu.harvard.iq.dataverse.search.advanced.field;

import static edu.harvard.iq.dataverse.search.SearchFields.GEONAME_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.GEONAME_NAME;
import static edu.harvard.iq.dataverse.search.advanced.SearchFieldType.GEONAME;
import static edu.harvard.iq.dataverse.search.advanced.query.QueryPartType.QUERY;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;

@SuppressWarnings("serial")
public class GeoNameSearchField extends SearchField {

    private String id = StringUtils.EMPTY;
    private String label = StringUtils.EMPTY ;

    public GeoNameSearchField(final String name, final String displayName,
            final String description) {
        super(name, displayName, description, GEONAME);
    }

    public GeoNameSearchField(final DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(),
                datasetFieldType.getDescription(),
                GEONAME, datasetFieldType);
    }

    @Override
    public List<String> getValidatableValues() {
        return this.id.isEmpty() ? emptyList() : singletonList(this.id);
    }

    @Override
    public QueryPart getQueryPart() {
        final String queryFragment = createQueryFragment();
        return queryFragment.isEmpty()
                ? QueryPart.EMPTY
                : new QueryPart(QUERY, queryFragment); 
    }
    
    private String createQueryFragment() {
        final StringBuilder result = new StringBuilder(40);
        if (! this.id.isEmpty()) {
            result.append(GEONAME_ID)
            	.append(':')
            	.append(escapeQueryChars(this.id))
            	.append('*');
        }
        if (! this.label.isEmpty()) {
            appendAndTo(result);
            result.append(GEONAME_NAME)
            	.append(':')
            	.append('"')
                .append(escapeQueryChars(this.label))
                .append('"');
        }
        return result.toString();
    }
    
    private static void appendAndTo(final StringBuilder builder) {
        if(builder.length() > 0) {
            builder.append(" AND ");
        }
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
    	this.id = trimToEmpty(id);
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = trimToEmpty(label);
    }
}
