package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.PERIODO;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import io.vavr.control.Try;

@Stateless
public class PeriodoInputFieldRendererFactory implements InputFieldRendererFactory<PeriodoRenderer>{

    @Override
    public InputRendererType isFactoryForType() {
        return PERIODO;
    }

    @Override
    public PeriodoRenderer createRenderer(final DatasetFieldType fieldType, 
            final JsonObject jsonOptions) {
        PeriodoRendererOptions rendererOptions = Try.of(() -> new Gson().fromJson(jsonOptions, PeriodoRendererOptions.class))
                .getOrElseThrow((e) -> new InputRendererInvalidConfigException("Invalid syntax of input renderer options " + jsonOptions + ")", e));


        return new PeriodoRenderer(rendererOptions.getConditionalRendering());
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * Class representing allowed options for {@link PeriodoRenderer}
     */
    public static class PeriodoRendererOptions {
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
