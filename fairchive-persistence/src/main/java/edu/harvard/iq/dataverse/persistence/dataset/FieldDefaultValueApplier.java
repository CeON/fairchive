package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class FieldDefaultValueApplier {

    /**
     * Fills up dataset field and its children with default values.
     */
    public void applyDefaultValue(DatasetField datasetField) {

        DatasetFieldType datasetFieldType = datasetField.getDatasetFieldType();

        if (StringUtils.isNotBlank(datasetFieldType.getDefaultValue())
                && datasetField.isEmpty()) {
            
            if (datasetFieldType.isControlledVocabulary()) {
                datasetField.setControlledVocabularyValues(
                        getControlledVocabularyValues(datasetFieldType, datasetFieldType.getDefaultValue()));
            } else {
                datasetField.setValue(datasetFieldType.getDefaultValue());
            }
            
        }

        datasetField.getChildren().forEach(this::applyDefaultValue);
    }

    private List<ControlledVocabularyValue> getControlledVocabularyValues(DatasetFieldType datasetFieldType, String defaultValue) {
        List<String> defaultValues = Arrays.asList(StringUtils.split(defaultValue, ';'));
        return datasetFieldType.getControlledVocabularyValues().stream()
                .filter(vocabValue -> defaultValues.contains(vocabValue.getStrValue()))
                .collect(toList());
    }
}
