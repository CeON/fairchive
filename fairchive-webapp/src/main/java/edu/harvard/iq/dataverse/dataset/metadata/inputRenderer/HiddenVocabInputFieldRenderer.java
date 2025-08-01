package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

import java.util.List;

public class HiddenVocabInputFieldRenderer implements InputFieldRenderer {

    // -------------------- GETTERS --------------------
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#HIDDEN_VOCABULARY}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.HIDDEN_VOCABULARY;
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
     * This implementation always returns {@code true}
     */
    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public boolean showOnCondition(List<DatasetField> subfields) {
        return false;
    }
    
}
