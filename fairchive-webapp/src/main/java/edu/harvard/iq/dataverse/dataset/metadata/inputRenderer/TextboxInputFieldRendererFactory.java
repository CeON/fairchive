package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

import javax.ejb.Stateless;

@Stateless
public class TextboxInputFieldRendererFactory implements InputFieldRendererFactory<TextboxInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#TEXTBOX}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.TEXTBOX;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public TextboxInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        TextboxInputFieldRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, TextboxInputFieldRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));

        return new TextboxInputFieldRenderer(rendererOptions.getConditionalRendering());
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * Class representing allowed options for {@link TextboxInputFieldRenderer}
     */
    public static class TextboxInputFieldRendererOptions {
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
