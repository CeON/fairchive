package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class MultiSuggestionHandler implements SuggestionHandler {

    public static final String CONTROLLED_VOCABULARY_NAME_COLUMN = "controlledVocabularyName";
    private ControlledVocabularyValueServiceBean vocabularyValueServiceBean;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public MultiSuggestionHandler() {
    }

    @Inject
    public MultiSuggestionHandler(ControlledVocabularyValueServiceBean controlledVocabularySuggestionRepository) {
        this.vocabularyValueServiceBean = controlledVocabularySuggestionRepository;
    }

    // -------------------- LOGIC --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation filers out base on controlled vocabulary (input) name,
     * a.k.a. dictionary name
     */
    @Override
    public List<String> getAllowedFilters() {
        return Lists.newArrayList(CONTROLLED_VOCABULARY_NAME_COLUMN);
    }

    @Override
    public List<Suggestion> generateSuggestions(Map<String, String> filters, String suggestionSourceFieldValue) {
        return vocabularyValueServiceBean
                .findByDatasetFieldTypeNameAndSuggestionLike(filters.get(CONTROLLED_VOCABULARY_NAME_COLUMN), suggestionSourceFieldValue, 10)
                .stream().map(
                        vocabulary ->
                                new Suggestion(vocabulary.getStrValue(), getDetails(vocabulary.getSuggestionDetails()))
                        )
                .collect(toList());
    }

    /***
     * Formatting of details can be changed for specific data field type.
     * Such specific implementation can be used in inputrendereroptions.suggestionSourceClass
     * Default implementation will not chang what is stored in database
     * @param values json array
     * @return can return html
     */
    public String getDetails(String values) {
        List details = Try.of(() -> new Gson().fromJson(values, List.class))
                .getOrElseThrow((e) -> new IllegalArgumentException("Invalid syntax of input renderer options " + values + ")", e));

        return String.join(",", details);
    }
}
