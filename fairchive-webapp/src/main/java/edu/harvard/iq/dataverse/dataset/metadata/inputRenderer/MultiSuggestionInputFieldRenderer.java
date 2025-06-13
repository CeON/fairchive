package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.MultiSuggestionInputFieldRendererFactory.SuggestionDisplayType;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class MultiSuggestionInputFieldRenderer implements InputFieldRenderer {

    private ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    private static final String VALUE_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.valueHeader";
    private static final String DETAILS_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.detailsHeader";

    private Map<String, String> datasetFieldTypeToSuggestionFilterMapping;
    private SuggestionDisplayType suggestionDisplayType;
    private String datasetFieldTypeName;
    private String metadataBlockName;
    private ConditionalRendering conditionalRendering;
    public static final String CONTROLLED_VOCABULARY_NAME_COLUMN = "controlledVocabularyName";

    // -------------------- CONSTRUCTORS --------------------

    public MultiSuggestionInputFieldRenderer() {
    }

    /**
     * Constructs renderer with support for suggestions.
     */
    public MultiSuggestionInputFieldRenderer(
            ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean,
            Map<String, String> datasetFieldTypeToSuggestionFilterMapping,
            SuggestionDisplayType suggestionDisplayType,
            String datasetFieldTypeName,
            String metadataBlockName,
            ConditionalRendering conditionalRendering) {
        this.controlledVocabularyValueServiceBean = controlledVocabularyValueServiceBean;
        this.datasetFieldTypeToSuggestionFilterMapping = datasetFieldTypeToSuggestionFilterMapping;
        this.suggestionDisplayType = suggestionDisplayType;
        this.datasetFieldTypeName = datasetFieldTypeName;
        this.metadataBlockName = metadataBlockName;
        this.conditionalRendering = conditionalRendering;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#MULTI_SUGGESTION_TEXT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.MULTI_SUGGESTION_TEXT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code true}
     */
    @Override
    public boolean renderInTwoColumns() {
        return true;
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

    public SuggestionDisplayType getSuggestionDisplayType() {
        return suggestionDisplayType;
    }

    public String getValueHeaderText() {
        return BundleUtil.getStringFromNonDefaultBundle(
                String.format(VALUE_HEADER_KEY_FORMAT, datasetFieldTypeName), metadataBlockName);
    }
    public String getDetailsHeaderText() {
        return BundleUtil.getStringFromNonDefaultBundle(
                String.format(DETAILS_HEADER_KEY_FORMAT, datasetFieldTypeName), metadataBlockName);
    }


    // -------------------- LOGIC --------------------

    /**
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<ControlledVocabularyValue> processSuggestionQuery(DatasetField datasetField, String autoCompleteId) {
        return createSuggestions(
                datasetField,
                SuggestionAutocompleteHelper.processSuggestionQuery(autoCompleteId)
                        .orElseThrow(() -> new IllegalStateException("Autocomplete query was not found.")));
    }

    /**
     * Creates suggestions, if suggestionFilteredBy is not empty it searches through datasetField group to find the inputed values.
     */
    public List<ControlledVocabularyValue> createSuggestions(DatasetField datasetField, String query) {

        Map<String, String> suggestionFilteredFields = new HashMap<>();

        if (!datasetFieldTypeToSuggestionFilterMapping.isEmpty()) {
            suggestionFilteredFields = getFilterValue(datasetField, datasetFieldTypeToSuggestionFilterMapping);

            if (suggestionFilteredFields.isEmpty()){
                return new ArrayList<>();
            }
        }

        return controlledVocabularyValueServiceBean
                .findByDatasetFieldTypeNameAndValueLike(suggestionFilteredFields.get(CONTROLLED_VOCABULARY_NAME_COLUMN), query, 10);
    }


    private String getDetails(String values) {
        String[] parts = values.split("\\|");
        StringBuilder html = new StringBuilder();
        html.append("<b>").append("Nazawa jakas").append("</b>: ");
        html.append(parts[0]);
        html.append("<b>").append("Nazawa alternatywbna").append("</b>: ");
        html.append(parts[1]);
        return html.toString();
    }

    // -------------------- PRIVATE --------------------

    /**
     * In this scenario we are using directly value from datasetFieldTypeToSuggestionFilterMapping
     * which comes from datasetField configuration.
     * This value is used directly to filter out suggestions.
     */
    private Map<String, String> getFilterValue(DatasetField datasetField, Map<String, String> datasetFieldTypeToSuggestionFilterMapping) {
        return datasetFieldTypeToSuggestionFilterMapping
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(datasetField.getTypeName()))
                .findFirst()
                .map(entry -> {
                    Map<String, String> filterValues = new HashMap<>();
                    filterValues.put(entry.getValue(), entry.getKey());
                    return filterValues;
                })
                .orElseGet(HashMap::new);
    }
}
