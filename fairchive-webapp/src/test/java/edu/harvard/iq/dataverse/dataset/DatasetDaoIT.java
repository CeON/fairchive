package edu.harvard.iq.dataverse.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.InReview;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Shoulder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

public class DatasetDaoIT extends WebappArquillianDeployment {

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetDao datasetDao;
    
    @Inject
    private SettingsServiceBean settings;

    @BeforeEach
    void setUp() {
        dataverseSession.logIn(authenticationServiceBean.getAdminUser());
    }

    @Test
    void test_inReview() {

        Dataset dataset = this.datasetDao.find(52L);

        assertThat(dataset.isLockedFor(InReview)).isFalse();
        assertThat(dataset.isInReview()).isFalse();
        
        AuthenticatedUser user = (AuthenticatedUser) dataverseSession.getUser();
        
        this.datasetDao.addDatasetLock(dataset, new DatasetLock(InReview, user));

        dataset = this.datasetDao.find(52L);
        assertThat(dataset.isLockedFor(InReview)).isTrue();
        assertThat(dataset.isInReview()).isTrue();
        
        List<DatasetLock> locks = this.datasetDao.getDatasetLocksByUser(user);
        
        assertThat(locks.size()).isEqualTo(1);
        assertThat(locks.get(0).getReason()).isEqualTo(InReview);
        assertThat(locks.get(0).getUser()).isEqualTo(user);
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
    
    
    @Test
    void generateDatasetIdentifier_throwsNPE_forNullIdentifier() {
        
        assertThrows(Exception.class, 
                () ->  this.datasetDao.generateDatasetIdentifier(null));
    }
    
    @Test
    void generateRandomDatasetIdentifier() {
        
        this.settings.setValueForKey(Shoulder, "");
        Dataset set = new Dataset();
        
        String id = this.datasetDao.generateDatasetIdentifier(set);
        
        assertThat(id).isNotBlank();
        assertThat(id).doesNotStartWith("ABC");
        
        this.settings.setValueForKey(Shoulder, "ABC");
        
        id = this.datasetDao.generateDatasetIdentifier(set);
        
        assertThat(id).isNotBlank();
        assertThat(id).startsWith("ABC");
    }
}


