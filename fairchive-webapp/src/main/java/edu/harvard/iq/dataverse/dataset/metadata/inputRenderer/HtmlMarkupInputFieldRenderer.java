package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Option;

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
    public Option<ConditionalRendering> getConditionalRendering() {
        return Option.of(conditionalRendering);
    }
}
