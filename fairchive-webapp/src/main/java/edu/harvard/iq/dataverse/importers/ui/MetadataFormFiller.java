package edu.harvard.iq.dataverse.importers.ui;

import edu.harvard.iq.dataverse.importers.ui.form.ItemType;
import edu.harvard.iq.dataverse.importers.ui.form.ProcessingType;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetadataFormFiller {

    private MetadataFormLookup lookup;

    // -------------------- CONSTRUCTORS --------------------

    public MetadataFormFiller(MetadataFormLookup lookup) {
        this.lookup = lookup;
    }

    // -------------------- LOGIC --------------------

    public void fillForm(List<ResultItem> importerFormData) {
        for (ResultItem item : importerFormData) {
            if (ProcessingType.UNPROCESSABLE.equals(item.getProcessingType()) || !item.getShouldProcess()) {
                continue;
            }
            switch (item.getProcessingType()) {
                case OVERWRITE:
                case MULTIPLE_OVERWRITE:
                    processItem(item, DatasetFieldsOfType::clearAndAddEmpty, this::setItemValue, this::overwriteVocabulary);
                    break;
                case MULTIPLE_CREATE_NEW:
                    processItem(item, this::createOrTakeEmptyField, this::setItemValue, this::overwriteVocabulary);
                    break;
                case FILL_IF_EMPTY:
                    processItem(item, DatasetFieldsOfType::getLast, this::setIfBlank, this::setVocabularyIfEmpty);
                    break;
                default:
                    break;
            }
        }
    }

    // -------------------- PRIVATE --------------------

    private void processItem(ResultItem item,
                             Function<DatasetFieldsOfType, DatasetField> fieldProvider,
                             BiConsumer<DatasetField, ResultItem> fieldSetter,
                             BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        DatasetFieldsOfType fieldsOfType = lookup.getLookup().get(item.getName());
        DatasetField field = fieldProvider.apply(fieldsOfType);
        switch (item.getItemType()) {
            case COMPOUND:
                fillCompoundField(field, item, fieldSetter, vocabularySetter);
                break;
            case SIMPLE:
                fieldSetter.accept(field, item);
                break;
            case VOCABULARY:
                fillVocabulary(field, item, vocabularySetter);
                break;
        }
    }

    private void fillCompoundField(DatasetField field, ResultItem item,
                                   BiConsumer<DatasetField, ResultItem> fieldSetter,
                                   BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        for (ResultItem childItem : item.getChildren()) {
            if (ProcessingType.UNPROCESSABLE.equals(childItem.getProcessingType()) || !childItem.getShouldProcess()) {
                continue;
            }
            DatasetField childField = matchChild(childItem, field);
            if (ItemType.VOCABULARY.equals(childItem.getItemType())) {
                fillVocabulary(childField, childItem, vocabularySetter);
            } else {
                fieldSetter.accept(childField, childItem);
            }
        }
    }

    private void fillVocabulary(DatasetField field, ResultItem item,
                                BiConsumer<DatasetField, List<ControlledVocabularyValue>> vocabularySetter) {
        List<ResultItem> items = item.getChildren().size() > 0 ? item.getChildren() : Collections.singletonList(item);
        List<ControlledVocabularyValue> vocabularyValues = items.stream()
                .filter(i -> !ProcessingType.UNPROCESSABLE.equals(i.getProcessingType()))
                .map(ResultItem::getVocabularyValue)
                .collect(Collectors.toList());
        vocabularySetter.accept(field, vocabularyValues);
    }

    private DatasetField matchChild(ResultItem childItem, DatasetField parent) {
        String name = childItem.getName();
        return parent.getDatasetFieldsChildren().stream()
                .filter(c -> name.equals(c.getTypeName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Child field [" + name + "] not found!"));
    }

    private DatasetField createOrTakeEmptyField(DatasetFieldsOfType fieldsOfType) {
        if (fieldsOfType.isEmpty()) {
            return fieldsOfType.addEmpty(0);
        } else {
            int index = fieldsOfType.size() - 1;
            DatasetField field = fieldsOfType.get(index);
            return field.isEmpty()
                    ? field
                    : fieldsOfType.addEmpty(index + 1);
        }
    }

    private void setItemValue(DatasetField field, ResultItem item) {
        field.setValue(item.getValue());
    }

    private void setIfBlank(DatasetField field, ResultItem item) {
        String value = field.getValue();
        field.setValue(StringUtils.isBlank(value) ? item.getValue() : value);
    }

    private void overwriteVocabulary(DatasetField field, List<ControlledVocabularyValue> vocabularyValues) {
        field.setControlledVocabularyValues(vocabularyValues);
    }

    private void setVocabularyIfEmpty(DatasetField field, List<ControlledVocabularyValue> vocabularyValues) {
        if (field.getControlledVocabularyValues().isEmpty()) {
            field.setControlledVocabularyValues(vocabularyValues);
        }
    }
}
