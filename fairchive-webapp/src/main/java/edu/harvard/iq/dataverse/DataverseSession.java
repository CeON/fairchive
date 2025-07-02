package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.persistence.ActionLogRecord.ActionType.SessionManagement;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Option;

/**
 * @author gdurand
 */
@SuppressWarnings("serial")
@Named
@SessionScoped
public class DataverseSession implements Serializable {

    private ActionLogServiceBean logSvc;
    private SystemConfig systemConfig;

    /* Note that on logout, variables must be cleared manually in DataverseHeaderFragment*/
    private User user = GuestUser.get();
    private boolean statusDismissed = false;
    private String localeCode;
    private int filesPerPage;

    private final UUID sessionId;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseSession() {
        this.sessionId = UUID.randomUUID();
    }

    @Inject
    public DataverseSession(ActionLogServiceBean logSvc, SystemConfig systemConfig) {
        this();
        this.logSvc = logSvc;
        this.systemConfig = systemConfig;
    }

    // -------------------- GETTERS --------------------

    public User getUser() {
        return this.user;
    }
    
    public boolean isUserLoggedIn() {
        return this.user.isAuthenticated();
    }
    
    public String getUserEmailAddress() {
        return this.user.getDisplayInfo().getEmailAddress();
    }
    
    public Locale getUserLocaleOr(final Locale defaultLocale) {
        return isUserLoggedIn()
                ?  this.user.getNotificationsLanguage()
                : defaultLocale;
    }
    
    public String getUserEmailOr(final String defaultEmail) {
        return isUserLoggedIn() ? getUserEmailAddress() : defaultEmail;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isStatusDismissed() {
        return statusDismissed;
    }

    public String getLocaleCode() {
        if (this.localeCode == null) {
            this.localeCode = initLocale();
        }
        return this.localeCode;
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(getLocaleCode());
    }

    public String getLocaleTitle() {
        return this.systemConfig.getConfiguredLocales().get(getLocaleCode());
    }

    public int getFilesPerPage() {
        return this.filesPerPage;
    }

    public boolean isViewedFromPrivateUrl(final DvObject dataset) {
        if(getUser() instanceof PrivateUrlUser) {
            return ((PrivateUrlUser)getUser()).isAllowedToView(dataset);
        } else {
            return false;
        }
    }
    
    public boolean isViewedFromAnonymizedPrivateUrl(final DvObject dataset) {
        if(getUser() instanceof PrivateUrlUser) {
            final PrivateUrlUser user = (PrivateUrlUser)getUser();
            return user.isAnonymized() && user.isAllowedToView(dataset);
        } else {
            return false;
        }
    }
    

    // -------------------- LOGIC --------------------

    public String initLocale() {
        final Set<String> dataverseLanguages = this.systemConfig
                .getConfiguredLocales().keySet();
        final String browserLagunage = getBrowserLanguage();
        return dataverseLanguages.contains(browserLagunage) ? browserLagunage : "en";
    }

    public void updateLocaleInViewRootForReload(String code) {
        localeCode = code;
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(localeCode));
    }

    public void updateLocaleInViewRoot() {
        final FacesContext context = FacesContext.getCurrentInstance();
        if (this.localeCode != null
                && context != null
                && context.getViewRoot() != null
                && !this.localeCode.equals(context.getViewRoot().getLocale().getLanguage())) {
            context.getViewRoot().setLocale(new Locale(this.localeCode));
        }
    }
    
    public boolean canEditDashboard() {
        return !this.systemConfig.isReadonlyMode() && getUser().isSuperuser();
    }

    public void logIn(final User user) {
        requireNonNull(user);
        this.logSvc.log(new ActionLogRecord(SessionManagement, "login")
                .setUserIdentifier(user.getIdentifier()));
        this.user = user;

        configureAuthenticatedSession();
    }

    public void logOut() {
        this.logSvc.log(new ActionLogRecord(SessionManagement, "logout")
                .setUserIdentifier(this.user.getIdentifier()));
        this.user = GuestUser.get();
    }

    // -------------------- PRIVATE --------------------

    /**
     * @return Top browser locale which is taken from 'Accept-Language header'.
     */
    private String getBrowserLanguage() {
        return FacesContext.getCurrentInstance().getExternalContext()
                .getRequestLocale().getLanguage();
    }

    private void configureAuthenticatedSession() {
        Option.of(FacesContext.getCurrentInstance())
                .flatMap(fc -> Option.of(fc.getExternalContext()))
                .flatMap(ec -> Option.of((HttpSession) ec.getSession(false)))
                .forEach(httpSession ->
                        httpSession.setMaxInactiveInterval(systemConfig.getAuthenticatedSessionTimeoutMinutes() * 60));
    }

    // -------------------- SETTERS --------------------
    
    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public void setStatusDismissed(boolean status) {
        statusDismissed = status; //MAD: Set to true to enable code!
    }

    public DataverseSession setFilesPerPage(int filesPerPage) {
        this.filesPerPage = filesPerPage;
        return this;
    }
}
