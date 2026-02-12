package edu.harvard.iq.dataverse.search.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.LazySelectSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;

class SearchFieldFactoryTest {

    private SearchFieldFactory factory = new SearchFieldFactory();
    
    @Test
    void create() {
    	testType(createGeobox(), GeoboxCoordSearchField.class);
    	testType(createTextBoxInsideGeoboxParent(), TextSearchField.class);
    	testType(createType(FieldType.TEXT), TextSearchField.class);
    	testType(createType(FieldType.TEXTBOX), TextSearchField.class);
    	testType(createType(FieldType.URL), TextSearchField.class);
    	testType(createType(FieldType.EMAIL), TextSearchField.class);
    	testType(createType(FieldType.INT), NumberSearchField.class);
    	testType(createType(FieldType.FLOAT), NumberSearchField.class);
    	testType(createVocabularyType(true), CheckboxSearchField.class);
    	testType(createVocabularyType(false), SelectOneSearchField.class);
    	testType(createEnhancedSelectVocabularyType(false), LazySelectSearchField.class);
    	testType(createEnhancedSelectVocabularyType(true), LazySelectSearchField.class);
    }
    
	private void testType(DatasetFieldType fieldTypeSupplier, 
			Class<? extends SearchField> expectedType) {
		// given & when
		SearchField field = factory.create(fieldTypeSupplier);
		// then
		assertThat(field).isInstanceOf(expectedType);
	}

    // -------------------- PRIVATE --------------------

    private static DatasetFieldType createGeobox() {
        DatasetFieldType parentType = createType(FieldType.GEOBOX);
        DatasetFieldType type = createType(FieldType.TEXTBOX);
        type.setParentDatasetFieldType(parentType);
        return type;
    }

    private static DatasetFieldType createTextBoxInsideGeoboxParent() {
        DatasetFieldType parentType = createType(FieldType.GEOBOX);
        DatasetFieldType type = createType(FieldType.TEXT);
        type.setParentDatasetFieldType(parentType);
        return type;
    }

    private static DatasetFieldType createType(FieldType type) {
        @SuppressWarnings("serial")
        DatasetFieldType datasetFieldType = new DatasetFieldType() {
            @Override
            public String getDisplayName() { return ""; }
        };
        datasetFieldType.setControlledVocabularyValues(Collections.emptyList());
        datasetFieldType.setFieldType(type);
        return datasetFieldType;
    }

    private static DatasetFieldType createVocabularyType(boolean allowMultiples) {
        DatasetFieldType fieldType = createType(FieldType.NONE);
        Random random = new Random();
        List<ControlledVocabularyValue> values = Arrays.asList("a", "b", "c").stream()
                .map(v -> new ControlledVocabularyValue(random.nextLong(), v, fieldType))
                .collect(Collectors.toList());
        fieldType.setControlledVocabularyValues(values);
        fieldType.setAllowMultiples(allowMultiples);
        return fieldType;
    }

    private static DatasetFieldType createEnhancedSelectVocabularyType(boolean allowMultiples) {
        DatasetFieldType fieldType = createType(FieldType.NONE);
        fieldType.setInputRendererType(InputRendererType.VOCABULARY_ENHANCED_SELECT);
        Random random = new Random();
        List<ControlledVocabularyValue> values = Arrays.asList("a", "b", "c").stream()
                .map(v -> new ControlledVocabularyValue(random.nextLong(), v, fieldType))
                .collect(Collectors.toList());
        fieldType.setControlledVocabularyValues(values);
        fieldType.setAllowMultiples(allowMultiples);
        return fieldType;
    }
}