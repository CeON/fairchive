package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DefaultAuthProvider;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SignUpUrl;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 * @author xyang
 * @author Michael Bar-Sinai
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("LoginPage")
public class LoginPage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(LoginPage.class.getName());

    private String redirectPage = "dataverse.xhtml";
    private AuthenticationProvider authProvider;
    private int numFailedLoginAttempts;
    private final Random random = new Random();
    private long op1;
    private long op2;
    private Long userSum;
    private Long selectedSamlIdpId;
    private String credentialsAuthProviderId;
    private List<FilledCredential> filledCredentials;

    private DataverseSession session;
    private DataverseDao dataverseDao;
    private AuthenticationServiceBean authSvc;
    private SettingsServiceBean settingsService;
    private DataverseRequestServiceBean dvRequestService;
    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public LoginPage() { }

    @Inject
    public LoginPage(DataverseSession session, 
                     DataverseDao dataverseDao,
                     AuthenticationServiceBean authSvc, 
                     SettingsServiceBean settingsService,
                     DataverseRequestServiceBean dvRequestService, 
                     SystemConfig systemConfig) {
        this.session = session;
        this.dataverseDao = dataverseDao;
        this.authSvc = authSvc;
        this.settingsService = settingsService;
        this.dvRequestService = dvRequestService;
        this.systemConfig = systemConfig;
    }
    
    public boolean displayLoginInfo() {
        return isNoneEmpty(getLoginInfo());
    }
    
    public boolean displayNoProvidersWarning() {
        return this.authSvc.getAuthenticationProviders().isEmpty();
    }
    
    public boolean displayBuiltInProviderForm() {
        return  this.authProvider.getId().equals("builtin");
    }
    
    public boolean displayShibbolethProviderForm() {
        return  this.authProvider.getId().equals("shib");
    }
    
    public boolean displayOAuthProviderForm() {
        return  this.authProvider.isOAuthProvider();
    }
    
    public boolean displaySamlProviderForm() {
        return  this.authProvider.getId().equals("saml");
    }
    
    public boolean displayOtherProvidersForm() {
        return this.authSvc.getAuthenticationProviders().size() > 1;
    }

    // -------------------- GETTERS --------------------
    
    public String getPageTitle() {
        return getStringFromBundle("login") + " - " + 
                this.dataverseDao.getRootDataverseName();
    }

    public Long getSelectedSamlIdpId() {
        return selectedSamlIdpId;
    }

    public String getRedirectPage() {
        return redirectPage;
    }

    public String getCredentialsAuthProviderId() {
        return credentialsAuthProviderId;
    }

    public List<FilledCredential> getFilledCredentials() {
        return filledCredentials;
    }

    public long getOp1() {
        return op1;
    }

    public long getOp2() {
        return op2;
    }

    public Long getUserSum() {
        return userSum;
    }

    public int getNumFailedLoginAttempts() {
        return numFailedLoginAttempts;
    }

    public AuthenticationProvider getAuthProvider() {
        return authProvider;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (dvRequestService.getDataverseRequest().getUser().isAuthenticated()) {
            return redirectPage + "?faces-redirect=true";
        }

        Iterator<String> credentialsIterator = 
                authSvc.getAuthenticationProviderIdsOfType(CredentialsAuthenticationProvider.class).
                iterator();
        if (credentialsIterator.hasNext()) {
            setCredentialsAuthProviderId(credentialsIterator.next());
        }
        resetFilledCredentials(null);
        authProvider = authSvc.getAuthenticationProvider(settingsService.getValueForKey(DefaultAuthProvider));

        return "";
    }
    
    public boolean isNotSelected(final AuthenticationProviderDisplayInfo providerInfo) {
        return ! providerInfo.getId().equals(this.authProvider.getId());
    }
    
    public List<AuthenticationProviderDisplayInfo> listAuthenticationProviders() {     
        return this.authSvc.getAuthenticationProviders().stream()
                .map(AuthenticationProvider::getInfo)
                .collect(toList());
    }

    private CredentialsAuthenticationProvider selectedCredentialsProvider() {
        return (CredentialsAuthenticationProvider) authSvc.getAuthenticationProvider(getCredentialsAuthProviderId());
    }

    public String login() {
        AuthenticationRequest authReq = new AuthenticationRequest();
        List<FilledCredential> filledCredentialsList = getFilledCredentials();
        if (filledCredentialsList == null) {
            logger.info("Credential list is null!");
            return null;
        }
        for (FilledCredential fc : filledCredentialsList) {
            authReq.putCredential(fc.getCredential().getKey(), fc.getValue());
        }
        authReq.setIpAddress(dvRequestService.getDataverseRequest().getSourceAddress());
        try {
            AuthenticatedUser r = authSvc.getUpdateAuthenticatedUser(credentialsAuthProviderId, authReq);
            logger.log(Level.FINE, "User authenticated: {0}", r.getEmail());
            session.logIn(r);

            if ("dataverse.xhtml".equals(redirectPage)) {
                redirectPage = redirectToRoot();
            }

            try {
                redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, null, ex);
                redirectPage = redirectToRoot();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);

            if(validateIsRedirectUrlAnExternalResource(redirectPage)) {
                return redirectToExternalResource();
            } else {
                return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";
            }
        } catch (AuthenticationFailedException ex) {
            numFailedLoginAttempts++;
            op1 = random.nextInt(10);
            op2 = random.nextInt(10);
            AuthenticationResponse response = ex.getResponse();
            switch (response.getStatus()) {
                case FAIL:
                    JsfHelper.addErrorMessage(getStringFromBundle("login.builtin.invalidUsernameEmailOrPassword"));
                    return null;
                case ERROR:
                    /**
                     * @todo How do we exercise this part of the code? Something
                     * with password upgrade? See
                     * https://github.com/IQSS/dataverse/pull/2922
                     */
                    JsfHelper.addErrorMessage(getStringFromBundle("login.error"));
                    logger.log(Level.WARNING, "Error logging in: " + 
                            response.getMessage(), response.getError());
                    return null;
                case BREAKOUT:
                    return response.getMessage();
                default:
                    JsfHelper.addErrorMessage("INTERNAL ERROR");
                    return null;
            }
        }
    }

    boolean validateIsRedirectUrlAnExternalResource(String urlToValidate) {
        boolean result = Pattern.compile("^(https?)://[^\\s/$.?#].[^\\s]*$",
                Pattern.CASE_INSENSITIVE).matcher(urlToValidate).matches();
        if(!result) {
            logger.severe("Invalid redirect URL: " + urlToValidate + 
                    ". Redirect URL must start with http:// or https://");
        }
        return result;
    }

    public void resetFilledCredentials(AjaxBehaviorEvent event) {
        if (selectedCredentialsProvider() == null) {
            return;
        }

        filledCredentials = new LinkedList<>();
        for (CredentialsAuthenticationProvider.Credential c : selectedCredentialsProvider().
                getRequiredCredentials()) {
            filledCredentials.add(new FilledCredential(c, ""));
        }
    }

    public boolean isMultipleProvidersAvailable() {
        return authSvc.getAuthenticationProviderIds().size() > 1;
    }

    public void selectProviderById(final String id) {
        this.authProvider = this.authSvc.getAuthenticationProvider(id);
    }

    public String getLoginButtonText() {
        if (authProvider != null) {
            // Note that for ORCID we do not want the normal "Log In with..." text. There is special logic in the xhtml.
            return getStringFromBundle("login.button", authProvider.getInfo().getTitle());
        } else {
            return getStringFromBundle("login.button", "???");
        }
    }

    public boolean requiresExtraValidation() {
        return this.numFailedLoginAttempts > 2;
    }

    // TODO: Consolidate with SendFeedbackDialog.validateUserSum?
    public void validateUserSum(FacesContext context, UIComponent component, 
            Object value) throws ValidatorException {
        // The FacesMessage text is on the xhtml side.
        FacesMessage msg = new FacesMessage("");
        ValidatorException validatorException = new ValidatorException(msg);
        if (value == null) {
            throw validatorException;
        }
        if (op1 + op2 != (Long) value) {
            throw validatorException;
        }
    }

    public String getLoginInfo() {
        return systemConfig.getLoginInfo(session.getLocale());
    }

    public String getSignUpRedirect() {
        String url = settingsService.getValueForKey(SignUpUrl);
        List<String> params = new ArrayList<>();
        if (StringUtils.isNotBlank(redirectPage)) {
            params.add("redirectPage=" + redirectPage);
        }
        params.add("faces-redirect=true");
        return url + (url.contains("?") ? "&" : "?") + String.join("&", params);
    }

    // -------------------- PRIVATE --------------------

    private String redirectToRoot() {
        return "dataverse.xhtml?alias=" + dataverseDao.findRootDataverse().getAlias();
    }

    private String redirectToExternalResource() {
        try {
            logger.info("Trying to redirect to external page: " + redirectPage);
            if(systemConfig.getAllowedExternalRedirectionUrl() == null || 
                    systemConfig.getAllowedExternalRedirectionUrl().isEmpty()) {
                logger.severe("External redirection not allowed.");
            } else if(redirectPage.startsWith(systemConfig.getAllowedExternalRedirectionUrl())) {
                FacesContext.getCurrentInstance().getExternalContext().redirect(redirectPage);
            } else {
                logger.severe("Chosen redirect page " + redirectPage + " is not allowed. " +
                        "Allowed pages: " + systemConfig.getAllowedExternalRedirectionUrl());
            }
        } catch (IOException e) {
            logger.severe("Unable to redirect to external page "+ e.getMessage());
        }
        // Internal Redirection: Uses navigation handling in JSF, where returning
        // a string tells JSF which page to navigate to next.
        // External Redirection: Directly interacts with the HTTP response to send a redirect.
        // No string return is necessary because the redirection is handled immediately
        // by the ExternalContext.redirect() method.
        return "";
    }

    // -------------------- SETTERS --------------------

    public void setSelectedSamlIdpId(Long selectedSamlIdpId) {
        this.selectedSamlIdpId = selectedSamlIdpId;
    }

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    }

    public void setUserSum(Long userSum) {
        this.userSum = userSum;
    }

    public void setCredentialsAuthProviderId(String authProviderId) {
        this.credentialsAuthProviderId = authProviderId;
    }

    public void setFilledCredentials(List<FilledCredential> filledCredentials) {
        this.filledCredentials = filledCredentials;
    }

    // -------------------- INNER CLASSES --------------------

    public static class FilledCredential {
        private CredentialsAuthenticationProvider.Credential credential;
        private String value;

        // -------------------- CONSTRUCTORS --------------------

        public FilledCredential() { }

        public FilledCredential(CredentialsAuthenticationProvider.Credential credential, 
                String value) {
            this.credential = credential;
            this.value = value;
        }

        // -------------------- GETTERS --------------------

        public CredentialsAuthenticationProvider.Credential getCredential() {
            return credential;
        }

        public String getValue() {
            return value;
        }

        // -------------------- SETTERS --------------------

        public void setCredential(CredentialsAuthenticationProvider.Credential credential) {
            this.credential = credential;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
