package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

public class CheckboxInputFieldRenderer implements InputFieldRenderer {

    @Override
    public InputRendererType getType() {
        return InputRendererType.CHECKBOX;
    }

    @Override
    public boolean renderInTwoColumns() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

}
