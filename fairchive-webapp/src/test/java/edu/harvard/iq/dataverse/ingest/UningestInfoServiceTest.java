package edu.harvard.iq.dataverse.ingest;

import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_ERROR;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_NONE;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_SCHEDULED;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

class UningestInfoServiceTest {

    private final IngestServiceBean ingestService = new IngestServiceBean();
    private final UningestInfoService service = new UningestInfoService(ingestService);

    @Test
    void emptyDataset_hasNoUningestableFiles() {
        Dataset set = createDataset();
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void releasedDataset_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_SCHEDULED, null);
        set.getLatestVersion().setVersionState(RELEASED);
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void draftDataset_withoutIngestableFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/xml-graphml", INGEST_STATUS_SCHEDULED, null);
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void draftDataset_withoutIngestedFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, null);
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void draftDataset_withUningestedFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, null);
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void draftDataset_withMultipleVersionsIngestedFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        DataFile file = addFile(set, "text/csv", INGEST_STATUS_NONE, new DataTable());
        final FileMetadata meta = new FileMetadata();
        meta.setDataFile(file);
        file.getFileMetadatas().add(meta);
        meta.setDatasetVersion(set.getLatestVersion());
        set.getLatestVersion().addFileMetadata(meta);
        
        assertThat(this.service.hasUningestableFiles(set)).isFalse();
        assertThat(this.service.listUningestableFiles(set)).isEmpty();
    }
    
    @Test
    void draftDataset_withIngestedFiles_hasUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, new DataTable());
        addFile(set, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", INGEST_STATUS_ERROR, null);
        addFile(set, "text/tsv", INGEST_STATUS_NONE, new DataTable());
        
        assertThat(this.service.hasUningestableFiles(set)).isTrue();
        List<DataFile> files = this.service.listUningestableFiles(set);
        assertThat(files).hasSize(3);
        assertThat(files.stream().map(DataFile::getContentType))
            .containsExactly("text/csv", 
                             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                             "text/tsv");
    }
    
    private Dataset createDataset() {
        final Dataset dataset = new Dataset();
        
        final DatasetVersion version = new DatasetVersion(); 
        version.setDataset(dataset);
        version.setVersionState(DRAFT);
        dataset.getVersions().add(version);
        
        return dataset;
    }
    
    private DataFile addFile(final Dataset set, final String mimeType, final char ingestStatus, final DataTable table) {
        final DataFile file = new DataFile();
        
        set.getFiles().add(file);
        final FileMetadata meta = new FileMetadata();
        file.getFileMetadatas().add(meta);
        set.getLatestVersion().addFileMetadata(meta);
        meta.setDatasetVersion(set.getLatestVersion());
        meta.setDataFile(file);
        file.setContentType(mimeType);
        file.setIngestStatus(ingestStatus);
        file.setDataTable(table);
        
        return file;
    }
}