package edu.harvard.iq.dataverse.dataset.metadata.valueRenderer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

@RequestScoped
@Named("datasetFieldValueRenderer")
public class DatasetFieldValueRenderer {
    private ValueRendererRepository valueRenderes;

    @Inject
    public DatasetFieldValueRenderer(ValueRendererRepository valueRenderes) {
        this.valueRenderes = valueRenderes;
    }

    public ValueRenderer getRendererFor(final DatasetField field) {
        return valueRenderes.getRendererFor(field);
    }
}
