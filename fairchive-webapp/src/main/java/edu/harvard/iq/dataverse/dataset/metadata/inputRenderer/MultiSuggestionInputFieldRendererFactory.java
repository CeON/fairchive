package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.api.client.util.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.suggestion.SuggestionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.collections4.IteratorUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class MultiSuggestionInputFieldRendererFactory implements InputFieldRendererFactory<MultiSuggestionInputFieldRenderer>{

    private final ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public MultiSuggestionInputFieldRendererFactory(ControlledVocabularyValueServiceBean controlledVocabularyValueServiceBean) {
        this.controlledVocabularyValueServiceBean = controlledVocabularyValueServiceBean;
    }


    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.MULTI_SUGGESTION_TEXT;
    }

    @Override
    public MultiSuggestionInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {

        SuggestionInputRendererOptions rendererOptions = parseRendererOptions(jsonOptions);



        Map<String, String> datasetFieldTypeToSuggestionFilterMapping = parseFilteredBy(
                rendererOptions.getSuggestionFilteredBy());

        return new MultiSuggestionInputFieldRenderer(
                this.controlledVocabularyValueServiceBean,
                datasetFieldTypeToSuggestionFilterMapping,
                rendererOptions.getSuggestionDisplayType() == null ? SuggestionDisplayType.SIMPLE : rendererOptions.getSuggestionDisplayType(),
                fieldType.getName(),
                fieldType.getMetadataBlock().getName(),
                rendererOptions.getConditionalRendering());
    }

    // -------------------- PRIVATE --------------------

    private SuggestionInputRendererOptions parseRendererOptions(JsonObject jsonOptions) {
        return Try.of(() -> new Gson().fromJson(jsonOptions, SuggestionInputRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));
    }


    private Map<String, String> parseFilteredBy(List<String> suggestionFilteredBy) {
        Map<String, String> datasetFieldTypeToSuggestionFilterMapping = Maps.newHashMap();
        if (suggestionFilteredBy == null) {
            return datasetFieldTypeToSuggestionFilterMapping;
        }

        for (String filter: suggestionFilteredBy) {
            String[] dftToSuggestionFilter = filter.split(":");
            if (dftToSuggestionFilter.length != 2) {
                throw new InputRendererInvalidConfigException("Invalid value for suggestionFilteredBy: " + filter);
            }
//            if (!suggestionHandler.getAllowedFilters().contains(dftToSuggestionFilter[1])) {
//                throw new InputRendererInvalidConfigException("Suggestion handler: " + suggestionHandler.getName() + " does not support filtering by: : " + dftToSuggestionFilter[1]);
//            }

            datasetFieldTypeToSuggestionFilterMapping.put(
                    dftToSuggestionFilter[0], dftToSuggestionFilter[1]);
        }
        return datasetFieldTypeToSuggestionFilterMapping;
    }

    // -------------------- INNER CLASSES --------------------
    public static class SuggestionInputRendererOptions {

        private String suggestionSourceClass;
        private List<String> suggestionFilteredBy;
        private SuggestionDisplayType suggestionDisplayType;
        private ConditionalRendering conditionalRendering;

        // -------------------- GETTERS --------------------

        /**
         * Defines which class will be used for retrieving suggestions.
         * Value must match {@link SuggestionHandler#getName()}.
         */
        public String getSuggestionSourceClass() {
            return suggestionSourceClass;
        }

        /**
         * Defines additional filters for retrieved suggestions.
         * Each value in list must be in form:
         * <code>datasetFieldTypeName:suggestionFilterName</code>.
         * <p>
         * Allowed values for <code>suggestionFilterName</code> are
         * specific to picked {@link SuggestionHandler} and they
         * are defined in {@link SuggestionHandler#getAllowedFilters()}
         * <p>
         * Suggestions mechanism will take value that is currently
         * entered in field <code>datasetFieldTypeName</code>
         * and pass it to {@link SuggestionHandler#generateSuggestions(Map, String)}
         * filters map in key: <code>suggestionFilterName</code>
         */
        public List<String> getSuggestionFilteredBy() {
            return suggestionFilteredBy;
        }

        /**
         * Defines style in which suggestions will be displayed for user.
         */
        public SuggestionDisplayType getSuggestionDisplayType() {
            return suggestionDisplayType;
        }

        public ConditionalRendering getConditionalRendering() {
            return conditionalRendering;
        }
    }

    public enum SuggestionDisplayType {
        SIMPLE,
        TWO_COLUMNS
    }
}
