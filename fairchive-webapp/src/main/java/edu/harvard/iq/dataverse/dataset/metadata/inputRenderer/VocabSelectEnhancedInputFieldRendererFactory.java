package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

import javax.ejb.Stateless;

@Stateless
public class VocabSelectEnhancedInputFieldRendererFactory implements InputFieldRendererFactory<VocabSelectEnhancedInputFieldRenderer> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_ENHANCED_SELECT}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.VOCABULARY_ENHANCED_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ignores provided options
     */
    @Override
    public VocabSelectEnhancedInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        VocabSelectEnhancedInputFieldRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, VocabSelectEnhancedInputFieldRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));

        return new VocabSelectEnhancedInputFieldRenderer(rendererOptions.getConditionalRendering());
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * Class representing allowed options for {@link VocabSelectEnhancedInputFieldRenderer}
     */
    public static class VocabSelectEnhancedInputFieldRendererOptions {
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
