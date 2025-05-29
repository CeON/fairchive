package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VocabSelectInputFieldRendererFactoryTest {

    private VocabSelectInputFieldRendererFactory inputFieldRendererFactory = new VocabSelectInputFieldRendererFactory();
    
    private DatasetFieldType fieldType = new DatasetFieldType();
    
    
    // -------------------- TESTS --------------------
    
    @Test
    public void createRenderer__withEmptyOptions() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertEquals(inputFieldRendererFactory.isFactoryForType(), renderer.getType());
        assertTrue(renderer.renderInTwoColumns());
        assertFalse(renderer.isSortByLocalisedStringsOrder());
    }
    
    @Test
    public void createRenderer__withRenderInTwoColumnsOption() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'renderInTwoColumns':false}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertFalse(renderer.renderInTwoColumns());
    }
    
    @Test
    public void createRenderer__withSortByLocalisedStringsOrderOption() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'sortByLocalisedStringsOrder':true}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertTrue(renderer.isSortByLocalisedStringsOrder());
    }

    @Test
    public void createRenderer__withConditionalRenderingParentOption() {
        // given
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement("{'conditionalRenderingParent':true}").getAsJsonObject();
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertTrue(renderer.isConditionalRenderingParent());
    }

    @Test
    public void createRenderer__withConditionalRenderingOption() {
        // given
        String json = "{'conditionalRendering': {'datasetFieldName':'country', 'renderOnValue':'Poland'}}";
        JsonObject rendererOptions = TestJsonCreator.stringAsJsonElement(json).getAsJsonObject();
        List<DatasetField> subfields = new ArrayList<>();
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(MocksFactory.makeControlledVocabDatasetFieldType("country",
                false,
                new MetadataBlock(),
                "Poland"));
        field.setSingleControlledVocabularyValue(new ControlledVocabularyValue(0L, "Poland", null));
        subfields.add(field);
        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptions);
        // then
        assertTrue(renderer.showOnCondition(subfields));
    }
}
