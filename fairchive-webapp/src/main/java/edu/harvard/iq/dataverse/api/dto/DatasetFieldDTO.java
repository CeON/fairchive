package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import io.vavr.control.Option;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@JsonInclude(NON_EMPTY)
@JsonAutoDetect(fieldVisibility = ANY,  getterVisibility = NONE, isGetterVisibility = NONE)
public class DatasetFieldDTO {
    public static final String PRIMITIVE = "primitive";
    public static final String VOCABULARY = "controlledVocabulary";
    public static final String COMPOUND = "compound";

    private String typeName;
    private Boolean multiple;
    private String typeClass;

    /**
     * This property can hold one of following types:
     * <ul>
     *     <li> {@code String} – for primitive/vocabulary value of non-multiple field
     *     <li> {@code List<String>} – for multiple primitive/vocabulary values
     *     <li> {@code Map<String, DatasetFieldDTO>} – for non-multiple compound field
     *     <li> {@code List<Map<String, DatasetFieldDTO>>} – for multiple compound field
     * </ul>
     */
    private Object value;

    @JsonIgnore
    private boolean emailType;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldDTO() { }

    public DatasetFieldDTO(String typeName, Boolean multiple, String typeClass, Object value) {
        this.typeName = typeName;
        this.multiple = multiple;
        this.typeClass = typeClass;
        this.value = value;
    }

    // -------------------- GETTERS --------------------

