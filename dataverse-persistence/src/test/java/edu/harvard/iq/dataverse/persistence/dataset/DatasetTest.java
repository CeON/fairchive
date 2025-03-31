package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.InReview;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Ingest;
import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.Workflow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Lists;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;


/**
 * @author michael
 */
public class DatasetTest {

    private final AuthenticatedUser user = makeAuthenticatedUser("jane", "doe");
    private final Date startTime = new Date();
    private final DatasetLock ingestLock = new DatasetLock(Ingest, this.user);
    private final DatasetLock inReviewLock = new DatasetLock(InReview, this.user);
    
    @BeforeEach
    public void setUp() {
        this.ingestLock.setId(1L);
        this.ingestLock.setStartTime(this.startTime);
        
        this.inReviewLock.setId(2L);
        this.inReviewLock.setStartTime(this.startTime);
    }
    

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
        dataset.setPublicationDate(Timestamp.from(Instant.now()));
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
        dataset.getVersions().add(buildVersionWithState(VersionState.DRAFT));
        dataset.getVersions().add(buildVersionWithState(VersionState.DEACCESSIONED));
        dataset.getVersions().add(buildVersionWithState(VersionState.DEACCESSIONED));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    
    @Test
    public void containsReleasedVersion__negative_for_dataset_that_was_never_published() {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        dataset.getVersions().add(buildVersionWithState(VersionState.DRAFT));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    /**
     * Test of isLockedFor method, of class Dataset.
     */
    @Test
    public void datasetIsOnlyLockForTheReason_isHasBeanExplicitelyLockedFor() {
        Dataset dataset = new Dataset();
        this.ingestLock.setDataset(dataset);
        
        assertFalse(dataset.isLockedFor(Ingest));
        assertFalse(dataset.isLockedFor(Ingest.name()));
        assertFalse(dataset.isLockedFor(Workflow));
        assertFalse(dataset.isLockedFor(Workflow.name()));
        assertFalse(dataset.isLockedFor(""));
        
        dataset.addLock(this.ingestLock);
        
        assertTrue(dataset.isLockedFor(Ingest));
        assertTrue(dataset.isLockedFor(Ingest.name()));
        assertFalse(dataset.isLockedFor(Workflow));
        assertFalse(dataset.isLockedFor(Workflow.name()));
        
        dataset.removeLock(null);
        
        assertTrue(dataset.isLockedFor(Ingest));
        
        dataset.removeLock(new DatasetLock(Workflow, this.user));
        
        assertTrue(dataset.isLockedFor(Ingest));
        assertTrue(dataset.isLockedFor(Ingest.name()));
        
        dataset.removeLock(this.ingestLock);
        
        assertFalse(dataset.isLockedFor(Ingest));
        assertFalse(dataset.isLockedFor(Ingest.name()));
    }

    @Test
    public void testLocksManagement() {
        Dataset dataset = new Dataset();
        this.ingestLock.setDataset(dataset);
        this.inReviewLock.setDataset(dataset);
        
        assertFalse(dataset.isLocked());

        dataset.addLock(this.ingestLock);
        assertTrue(dataset.isLocked());

        dataset.addLock(this.inReviewLock);
        
        assertEquals(2, dataset.getLocks().size());
        assertEquals(this.ingestLock, dataset.getLockFor(Ingest).get());
        
        dataset.removeLock(this.ingestLock);
        
        assertTrue(dataset.isLocked());
        assertFalse(dataset.getLockFor(Ingest).isPresent());
        assertEquals(1, dataset.getLocks().size());

        dataset.removeLock(this.inReviewLock);
        
        assertFalse(dataset.isLocked());
        assertFalse(dataset.getLockFor(InReview).isPresent());
        assertEquals(0, dataset.getLocks().size());
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

    // -------------------- PRIVATE --------------------

    private static Stream<Arguments> isDeacessionedArguments() {
        return Stream.of(
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED)),
                Arguments.of(true, Lists.newArrayList(VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.DEACCESSIONED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DEACCESSIONED, VersionState.RELEASED)),
                Arguments.of(true, Lists.newArrayList(VersionState.DEACCESSIONED, VersionState.DEACCESSIONED))
        );
    }
    
    private static Stream<Arguments> containsReleasedVersionArguments__positive() {
        return Stream.of(
                Arguments.of(Lists.newArrayList(VersionState.RELEASED)),
                Arguments.of(Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(Lists.newArrayList(VersionState.RELEASED, VersionState.RELEASED))
        );
    }
    
    
    private DatasetVersion buildVersionWithState(VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setVersionState(state);
        return version;
    }
}
