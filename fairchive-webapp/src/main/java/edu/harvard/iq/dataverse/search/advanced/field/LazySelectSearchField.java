package edu.harvard.iq.dataverse.search.advanced.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;

/**
 * Search field rendered as autocomplete component on UI. Class is
 * intended to use for controlled vocabulary {@link DatasetFieldType}s
 * where there are too many available options to render them all at once.
 * 
 * @author Krzysztof Mądry, Sylwester Niewczas
 */
@SuppressWarnings("serial")
public class LazySelectSearchField extends SearchField {

    private static final int RESULTS_INCREMENT_STEP = 100;

    private List<String> selected = new ArrayList<>();
    private Collection<String> all = new ArrayList<>();
    private int numberOfResults = 100;
    private boolean allowMultiple;

    public LazySelectSearchField(DatasetFieldType datasetFieldType, boolean allowMultiple) {
        super(datasetFieldType.getName(), datasetFieldType.getDisplayName(), datasetFieldType.getDescription(),
                SearchFieldType.LAZY_SELECT_VALUE, datasetFieldType);
        this.allowMultiple = allowMultiple;
    }

    @Override
    public List<String> getValidatableValues() {
        return Collections.emptyList();
    }

    @Override
    public QueryPart getQueryPart() {
        return selected != null && !selected.isEmpty()
                ? new QueryPart(QueryPartType.QUERY, selected.stream()
                    .map(v -> String.format("%s:\"%s\"", getName(), v))
                    .collect(Collectors.joining(" AND ")))
                : QueryPart.EMPTY;
    }

    public List<String> getSelected() {
        return selected;
    }

    public String getOneSelected() {
        return selected.isEmpty() ? null : selected.get(0);
    }

    public int getNumberOfResults() {
        return numberOfResults;
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public void onSelection() {
        queryControlledVocabularyValues("");
    }

    public void onDeselection() {
        queryControlledVocabularyValues("");
    }

    public List<String> complete(String query) {
        return queryControlledVocabularyValues(query);
    }

    public List<String> loadMore() {
        numberOfResults += RESULTS_INCREMENT_STEP;
        return queryControlledVocabularyValues("");
    }

    private List<String> queryControlledVocabularyValues(String query) {
        if (all == null || all.isEmpty()) {
            all = getDatasetFieldType().getControlledVocabularyValues().stream().map(x -> x.getStrValue()).collect(Collectors.toList());
        }

        return all.stream()
                .filter(item -> item.toLowerCase().contains(query.toLowerCase()))
                .filter(item -> !selected.contains(item))
                .limit(numberOfResults)
                .collect(Collectors.toList());
    }


    public void setSelected(List<String> selected) {
        this.selected = selected;
    }

    public void setOneSelected(String selected) {
        this.selected.clear();
        this.selected.add(selected);
    }

    public void setNumberOfResults(int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }
}
