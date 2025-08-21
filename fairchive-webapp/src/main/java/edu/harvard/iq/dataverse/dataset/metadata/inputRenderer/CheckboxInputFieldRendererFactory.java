package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import javax.ejb.Stateless;

import com.google.gson.JsonObject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

@Stateless
public class CheckboxInputFieldRendererFactory implements InputFieldRendererFactory<CheckboxInputFieldRenderer> {

    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.CHECKBOX;
    }

    @Override
    public CheckboxInputFieldRenderer createRenderer(DatasetFieldType fieldType, JsonObject rendererOptions) {
        return new CheckboxInputFieldRenderer();
    }

}
