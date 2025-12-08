package edu.harvard.iq.dataverse.persistence.dataset;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FieldValueCopy {
    private static final FieldValueCopy EMPTY = new FieldValueCopy();
    private String type = StringUtils.EMPTY;
    private String typeField = StringUtils.EMPTY;
    private List<Map<String, String>> fieldsToCopyNames = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    private FieldValueCopy() { }

    // -------------------- LOGIC --------------------

    @SuppressWarnings("unchecked")
    public static FieldValueCopy create(DatasetFieldType fieldType, String copySourceName) {
        boolean hasCopyFrom = fieldType.hasMetadata("copyFrom");
        if (!hasCopyFrom) {
            return EMPTY;
        }
        List<Map<String, Object>> copyDataList = (List<Map<String, Object>>) fieldType.getMetadata("copyFrom");

        Map<String, Object> copyData = null;
        for (Map<String, Object> fieldCopyData : copyDataList) {
            if (copySourceName.equals(fieldCopyData.get("source"))) {
                copyData = fieldCopyData;
            }
        }
        FieldValueCopy copier = new FieldValueCopy();
        copier.type = (String) copyData.get("setType");
        if (copyData.containsKey("setTypeField")) {
            copier.typeField = (String) copyData.get("setTypeField");
        }
        copier.fieldsToCopyNames.addAll((List<Map<String, String>>) copyData.get("copy"));
        return copier;
    }

    public List<DatasetField> copy(List<DatasetField> sourceCompounds, DatasetField targetCompound) {
        List<DatasetField> fields = new ArrayList<>();
        if (sourceCompounds.isEmpty()) {
            return fields;
        }

        for (DatasetField sourceCompound : sourceCompounds) {
            DatasetField target = DatasetField.createNewEmptyDatasetField(targetCompound.getDatasetFieldType(), null);
            fields.add(target);

            for (Map<String, String> copyMapping : fieldsToCopyNames) {
                DatasetField sourceMapping = getChildrenField(sourceCompound, copyMapping.get("from"));
                DatasetField targetMapping = getChildrenField(target, copyMapping.get("to"));

                targetMapping.setValue(sourceMapping.getValue());
            }
            if (!type.equals("copy")) {
                getChildrenField(target, typeField).setValue(type);
            }
        }
        return fields;
    }

    // -------------------- PRIVATE --------------------

    private DatasetField getChildrenField(DatasetField compound, String name) {
        for (DatasetField child : compound.getDatasetFieldsChildren()) {
            if (name.equals(child.getTypeName())) {
                return child;
            }
        }
        return null;
    }
}
