package edu.harvard.iq.dataverse.search.advanced.field;

import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_AUTHORITY_TITLE;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_COVERAGE_NAME;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_LABEL;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_LOCATIONS;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_START;
import static edu.harvard.iq.dataverse.search.SearchFields.PERIODO_STOP;
import static edu.harvard.iq.dataverse.search.advanced.SearchFieldType.PERIODO;
import static edu.harvard.iq.dataverse.search.advanced.query.QueryPartType.QUERY;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;

@SuppressWarnings("serial")
public class PeriodoSearchField extends SearchField {

    private String id = StringUtils.EMPTY;
    private String label = StringUtils.EMPTY;
    private String coverageName = StringUtils.EMPTY;
    private List<String> locations = emptyList();
    private String startEarliest = StringUtils.EMPTY;
    private String startLatest = StringUtils.EMPTY;
    private String stopEarliest = StringUtils.EMPTY;
    private String stopLatest = StringUtils.EMPTY;
    private String authorityTitle  = StringUtils.EMPTY;

    public PeriodoSearchField(final String name, final String displayName,
            final String description) {
        super(name, displayName, description, PERIODO);
    }

    public PeriodoSearchField(final DatasetFieldType datasetFieldType) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(),
                datasetFieldType.getDescription(),
                PERIODO, datasetFieldType);
    }

    @Override
    public List<String> getValidatableValues() {
        return emptyList();
    }
  
    @Override
    public QueryPart getQueryPart() {
        final String queryFragment = createQueryFragment();
        return queryFragment.isEmpty()
                ? QueryPart.EMPTY
                : new QueryPart(QUERY, queryFragment); 
    }
    
    private String createQueryFragment() {
        final StringBuilder result = new StringBuilder(150);
        appendId(result);
        appendLabel(result);
        appendCoverageName(result);
        appendLocations(result);
        appendDateRange(result, PERIODO_START, this.startEarliest, this.startLatest);
        appendDateRange(result, PERIODO_STOP, this.stopEarliest, this.stopLatest);
        appendAuthorityTitle(result);
        return result.toString();
    }

	private void appendAuthorityTitle(final StringBuilder result) {
		if (! this.authorityTitle.isEmpty()) {
            appendAndTo(result);
            result.append(PERIODO_AUTHORITY_TITLE)
            	.append(":\"")
                .append(escapeQueryChars(this.authorityTitle))
                .append('"');
        }
	}

	private void appendId(final StringBuilder result) {
		if (! this.id.isEmpty()) {
            result.append(PERIODO_ID)
            	.append(':')
            	.append('*')
            	.append(escapeQueryChars(this.id))
            	.append('*');
        }
	}

	private void appendLabel(final StringBuilder result) {
		if (! this.label.isEmpty()) {
            appendAndTo(result);
            result.append(PERIODO_LABEL)
            	.append(":\"")
            	.append(escapeQueryChars(this.label))
            	.append('"');
        }
	}

	private void appendCoverageName(final StringBuilder result) {
		if (! this.coverageName.isEmpty()) {
            appendAndTo(result);
            result.append(PERIODO_COVERAGE_NAME)
            	.append(":\"")
                .append(escapeQueryChars(this.coverageName))
                .append('"');
        }
	}

	private void appendLocations(final StringBuilder result) {
		if(! this.locations.isEmpty()) {
            final Iterator<String> iterator = this.locations.iterator();
            if(iterator.hasNext()) {
                appendAndTo(result);
                result.append(PERIODO_LOCATIONS)
                	.append(":\"")
                	.append(escapeQueryChars(iterator.next()))
                	.append('"');
                while(iterator.hasNext()) {
                    appendOrTo(result);
                    result.append(PERIODO_LOCATIONS)
                    	.append(":\"")
                        .append(escapeQueryChars(iterator.next()))
                        .append('"');
                }
            }
        }
	}
	
	private static void appendDateRange(final StringBuilder result, final String field, 
			final String earliest, final String latest) {
	    if (!earliest.isEmpty() && !latest.isEmpty()) {
	        appendAndTo(result);
	        result.append(field).append(":[")
	              .append(escapeQueryChars(earliest))
	              .append(" TO ")
	              .append(escapeQueryChars(latest))
	              .append("]");
	    } else if (!earliest.isEmpty()) {
	        appendAndTo(result);
	        result.append(field).append(":[")
	              .append(escapeQueryChars(earliest))
	              .append(" TO *]");
	    } else if (!latest.isEmpty()) {
	        appendAndTo(result);
	        result.append(field).append(":[* TO ")
	              .append(escapeQueryChars(latest))
	              .append("]");
	    }
	}
    
    private static void appendAndTo(final StringBuilder builder) {
        if(builder.length() > 0) {
            builder.append(" AND ");
        }
    }
    
    private static void appendOrTo(final StringBuilder builder) {
        if(builder.length() > 0) {
            builder.append(" OR ");
        }
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = trimToEmpty(id);
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = trimToEmpty(label);
    }

    public String getCoverageName() {
        return this.coverageName;
    }

    public void setCoverageName(final String coverageName) {
        this.coverageName = trimToEmpty(coverageName);;
    }

    public List<String> getLocations() {
        return this.locations;
    }

    public void setLocations(final List<String> locations) {
        this.locations = emptyIfNull(locations);
    }

    public String getStartEarliest() {
        return this.startEarliest;
    }

    public void setStartEarliest(final String startEarliest) {
        this.startEarliest = trimToEmpty(startEarliest);
    }

    public String getStartLatest() {
        return this.startLatest;
    }

    public void setStartLatest(final String startLatest) {
        this.startLatest = trimToEmpty(startLatest);
    }

    public String getStopEarliest() {
        return this.stopEarliest;
    }

    public void setStopEarliest(final String stopEarliest) {
        this.stopEarliest = trimToEmpty(stopEarliest);
    }

    public String getStopLatest() {
        return this.stopLatest;
    }

    public void setStopLatest(final String stopLatest) {
        this.stopLatest = trimToEmpty(stopLatest);
    }

    public String getAuthorityTitle() {
        return this.authorityTitle;
    }

    public void setAuthorityTitle(final String authorityTitle) {
        this.authorityTitle = trimToEmpty(authorityTitle);
    }
}
