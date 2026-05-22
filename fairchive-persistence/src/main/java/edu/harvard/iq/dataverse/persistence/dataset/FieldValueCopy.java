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
    private String copySourceName = StringUtils.EMPTY;
    private DatasetFieldType fieldType;

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
        copier.copySourceName = copySourceName;
        copier.fieldType = fieldType;
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

            target.copyChildValuesFrom(sourceCompound);
            
            if (!type.equals("copy")) {
                DatasetField targetMapping = getChildrenField(target, typeField);
                targetMapping.setValue(type);
                setControlledVocabulary(targetMapping);
            }
        }
        return fields;
    }
}
