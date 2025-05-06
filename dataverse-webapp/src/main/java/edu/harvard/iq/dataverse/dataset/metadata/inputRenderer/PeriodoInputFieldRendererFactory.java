package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.PERIODO;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.google.gson.JsonObject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

@Stateless
public class PeriodoInputFieldRendererFactory implements InputFieldRendererFactory<PeriodoRenderer>{

    @Override
    public InputRendererType isFactoryForType() {
        return PERIODO;
    }

    @Override
    public PeriodoRenderer createRenderer(final DatasetFieldType fieldType, 
            final JsonObject jsonOptions) {
        return new PeriodoRenderer();
    }
}
