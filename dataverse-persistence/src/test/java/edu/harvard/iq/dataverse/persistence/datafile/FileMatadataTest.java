package edu.harvard.iq.dataverse.persistence.datafile;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

public class FileMatadataTest {

    private final DataFileCategory category1 = new DataFileCategory();
    private final DataFileCategory category2 = new DataFileCategory();

    @BeforeEach
    public void setUp() {
        this.category1.setName("category1");
        this.category2.setName("category2");
    }

    @Test
    public void getCategoriesByName_works() {

        FileMetadata meta = new FileMetadata();

        assertThat(meta.getCategoriesByName()).isEmpty();

        meta.addCategory(this.category1);

        assertThat(meta.getCategoriesByName())
                .containsExactly(this.category1.getName());

        meta.addCategory(this.category2);

        assertThat(meta.getCategoriesByName())
                .containsExactly(this.category1.getName(), this.category2.getName());
    }
    
    @Test
    public void getFileNameExtention() {
        
        final FileMetadata fileMeta = new FileMetadata();
        
        fileMeta.setLabel("abc");
        
        assertThat(fileMeta.getFileNameExtention()).isEmpty();
        
        
        fileMeta.setLabel("abc.");
        
        assertThat(fileMeta.getFileNameExtention()).isEmpty();
        
        fileMeta.setLabel("abc.txt");
        
        assertThat(fileMeta.getFileNameExtention()).isEqualTo("txt");
        
        fileMeta.setLabel("abc.tar.gz");
        
        assertThat(fileMeta.getFileNameExtention()).isEqualTo("gz");
    }
    
    @Test
    public void getFileDateToDisplay() {
        final FileMetadata fileMeta = new FileMetadata();
        fileMeta.setDataFile(new DataFile());
        fileMeta.getDataFile()
                .setCreateDate(Timestamp.valueOf("2000-01-01 00:00:00"));

        assertThat(fileMeta.getFileDateToDisplay())
                .isEqualTo(fileMeta.getDataFile().getCreateDate());

        fileMeta.getDataFile()
                .setPublicationDate(Timestamp.valueOf("2010-10-10 00:00:00"));

        assertThat(fileMeta.getFileDateToDisplay())
                .isEqualTo(fileMeta.getDataFile().getPublicationDate());
    }
    
    @Test
    public void addingCategoryByName_withNullArgument_doesNothing() {
        FileMetadata meta = new FileMetadata();
        
        meta.addCategoryByName(null);
        
        assertThat(meta.getCategories()).isEmpty();
    }
    
    @Test
    public void addingCategoryByName_withEmptyArgument_doesNothing() {
        FileMetadata meta = new FileMetadata();
        
        meta.addCategoryByName("");
        
        assertThat(meta.getCategories()).isEmpty();
    }
    
    @Test
    public void addingCategoryByName_whenItIsAlreadyPresent_doesNothing() {
        FileMetadata meta = new FileMetadata();
        meta.addCategory(this.category1);
        
        meta.addCategoryByName(this.category1.getName());
        
        assertThat(meta.getCategories().size()).isEqualTo(1);
        assertThat(meta.getCategories().get(0)).isSameAs(this.category1);
    }
    
    @Test
    public void addingCategoryByName_whenItAlreadyExistsinDataset_addsExistingCategory() {
        FileMetadata meta = new FileMetadata();
        meta.setDatasetVersion(new DatasetVersion());
        meta.getDatasetVersion().setDataset(new Dataset());
        meta.getDatasetVersion().getDataset().setFileCategories(asList(this.category2));
        meta.addCategory(this.category1);
        
        meta.addCategoryByName(this.category2.getName());
        
        assertThat(meta.getCategories().size()).isEqualTo(2);
        assertThat(meta.getCategories().get(0)).isSameAs(this.category1);
        assertThat(meta.getCategories().get(1)).isSameAs(this.category2);
    }
    
    @Test
    public void addingCategoryByName_withNeName_addsNewCategory() {
        FileMetadata meta = new FileMetadata();
        meta.setDatasetVersion(new DatasetVersion());
        meta.getDatasetVersion().setDataset(new Dataset());
        meta.addCategory(this.category1);
        
        meta.addCategoryByName("NewCategory");
        
        assertThat(meta.getCategories().size()).isEqualTo(2);
        assertThat(meta.getCategories().get(0)).isSameAs(this.category1);
        assertThat(meta.getCategories().get(1).getName()).isSameAs("NewCategory");
    }
}
