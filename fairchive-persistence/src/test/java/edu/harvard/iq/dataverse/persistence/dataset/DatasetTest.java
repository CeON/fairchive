package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.nextId;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_ERROR;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_NONE;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.INGEST_STATUS_SCHEDULED;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.IngestType.NON;
import static edu.harvard.iq.dataverse.persistence.datafile.DataFile.IngestType.OCR;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.InReview;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Ingest;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Workflow;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DEACCESSIONED;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.DRAFT;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState.RELEASED;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;


/**
 * @author michael
 */
public class DatasetTest {

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @MethodSource("isDeacessionedArguments")
    public void isDeacessioned(boolean expectedIsDeacessioned, List<VersionState> versionStates) {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        versionStates.forEach(state -> dataset.getVersions().add(buildVersionWithState(state)));

        // when & then
        assertEquals(expectedIsDeacessioned, dataset.isDeaccessioned());
    }
    
    @ParameterizedTest
    @MethodSource("containsReleasedVersionArguments__positive")
    public void containsReleasedVersion__positive(List<VersionState> versionStates) {
        // given
        Dataset dataset = new Dataset();
        dataset.setPublicationDate(Timestamp.from(now()));
        dataset.getVersions().clear();
        versionStates.forEach(state -> dataset.getVersions().add(buildVersionWithState(state)));

        // when & then
        assertTrue(dataset.containsReleasedVersion());
    }
    
    @Test
    public void containsReleasedVersion__negative_for_dataset_that_was_deaccessioned() {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        dataset.getVersions().add(buildVersionWithState(DRAFT));
        dataset.getVersions().add(buildVersionWithState(DEACCESSIONED));
        dataset.getVersions().add(buildVersionWithState(DEACCESSIONED));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    
    @Test
    public void containsReleasedVersion__negative_for_dataset_that_was_never_published() {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        dataset.getVersions().add(buildVersionWithState(DRAFT));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    /**
     * Test of isLockedFor method, of class Dataset.
     */
    @Test
    public void testIsLockedFor() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLockedFor(Ingest));
        DatasetLock dl = new DatasetLock(Ingest, makeAuthenticatedUser("jane", "doe"));
        sut.addLock(dl);
        assertTrue(sut.isLockedFor(Ingest));
        assertFalse(sut.isLockedFor(Workflow));
    }

