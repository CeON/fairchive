package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRendererManager;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Stateless
public class DatasetFieldValidationDispatcherFactory {

    private FieldValidatorRegistry registry;
    private InputFieldRendererManager inputFieldRendererManager;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldValidationDispatcherFactory() { }

    @Inject
    public DatasetFieldValidationDispatcherFactory(FieldValidatorRegistry registry,
                                                   InputFieldRendererManager inputFieldRendererManager) {
        this.registry = registry;
        this.inputFieldRendererManager = inputFieldRendererManager;
    }

    // -------------------- LOGIC --------------------

    public DatasetFieldValidationDispatcher create(List<DatasetField> parentAndChildrenFields) {
        return new DatasetFieldValidationDispatcher(registry, inputFieldRendererManager)
                .init(parentAndChildrenFields);
    }
}
