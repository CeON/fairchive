package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataFile;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataset;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeDataverse;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeRole;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.ADMIN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.COLLECTION_CUSTODIAN;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataset;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageMinorDataset;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;

public class AssignRoleCommandTest {

    // -------------------- LOGIC --------------------

    @Test
    void getRequiredPermissions__manageDataverse_forCollection() {

        // given
        final Dataverse collection = makeDataverse();

        // when
        final Map<String, Set<Permission>> required =
                requiredPermissionsFor(collection, makeRole("admin"));

        // then
        assertThat(required.get("")).containsExactly(ManageDataverse);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"admin", "collectionCustodian", "editor"})
    void getRequiredPermissions__manageDataverse_forSubcollection_whateverTheParentDefaultRoleIs(
            final String parentDefaultDataverseContributorRole) {

        // given - a subcollection whose parent grants this role to whoever creates
        // a collection in it. The parent's default role must not weaken the check:
        // assigning a role on a collection always needs ManageDataverse.
        final Dataverse parent = makeDataverse();
        parent.setDefaultDataverseContributorRole(parentDefaultDataverseContributorRole == null
                ? null
                : makeRole(parentDefaultDataverseContributorRole));

        final Dataverse subcollection = makeDataverse();
        subcollection.setOwner(parent);

        // when
        final Map<String, Set<Permission>> required =
                requiredPermissionsFor(subcollection, makeRole(ADMIN.getAlias()));

        // then
        assertThat(required.get("")).containsExactly(ManageDataverse);
    }

    @Test
    void getRequiredPermissions__manageDataverse_whenGranteeIsAGroup() {

        // given
        final Dataverse parent = makeDataverse();
        parent.setDefaultDataverseContributorRole(makeRole(COLLECTION_CUSTODIAN.getAlias()));

        final Dataverse subcollection = makeDataverse();
        subcollection.setOwner(parent);

        // when
        final Map<String, Set<Permission>> required = new AssignRoleCommand(
                AuthenticatedUsers.get(), makeRole("dsContributor"), subcollection,
                makeRequest(), null).getRequiredPermissions();

        // then
        assertThat(required.get("")).containsExactly(ManageDataverse);
    }

    @Test
    void getRequiredPermissions__manageDataset_forDataset() {

        // given
        final Dataset dataset = makeDataset();

        // when
        final Map<String, Set<Permission>> required =
                requiredPermissionsFor(dataset, makeRole("curator"));

        // then
        assertThat(required.get("")).containsExactly(ManageDataset);
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "fileDownloader", "editor", "depositor"})
    void getRequiredPermissions__manageDatasetOrMinor_forRolesAssignableByMinorPermissions(
            final String roleAlias) {

        // given
        final Dataset dataset = makeDataset();

        // when
        final Map<String, Set<Permission>> required =
                requiredPermissionsFor(dataset, makeRole(roleAlias));

        // then
        assertThat(required.get("")).containsExactlyInAnyOrder(ManageDataset, ManageMinorDataset);
    }

    @Test
    void getRequiredPermissions__manageDataset_forFile() {

        // given
        final Dataset dataset = makeDataset();
        final DataFile file = makeDataFile();
        file.setOwner(dataset);

        // when
        final Map<String, Set<Permission>> required =
                requiredPermissionsFor(file, makeRole("curator"));

        // then
        assertThat(required.get("")).containsExactly(ManageDataset);
    }

    @Test
    void getAffectedDvObjects__owningDataset_forFile() {

        // given
        final Dataset dataset = makeDataset();
        final DataFile file = makeDataFile();
        file.setOwner(dataset);

        // when
        final Map<String, DvObject> affected = new AssignRoleCommand(
                AuthenticatedUsers.get(), makeRole("curator"), file,
                makeRequest(), null).getAffectedDvObjects();

        // then - the permission is checked on the owning dataset, not on the file
        assertThat(affected.get("")).isSameAs(dataset);
    }

    // -------------------- PRIVATE --------------------

    private Map<String, Set<Permission>> requiredPermissionsFor(final DvObject assignmentPoint,
            final DataverseRole role) {

        return new AssignRoleCommand(AuthenticatedUsers.get(), role, assignmentPoint,
                makeRequest(), null).getRequiredPermissions();
    }
}
