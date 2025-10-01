package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import edu.harvard.iq.dataverse.search.ror.RorSolrDataFinder;

@Stateless
public class RorSuggestionHandler implements SuggestionHandler {

    @Inject
    private RorSolrDataFinder rorSolrDataFinder;
    
    // -------------------- LOGIC --------------------

    /**
     * This suggestion is dependent on sibling input value.
     * Only values that match pointed out sibling input value will be taken
     * to create suggestion.
     * @see this#getAllowedFilters()
     */
    @Override
    public boolean isDependentOnSiblings() {
        return !getAllowedFilters().isEmpty();
    }

    @Override
    public List<Suggestion> generateSuggestions(final Map<String, String> filteredBy, 
            final String query) {
        
        return this.rorSolrDataFinder.findRorData(query, 5).stream()
            .map(this::convertSolrRorToSuggestion)
            .collect(toList());
    }

    // -------------------- PRIVATE --------------------
    
    private Suggestion convertSolrRorToSuggestion(final RorDto solrRor) {
        return new Suggestion(solrRor.getRorUrl(), generateDisplayName(solrRor));
    }
    
    private String generateDisplayName(final RorDto solrRor) {
        final StringBuilder builder = new StringBuilder(solrRor.getName());
        
        if (isNotEmpty(solrRor.getCountryName())) {
            builder.append(" (").append(solrRor.getCountryName()).append(")");
        }
        builder.append('.');
        
        
        final String otherNamesString = generateOtherNames(solrRor);
        if (isNotEmpty(otherNamesString)) {
            builder
                .append(' ')
                .append(getStringFromBundle("dataset.metadata.inputRenderer.suggestion.ror.otherNames"))
                .append(": ")
                .append(otherNamesString);
        }
        
        return builder.toString();
    }
    
    private String generateOtherNames(final RorDto solrRor) {
        List<String> otherNames = Lists.newArrayList();
        otherNames.addAll(solrRor.getNameAliases());
        otherNames.addAll(solrRor.getAcronyms());
        otherNames.addAll(solrRor.getLabels());
        return otherNames.stream().collect(joining("; "));
    }
}
