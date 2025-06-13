package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.SuggestionInputFieldRendererFactory.SuggestionDisplayType;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.MultiSuggestionHandler;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.*;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.AjaxBehaviorEvent;
import java.util.*;
import java.util.stream.Collectors;

public class SuggestionInputFieldRenderer implements InputFieldRenderer {

    //private ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;
    public static final String CONTROLLED_VOCABULARY_NAME_COLUMN = "controlledVocabularyName";

    private static final String VALUE_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.valueHeader";
    private static final String DETAILS_HEADER_KEY_FORMAT = "datasetfieldtype.%s.suggestionDisplay.detailsHeader";

    private SuggestionHandler suggestionHandler;
    private Map<String, String> datasetFieldTypeToSuggestionFilterMapping;
    private SuggestionDisplayType suggestionDisplayType;
    private String datasetFieldTypeName;
    private String metadataBlockName;
    private ConditionalRendering conditionalRendering;
    private final CapturingConverter converter = new CapturingConverter();
    private DatasetField df = null;

    // -------------------- CONSTRUCTORS --------------------

    public SuggestionInputFieldRenderer() {
    }

    /**
     * Constructs renderer with support for suggestions.
     */
    public SuggestionInputFieldRenderer(
            //ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean,
            SuggestionHandler suggestionHandler,
            Map<String, String> datasetFieldTypeToSuggestionFilterMapping,
            SuggestionDisplayType suggestionDisplayType,
            String datasetFieldTypeName,
            String metadataBlockName,
            ConditionalRendering conditionalRendering) {

        //this.controlledVocabularyValueServiceBean = controlledVocabularyValueServiceBean;
        this.suggestionHandler = suggestionHandler;
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
     * This implementation always returns {@link InputRendererType#SUGGESTION_TEXT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.SUGGESTION_TEXT;
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

    public Converter getConverter() {
        return this.converter;
    }

    // -------------------- LOGIC --------------------

    /**
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<Suggestion> processSuggestionQuery(DatasetField datasetField, String autoCompleteId) {
        this.df = datasetField;
        return createSuggestions(
                datasetField,
                SuggestionAutocompleteHelper.processSuggestionQuery(autoCompleteId)
                        .orElseThrow(() -> new IllegalStateException("Autocomplete query was not found.")));
    }

    /**
     * Creates suggestions, if suggestionFilteredBy is not empty it searches through datasetField group to find the inputed values.
     */
    public List<Suggestion> createSuggestions(DatasetField datasetField, String query) {

        Map<String, String> suggestionFilteredFields = new HashMap<>();

        if (!datasetFieldTypeToSuggestionFilterMapping.isEmpty()) {
            if(suggestionHandler.isDependentOnSiblings()) {
                suggestionFilteredFields = findFilterValuesInSiblings(datasetField, datasetFieldTypeToSuggestionFilterMapping);
            } else {
                suggestionFilteredFields = getFilterValue(datasetField, datasetFieldTypeToSuggestionFilterMapping);
            }

            if (suggestionFilteredFields.isEmpty()){
                return new ArrayList<>();
            }
        }

        return suggestionHandler.generateSuggestions(suggestionFilteredFields, query);
    }


    public void handleAutocompleteSelect(AjaxBehaviorEvent event) {
        // event.getObject() will contain the object from your completeMethod's list
        // In your case, it will be an instance of AutocompleteClass (or whatever your completeMethod returns).
//        Suggestion selectedSuggestion = null;//(Suggestion) event.getObject();

//        this.df.setSingleControlledVocabularyValue(((SelectEvent<ControlledVocabularyValue>)event).getObject());
        // Now, convert this AutocompleteClass to your ControlledVocabularyClass
        // and add it to your controlledVocabularyValues list.
        // This is where you implement the conversion logic (as discussed in previous answers).

      //  Optional<ControlledVocabularyValue> controlledVocabularyValues = controlledVocabularyValueServiceBean.findByIdentifier(selectedSuggestion.value).stream().findFirst();

//        controlledVocabularyValues.ifPresent(c ->
//
//        );
//
//        // Add to your main list, handling duplicates if multiple="true"
//        if (!controlledVocabularyValues.contains(convertedValue)) {
//            controlledVocabularyValues.add(convertedValue);
//        }

        //System.out.println("Selected: " + selectedSuggestion.getDisplayString());
        // You might log or perform other business logic here
    }

    // Optional: If you use itemUnselect for multiple="true"
    public void handleAutocompleteUnselect(UnselectEvent event) {
//        AutocompleteClass unselectedSuggestion = (AutocompleteClass) event.getObject();
//
//        // Find and remove the corresponding ControlledVocabularyClass from your list
//        controlledVocabularyValues.removeIf(cv -> cv.getId().equals(unselectedSuggestion.getAutocompleteId()));
//
//        System.out.println("Unselected: " + unselectedSuggestion.getDisplayString());
    }

    // -------------------- PRIVATE --------------------

    /**
     * In this scenario we are using indirectly value from datasetFieldTypeToSuggestionFilterMapping
     * which comes from datasetField configuration.
     * This value is used indirectly to filter out suggestions.
     * This value is the name of other siblings datasetField in datasetField group,
     * that shares common parent. Method loops over all siblings to find out
     * the one that have corresponding name, then its value is used to filter out suggestions.
     */
    private Map<String, String> findFilterValuesInSiblings(DatasetField datasetField, Map<String, String> datasetFieldTypeToSuggestionFilterMapping) {
        Map<String, String> filteredValues = datasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + datasetField.getTypeName() + " didn't have any parent required to generate suggestions"))
                .getDatasetFieldsChildren().stream()
                .filter(dsf -> datasetFieldTypeToSuggestionFilterMapping.containsKey(dsf.getTypeName()))
                .map(dsf -> Tuple.of(
                        datasetFieldTypeToSuggestionFilterMapping.get(dsf.getTypeName()),
                        dsf.getFieldValue().getOrElse(StringUtils.EMPTY)))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        return filteredValues.containsValue(StringUtils.EMPTY) ? new HashMap<>() : filteredValues;
    }

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

    private class CapturingConverter implements Converter {

        @Override
        public Object getAsObject(final FacesContext context,
                                                     final UIComponent component,
                                                     final String value) {
            System.out.print("Value: " + value);
            return new ControlledVocabularyValue(2592L, "warszawa", new DatasetFieldType());
        }

        @Override
        public String getAsString(final FacesContext context,
                                  final UIComponent component,
                                  final Object value) {
            if (value == null || "".equals(value)) {
                return "";
            }
            return value instanceof ControlledVocabularyValue ?
                    String.valueOf(((ControlledVocabularyValue) value).getIdentifier()):
                    String.valueOf(value);
        }
    }
}
