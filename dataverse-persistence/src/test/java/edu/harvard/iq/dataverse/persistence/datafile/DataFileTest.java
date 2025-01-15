package edu.harvard.iq.dataverse.persistence.datafile;

import static edu.harvard.iq.dataverse.persistence.datafile.DataFileTag.TagType.Survey;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFileTag.TagType.TimeSeries;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

public class DataFileTest {

    private final DataFileTag survey = new DataFileTag();
    private final DataFileTag timeSeries = new DataFileTag();
    private final FileMetadata olderVersionMetadata = new FileMetadata();
    private final FileMetadata newerVersionMetadata = new FileMetadata();
    private final FileMetadata draftMetadata = new FileMetadata();
    
    
    @BeforeEach
    public void setUp() {
        this.survey.setType(Survey);
        this.timeSeries.setType(TimeSeries);
        
        this.olderVersionMetadata.setDatasetVersion(new DatasetVersion());
        this.olderVersionMetadata.getDatasetVersion().setVersionState(RELEASED);
        this.olderVersionMetadata.getDatasetVersion().setVersionNumber(1L);
        this.olderVersionMetadata.getDatasetVersion().setMinorVersionNumber(0L);
        
        this.newerVersionMetadata.setDatasetVersion(new DatasetVersion());
        this.newerVersionMetadata.getDatasetVersion().setVersionState(RELEASED);
        this.newerVersionMetadata.getDatasetVersion().setVersionNumber(2L);
        this.newerVersionMetadata.getDatasetVersion().setMinorVersionNumber(0L);
        
        this.draftMetadata.setDatasetVersion(new DatasetVersion());
        this.draftMetadata.getDatasetVersion().setVersionState(DRAFT);
        this.draftMetadata.getDatasetVersion().setVersionNumber(2L);
        this.draftMetadata.getDatasetVersion().setMinorVersionNumber(1L);
        this.draftMetadata.setLabel("draft");
    }
    
    @Test
    public void settingTagLabels_works() {
        
        DataFile file = new DataFile();
        
        assertThat(file.getTagLabels()).isEmpty();
        
        file.addTag(this.survey);
        file.addTag(this.timeSeries);
        
        assertThat(file.getTagLabels()).containsExactly("Survey", "Time Series");
        
        file.setTags(null);
        
        assertThat(file.getTagLabels()).isEmpty();
    }
    
    @Test
    public void gettingLatestFileMatadataWorks()  {
        
        DataFile file = new DataFile();
        
        assertThat(file.getLatestFileMetadata()).isNull();
        
        file.setFileMetadatas(asList(draftMetadata));
        
        assertThat(file.getLatestFileMetadata()).isSameAs(draftMetadata);
        
        file.setFileMetadatas(asList(olderVersionMetadata));
        
        assertThat(file.getLatestFileMetadata()).isSameAs(olderVersionMetadata);
        
        file.setFileMetadatas(asList(olderVersionMetadata, newerVersionMetadata));
        
        assertThat(file.getLatestFileMetadata()).isSameAs(newerVersionMetadata);
        
        file.setFileMetadatas(asList(olderVersionMetadata, newerVersionMetadata, draftMetadata));
        
        assertThat(file.getLatestFileMetadata()).isSameAs(draftMetadata);
    }
    
    @Test
    public void isHarvested_returnsTrue() {
        DataFile file = new DataFile();  
        file.setStorageIdentifier("http://google.com");
        
        assertThat(file.isHarvested()).isTrue();
        
        file.setStorageIdentifier("https://google.com");
        
        assertThat(file.isHarvested()).isTrue();
  
        file.setStorageIdentifier("ftp://google.com");
        file.setOwner(new Dataset());
        file.getOwner().setHarvestedFrom(new HarvestingClient());
        assertThat(file.isHarvested()).isTrue();
    }
    
    @Test
    public void isHarvested_returnsFalse() {
        DataFile file = new DataFile();
        file.setStorageIdentifier("ftp://google.com");
        file.setOwner(new Dataset());
        file.getOwner().setHarvestedFrom(null);
        
        assertThat(file.isHarvested()).isFalse();
    }
    
    @Test
    public void toStringExtras_works() {
        DataFile file = new DataFile();
        
        assertThat(file.toStringExtras()).isEqualTo("label:[no metadata]");
        
        file.setFileMetadatas(asList(draftMetadata));
        assertThat(file.toStringExtras()).isEqualTo("label:draft");
    }
    
    @Test
    public void checsumType_fromString_returnsCheckSumType() {
        assertThat(ChecksumType.fromString("MD5")).isSameAs(MD5);
        assertThat(ChecksumType.fromString("SHA-1")).isSameAs(SHA1);
        assertThat(ChecksumType.fromString("SHA-256")).isSameAs(SHA256);
        assertThat(ChecksumType.fromString("SHA-512")).isSameAs(SHA512);
    }
    
    @Test
    public void checsumType_fromString_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ChecksumType.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> ChecksumType.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> ChecksumType.fromString("abc"));
    }
}
