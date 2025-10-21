package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import javax.faces.model.SelectItem;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;


/**
 * @author skraffmi
 */
public class DatasetFieldTypeTest {


    @Test
    public void testIsSanitizeHtml() {
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertThat(result).isFalse();

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertThat(result).isTrue();

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertThat(result).isFalse();

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isSanitizeHtml();
        assertThat(result).isTrue();
    }

    @Test
    public void testIsEscapeOutputText() {
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isEscapeOutputText();
        assertThat(result).isTrue();

        //if Disaplay Format includes a link then don't escape
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isEscapeOutputText();
        assertThat(result).isTrue();

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsTrue() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(true);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        assertThat(parentAllowsMutlipleValues).isTrue();
    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsFalse() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(false);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        assertThat(parentAllowsMutlipleValues).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, true, true",
            "true, true, false, false",
            "true, false, true, true",
            "true, false, false, false",
            "false, true, true, false",
            "false, true, false, false",
            "false, false, true, false",
            "false, false, false, false",
    })
    void isSeparableOnGui(boolean allowMultiples, boolean compound, boolean containsTextbox, boolean shouldBeSeparable) {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(allowMultiples);
        if (compound) {
            DatasetFieldType child = new DatasetFieldType();
            child.setFieldType(containsTextbox ? FieldType.TEXTBOX : FieldType.TEXT);
            fieldType.getChildDatasetFieldTypes().add(child);
        } else {
            fieldType.setFieldType(containsTextbox ? FieldType.TEXTBOX : FieldType.TEXT);
        }

        // when
        boolean separableOnGui = fieldType.isSeparableOnGui();

        // then
        assertThat(separableOnGui).isEqualTo(shouldBeSeparable);
    }
    
    @Test
    public void test_isHasRequiredChildren() {
        DatasetFieldType fieldType = new DatasetFieldType("type1", FieldType.TEXT, false);
        
        assertThat(fieldType.getChildDatasetFieldTypes().size()).isEqualTo(0);
        assertThat(fieldType.isHasRequiredChildren()).isFalse();
        
        fieldType.getChildDatasetFieldTypes().add(new DatasetFieldType());
        
        assertThat(fieldType.getChildDatasetFieldTypes().size()).isEqualTo(1);
        assertThat(fieldType.isHasRequiredChildren()).isFalse();
        
        fieldType.getChildDatasetFieldTypes().get(0).setRequired(true);
        
        assertThat(fieldType.getChildDatasetFieldTypes().size()).isEqualTo(1);
        assertThat(fieldType.isHasRequiredChildren()).isTrue();
        
        fieldType.getChildDatasetFieldTypes().add(new DatasetFieldType());
        
        assertThat(fieldType.getChildDatasetFieldTypes().size()).isEqualTo(2);
        assertThat(fieldType.isHasRequiredChildren()).isTrue();
    }

    @Test
    public void test_getControlledVocabSelectItems__polishLetters() {
        try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
            bundleUtilMock.when(BundleUtil::getCurrentLocale).thenReturn(new Locale("pl", "PL"));

            DatasetFieldType fieldType = new DatasetFieldType("type1", FieldType.TEXT, false);
            fieldType.setControlledVocabularyValues(asList(
                    new ControlledVocabularyValue(3L, "zamek", fieldType),
                    new ControlledVocabularyValue(1L, "test", fieldType),
                    new ControlledVocabularyValue(2L, "łuk", fieldType),
                    new ControlledVocabularyValue(4L, "ćma", fieldType),
                    new ControlledVocabularyValue(4L, "balia", fieldType)
            ));

            Collection<SelectItem> items = fieldType.getControlledVocabSelectItems(true);

            List<String> labels = items.stream().map(SelectItem::getLabel).collect(Collectors.toList());

            assertThat(labels).containsExactly("balia", "ćma", "łuk", "test", "zamek");
        }
    }

    @Test
    public void test_getControlledVocabSelectItems__englishLetters() {
        DatasetFieldType fieldType = new DatasetFieldType("type1", FieldType.TEXT, false);
        fieldType.setControlledVocabularyValues(asList(
                new ControlledVocabularyValue(3L, "one", fieldType),
                new ControlledVocabularyValue(1L, "zone", fieldType),
                new ControlledVocabularyValue(2L, "two", fieldType),
                new ControlledVocabularyValue(4L, "three", fieldType)
        ));

        Collection<SelectItem> items = fieldType.getControlledVocabSelectItems(true);

        List<String> labels = items.stream().map(SelectItem::getLabel).collect(Collectors.toList());

        assertThat(labels).containsExactly("one", "three", "two", "zone");
    }
    
}
