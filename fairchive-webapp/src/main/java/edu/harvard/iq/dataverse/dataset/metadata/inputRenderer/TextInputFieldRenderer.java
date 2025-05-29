package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction.FieldButtonActionHandler;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.List;

public class TextInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns;
    private FieldButtonActionHandler actionButtonHandler;
    private List<MetadataOperationSource> enableActionForOperations;
    private String actionButtonTextKey;
    private ConditionalRendering conditionalRendering;


    // -------------------- CONSTRUCTORS --------------------

    /**
     * Constructs simple renderer (without additional action button)
     */
    public TextInputFieldRenderer(boolean renderInTwoColumns, ConditionalRendering conditionalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.conditionalRendering = conditionalRendering;
    }

    /**
     * Constructs renderer with support for action button.
     */
    public TextInputFieldRenderer(boolean renderInTwoColumns, FieldButtonActionHandler actionButtonHandler, String actionButtonTextKey, List<MetadataOperationSource> enableActionForOperations, ConditionalRendering conditonalRendering) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.actionButtonHandler = actionButtonHandler;
        this.enableActionForOperations = enableActionForOperations;
        this.actionButtonTextKey = actionButtonTextKey;
        this.conditionalRendering = conditonalRendering;
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
        return ConditionalRenderingHelper.shouldRender(subfields, this.conditionalRendering);
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
