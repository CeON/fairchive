package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.nextId;
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
