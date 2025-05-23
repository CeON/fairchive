package edu.harvard.iq.dataverse.persistence.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class MetadataBlockTest {
    
    @Test
    void isDisplayOnCreate_works() {
        MetadataBlock block = new MetadataBlock();
        block.setDatasetFieldTypes(new ArrayList<>());
        
        assertThat(block.isDisplayOnCreate()).isFalse();
        
        block.getDatasetFieldTypes().add(new DatasetFieldType());
        
        assertThat(block.isDisplayOnCreate()).isFalse();
        
        block.getDatasetFieldTypes().add(new DatasetFieldType());
        
        assertThat(block.isDisplayOnCreate()).isFalse();
        
        block.getDatasetFieldTypes().get(1).setDisplayOnCreate(true);
        
        assertThat(block.isDisplayOnCreate()).isTrue();
    }
}
