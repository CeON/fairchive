package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

/**
 * @todo Rename this to ApiTokenFragment? The separate page is being taken out
 * per https://github.com/IQSS/dataverse/issues/3086
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("ApiTokenPage")
public class ApiTokenPage implements java.io.Serializable {
	
    private DataverseSession session;
    private AuthenticationServiceBean authenticationService;
        
    public ApiTokenPage() {}
    
    @Inject
    public ApiTokenPage(final DataverseSession session, 
    					final AuthenticationServiceBean authenticationService) {

		this.session = session;
		this.authenticationService = authenticationService;
	}

	public boolean hasApiToken() {
		
        return this.session.isUserLoggedIn() &&
             this.authenticationService.findApiTokenByUser(
            		 this.session.getAuthenticatedUser()) != null;
    }

    public String getApiToken() {

        if (this.session.isUserLoggedIn()) {
            final AuthenticatedUser user = this.session.getAuthenticatedUser();
            final ApiToken token = this.authenticationService.findApiTokenByUser(user);
            return token != null
                ? token.getTokenString()
                : getStringFromBundle("apitoken.notFound", user.getName());
        } else {
            // It should be impossible to get here from the UI.
            return getStringFromBundle("apitoken.noUser");
        }
    }

    public void generate() {
    	
        if (this.session.isUserLoggedIn()) {
        	this.authenticationService.regenerateApiTokenForUser(
        			this.session.getAuthenticatedUser());
        }
    }
}