package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class FieldValueDivider {
    private static final FieldValueDivider EMPTY = new FieldValueDivider();

    private String sourceFieldName = StringUtils.EMPTY;
    private List<String> fieldsToCopyNames = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    private FieldValueDivider() { }

    // -------------------- LOGIC --------------------

    @SuppressWarnings("unchecked")
	public static FieldValueDivider create(final DatasetFieldType fieldType) {
		if (fieldType.hasMetadata("divider")) {
			final Map<String, Object> dividerData = (Map<String, Object>) fieldType.getMetadata("divider");
			final FieldValueDivider divider = new FieldValueDivider();
			divider.sourceFieldName = (String) dividerData.get("source");
			divider.fieldsToCopyNames.addAll((List<String>) dividerData.get("copy"));
			return divider;
		} else {
			return EMPTY;
		}
	}

    public List<DatasetField> divide(DatasetField sourceCompound, String delimiter) {
        final List<DatasetField> result = new ArrayList<>();
        sourceCompound.getDatasetFieldsChildren().stream()
                .filter(f -> this.sourceFieldName.equals(f.getTypeName()))
                .findFirst()
                .map(sourceField -> splitValue(sourceField, delimiter))
                .ifPresent(values -> {
                    final Map<String, String> valuesToCopy = prepareValuesToCopy(sourceCompound);
                    for (int i = 0; i < values.size(); i++) {
                        result.add(i == 0
                                ? sourceCompound
                                : DatasetField.createNewEmptyDatasetField(sourceCompound.getDatasetFieldType(), null));
                        
                        for (DatasetField subfield : result.get(i).getDatasetFieldsChildren()) {
                            String name = subfield.getTypeName();
                            if (this.sourceFieldName.equals(name)) {
                                subfield.setFieldValue(values.get(i));
                            } else if (valuesToCopy.containsKey(name)) {
                                subfield.setFieldValue(valuesToCopy.get(name));
                            }
                        }
                    }
                });
        return result;
    }

    // -------------------- PRIVATE --------------------

    private List<String> splitValue(final DatasetField sourceField, final String delimiter) {
        final String value = sourceField.getFieldValue().getOrElse(StringUtils.EMPTY);
        return Arrays.stream(value.split(delimiter))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(toList());
    }

    private Map<String, String> prepareValuesToCopy(final DatasetField sourceCompound) {
        return sourceCompound.getDatasetFieldsChildren().stream()
                .filter(f -> this.fieldsToCopyNames.contains(f.getTypeName())
                        && isNotBlank(f.getFieldValue().getOrElse(StringUtils.EMPTY)))
                .collect(toMap(DatasetField::getTypeName, f -> f.getFieldValue().get(), (prev, next) -> next));
    }
}
