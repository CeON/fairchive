package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.TestJsonCreator;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    public void createRenderer__clearValidationMessage() {
        // given
        JsonObject rendererOptionsConditionalRendering = TestJsonCreator
                .stringAsJsonElement("{'conditionalRendering': {'datasetFieldName':'country', 'renderOnValue':'Poland'}}")
                .getAsJsonObject();
        JsonObject rendererOptionsEmpty = TestJsonCreator
                .stringAsJsonElement("{}")
                .getAsJsonObject();
        DatasetField mainField = new DatasetField();
        DatasetField parent = new DatasetField();
        List<DatasetField> subfields = new ArrayList<>();
        DatasetField field = new DatasetField();
        DatasetFieldType typeMain = MocksFactory.makeControlledVocabDatasetFieldType("country",
                false,
                new MetadataBlock(),
                "Poland");
        DatasetFieldType typeSubField = MocksFactory.makeControlledVocabDatasetFieldType("test", false, new MetadataBlock(), "test");
        field.setDatasetFieldType(typeSubField);
        field.setSingleControlledVocabularyValue(new ControlledVocabularyValue(0L, "Poland", null));
        field.setValidationMessage("Error");
        subfields.add(field);
        parent.setDatasetFieldsChildren(subfields);
        mainField.setDatasetFieldParent(parent);
        mainField.setDatasetFieldType(typeMain);
        Map<DatasetFieldType, InputFieldRenderer> map = new HashMap<>();
        map.put(typeMain, inputFieldRendererFactory.createRenderer(typeMain, rendererOptionsEmpty));
        map.put(typeSubField, inputFieldRendererFactory.createRenderer(typeMain, rendererOptionsConditionalRendering));

        // when
        VocabSelectInputFieldRenderer renderer = inputFieldRendererFactory.createRenderer(fieldType, rendererOptionsEmpty);
        renderer.processValueChange(mainField, map);
        // then
        assertTrue(subfields.stream().allMatch(df -> StringUtils.isBlank(df.getValidationMessage())));
    }
}
