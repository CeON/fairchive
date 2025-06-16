package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

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

    public void processValueChange(DatasetField datasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
        clearSiblingsDatasetField(datasetField, inputRenderersByFieldType);
    }

    public boolean hasChangeListener(DatasetField vocabDatasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
        String typeName = vocabDatasetField.getDatasetFieldType().getName();
        return vocabDatasetField
                .getDatasetFieldParent()
                .map(parent ->
                        parent.getDatasetFieldsChildren().stream()
                                .filter(df -> !df.getDatasetFieldType().getName().equals(typeName))
                                .map(df -> inputRenderersByFieldType.get(df.getDatasetFieldType()))
                                .anyMatch(renderer -> hasConditionalRenderingFor(typeName, renderer)))
                .getOrElse(false);
    }

    private boolean hasConditionalRenderingFor(String fieldTypeName, InputFieldRenderer renderer) {
        return Option.of(renderer)
                .flatMap(InputFieldRenderer::getConditionalRendering)
                .map(cr -> cr.getDatasetFieldName().equals(fieldTypeName))
                .getOrElse(false);
    }

    private void clearSiblingsDatasetField(DatasetField vocabDatasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
        List<DatasetField> siblingsFields = vocabDatasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + vocabDatasetField.getTypeName()
                        + " didn't have any parent required for conditional rendering"))
                .getDatasetFieldsChildren()
                .stream()
                .filter(df -> !df.getDatasetFieldType().getName().equals(vocabDatasetField.getDatasetFieldType().getName()))
                .collect(Collectors.toList());

        for (DatasetField sibling : siblingsFields) {
            InputFieldRenderer renderer = inputRenderersByFieldType.get(sibling.getDatasetFieldType());
            if (renderer != null && renderer.getConditionalRendering().isDefined()) {
                sibling.clearValue();
                sibling.setValidationMessage(null);
            }
        }
    }
}
