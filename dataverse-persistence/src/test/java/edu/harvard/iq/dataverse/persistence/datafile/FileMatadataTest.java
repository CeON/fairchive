package edu.harvard.iq.dataverse.persistence.datafile;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
