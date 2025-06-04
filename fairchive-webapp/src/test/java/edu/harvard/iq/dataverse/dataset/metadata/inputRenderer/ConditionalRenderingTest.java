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
class ConditionalRenderingTest {


    @Test
    public void shouldRender__withConditionalRendering() {
        // given
        String fieldName = "test";
        String fieldValue = "value";
        List<DatasetField> subfields = new ArrayList<>();
        subfields.add(createDatasetField(fieldName, fieldValue));
        subfields.add(createDatasetField("aaa", ""));
        // when
        ConditionalRendering cr = new ConditionalRendering(fieldName, fieldValue);
        boolean result = cr.shouldRender(subfields);
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
        ConditionalRendering cr = new ConditionalRendering(fieldName, "different_value");
        boolean result = cr.shouldRender(subfields);
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