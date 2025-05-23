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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;

@SuppressWarnings("serial")
public class PeriodoSearchField extends SearchField {

    private String id;
    private String label;
    private String coverageName;
    private List<String> locations = new ArrayList<>();
    private String startEarliest;
    private String startLatest;
    private String stopEarliest;
    private String stopLatest;
    private String authorityTitle;

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
    public SearchFieldType getSearchFieldType() {
        return super.getSearchFieldType();
    }

    @Override
    public List<String> getValidatableValues() {
        final ArrayList<String> result = new ArrayList<>();
        if (this.id != null) {
            result.add(this.id);
        }
        if (this.label != null) {
            result.add(this.label);
        }
        if (this.coverageName != null) {
            result.add(this.coverageName);
        }
        this.locations.forEach(result::add);
        if(this.startEarliest != null) {
            result.add(this.startEarliest);
        }
        if(this.startLatest != null) {
            result.add(this.startLatest);
        }
        if(this.stopEarliest != null) {
            result.add(this.stopEarliest);
        }
        if(this.stopLatest != null) {
            result.add(this.stopLatest);
        }
        if(this.authorityTitle != null) {
            result.add(this.authorityTitle);
        }
        return result;
    }

    @Override
    public QueryPart getQueryPart() {
        final String queryFragment = createQueryFramgment();
        return queryFragment.isEmpty()
                ? QueryPart.EMPTY
                : new QueryPart(QUERY, createQueryFramgment()); 
    }
    
    private String createQueryFramgment() {
        final StringBuilder result = new StringBuilder(150);
        if (this.id != null) {
            result.append(PERIODO_ID).append(":\"").append(this.id.trim()).append('"');
        }
        if (this.label != null) {
            appendAndTo(result);
            result.append(PERIODO_LABEL).append(":\"").append(this.label.trim())
                    .append('"');
        }
        if (this.coverageName != null) {
            appendAndTo(result);
            result.append(PERIODO_COVERAGE_NAME).append(":\"")
                    .append(this.coverageName.trim()).append('"');
        }
        if(!this.locations.isEmpty()) {
            final Iterator<String> iterator = this.locations.iterator();
            if(iterator.hasNext()) {
                appendAndTo(result);
                result.append(PERIODO_LOCATIONS).append(":\"")
                .append(iterator.next()).append('"');
                while(iterator.hasNext()) {
                    appendOrTo(result);
                    result.append(PERIODO_LOCATIONS).append(":\"")
                            .append(iterator.next()).append('"');
                }
            }
        }
        // start date
        if (this.startEarliest != null & this.startLatest != null) {
            appendAndTo(result);
            result.append(PERIODO_START).append(":[").append(this.startEarliest.trim())
                    .append(" TO ").append(this.startLatest.trim()).append("]");
        } else if (this.startEarliest != null & this.startLatest == null) {
            appendAndTo(result);
            result.append(PERIODO_START).append(":[").append(this.startEarliest.trim())
                    .append(" TO *]");
        } else if (this.startEarliest == null & this.startLatest != null) {
            appendAndTo(result);
            result.append(PERIODO_START).append(":[* TO ")
                    .append(this.startLatest.trim()).append("]");
        }
        // stop date
        if (this.stopEarliest != null & this.stopLatest != null) {
            appendAndTo(result);
            result.append(PERIODO_STOP).append(":[").append(this.stopEarliest.trim())
                    .append(" TO ").append(this.stopLatest.trim()).append("]");
        } else if (this.stopEarliest != null & this.stopLatest == null) {
            appendAndTo(result);
            result.append(PERIODO_STOP).append(":[").append(this.stopEarliest.trim())
                    .append(" TO *]");
        } else if (this.stopEarliest == null & this.stopLatest != null) {
            appendAndTo(result);
            result.append(PERIODO_STOP).append(":[* TO ")
                    .append(this.stopLatest.trim()).append("]");
        }
        if (this.authorityTitle != null) {
            appendAndTo(result);
            result.append(PERIODO_AUTHORITY_TITLE).append(":\"")
                    .append(this.authorityTitle.trim()).append('"');
        }
        return result.toString();
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

    public void setId(final String id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getCoverageName() {
        return this.coverageName;
    }

    public void setCoverageName(final String coverageName) {
        this.coverageName = coverageName;
    }

    public List<String> getLocations() {
        return this.locations;
    }

    public void setLocations(final List<String> locations) {
        this.locations = locations;
    }
    
    public List<String> getAllLocations() {
        return PeriodoDictionary.locations();
    }

    public String getStartEarliest() {
        return this.startEarliest;
    }

    public void setStartEarliest(final String startEarliest) {
        this.startEarliest = startEarliest;
    }

    public String getStartLatest() {
        return this.startLatest;
    }

    public void setStartLatest(final String startLatest) {
        this.startLatest = startLatest;
    }

    public String getStopEarliest() {
        return this.stopEarliest;
    }

    public void setStopEarliest(final String stopEarliest) {
        this.stopEarliest = stopEarliest;
    }

    public String getStopLatest() {
        return this.stopLatest;
    }

    public void setStopLatest(final String stopLatest) {
        this.stopLatest = stopLatest;
    }

    public String getAuthorityTitle() {
        return this.authorityTitle;
    }

    public void setAuthorityTitle(final String authorityTitle) {
        this.authorityTitle = authorityTitle;
    }
}
