package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The class is used to match (by name) metadata that comes as a result of metadata import
 * with that on dataset metadata form. In the constructor we pass the name of metadata
 * block, and supplier of metadata from form. Then we could search for top-level metadata
 * names and for those that are children of that top-level items.
 */
public class MetadataFormLookup {
    private String metadataBlockName;
    private Supplier<Map<MetadataBlock, List<DatasetFieldsOfType>>> metadataSupplier;

    private Map<String, DatasetFieldsOfType> lookup;
    private Map<String, DatasetFieldType> childrenLookup;

    // -------------------- CONSTRUCTORS --------------------

    public MetadataFormLookup(String metadataBlockName,
                       Supplier<Map<MetadataBlock, List<DatasetFieldsOfType>>> metadataSupplier) {
        this.metadataBlockName = metadataBlockName;
        this.metadataSupplier = metadataSupplier;
    }

    // -------------------- GETTERS --------------------

    public Map<String, DatasetFieldsOfType> getLookup() {
        return lookup;
    }

    public Map<String, DatasetFieldType> getChildrenLookup() {
        return childrenLookup;
    }

    // -------------------- LOGIC --------------------

    public static MetadataFormLookup create(String metadataBlockName,
                                            Supplier<Map<MetadataBlock, List<DatasetFieldsOfType>>> metadataSupplier) {
        MetadataFormLookup instance = new MetadataFormLookup(metadataBlockName, metadataSupplier);
        instance.lookup = instance.create();
        instance.childrenLookup = instance.createChildrenLookup(instance.lookup);
        return instance;
    }

    // -------------------- PRIVATE --------------------

    private Map<String, DatasetFieldsOfType> create() {
        Map<MetadataBlock, List<DatasetFieldsOfType>> metadata = metadataSupplier.get();
        List<DatasetFieldsOfType> fieldsForBlock = metadata.entrySet().stream()
                .filter(e -> metadataBlockName.equals(e.getKey().getName()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(Collections.emptyList());

        return fieldsForBlock.stream()
                .collect(Collectors.toMap(f -> f.getType().getName(), Function.identity()));
    }

    private Map<String, DatasetFieldType> createChildrenLookup(Map<String, DatasetFieldsOfType> parentLookup) {
        return parentLookup.values().stream()
                .flatMap(f -> f.getType().getChildDatasetFieldTypes().stream())
                .collect(Collectors.toMap(DatasetFieldType::getName, Function.identity()));
    }
}
