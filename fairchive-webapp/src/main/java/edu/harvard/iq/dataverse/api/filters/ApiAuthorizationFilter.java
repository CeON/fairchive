package edu.harvard.iq.dataverse.api.filters;

import java.io.IOException;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 *  When one is using Dataverse REST Api we need to set user in session
 * so that user data will be accessible to all the services.
 *  This filter is only used when Api call is done and it tries to log
 * user into the session if following conditions are all met:
 * <ol>
 * <li>user is not already set in the session (i.e. user is not the guest user)</li>
 * <li>user token is passed in header <i>X-Dataverse-key</i> or in the <i>key</i> query parameter.</li>
 * </ol>
 *  If the user was logged in by the filter, after the service call is done, user's
 * session is invalidated.
 */
public class ApiAuthorizationFilter implements Filter {

    private DataverseSession session;
    private AuthenticationServiceBean authenticationService;
    private UserServiceBean userService;
    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS ---------------------

    @Inject
    public ApiAuthorizationFilter(final DataverseSession session, 
    		final AuthenticationServiceBean authenticationService,
            final UserServiceBean userService, 
            final SystemConfig systemConfig) {
    	
        this.session = session;
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.systemConfig = systemConfig;
    }

    // -------------------- LOGIC ---------------------

    @Override
    public void doFilter(final ServletRequest servletRequest, 
    		final ServletResponse response, final FilterChain chain) 
    				throws IOException, ServletException {
    	
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final BiConsumer<HttpServletRequest, DataverseSession> logoutHandler = 
        		logInUserByTokenIfNeeded(request);
        try {
        	chain.doFilter(servletRequest, response);
        } finally {
        	logoutHandler.accept(request, this.session);
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

    // -------------------- PRIVATE ---------------------

    private BiConsumer<HttpServletRequest, DataverseSession> logInUserByTokenIfNeeded(
    		final HttpServletRequest request) {
    	
    	if(! this.session.isUserLoggedIn()) {
            final String token = getRequestApiKey(request);
            AuthenticatedUser user = this.authenticationService.lookupUser(token);
            if (user != null) {
                if (!this.systemConfig.isReadonlyMode()) {
                    user = this.userService.updateLastApiUseTime(user);
                }
                this.session.logIn(user);
                return (r, ds) -> {
                    r.getSession().invalidate();
                    ds.logOut();
                };
            }
        }
        return (r, ds) -> { };
    }

    private String getRequestApiKey(final HttpServletRequest request) {
    	
        final String headerParamApiKey = request.getHeader("X-Dataverse-key");
        final String queryParamApiKey = request.getParameter("key");
        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }
}
