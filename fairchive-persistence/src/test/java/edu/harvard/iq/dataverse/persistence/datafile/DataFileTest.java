package edu.harvard.iq.dataverse.persistence.datafile;

import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.MD5;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.SHA1;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.SHA256;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType.SHA512;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFileTag.TagType.Survey;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFileTag.TagType.TimeSeries;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
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
    
    @Test
    public void testGetFileClass() throws Exception {
    	
    	assertEquals("image", getFileClass("image/png"));
    	
    	assertEquals("video", getFileClass("video/mp4"));	
    	
    	assertEquals("audio", getFileClass("audio/wav"));
    	
    	assertEquals("code", getFileClass("application/x-r-syntax"));
    	assertEquals("code", getFileClass("text/x-stata-syntax"));
    	assertEquals("code", getFileClass("text/x-sas-syntax"));
    	assertEquals("code", getFileClass("text/x-spss-syntax"));
    	
    	assertEquals("document", getFileClass("text/plain"));
    	assertEquals("document", getFileClass("application/pdf"));
    	assertEquals("document", getFileClass("application/msword"));
    	assertEquals("document", getFileClass("application/vnd.ms-excel"));
    	assertEquals("document", getFileClass("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    	assertEquals("document", getFileClass("text/plain;something"));
    	
    	assertEquals("astro", getFileClass("application/fits"));
    	assertEquals("astro", getFileClass("image/fits"));
    	
    	assertEquals("network", getFileClass("text/xml-graphml"));
    	
    	assertEquals("geodata", getFileClass("application/zipped-shapefile"));
    	
    	assertEquals("tabular", getFileClass("text/tsv"));
    	assertEquals("tabular", getFileClass("text/x-fixed-field"));
    	assertEquals("tabular", getFileClass("application/x-sas-transport"));
    	assertEquals("tabular", getFileClass("application/x-sas-system"));
    	
    	assertEquals("tabular", getFileClass("application/x-stata"));
    	assertEquals("tabular", getFileClass("application/x-stata-13"));
    	assertEquals("tabular", getFileClass("application/x-stata-14"));
    	assertEquals("tabular", getFileClass("application/x-stata-15"));
    	assertEquals("tabular", getFileClass("application/x-rlang-transport"));
    	assertEquals("tabular", getFileClass("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    	assertEquals("tabular", getFileClass("application/x-spss-sav"));
    	assertEquals("tabular", getFileClass("application/x-spss-por"));
    	assertEquals("tabular", getFileClass("text/csv"));
    	assertEquals("tabular", getFileClass("text/comma-separated-values"));
    	assertEquals("tabular", getFileClass("text/tsv"));
    	assertEquals("tabular", getFileClass("text/tab-separated-values"));
    	
    	DataFile tabular = new DataFile("");
    	tabular.setDataTable(new DataTable());
    	assertEquals("tabular", tabular.getFileClass());
    	
    	assertEquals("package", getFileClass("application/vnd.dataverse.file-package"));
    	
        assertEquals("other", getFileClass(""));
        assertEquals("other", getFileClass("foo/bar"));
    }
    
    @Test
    void isIngestableAsTabular() {
    	
    	assertFalse(new DataFile("").isIngestableAsTabular());
    	assertFalse(new DataFile("text/plain").isIngestableAsTabular());
    	
    	assertTrue(new DataFile("text/tsv").isIngestableAsTabular());
    	assertTrue(new DataFile("application/x-stata").isIngestableAsTabular());
    }
    
    
    @Test
    public void testIsThumbnailSupported() throws Exception {    
        
        DataFile file = new DataFile("");
        file.setOwner(new Dataset());
        file.setStorageIdentifier("https://abc.com");
        assertFalse(file.isThumbnailSupported());
        
        file.setStorageIdentifier("");
        file.setContentType("image/png");
        assertFalse(file.isThumbnailSupported());
        
        file.setStorageIdentifier("abc");
        
        file.setContentType("image/fits");
        assertFalse(file.isThumbnailSupported());   
        
        file.setContentType("image/png");
        assertTrue(file.isThumbnailSupported());
        
        file.setContentType("application/pdf");
        assertTrue(file.isThumbnailSupported());
        
        file.setContentType("application/zipped-shapefile");
        assertTrue(file.isThumbnailSupported());
        
        file.setContentType("");
        file.setDataTable(new DataTable());
        assertFalse(file.isThumbnailSupported());
 
        DataFileTag tag = new DataFileTag();
        tag.setType(DataFileTag.TagType.Geospatial);
        file.addTag(tag);
        assertTrue(file.isThumbnailSupported());
        
        file.setDataTable(null);
        assertFalse(file.isThumbnailSupported());
        
        file.setContentType(null);
        assertFalse(file.isThumbnailSupported());
    }
    
    @ParameterizedTest
    @CsvSource({
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, true",
            "text/tsv, true",
            "text/tab-separated-values, true",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void isSelectivelyIngestable(String mimeType, boolean expected) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = dataFile. isSelectivelyIngestable();
        // then
        assertThat(ret).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
            "application/x-spss-por, true",
            "text/tsv, false",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void supportsInclusionOfLabels(String mimeType, boolean expected) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = dataFile.supportsInclusionOfLabels();
        // then
        assertThat(ret).isEqualTo(expected);
    }
    
    @ParameterizedTest
    @CsvSource({
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, false",
            "text/csv, true",
            "text/comma-separated-values, true",
            "text/tsv, false",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void supportsPickingEncoding(String mimeType, boolean expected) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = dataFile.supportsPickingEncoding();
        // then
        assertThat(ret).isEqualTo(expected);
    }
    
    private static String getFileClass(final String mime) {
    	return new DataFile(mime).getFileClass();
    }
}
