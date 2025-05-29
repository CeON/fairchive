package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

import javax.ejb.Stateless;

@Stateless
public class HtmlMarkupInputFieldRendererFactory implements InputFieldRendererFactory<HtmlMarkupInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HTML_MARKUP}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.HTML_MARKUP;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public HtmlMarkupInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        HtmlMarkupInputFieldRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, HtmlMarkupInputFieldRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));

        return new HtmlMarkupInputFieldRenderer(rendererOptions.getConditionalRendering());
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * Class representing allowed options for {@link HtmlMarkupInputFieldRenderer}
     */
    public static class HtmlMarkupInputFieldRendererOptions {
        private ConditionalRendering conditionalRendering;

        // -------------------- GETTERS --------------------

        public ConditionalRendering getConditionalRendering() {
            return conditionalRendering;
        }

        // -------------------- SETTERS --------------------

        public void setConditionalRendering(ConditionalRendering conditionalRendering) {
            this.conditionalRendering = conditionalRendering;
        }
    }

}
