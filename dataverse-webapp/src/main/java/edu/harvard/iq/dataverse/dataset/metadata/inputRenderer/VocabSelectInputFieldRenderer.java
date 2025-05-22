package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;
import org.primefaces.PrimeFaces;

import javax.faces.component.UIComponent;
import javax.faces.event.AjaxBehaviorEvent;
import java.util.List;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns = true;

    /**
     * Sort vocabulary values list in localized labels order 
     */
    private boolean sortByLocalisedStringsOrder = false;

    /**
     * If true event is attached to refresh div containing this vocab select
     */
    private boolean conditionalRenderingParent = false;

    private final ConditionalRendering conditionalRendering;


    // -------------------- CONSTRUCTORS --------------------

    public VocabSelectInputFieldRenderer(
            boolean renderInTwoColumns,
            boolean sortByLocalisedStringsOrder,
            boolean conditionalRenderingParent,
            ConditionalRendering conditionalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.sortByLocalisedStringsOrder = sortByLocalisedStringsOrder;
        this.conditionalRenderingParent = conditionalRenderingParent;
        this.conditionalRendering = conditionalRendering;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns value provided in constructor
     */
    @Override
    public boolean renderInTwoColumns() {
        return renderInTwoColumns;
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
    public boolean showOnCondition(List<DatasetField> subfields) {
        return ConditionalRenderingHelper.shouldRender(subfields, this.conditionalRendering);
    }

    public boolean isSortByLocalisedStringsOrder() {
        return sortByLocalisedStringsOrder;
    }

    public boolean isConditionalRenderingParent() {
        return conditionalRenderingParent;
    }

    // -------------------- LOGIC --------------------

    public void processValueChange(AjaxBehaviorEvent event) {
        UIComponent component = event.getComponent();
        String updateClientId = (String) component.getAttributes().get("updateClientId");
        DatasetField datasetField = (DatasetField) component.getAttributes().get("datasetField");

        clearSiblingsDatasetFieldValue(datasetField);
        // partial refresh of web page
        PrimeFaces.current().ajax().update(updateClientId);
    }

    private void clearSiblingsDatasetFieldValue(DatasetField vocabDatasetField) {
        List<DatasetField> siblingsFields = vocabDatasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + vocabDatasetField.getTypeName()
                        + " didn't have any parent required for conditional rendering"))
                .getDatasetFieldsChildren();

        for (DatasetField sibling : siblingsFields) {
            VocabSelectInputFieldRendererFactory.VocabularyInputRendererOptions siblingRendererOptions = parseRendererOptions(sibling);
            if (hasMatchingConditionalRendering(vocabDatasetField, siblingRendererOptions)) {
                sibling.clearValue();
            }
        }
    }

    private VocabSelectInputFieldRendererFactory.VocabularyInputRendererOptions parseRendererOptions(DatasetField field) {
        String jsonOptions = field.getDatasetFieldType().getInputRendererOptions();
        return Try.of(() -> new Gson().fromJson(jsonOptions,
                        VocabSelectInputFieldRendererFactory.VocabularyInputRendererOptions.class))
                .getOrElseThrow(e -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options: " + jsonOptions, e));
    }

    private boolean hasMatchingConditionalRendering(
            DatasetField current,
            VocabSelectInputFieldRendererFactory.VocabularyInputRendererOptions options) {
        return options.hasConditionalRendering() &&
                current.getDatasetFieldType().getName().equals(options.getConditionalRendering().getDatasetFieldName());
    }
}
