package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HiddenVocabInputFieldRendererFactoryTest {

    @InjectMocks
    private HiddenVocabInputFieldRendererFactory inputFieldRendererFactory;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer() {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setControlledVocabularyValues(Lists.newArrayList());
        
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        HiddenVocabInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
    }

    @Test
    public void createRenderer__withConditionalRenderingOption() {
        // given
        String json = "{'conditionalRendering': {'datasetFieldName':'country', 'renderOnValue':'Poland'}}";
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(json).getAsJsonObject();
        List<DatasetField> subfields = new ArrayList<>();
        DatasetFieldType datasetFieldType = MocksFactory.makeControlledVocabDatasetFieldType("country",
                false,
                new MetadataBlock(),
                "Poland");
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(datasetFieldType);
        field.setSingleControlledVocabularyValue(new ControlledVocabularyValue(0L, "Poland", null));
        subfields.add(field);
        // when
        HiddenVocabInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(
                MocksFactory.makeControlledVocabDatasetFieldType(
                        "test",
                        false,
                        new MetadataBlock(),
                        "value"),
                rendererOptions);
        // then
        assertFalse(renderer.showOnCondition(subfields));
    }
}