    @Test
    public void testLocksManagement() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLocked());

        DatasetLock dlIngest = new DatasetLock(Ingest, makeAuthenticatedUser("jane", "doe"));
        dlIngest.setId(nextId());
        sut.addLock(dlIngest);
        assertTrue(sut.isLocked());

        final DatasetLock dlInReview = new DatasetLock(InReview, makeAuthenticatedUser("jane", "doe"));
        dlInReview.setId(nextId());
        sut.addLock(dlInReview);
        assertEquals(2, sut.getLocks().size());

        DatasetLock retrievedDl = sut.getLockFor(Ingest);
        assertEquals(dlIngest, retrievedDl);
        sut.removeLock(dlIngest);
        assertNull(sut.getLockFor(Ingest));

        assertTrue(sut.isLocked());

        sut.removeLock(dlInReview);
        assertFalse(sut.isLocked());
    }
    
    @Test
    public void testGetRoot() {
        
        Dataverse root = new Dataverse();
        Dataverse child = new Dataverse();
        child.setOwner(root);
        Dataset grandChild = new Dataset();
        grandChild.setOwner(child);
        
        assertThat(root.getOwner()).isNull();
        assertThat(root.isRoot()).isTrue();
        assertThat(root.isNotRoot()).isFalse();
        assertThat(root.getDataverseContext()).isSameAs(root);
        
        assertThat(child.getOwner()).isSameAs(root);
        assertThat(child.isRoot()).isFalse();
        assertThat(child.isNotRoot()).isTrue();       
        assertThat(child.getRoot()).isSameAs(root);
        assertThat(child.getDataverseContext()).isSameAs(child);
        
        assertThat(grandChild.getOwner()).isSameAs(child);
        assertThat(grandChild.getRoot()).isSameAs(root);
        assertThat(grandChild.getDataverseContext()).isSameAs(child);
    }
    
    @Test
    void emptyDataset_hasNoUningestableFiles() {
        Dataset set = createDataset();
        
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
    }
    
    @Test
    void releasedDataset_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_SCHEDULED, null);
        set.getLatestVersion().setVersionState(RELEASED);
        
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
    }
    
    @Test
    void draftDataset_withoutIngestableFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/xml-graphml", INGEST_STATUS_SCHEDULED, null);
        
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
    }
    
    @Test
    void draftDataset_withoutIngestedFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, null);
            
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
    }
    
    @Test
    void draftDataset_withUningestedFiles_hasNoUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, null);
        
        DataFile image1 = addFile(set, "image/png", INGEST_STATUS_NONE, null);
        image1.setIngestType(NON);
        
        DataFile image2 = addFile(set, "image/png", INGEST_STATUS_SCHEDULED, null);
        image2.setIngestType(NON);
        
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
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
        
        assertThat(set.hasUningestableFiles()).isFalse();
        assertThat(set.listUningestableFiles()).isEmpty();
    }
    
    @Test
    void draftDataset_withIngestedFiles_hasUningestableFiles() {
        Dataset set = createDataset();
        addFile(set, "text/csv", INGEST_STATUS_NONE, new DataTable());
        addFile(set, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", INGEST_STATUS_ERROR, null);
        addFile(set, "text/tsv", INGEST_STATUS_NONE, new DataTable());
        DataFile image = addFile(set, "image/png", INGEST_STATUS_SCHEDULED, null);
        image.setIngestType(OCR);
        
        assertThat(set.hasUningestableFiles()).isTrue();
        List<DataFile> files = set.listUningestableFiles();
        files.stream().map(DataFile::getContentType).forEach(System.out::println);
        
        assertThat(files).hasSize(4);
        assertThat(files.stream().map(DataFile::getContentType))
            .containsExactly("text/csv", 
                             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                             "text/tsv",
                             "image/png");
    }
    
    private Dataset createDataset() {
        final Dataset dataset = new Dataset();
        
        final DatasetVersion version = new DatasetVersion(); 
        version.setDataset(dataset);
        version.setVersionState(DRAFT);
        dataset.getVersions().add(version);
        
        return dataset;
    }
    
    private DataFile addFile(final Dataset set, final String mimeType,
            final char ingestStatus, final DataTable table) {
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

    // -------------------- PRIVATE --------------------

    private static Stream<Arguments> isDeacessionedArguments() {
        return Stream.of(
                Arguments.of(false, Lists.newArrayList(DRAFT)),
                Arguments.of(false, Lists.newArrayList(RELEASED)),
                Arguments.of(true, Lists.newArrayList(DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(DRAFT, RELEASED, RELEASED)),
                Arguments.of(false, Lists.newArrayList(DRAFT, RELEASED, DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(DRAFT, DEACCESSIONED, RELEASED)),
                Arguments.of(false, Lists.newArrayList(RELEASED, RELEASED)),
                Arguments.of(false, Lists.newArrayList(RELEASED, DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(DEACCESSIONED, RELEASED)),
                Arguments.of(true, Lists.newArrayList(DEACCESSIONED, DEACCESSIONED))
        );
    }
    
    private static Stream<Arguments> containsReleasedVersionArguments__positive() {
        return Stream.of(
                Arguments.of(Lists.newArrayList(RELEASED)),
                Arguments.of(Lists.newArrayList(DRAFT, RELEASED, DEACCESSIONED)),
                Arguments.of(Lists.newArrayList(RELEASED, RELEASED))
        );
    }
    
    
    private DatasetVersion buildVersionWithState(VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setVersionState(state);
        return version;
    }
}
