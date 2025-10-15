package edu.harvard.iq.dataverse.api;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;

@Transactional(TransactionMode.ROLLBACK)
public class EmbargoAccessServiceIT extends WebappArquillianDeployment {

    @Inject
    private EmbargoAccessService embargoAccess;

    @Inject
    private DatasetService datasetService;

    @Inject
    private DataverseSession dataverseSession;

    @EJB
    private AuthenticationServiceBean authenticationService;
    
    @Test
    public void shouldCheckEmbargoRestriction_userWithPermissions() {
        // given
        Dataset dataset = datasetService.find(57L);
        dataset.setEmbargoDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        dataverseSession.logIn(authenticationService.getAdminUser());

        // when&then
        Assertions.assertFalse(embargoAccess.isRestrictedByEmbargo(dataset));
    }

    @Test
    public void shouldCheckEmbargoRestriction_userWithoutPermissions() {
        // given
        Dataset dataset = datasetService.find(57L);
        dataset.setEmbargoDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        dataverseSession.logIn(GuestUser.get());

        // when&then
        Assertions.assertTrue(embargoAccess.isRestrictedByEmbargo(dataset));
    }
}
