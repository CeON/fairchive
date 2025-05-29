package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConditionalRenderingHelperTest {

    @Test
    public void shouldRender__withEmptyConditionalRendering() {
        // given
        List<DatasetField> subfields = new ArrayList<>();
        subfields.add(new DatasetField());
        // when
        boolean result = ConditionalRenderingHelper.shouldRender(subfields, null);
        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldRender__withConditionalRendering() {
        // given
        String fieldName = "test";
        String fieldValue = "value";
        List<DatasetField> subfields = new ArrayList<>();
        subfields.add(createDatasetField(fieldName, fieldValue));
        subfields.add(createDatasetField("aaa", ""));
        // when
        boolean result = ConditionalRenderingHelper.shouldRender(
                subfields,
                new ConditionalRendering(fieldName, fieldValue)
        );
        // then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldRender__withConditionalRenderingAndNotRender() {
        // given
        String fieldName = "test";
        String fieldValue = "value";
        List<DatasetField> subfields = new ArrayList<>();
        subfields.add(createDatasetField(fieldName, fieldValue));
        subfields.add(createDatasetField("aaa", ""));
        // when
        boolean result = ConditionalRenderingHelper.shouldRender(
                subfields,
                new ConditionalRendering(fieldName, "different_value")
        );
        // then
        assertThat(result).isFalse();
    }

    private DatasetField createDatasetField(String name, String value) {
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType(name, FieldType.TEXT, false));
        datasetField.setValue(value);

        return datasetField;
    }
}