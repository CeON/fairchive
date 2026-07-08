package edu.harvard.iq.dataverse.importers.ui;

import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.BLOCK_NAME;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.CHILD;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.COMPOUND;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.PARENT;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.SIMPLE;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VALUE;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOCABULARY;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_1;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_2;
import static edu.harvard.iq.dataverse.importers.ui.MetadataNamesConstants.VOC_3;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.createItems;
import static edu.harvard.iq.dataverse.importers.ui.TestMetadataUtils.extract;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.importer.metadata.ResultField;
import edu.harvard.iq.dataverse.importers.ui.form.ProcessingType;
import edu.harvard.iq.dataverse.importers.ui.form.ResultItem;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;

public class MetadataFormFillerTest {

    private MetadataFormLookup lookup;

    private MetadataFormFiller filler;

    @BeforeEach
    public void setUp() {
        this.lookup = MetadataFormLookup.create(BLOCK_NAME, TestMetadataCreator::createTestMetadata);
        this.filler = new MetadataFormFiller(lookup);
    }

    @Test
    @DisplayName("Field should be overwritten on demand")
    public void overwriteSimpleField() {
        // given
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();
        DatasetFieldsOfType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addEmpty();
        field.setId(1L);

        List<ResultItem> items = createItems(singletonList(ResultField.of(PARENT + SIMPLE, "Value")));
        items.get(0).setProcessingType(ProcessingType.OVERWRITE);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.get(0);
        assertEquals(1, parentSimpleOnForm.size());
        assertThat(fieldAfterFill, not(equalTo(field)));
    }

    @Test
    @DisplayName("Field should be filled if it's empty and 'fill if empty' was set")
    public void fillIfEmptySimpleField() {
        // given
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();
        DatasetFieldsOfType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addEmpty();
        field.setId(1L); // id does not have impact on field emptiness

        List<ResultItem> items = createItems(singletonList(ResultField.of(PARENT + SIMPLE, VALUE)));
        items.get(0).setProcessingType(ProcessingType.FILL_IF_EMPTY);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.get(0);
        assertEquals(1, parentSimpleOnForm.size());
        assertThat(fieldAfterFill.getValue(), is(VALUE));
        assertThat(fieldAfterFill, equalTo(field));
    }

    @Test
    @DisplayName("Field should not be filled if not empty when 'fill if empty' is set")
    public void doNotFillIfNotEmpty() {
        // given
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();
        DatasetFieldsOfType parentSimpleOnForm = formLookup.get(PARENT + SIMPLE);

        DatasetField field = parentSimpleOnForm.addEmpty();
        field.setId(1L);
        field.setValue("some other value");

        List<ResultItem> items = createItems(singletonList(ResultField.of(PARENT + SIMPLE, VALUE)));
        items.get(0).setProcessingType(ProcessingType.FILL_IF_EMPTY);

        // when
        filler.fillForm(items);

        // then
        DatasetField fieldAfterFill = parentSimpleOnForm.get(0);
        assertEquals(1, parentSimpleOnForm.size());
        assertThat(fieldAfterFill.getValue(), not(equalTo(VALUE)));
        assertThat(fieldAfterFill, equalTo(field));
    }

    @Test
    @DisplayName("Should create new fields for multiple compound fields on demand")
    public void shouldCreateNewFields() {
        // given
        List<ResultItem> items = createItems(
                Stream.of(
                        // NB without VALUE these fields would be treated as empty by filler
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)))
                        .collect(Collectors.toList()));
        items.forEach(i -> i.setProcessingType(ProcessingType.MULTIPLE_CREATE_NEW));

        // when
        filler.fillForm(items);

        // then
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();
        DatasetFieldsOfType parentCompounds = formLookup.get(PARENT + COMPOUND);
        assertEquals(3, parentCompounds.size());
    }

    @Test
    @DisplayName("Should destroy any existing fields when overwriting compound fields")
    public void shouldOverwriteMultipleFields() {
        // given
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();

        DatasetFieldsOfType parentCompound = formLookup.get(PARENT + COMPOUND);
        range(0, 10).forEach(i -> parentCompound.addEmpty().setValue(VALUE));

        List<ResultItem> items = createItems(
                Stream.of(
                        // NB without VALUE these fields would be treated as empty by filler
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)),
                        ResultField.of(PARENT + COMPOUND, ResultField.of(CHILD + SIMPLE, VALUE)))
                        .collect(Collectors.toList()));

        items.forEach(i -> i.setProcessingType(ProcessingType.MULTIPLE_CREATE_NEW));
        items.get(0).setProcessingType(ProcessingType.MULTIPLE_OVERWRITE); // only first in the group should be set as to overwrite

        // when
        filler.fillForm(items);

        // then
        assertEquals(3, parentCompound.size());
        
    }

    @Test
    @DisplayName("Vocabulary fields should have ControlledVocabularyValues written into them")
    public void shouldWriteVocabularyValues() {
        // given
        List<ResultItem> items = createItems(
                Stream.of(
                        ResultField.of(PARENT + VOCABULARY,
                                ResultField.ofValue(VOC_1),
                                ResultField.ofValue(VOC_2),
                                ResultField.ofValue(VOC_3)))
                        .collect(toList()));
        items.get(0).setProcessingType(ProcessingType.OVERWRITE);

        // when
        filler.fillForm(items);

        // then
        Map<String, DatasetFieldsOfType> formLookup = lookup.getLookup();
        DatasetFieldsOfType parentVocabulary = formLookup.get(PARENT + VOCABULARY);
        assertEquals(1, parentVocabulary.size());
        List<ControlledVocabularyValue> vocabularyValues = parentVocabulary.get(0).getControlledVocabularyValues();
        assertThat(vocabularyValues, hasSize(3));
        assertThat(extract(vocabularyValues, ControlledVocabularyValue::getStrValue),
                Matchers.contains(VOC_1, VOC_2, VOC_3));
    }
}