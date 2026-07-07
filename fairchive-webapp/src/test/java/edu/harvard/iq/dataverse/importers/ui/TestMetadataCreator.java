package edu.harvard.iq.dataverse.importers.ui;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.BLOCK_NAME;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.CHILD;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.COMPOUND;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.OF;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.PARENT;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.SECOND;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.SIMPLE;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOCABULARY;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_1;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_2;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_3;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_A;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_B;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_C;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

public class TestMetadataCreator {
    public static final DatasetFieldType PARENT_SIMPLE = createDFT(PARENT + SIMPLE, false, false, 1);
    public static final DatasetFieldType PARENT_VOCABULARY =
            withVocabulary(
                    createDFT(PARENT + VOCABULARY, true, true, 2),
                    VOC_1, VOC_2, VOC_3);
    public static final DatasetFieldType PARENT_COMPOUND = createDFT(PARENT + COMPOUND, true, false, 3);
    public static final DatasetFieldType CHILD_SIMPLE =
            makeChildDFT(createDFT(CHILD + SIMPLE, false, false, 1), PARENT_COMPOUND);
    public static final DatasetFieldType CHILD_VOCABULARY =
            makeChildDFT(
                    withVocabulary(
                            createDFT(CHILD + VOCABULARY, true, true, 2),
                            VOC_A, VOC_B, VOC_C),
                    PARENT_COMPOUND);
    public static final DatasetFieldType PARENT_SECOND_COMPOUND = createDFT(PARENT + SECOND + COMPOUND, true, false, 4);
    public static final DatasetFieldType CHILD_SIMPLE_OF_SECOND =
            makeChildDFT(createDFT(CHILD + SIMPLE + OF + SECOND, false, false, 1), PARENT_SECOND_COMPOUND);

    // -------------------- LOGIC --------------------

    public static Map<MetadataBlock, List<DatasetFieldsOfType>> createTestMetadata() {
        MetadataBlock block = new MetadataBlock();
        block.setName(BLOCK_NAME);
        return singletonMap(block, createDFsByType(TestMetadataCreator::getTestDFTs));
    }

    public static List<DatasetFieldType> getTestDFTs() {
        return Stream.of(PARENT_SIMPLE, PARENT_VOCABULARY, PARENT_COMPOUND, PARENT_SECOND_COMPOUND)
                .collect(toList());
    }

    public static List<DatasetFieldsOfType> createDFsByType(Supplier<List<DatasetFieldType>> fieldTypes) {
        return fieldTypes.get().stream()
                .map(DatasetFieldsOfType::new)
                .collect(toList());
    }

    // -------------------- PRIVATE --------------------

    private static DatasetFieldType createDFT(String name, boolean multiples, boolean vocabulary, int displayOrder) {
        DatasetFieldType datasetFieldType = new DatasetFieldType(name, FieldType.TEXT, multiples);
        datasetFieldType.setAllowControlledVocabulary(vocabulary);
        datasetFieldType.setDisplayOrder(displayOrder);
        datasetFieldType.setTitle(name);
        return datasetFieldType;
    }

    private static DatasetFieldType makeChildDFT(DatasetFieldType datasetFieldType, DatasetFieldType parent) {
        parent.getChildDatasetFieldTypes().add(datasetFieldType);
        datasetFieldType.setParentDatasetFieldType(parent);
        return datasetFieldType;
    }

    private static DatasetFieldType withVocabulary(DatasetFieldType hostType, String... values) {
        List<ControlledVocabularyValue> vocabularyValues = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ControlledVocabularyValue value = new ControlledVocabularyValue((long) i, values[i], hostType);
            value.setDisplayOrder(i + 1);
            vocabularyValues.add(value);
        }
        hostType.setControlledVocabularyValues(vocabularyValues);
        return hostType;
    }
}
