package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.author;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorAffiliationIdentifier;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorIdType;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorIdValue;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.authorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributionDate;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributor;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorAffiliation;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorContactName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorLogo;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorName;
import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.distributorURL;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeControlledVocabDatasetFieldType;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDatasetFieldType;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.*;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.newField;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.newType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.config.JsonMapConverter;

public class DatasetFieldTest {

    // -------------------- TESTS --------------------
    
    @Test
    public void getParentDisplayFormat() {
        // given
        DatasetFieldType parentType = makeDatasetFieldType();
        parentType.setDisplayFormat("format");
 
        DatasetField parentField = newField(1L, null, parentType);
        
        DatasetField field = newField(2L, null, makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertThat(field.getParentDisplayFormat()).isEqualTo("format");
    }
    
    @Test
    public void getParentDisplayFormat__withoutParent() {
        // given
        DatasetField field = newField(1L, null, makeDatasetFieldType());
        
        // when & then
        assertThat(field.getParentDisplayFormat()).isEmpty();
    }
    
    @Test
    public void getParentDisplayFormat__parentDisplayFormatIsNewLine() {
        // given
        DatasetFieldType parentType = makeDatasetFieldType();
        parentType.setDisplayFormat("#NEWLINE");
        DatasetField parentField = newField(1L, null, parentType);
        
        DatasetField field = newField(2L, null, makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertThat(field.getParentDisplayFormat()).isEmpty();
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__true() {
        // given
        DatasetFieldType parentType = makeDatasetFieldType();
        parentType.setDisplayFormat("#NEWLINE");
        
        DatasetField parentField = newField(1L, null, parentType);
        
        DatasetField field = newField(2L, null, makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertThat(field.getParentDisplayFormatIsNewLine()).isTrue();
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__false() {
        // given
        DatasetFieldType parentType = makeDatasetFieldType();
        parentType.setDisplayFormat("format");
        DatasetField parentField = newField(1L, null, parentType);
        
        DatasetField field = newField(2L, null, makeDatasetFieldType());
        field.setDatasetFieldParent(parentField);
        
        // when & then
        assertThat(field.getParentDisplayFormatIsNewLine()).isFalse();
    }
    
    @Test
    public void getParentDisplayFormatIsNewLine__withoutParent() {
        // given
        DatasetField field = newField(1L, null, makeDatasetFieldType());
        
        // when & then
        assertThat(field.getParentDisplayFormatIsNewLine()).isFalse();
    }

    @Test
    public void clearValue__controlledVocabDatasetField() {
        // given
        DatasetField field = newField(1L, null, makeControlledVocabDatasetFieldType("test",
                false, new MetadataBlock(), "testValue"));

        // when
        field.clearValue();

        // then
        assertThat(field.getControlledVocabularyValues()).isEmpty();
    }

    @Test
    public void clearValue__textDatasetField() {
        // given
        DatasetFieldType fieldType = makeDatasetFieldType("text_field",
                FieldType.TEXT,
                false,
                new MetadataBlock());
        DatasetField field = makeEmptyDatasetField(fieldType, 1);

        // when
        field.clearValue();

        // then
        assertThat(field.getFieldValue()).isEmpty();
    }
    
    @Test
    public void copyFieldValues() throws Exception {
    	// source fields
    	DatasetField authorField = newField(10, null, newType(10, author));
    	DatasetField authorNameField = newField(11, "John Doe", newType(11, authorName));
        DatasetField authorAffiliationField = newField(12, "University Of Nowhere", newType(12, authorAffiliation));
        DatasetField authorAffiliationIdentifierField = newField(13, null, newType(13, authorAffiliationIdentifier));
        DatasetField authorIdTypeField = newField(14, "TYPE1", newType(14, authorIdType));
        DatasetField authorIdValueField = newField(15, null, newType(15, authorIdValue));
        authorField.setDatasetFieldsChildren(asList(authorNameField, 
        		authorAffiliationField, authorAffiliationIdentifierField, 
        		authorIdTypeField, authorIdValueField));
        
        //target field
        DatasetField distributorField = newField(20, null, newType(20, distributor));
        distributorField.getDatasetFieldType().setMetadata(createMetadata());
    	DatasetField distributorNameField = newField(21, "Rupert Smith", newType(21, distributorName));
        DatasetField distributorAffiliationField = newField(22, null, 
        		makeControlledVocabDatasetFieldType(distributorAffiliation, false, 
        				new MetadataBlock(), "University Of Nowhere"));
        distributorAffiliationField.getDatasetFieldType().setAllowControlledVocabulary(true);
        DatasetField distributorURLField = newField(23, "URL", newType(23, distributorURL));
        DatasetField distributorLogoField = newField(24, null, newType(24, distributorLogo));
        DatasetField distributionDateField = newField(25, "abc", newType(25, distributionDate));
        DatasetField distributorContactNameField = newField(26, null, newType(26, distributorContactName));
        distributorField.setDatasetFieldsChildren(asList(distributorNameField, 
        		distributorAffiliationField, distributorURLField, 
        		distributorLogoField, distributionDateField, distributorContactNameField));
        
        distributorField.copyChildValuesFrom(authorField);
        
        assertThat(distributorField.getChildren()).contains(distributorNameField, 
        		distributorAffiliationField, distributorURLField, 
        		distributorLogoField, distributionDateField, distributorContactNameField);
        
        assertThat(distributorNameField.getValue()).isEqualTo(authorNameField.getValue());
        assertThat(distributorAffiliationField.getValue()).isEqualTo(authorAffiliationField.getValue());
        assertThat(distributorURLField.getValue()).isNull();
        assertThat(distributorLogoField.getValue()).isNull();
        assertThat(distributionDateField.getValue()).isEqualTo("abc");
        assertThat(distributorContactNameField.getValue()).isEqualTo(authorNameField.getValue());
        
    }
    
    private static Map<String, Object> createMetadata() throws Exception {
    	return new JsonMapConverter().
    			convertToEntityAttribute(resourceToString("/copyFrom_fileTypeMetadata.json", UTF_8));
    }
    
}
