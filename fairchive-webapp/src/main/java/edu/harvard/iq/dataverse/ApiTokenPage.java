package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @todo Rename this to ApiTokenFragment? The separate page is being taken out
 * per https://github.com/IQSS/dataverse/issues/3086
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("ApiTokenPage")
public class ApiTokenPage implements java.io.Serializable {


    @Inject
    DataverseSession session;
    @EJB
    AuthenticationServiceBean authSvc;

    ApiToken apiToken;

    public boolean checkForApiToken() {
        if (session.isUserLoggedIn()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authSvc.findApiTokenByUser(au);
            return apiToken != null;
        }
        return false;
    }

    public String getApiToken() {

        if (session.isUserLoggedIn()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                return apiToken.getTokenString();
            } else {
                return BundleUtil.getStringFromBundle("apitoken.notFound", au.getName());
            }
        } else {
            // It should be impossible to get here from the UI.
            return "Only authenticated users can have API tokens.";
        }

    }

    public void generate() {
        if (session.isUserLoggedIn()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();

            apiToken = authSvc.findApiTokenByUser(au);
            if (apiToken != null) {
                authSvc.removeApiToken(au);
            }
            /**
             * @todo DRY! Stolen from BuiltinUsers API page
             */
            ApiToken newToken = new ApiToken();
            newToken.setTokenString(java.util.UUID.randomUUID().toString());
            newToken.setAuthenticatedUser(au);
            Calendar c = Calendar.getInstance();
            newToken.setCreateTime(new Timestamp(c.getTimeInMillis()));
            c.roll(Calendar.YEAR, 1);
            newToken.setExpireTime(new Timestamp(c.getTimeInMillis()));
            authSvc.save(newToken);

        }
    }
}