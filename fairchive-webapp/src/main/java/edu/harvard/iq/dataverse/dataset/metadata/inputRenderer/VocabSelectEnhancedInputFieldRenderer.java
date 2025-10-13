package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

import static edu.harvard.iq.dataverse.common.BundleUtil.hasKeyInNonDefaultBundle;

import java.util.*;
import java.util.stream.Collectors;


public class VocabSelectEnhancedInputFieldRenderer implements InputFieldRenderer {

    private Collection<ControlledVocabularyValue> all = new ArrayList<>();
    private int numberOfResults = 100;
    private final ConditionalRendering conditionalRendering;
    private static final int RESULTS_INCREMENT_STEP = 100;

    // -------------------- CONSTRUCTORS --------------------

    public VocabSelectEnhancedInputFieldRenderer(ConditionalRendering conditionalRendering) {
        this.conditionalRendering = conditionalRendering;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_ENHANCED_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_ENHANCED_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns value provided in constructor
     */
    @Override
    public boolean renderInTwoColumns() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public Option<ConditionalRendering> getConditionalRendering() {
        return Option.of(conditionalRendering);
    }

    public Integer getNumberOfResults() {
        return numberOfResults;
    }

    /**
     * Text providing a short hint that describes what to enter in autocomplete
     * input field.
     */
    public String getAutocompletePlaceholderMessage(DatasetField datasetField) {
        String key = "datasetfieldtype." + datasetField.getDatasetFieldType().getName() + ".autocomplete.placeholder";
        return getStringFromMetadataBlockBundle(datasetField.getDatasetFieldType(), key, "common.forms.autocomplete.placeholder");
    }

    /**
     * Text to display in load more button. It is shown only when there are
     * more results matching the autocomplete input text but they are not
     * currently displayed.
     */
    public String getAutocompleteLoadMoreMessage(DatasetField datasetField) {
        String key = "datasetfieldtype." + datasetField.getDatasetFieldType().getName() + ".autocomplete.loadMore";
        return getStringFromMetadataBlockBundle(datasetField.getDatasetFieldType(), key, "common.forms.autocomplete.loadMore");
    }

    /**
     * Text to display when there is no data to display.
     */
    public String getAutocompleteEmptyMessage(DatasetField datasetField) {
        String key = "datasetfieldtype." + datasetField.getDatasetFieldType().getName() + ".autocomplete.emptySuggestionMessage";
        return getStringFromMetadataBlockBundle(datasetField.getDatasetFieldType(), key, "common.forms.autocomplete.emptySuggestionMessage");
    }

    /**
     * Hint text for screen readers to provide information about the search
     * results. Default is [NUMBER_OF_RESULTS] + "results are available, use
     * up and down arrow keys to navigate".
     */
    public String getAutocompleteResultsMessage(DatasetField datasetField) {
        String key = "datasetfieldtype." + datasetField.getDatasetFieldType().getName() + ".autocomplete.resultsMessage";
        return getStringFromMetadataBlockBundle(datasetField.getDatasetFieldType(), key, "common.forms.autocomplete.resultsMessage");
    }

    public void onSelection(DatasetField datasetField, String autoCompleteId) {
        queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public void onDeselection(DatasetField datasetField, String autoCompleteId) {
        queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public List<ControlledVocabularyValue> complete(DatasetField datasetField, String autoCompleteId) {
        return queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    public List<ControlledVocabularyValue> loadMore(DatasetField datasetField, String autoCompleteId) {
        numberOfResults += RESULTS_INCREMENT_STEP;
        return queryControlledVocabularyValues(datasetField, autoCompleteId);
    }

    private List<ControlledVocabularyValue> queryControlledVocabularyValues(DatasetField datasetField, String autoCompleteId) {
        if (all == null || all.isEmpty()) {
            all = datasetField.getDatasetFieldType().getControlledVocabularyValues();
        }
        String query = SuggestionAutocompleteHelper.processSuggestionQuery(autoCompleteId).orElse("");
        return all.stream()
                .filter(item -> !datasetField.getControlledVocabularyValues().contains(item))
                .filter(item -> item.getStrValue().toLowerCase().contains(query.toLowerCase()))
                .limit(numberOfResults)
                .collect(Collectors.toList());
    }

    private String getStringFromMetadataBlockBundle(DatasetFieldType datasetFieldType, String key, String fallbackKey) {

        if (datasetFieldType.getMetadataBlock() != null &&
                hasKeyInNonDefaultBundle(key, datasetFieldType.getMetadataBlock().getName())) {

            BundleUtil.getStringFromNonDefaultBundle(key, datasetFieldType.getMetadataBlock().getName());
        }
        return BundleUtil.getStringFromBundle(fallbackKey);
    }
}
