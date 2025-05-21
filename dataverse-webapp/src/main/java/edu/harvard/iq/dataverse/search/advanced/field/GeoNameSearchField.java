package edu.harvard.iq.dataverse.search.advanced.field;

import static edu.harvard.iq.dataverse.search.advanced.SearchFieldType.GEONAME;
import static edu.harvard.iq.dataverse.search.advanced.query.QueryPartType.QUERY;

import java.util.ArrayList;
import java.util.List;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;

@SuppressWarnings("serial")
public class GeoNameSearchField extends SearchField {

    private String id;
    private String name;

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
    public SearchFieldType getSearchFieldType() {
        return super.getSearchFieldType();
    }

    @Override
    public List<String> getValidatableValues() {
        final ArrayList<String> result = new ArrayList<>();
        if (this.id != null) {
            result.add(this.id);
        }
        if (this.name != null) {
            result.add(this.name);
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
            result.append(SearchFields.GEONAME_ID).append(":\"").append(this.id.trim())
                    .append('"');
        }
        if (this.name != null) {
            appendAndTo(result);
            result.append(SearchFields.GEONAME_NAME).append(":\"")
                    .append(this.name.trim()).append('"');
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
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
