package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.Suggestion;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class ControlledVocabularyDetailsSuggestionHandler implements SuggestionHandler {

    public static final String CONTROLLED_VOCABULARY_NAME_COLUMN = "controlledVocabularyName";
    private ControlledVocabularyValueServiceBean vocabularyValueServiceBean;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ControlledVocabularyDetailsSuggestionHandler() {
    }

    @Inject
    public ControlledVocabularyDetailsSuggestionHandler(ControlledVocabularyValueServiceBean controlledVocabularySuggestionRepository) {
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
                .findByDatasetFieldTypeNameAndValueLike(filters.get(CONTROLLED_VOCABULARY_NAME_COLUMN), suggestionSourceFieldValue, 10)
                .stream().map(
                        vocabulary ->
                                new Suggestion(vocabulary.getStrValue(), getDetails(vocabulary))
                        )
                .collect(toList());
    }

    /***
     * Formatting of details can be changed for specific data field type.
     * Such specific implementation can be used in inputrendereroptions.suggestionSourceClass
     * Default implementation will not chang what is stored in database
     * @param controlledVocabularyValue - value of vocab
     * @return can return html
     */
    public String getDetails(ControlledVocabularyValue controlledVocabularyValue) {
        if (StringUtils.isNotBlank(controlledVocabularyValue.getSuggestionDetails())) {
            List details = Try.of(() -> new Gson().fromJson(controlledVocabularyValue.getSuggestionDetails(), List.class))
                .getOrElseThrow((e) -> new IllegalArgumentException("Invalid json of suggestion details " + controlledVocabularyValue.getSuggestionDetails() + ")", e));

            return String.join(",", details);
        }

        return controlledVocabularyValue.getStrValue();
    }
}
