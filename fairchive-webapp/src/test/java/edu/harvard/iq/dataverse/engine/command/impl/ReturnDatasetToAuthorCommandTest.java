package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReturnDatasetToAuthorCommandTest {

    private Dataset dataset;
    private DataverseRequest dataverseRequest;
    private TestDataverseEngine testEngine;
    private UserNotificationService notificationService = mock(UserNotificationService.class);
    private List<AuthenticatedUser> usersWithEditDataset = new ArrayList<>();

    private static final String TEST_MAIL="test@reply.to";

    @BeforeEach
    public void setUp() {
        dataset = new Dataset();

        HttpServletRequest aHttpServletRequest = null;
        dataverseRequest = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"), aHttpServletRequest);

        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public AuthenticationServiceBean authentication() {
                return new AuthenticationServiceBean() {
                    @Override
                    public AuthenticatedUser getAuthenticatedUser(String id) {
                        return MocksFactory.makeAuthenticatedUser("First", "Last");
                    }
                };
            }

            @Override
            public IndexServiceBean index() {
                return new IndexServiceBean() {
                    @Override
                    public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
                        return null;
                    }
                };
            }

            @Override
            public EntityManager em() {
                return new NoOpTestEntityManager();
            }

            @SuppressWarnings("serial")
            @Override
            public DatasetDao datasets() {
                return new DatasetDao() {
                    {
                        em = new NoOpTestEntityManager();
                    }

                    @Override
                    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {
                        return null;
                    }

                    @Override
                    public WorkflowComment addWorkflowComment(WorkflowComment comment) {
                        return comment;
                    }

                    @Override
                    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) { }
                };
            }

            @SuppressWarnings("serial")
            @Override
            public DataverseRoleServiceBean roles() {
                return new DataverseRoleServiceBean() {

                    @Override
                    public DataverseRole findBuiltinRoleByAlias(BuiltInRole builtInRole) {
                        return new DataverseRole();
                    }

                    @Override
                    public RoleAssignment save(RoleAssignment assignment) {
                        // no-op
                        return assignment;
                    }
                };
            }

            @Override
            public PermissionServiceBean permissions() {
                return new PermissionServiceBean() {
                    @Override
                    public List<AuthenticatedUser> getUsersWithPermissionOn(Permission permission, DvObject dvObject) {
                        return permission == Permission.EditDataset
                                ? new ArrayList<>(usersWithEditDataset) : new ArrayList<>();
                    }
                };
            }

            @Override
            public UserNotificationService notifications() {
                return notificationService;
            }
        });
    }

    // -------------------- TESTS --------------------

    @Test
    public void testDatasetNull()  {
        assertThrows(IllegalArgumentException.class, () -> new ReturnDatasetToAuthorCommand(dataverseRequest, null, createParams("", TEST_MAIL)));
    }

    @Test
    public void testReleasedDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        String expected = "This dataset cannot be return to the author(s) because the latest version is not In Review. The author(s) needs to click Submit for Review first.";
        String actual = null;
        try {
            testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset, createParams("", TEST_MAIL)));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testNotInReviewDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        String expected = "This dataset cannot be return to the author(s) because the latest version is not In Review. The author(s) needs to click Submit for Review first.";
        String actual = null;
        try {
            testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset, createParams("", TEST_MAIL)));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testAllGood() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset updatedDataset = null;
        try {
            testEngine.submit(new AddLockCommand(dataverseRequest, dataset,
                                                 new DatasetLock(DatasetLock.Reason.InReview, dataverseRequest.getAuthenticatedUser())));
            updatedDataset = testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset,
                    createParams("Update Your Files, Dummy", TEST_MAIL)));
        } catch (CommandException ex) {
            System.out.println("Error updating dataset: " + ex.getMessage());
        }
        assertNotNull(updatedDataset);
    }

    @ParameterizedTest
    @MethodSource("authorsAndSendCopy")
    public void execute__sendCopyOnlyWithFirstNotification(int authorsCount, String sendCopy,
                                                           List<String> expectedSendCopy) throws CommandException {
        // given
        IntStream.range(0, authorsCount)
                .mapToObj(i -> MocksFactory.makeAuthenticatedUser("Author", "No" + i))
                .forEach(usersWithEditDataset::add);
        Map<String, String> params = createParams("Update Your Files", TEST_MAIL);
        params.put(NotificationParameter.SEND_COPY.key(), sendCopy);
        lockDatasetForReview();

        // when
        testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset, params));

        // then
        assertThat(captureNotificationParameters(authorsCount))
                .extracting(p -> p.get(NotificationParameter.SEND_COPY.key()))
                .containsExactlyElementsOf(expectedSendCopy);
    }

    private static Stream<Arguments> authorsAndSendCopy() {
        return Stream.of(
                Arguments.of(0, "true", emptyList()),
                Arguments.of(1, "true", singletonList("true")),
                Arguments.of(3, "true", asList("true", null, null)),
                Arguments.of(3, "false", asList("false", null, null)));
    }

    // -------------------- PRIVATE --------------------

    private void lockDatasetForReview() throws CommandException {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        testEngine.submit(new AddLockCommand(dataverseRequest, dataset,
                new DatasetLock(DatasetLock.Reason.InReview, dataverseRequest.getAuthenticatedUser())));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> captureNotificationParameters(int expectedNotifications) {
        ArgumentCaptor<Map<String, String>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(expectedNotifications))
                .sendNotificationWithEmail(any(), any(), any(), any(), any(), parameters.capture());
        return parameters.getAllValues();
    }

    private Map<String, String> createParams(String message, String replyTo) {
        Map<String, String> params = new HashMap<>();
        params.put(NotificationParameter.MESSAGE.key(), message);
        params.put(NotificationParameter.REPLY_TO.key(), replyTo);
        return params;
    }
}