    public String getTypeName() {
        return typeName;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public String getTypeClass() {
        return typeClass;
    }

    public Object getValue() {
        return value;
    }

    @JsonIgnore
    public boolean isEmailType() {
        return emailType;
    }

    // -------------------- LOGIC --------------------

    public String getSinglePrimitive() {
        return value == null ? "" : value.toString();
    }

    public String getSingleVocabulary() {
        return getSinglePrimitive();
    }

    /**
     * Removes all email-type subfields from compound field.
     * @return true if after email fields removal there are no other subfields
     *  left and the field should be removed. False otherwise.
     */
    public boolean clearEmailSubfields() {
        if (!COMPOUND.equals(typeClass) || value == null) {
            return false;
        }
        if (Map.class.isAssignableFrom(value.getClass())) {
            return clearEmailSubfields(value).isEmpty();
        }
        List<?> valueList = (List<?>) value;
        for (Object element : valueList) {
            if (!Map.class.isAssignableFrom(element.getClass())) {
                break;
            } else {
                clearEmailSubfields(element);
            }
        }
        valueList.removeIf(f -> Map.class.isAssignableFrom(f.getClass()) && ((Map<?, ?>) f).isEmpty());
        return valueList.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public Set<DatasetFieldDTO> getSingleCompound() {
        return value != null
                ? new LinkedHashSet<>(((Map<String, DatasetFieldDTO>) value).values())
                : emptySet();
    }

    @SuppressWarnings("unchecked")
    public List<String> getMultiplePrimitive() {
        return value != null ? (List<String>) value : emptyList();
    }

    public List<String> getMultipleVocabulary() {
        return getMultiplePrimitive();
    }

    public List<Set<DatasetFieldDTO>> getMultipleCompound() {
        if (value == null) {
            return emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, DatasetFieldDTO>> fieldList = (List<Map<String, DatasetFieldDTO>>) value;
        return fieldList.stream()
                .map(v -> new LinkedHashSet<>(v.values()))
                .collect(toList());
    }

    // -------------------- PRIVATE --------------------

    private Map<String, DatasetFieldDTO> clearEmailSubfields(Object value) {
        @SuppressWarnings("unchecked")
        Map<String, DatasetFieldDTO> subfieldsMap = (Map<String, DatasetFieldDTO>) value;
        subfieldsMap.values().removeIf(DatasetFieldDTO::isEmailType);
        return subfieldsMap;
    }

    // -------------------- SETTERS --------------------

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setEmailType(boolean emailType) {
        this.emailType = emailType;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(typeName, multiple, typeClass, value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        DatasetFieldDTO that = (DatasetFieldDTO) other;
        return Objects.equals(typeName, that.typeName) &&
                Objects.equals(multiple, that.multiple) &&
                Objects.equals(typeClass, that.typeClass) &&
                Objects.equals(value, that.value);
    }

    // -------------------- INNER CLASSES --------------------

    public static class Creator {

        // -------------------- LOGIC --------------------

        public List<DatasetFieldDTO> create(List<DatasetField> datasetFields) {
            datasetFields.sort(comparing(DatasetField::getDatasetFieldTypeDisplayOrder));
            datasetFields.forEach(f -> f.getDatasetFieldsChildren()
                    .sort(comparing(DatasetField::getDatasetFieldTypeDisplayOrder)));
            List<DatasetFieldDTO> fields = new ArrayList<>();
            for (DatasetFieldsByType fieldsByType : DatasetFieldUtil.groupByType(datasetFields)) {
                DatasetFieldType fieldType = fieldsByType.getDatasetFieldType();
                DatasetFieldDTO field = createForType(fieldType);
                List<DatasetField> fieldsOfType = fieldsByType.getDatasetFields();
                List<?> values = emptyList();
                if (fieldType.isControlledVocabulary()) {
                    values = fieldsOfType.stream()
                            .flatMap(f -> f.getControlledVocabularyValues().stream())
                            .sorted(ControlledVocabularyValue.DisplayOrder)
                            .map(ControlledVocabularyValue::getStrValue)
                            .collect(toList());
                } else if (fieldType.isPrimitive()) {
                    values = fieldsOfType.stream()
                            .map(DatasetField::getFieldValue)
                            .filter(Option::isDefined)
                            .map(Option::get)
                            .collect(toList());
                } else if (fieldType.isCompound()) {
                    values = fieldsOfType.stream()
                            .map(this::extractChildren)
                            .collect(toList());
                }
                if (values.isEmpty()) {
                    continue;
                }
                field.setValue(extractValue(fieldType, values));
                fields.add(field);
            }
            return fields;
        }

        // -------------------- PRIVATE --------------------

        private DatasetFieldDTO createForType(DatasetFieldType fieldType) {
            DatasetFieldDTO field = new DatasetFieldDTO();
            field.setTypeName(fieldType.getName());
            field.setMultiple(fieldType.isAllowMultiples());
            field.setTypeClass(fieldType.isControlledVocabulary()
                    ? VOCABULARY
                    : fieldType.isCompound()
                    ? COMPOUND : PRIMITIVE);
            field.setEmailType(fieldType.getFieldType() == FieldType.EMAIL);
            return field;
        }

        private Object extractChildren(DatasetField datasetField) {
            Map<String, DatasetFieldDTO> children = new LinkedHashMap<>();
            for(DatasetField child : datasetField.getDatasetFieldsChildren()) {
                DatasetFieldType fieldType = child.getDatasetFieldType();
                DatasetFieldDTO field = createForType(fieldType);
                if (fieldType.isControlledVocabulary()) {
                    List<String> values = child.getControlledVocabularyValues().stream()
                            .sorted(comparing(ControlledVocabularyValue::getDisplayOrder))
                            .map(ControlledVocabularyValue::getStrValue)
                            .collect(toList());
                    if (values.isEmpty()) {
                        continue;
                    }
                    field.setValue(extractValue(fieldType, values));
                } else if (fieldType.isPrimitive()) {
                    field.setValue(child.getFieldValue().getOrElse((String) null));
                }
                children.put(field.getTypeName(), field);
            }
            return children;
        }

        private <T> Object extractValue(DatasetFieldType fieldType, List<T> values) {
            return fieldType.isAllowMultiples() || values.size() > 1
                    ? values : values.get(0);
        }
    }
}
