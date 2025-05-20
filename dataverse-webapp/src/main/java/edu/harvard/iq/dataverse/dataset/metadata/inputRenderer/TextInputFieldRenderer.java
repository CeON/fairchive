package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.List;
import java.util.Optional;

public class TextInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns;
    private FieldButtonActionHandler actionButtonHandler;
    private List<MetadataOperationSource> enableActionForOperations;
    private String actionButtonTextKey;
    private ConditionalRendering conditonalRendering;


    // -------------------- CONSTRUCTORS --------------------

    /**
     * Constructs simple renderer (without additional action button)
     */
    public TextInputFieldRenderer(boolean renderInTwoColumns, ConditionalRendering conditonalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.conditonalRendering = conditonalRendering;
    }

    /**
     * Constructs renderer with support for action button.
     */
    public TextInputFieldRenderer(boolean renderInTwoColumns, FieldButtonActionHandler actionButtonHandler, String actionButtonTextKey, List<MetadataOperationSource> enableActionForOperations, ConditionalRendering conditonalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.actionButtonHandler = actionButtonHandler;
        this.enableActionForOperations = enableActionForOperations;
        this.actionButtonTextKey = actionButtonTextKey;
        this.conditonalRendering = conditonalRendering;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#TEXT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.TEXT;
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
        if (this.conditonalRendering == null) {
            return true;
        }

        Optional<DatasetField> mainField = subfields.stream().filter(df -> df.getDatasetFieldType().getName().equals(conditonalRendering.getDatasetFieldName())).findFirst();
        if (mainField.isPresent()) {
            String value = mainField.get().getValue();
            return conditonalRendering.getRenderOnValue().equals(value);
        } else {
            return true;
        }
    }

    // -------------------- LOGIC --------------------

    public boolean hasActionButton() {
        return actionButtonHandler != null;
    }

    public boolean showActionButtonForOperation(String operation) {
        return enableActionForOperations.contains(MetadataOperationSource.valueOf(operation));
    }

    public String getActionButtonText() {
        return BundleUtil.getStringFromBundle(actionButtonTextKey);
    }

    public void executeButtonAction(DatasetField datasetField, List<DatasetFieldsByType> allBlockFields) {

        actionButtonHandler.handleAction(datasetField, allBlockFields);
    }

}
