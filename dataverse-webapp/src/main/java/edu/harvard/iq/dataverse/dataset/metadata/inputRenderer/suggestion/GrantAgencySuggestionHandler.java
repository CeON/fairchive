package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.suggestion.GrantSuggestion;

@Stateless
public class GrantAgencySuggestionHandler implements SuggestionHandler {

    private GrantSuggestionDao grantSuggestionDao;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public GrantAgencySuggestionHandler() {
    }

    @Inject
    public GrantAgencySuggestionHandler(GrantSuggestionDao grantSuggestionDao) {
        this.grantSuggestionDao = grantSuggestionDao;
    }

    // -------------------- LOGIC --------------------

    /**
     * This suggestion is not dependent on siblings.
     * All values will be taken to mach the suggestion string.
     */
    @Override
    public boolean isDependentOnSiblings() {
        return !getAllowedFilters().isEmpty();
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldValue) {

        return grantSuggestionDao.fetchSuggestions(filters, GrantSuggestion.SUGGESTION_NAME_COLUMN, suggestionSourceFieldValue.trim(), 10)
                .stream().map(suggestionString -> new Suggestion(suggestionString))
                .collect(toList());
    }
}
