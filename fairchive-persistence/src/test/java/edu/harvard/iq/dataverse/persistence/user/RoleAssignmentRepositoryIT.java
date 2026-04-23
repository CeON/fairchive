package edu.harvard.iq.dataverse.persistence.user;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;

public class RoleAssignmentRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private RoleAssignmentRepository repository;

    //-------------------- TESTS --------------------

    @Test
    public void findByDefinitionPointId() {
        // when
        List<RoleAssignment> assignments = this.repository.findByDefinitionPointId(1L);

        // then
        assertThat(
                assignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 32L, 33L));
    }

    @Test
    public void findByDefinitionPointIds() {
        // given
        ArrayList<Long> definitionPointIds = newArrayList(1L, 19L, 51L);

        // when
        List<RoleAssignment> roleAssignments = this.repository.findByDefinitionPointIds(definitionPointIds);

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 7L, 29L, 32L, 33L, 101L, 102L));
    }

    @Test
    public void findByAssigneeIdentifier() {
        // when
        List<RoleAssignment> roleAssignments = this.repository.findByAssigneeIdentifier("@dataverseAdmin");

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 7L, 29L));
    }

    @Test
    public void findByRoleId() {
        // when
        List<RoleAssignment> roleAssignments = this.repository.findByRoleId(2L);

        // then
        assertThat(
                roleAssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(30L, 31L));
    }

    @Test
    public void findByAssigneeIdentifiersAndDefinitionPointIds() {
        // when
        List<RoleAssignment> ssignments = this.repository.findByAssigneeIdentifiersAndDefinitionPointIds(
                newArrayList("@dataverseAdmin", "@superuser"),
                newArrayList(1L, 51L));

        // then
        assertThat(
                ssignments.stream().map(RoleAssignment::getId).collect(toList()),
                containsInAnyOrder(5L, 29L, 33L));
    }

    @Test
    public void deleteAllByAssigneeIdentifier() {

        // when
        int deletedCount = this.repository.deleteAllByAssigneeIdentifier("&mail/toDelete");

        // then
        assertThat(deletedCount, is(2));
    }
}
