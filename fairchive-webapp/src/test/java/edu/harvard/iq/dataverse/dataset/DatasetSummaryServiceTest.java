package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeDatasetField;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class DatasetSummaryServiceTest {

    private static final String VISIBLE_FIELD = "dsDescription";

    private static final String HIDDEN_FIELD = "publication";

    private final DatasetSummaryService datasetSummaryService = new DatasetSummaryService();

    private final DatasetVersion datasetVersion = new DatasetVersion();

    private long nextFieldTypeId = 1L;

    // -------------------- TESTS --------------------

    @Test
    void getDatasetSummaryFields__anonymizedViewSkipsFieldHiddenFromAnonymizedUrl() {
        // given
        addField(VISIBLE_FIELD, true, "Some description");
        addField(HIDDEN_FIELD, false, "Some related publication");

        // when
        List<DatasetFieldsOfType> summaryFields = datasetSummaryService.getDatasetSummaryFields(
                datasetVersion, asList(VISIBLE_FIELD, HIDDEN_FIELD), true);

        // then
        assertThat(summaryFields)
                .extracting(fieldsOfType -> fieldsOfType.getType().getName())
                .containsExactly(VISIBLE_FIELD);
    }

    @Test
    void getDatasetSummaryFields__ordinaryViewKeepsFieldHiddenFromAnonymizedUrl() {
        // given
        addField(VISIBLE_FIELD, true, "Some description");
        addField(HIDDEN_FIELD, false, "Some related publication");

        // when
        List<DatasetFieldsOfType> summaryFields = datasetSummaryService.getDatasetSummaryFields(
                datasetVersion, asList(VISIBLE_FIELD, HIDDEN_FIELD), false);

        // then
        assertThat(summaryFields)
                .extracting(fieldsOfType -> fieldsOfType.getType().getName())
                .containsExactly(VISIBLE_FIELD, HIDDEN_FIELD);
    }

    @Test
    void getDatasetSummaryFields__fieldsFollowCustomFieldListOrder() {
        // given
        addField(VISIBLE_FIELD, true, "Some description");
        addField("keyword", true, "Some keyword");

        // when
        List<DatasetFieldsOfType> summaryFields = datasetSummaryService.getDatasetSummaryFields(
                datasetVersion, asList("keyword", VISIBLE_FIELD), false);

        // then
        assertThat(summaryFields)
                .extracting(fieldsOfType -> fieldsOfType.getType().getName())
                .containsExactly("keyword", VISIBLE_FIELD);
    }

    @Test
    void getDatasetSummaryFields__customFieldAbsentFromVersionIsSkipped() {
        // given
        addField(VISIBLE_FIELD, true, "Some description");

        // when
        List<DatasetFieldsOfType> summaryFields = datasetSummaryService.getDatasetSummaryFields(
                datasetVersion, singletonList("notesText"), false);

        // then
        assertThat(summaryFields).isEmpty();
    }

    // -------------------- PRIVATE --------------------

    /**
     * Field types are grouped by their identity, and {@link DatasetFieldType}
     * compares by id alone, so every fixture type needs a distinct one.
     */
    private void addField(String name, boolean visibleThroughAnonymizedUrl, String value) {
        DatasetFieldType fieldType = new DatasetFieldType(name, FieldType.TEXT, false);
        fieldType.setId(nextFieldTypeId++);
        fieldType.setVisibleThroughAnonymizedUrl(visibleThroughAnonymizedUrl);
        DatasetField field = makeDatasetField(fieldType, value);

        datasetVersion.addField(field);
    }

}
