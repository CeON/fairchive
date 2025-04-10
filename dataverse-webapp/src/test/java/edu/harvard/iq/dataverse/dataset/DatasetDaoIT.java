package edu.harvard.iq.dataverse.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.InReview;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

public class DatasetDaoIT extends WebappArquillianDeployment {

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetDao datasetDao;

    @BeforeEach
    void setUp() {
        dataverseSession.logIn(authenticationServiceBean.getAdminUser());
    }

    @Test
    void test_inReview() {

        Dataset dataset = this.datasetDao.find(52L);

        assertThat(dataset.isLockedFor(InReview)).isFalse();

        AuthenticatedUser user = (AuthenticatedUser) dataverseSession.getUser();

        this.datasetDao.addDatasetLock(dataset, new DatasetLock(InReview, user));

        dataset = this.datasetDao.find(52L);
        assertThat(dataset.isLockedFor(InReview)).isTrue();

        List<DatasetLock> locks = this.datasetDao.getDatasetLocksByUser(user);

        assertThat(locks.size()).isEqualTo(1);
        assertThat(locks.get(0).getReason()).isEqualTo(InReview);
        assertThat(locks.get(0).getUser()).isEqualTo(user);
    }

    @Test
    void getTitleFromLatestVersion() {
        Dataset dataset = this.datasetDao.find(52L);
        assertThat(this.datasetDao.getTitleFromLatestVersion(52L))
                .isEqualTo(dataset.getLatestVersion().getTitle());
    }

    @Test
    void getDatasetVersionUsersByAuthenticatedUser() {
        AuthenticatedUser user = (AuthenticatedUser) dataverseSession.getUser();
        List<DatasetVersionUser> versions = this.datasetDao
                .getDatasetVersionUsersByAuthenticatedUser(user);
        
        assertThat(versions.size()).isEqualTo(2);
        assertThat(versions.get(0).getAuthenticatedUser()).isEqualTo(user);
        assertThat(versions.get(0).getDatasetVersion().getId()).isEqualTo(41L);
        assertThat(versions.get(0).getId()).isEqualTo(44L);
        
        assertThat(versions.get(1).getAuthenticatedUser()).isEqualTo(user);
        assertThat(versions.get(1).getDatasetVersion().getId()).isEqualTo(36L);
        assertThat(versions.get(1).getId()).isEqualTo(39L);

    }
}
