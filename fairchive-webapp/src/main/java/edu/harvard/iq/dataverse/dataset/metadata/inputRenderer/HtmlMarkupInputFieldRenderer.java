package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.List;

public class HtmlMarkupInputFieldRenderer implements InputFieldRenderer {

    private final ConditionalRendering conditionalRendering;

    // -------------------- CONSTRUCTORS --------------------

    public HtmlMarkupInputFieldRenderer(ConditionalRendering conditionalRendering) {
        this.conditionalRendering = conditionalRendering;
    }
    // -------------------- GETTERS --------------------
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HTML_MARKUP}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.HTML_MARKUP;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
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
    public boolean showOnCondition(List<DatasetField> subfields) {
        return ConditionalRenderingHelper.shouldRender(subfields, this.conditionalRendering);
    }

}
