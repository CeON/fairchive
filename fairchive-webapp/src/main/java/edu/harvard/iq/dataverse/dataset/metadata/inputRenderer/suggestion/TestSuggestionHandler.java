package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import edu.harvard.iq.dataverse.search.ror.RorSolrDataFinder;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Stateless
public class TestSuggestionHandler implements SuggestionHandler {

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
    public List<Suggestion> generateSuggestions(Map<String, String> filteredBy, String query) {
        
       /* return rorSolrDataFinder.findRorData(query, 5).stream()
            .map(this::convertSolrRorToSuggestion)
            .collect(toList());*/
        List<Suggestion> a = new ArrayList<>();
        a.add(new Suggestion("Kultura Praska", generateDisplayName()));
        return a;
    }

    // -------------------- PRIVATE --------------------
    
//    private Suggestion convertSolrRorToSuggestion(RorDto solrRor) {
//        return new Suggestion(solrRor.getRorUrl(), generateDisplayName(solrRor));
//    }
    
    private String generateDisplayName() {
        StringBuilder rorDisplay = new StringBuilder("<b>Test: </b>");
        

        rorDisplay.append("Jaka tam wartosc po angliesku");
        rorDisplay.append("<b>Era</b>");
        rorDisplay.append("XV - XVII");

        return rorDisplay.toString();
    }
    
//    private String generateOtherNames(RorDto solrRor) {
//        List<String> otherNames = Lists.newArrayList();
//        otherNames.addAll(solrRor.getNameAliases());
//        otherNames.addAll(solrRor.getAcronyms());
//        otherNames.addAll(solrRor.getLabels());
//        return otherNames.stream().collect(joining("; "));
//    }
}
