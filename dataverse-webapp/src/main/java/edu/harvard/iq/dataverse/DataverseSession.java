package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.persistence.ActionLogRecord.ActionType.SessionManagement;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;

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
    private User user;
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
        if (user == null) {
            user = GuestUser.get();
        }
        return user;
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
        return getUser() instanceof PrivateUrlUser && getUser().isAllowedToView(dataset);
    }
    
    public boolean isViewedFromAnonymizedPrivateUrl(final DvObject dataset) {
        return getUser().isAnonymized() && isViewedFromPrivateUrl(dataset);
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

    // -------------------- PRIVATE --------------------

    /**
     * @return Top browser locale which is taken from 'Accept-Language header'.
     */
    private String getBrowserLanguage() {
        return FacesContext.getCurrentInstance().getExternalContext()
                .getRequestLocale().getLanguage();
    }

    // -------------------- SETTERS --------------------

    public void setUser(User aUser) {
        logSvc.log(
                new ActionLogRecord(SessionManagement, (aUser == null) ? "logout" : "login")
                        .setUserIdentifier((aUser != null) ? aUser.getIdentifier() : (user != null ? user.getIdentifier() : "")));
        this.user = aUser;
    }

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
