package edu.harvard.iq.dataverse.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;

public class ValidationMessageResolverTest {
    
    @Test
    void resolveValidationMessage__message_from_metadatablock_translations_file() {
        // given
        DatasetField field = new DatasetField();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName("somefieldtype");
        MetadataBlock metadataBlock = new MetadataBlock();
        metadataBlock.setName("custommetadata");
        fieldType.setMetadataBlock(metadataBlock);
        field.setDatasetFieldType(fieldType);
        
        FieldValidationResult result = FieldValidationResult.invalid(field, "customValidationError");
        
        
        // then
        assertThat(result.getMessage()).isEqualTo("Custom validation message");
    }
    
    @Test
    void resolveValidationMessage__message_from_bundle() {
        // given
        DatasetField field = new DatasetField();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName("somefieldtype");
        MetadataBlock metadataBlock = new MetadataBlock();
        metadataBlock.setName("custommetadata");
        fieldType.setMetadataBlock(metadataBlock);
        field.setDatasetFieldType(fieldType);
        
        FieldValidationResult result = FieldValidationResult.invalid(field, "isrequired", "Some field");
        
        // then
        assertThat(result.getMessage()).isEqualTo("Some field is required.");
    }
}
