package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import javax.ejb.Stateless;


@Stateless
public class HiddenVocabInputFieldRendererFactory implements InputFieldRendererFactory<HiddenVocabInputFieldRenderer> {
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HIDDEN_VOCABULARY}
     */
    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.HIDDEN_VOCABULARY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allowed options for renderer should match {@link HiddenVocabInputFieldRendererOptions}
     */
    @Override
    public HiddenVocabInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {

        return new HiddenVocabInputFieldRenderer();
    }

}
