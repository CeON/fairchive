package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        clearSiblingsDatasetFieldValue(datasetField, inputRenderersByFieldType);
    }

    public boolean hasChangeListener(DatasetField vocabDatasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
        Optional<DatasetField> siblingField = vocabDatasetField.getDatasetFieldParent()
                .getOrElseThrow(() -> new NullPointerException("datasetfield with type: " + vocabDatasetField.getTypeName()
                        + " didn't have any parent required for conditional rendering"))
                .getDatasetFieldsChildren()
                .stream()
                .filter(df -> !df.getDatasetFieldType().getName().equals(vocabDatasetField.getDatasetFieldType().getName()))
                .findFirst();

        return siblingField
                .map(s -> inputRenderersByFieldType.get(s.getDatasetFieldType()))
                .flatMap(rr -> rr.getConditionalRendering().toJavaOptional())
                .map(cr -> cr.getDatasetFieldName().equals(vocabDatasetField.getDatasetFieldType().getName()))
                .orElse(false);
    }

    private void clearSiblingsDatasetFieldValue(DatasetField vocabDatasetField, Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType) {
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
            }
        }
    }
}
