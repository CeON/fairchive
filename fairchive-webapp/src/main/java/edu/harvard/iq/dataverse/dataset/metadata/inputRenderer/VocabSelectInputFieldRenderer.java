package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

import javax.faces.component.UIComponent;
import javax.faces.event.AjaxBehaviorEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns = true;

    /**
     * Sort vocabulary values list in localized labels order 
     */
    private boolean sortByLocalisedStringsOrder = false;


    private final ConditionalRendering conditionalRendering;


    // -------------------- CONSTRUCTORS --------------------

    public VocabSelectInputFieldRenderer(
            boolean renderInTwoColumns,
            boolean sortByLocalisedStringsOrder,
            ConditionalRendering conditionalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.sortByLocalisedStringsOrder = sortByLocalisedStringsOrder;
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
    public Option<ConditionalRendering> getConditionalRendering() {
        return Option.of(conditionalRendering);
    }

    public boolean isSortByLocalisedStringsOrder() {
        return sortByLocalisedStringsOrder;
    }


    // -------------------- LOGIC --------------------

    public void processValueChange(AjaxBehaviorEvent event) {
        UIComponent component = event.getComponent();
        DatasetField datasetField = (DatasetField) component.getAttributes().get("datasetField");
        Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();
            component.getAttributes().put("inputRenderersByFieldType", inputRenderersByFieldType);

        clearSiblingsDatasetFieldValue(datasetField, inputRenderersByFieldType);
    }

    public boolean hasChangeListener(DatasetField datasetField) {
        return this.conditionalRendering != null && this.conditionalRendering.controlledBy(datasetField);
    }

    private void clearSiblingsDatasetFieldValue(DatasetField vocabDatasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
        List<DatasetField> siblingsFields = vocabDatasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + vocabDatasetField.getTypeName()
                        + " didn't have any parent required for conditional rendering"))
                .getDatasetFieldsChildren()
                .stream()
                .filter(df -> !df.getDatasetFieldType().getName().equals(vocabDatasetField.getDatasetFieldType().getName()))
                .collect(Collectors.toList());

        // Assume that all subfield will be used in conditional rendering(excluding main vocab)
        // If we will need more fields visible all the time this clearing will not work properly
        for (DatasetField sibling : siblingsFields) {
            InputFieldRenderer renderer = inputRenderersByFieldType.get(sibling.getDatasetFieldType());
            if (renderer.getConditionalRendering().isDefined()) {
                sibling.clearValue();
            }
        }
    }
}
