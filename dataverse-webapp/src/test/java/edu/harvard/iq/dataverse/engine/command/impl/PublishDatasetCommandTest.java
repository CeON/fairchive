package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.mocks.MockRequestFactory.makeRequest;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeAuthenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.NoDatasetFilesException;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBeanResolver;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;

@ExtendWith(MockitoExtension.class)
public class PublishDatasetCommandTest {

    @Mock
    private DatasetDao datasetDao;
    @Mock
    private GlobalIdServiceBeanResolver globalIdServiceBeanResolver;
    @Mock
    private GlobalIdServiceBean globalIdServiceBean;
    @Mock
    private WorkflowServiceBean workflowServiceBean;
    @Mock
    private SettingsServiceBean settingsServiceBean;
    @Mock
    private DataverseRoleServiceBean dataverseRoleServiceBean;
    @Mock
    private EntityManager em;
    @Mock
    private AuthenticationServiceBean authenticationServiceBean;
    @Mock
    private IndexServiceBean indexServiceBean;
    @Mock
    private SolrIndexServiceBean solrIndexServiceBean;

    private TestDataverseEngine testEngine;

    @BeforeEach
    void beforeEach() {

        lenient().when(globalIdServiceBeanResolver.resolve(any())).thenReturn(globalIdServiceBean);
        lenient().when(globalIdServiceBean.publicizeIdentifier(any())).thenReturn(true);
        lenient().when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn("doi");
        lenient().when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("");
        lenient().when(settingsServiceBean.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat)).thenReturn("");
        lenient().when(em.merge(any())).thenAnswer(args -> args.getArgument(0));

        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public DatasetDao datasets() {
                return datasetDao;
            }
            @Override
            public GlobalIdServiceBeanResolver globalIdServiceBeanResolver() {
                return globalIdServiceBeanResolver;
            }
            @Override
            public WorkflowServiceBean workflows() {
                return workflowServiceBean;
            }
            @Override
            public SettingsServiceBean settings() {
                return settingsServiceBean;
            }
            @Override
            public DataverseRoleServiceBean roles() {
                return dataverseRoleServiceBean;
            }
            @Override
            public EntityManager em() {
                return em;
            }
            @Override
            public AuthenticationServiceBean authentication() {
                return authenticationServiceBean;
            }
            @Override
            public IndexServiceBean index() {
                return indexServiceBean;
            }
            @Override
            public SolrIndexServiceBean solrIndex() {
                return solrIndexServiceBean;
            }
        });
    }


    @Test
    void execute__fail_when_no_files() {
        // given
        Dataset dataset = MocksFactory.makeDataset();
        dataset.getFiles().clear();
        dataset.getLatestVersion().getFileMetadatas().clear();

        dataset.getOwner().setPublicationDate(new Timestamp(new Date().getTime()));
        dataset.setGlobalIdCreateTime(new Date());
        PublishDatasetCommand sut = new PublishDatasetCommand(dataset, makeRequest(makeAuthenticatedUser("Jane", "Doe")), false);

        // when & then
        assertThatThrownBy(() -> testEngine.submit(sut)).isInstanceOf(NoDatasetFilesException.class);
    }

    @Test
    void execute__success_when_no_files_but_no_files_allowed() {
        // given
        Dataset dataset = MocksFactory.makeDataset();
        dataset.getFiles().clear();
        dataset.getLatestVersion().getFileMetadatas().clear();

        lenient().when(settingsServiceBean.isTrueForKey(SettingsServiceBean.Key.AllowDatasetPublishWithoutFiles)).thenReturn(true);

        dataset.getOwner().setPublicationDate(new Timestamp(new Date().getTime()));
        dataset.setGlobalIdCreateTime(new Date());
        PublishDatasetCommand sut = new PublishDatasetCommand(dataset, makeRequest(makeAuthenticatedUser("Jane", "Doe")), false);

        // when
        PublishDatasetResult result = testEngine.submit(sut);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.isCompleted()).isTrue();
    }
}
