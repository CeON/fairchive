package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

@Stateless
public class InputFieldRendererManager {

    private Instance<InputFieldRendererFactory<?>> inputRendererFactoriesInstance;

    private Map<InputRendererType, InputFieldRendererFactory<?>> inputRendererFactories = new EnumMap<>(
            InputRendererType.class);

    // -------------------- CONSTRUCTORS --------------------

    public InputFieldRendererManager() { }

    @Inject
    public InputFieldRendererManager(Instance<InputFieldRendererFactory<?>> fatories) {
        this.inputRendererFactoriesInstance = fatories;
    }

    @PostConstruct
    public void postConstruct() {
        this.inputRendererFactoriesInstance.iterator()
                .forEachRemaining(factory -> this.inputRendererFactories
                        .put(factory.isFactoryForType(), factory));
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns {@link InputFieldRenderer}s grouped by
     * {@link DatasetFieldType}.
     *
     * @see #obtainRenderer(DatasetFieldType)
     */
    public Map<DatasetFieldType, InputFieldRenderer> obtainRenderersByType(List<DatasetField> datasetFields) {
        Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();
        Set<DatasetFieldType> fieldTypes = new HashSet<>();

        for (DatasetField field: datasetFields) {
            fieldTypes.add(field.getDatasetFieldType());
            for (DatasetField child: field.getDatasetFieldsChildren()) {
                fieldTypes.add(child.getDatasetFieldType());
            }
        }
        for (DatasetFieldType fieldType: fieldTypes) {
            inputRenderersByFieldType.put(fieldType, obtainRenderer(fieldType));
        }

        return inputRenderersByFieldType;
    }

    /**
     * Returns {@link InputFieldRenderer} associated with
     * the given {@link DatasetFieldType}
     */
    public InputFieldRenderer obtainRenderer(final DatasetFieldType fieldType) {
        return  this.inputRendererFactories.get(fieldType.getInputRendererType())
            .createRenderer(fieldType, fieldType.getInputRendererOptionsAsJson());
    }
}
