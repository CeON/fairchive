package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.users.SamlSessionRegistry;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundleWithLocale;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements Serializable {

    private DataverseDao dataverseDao;
    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;
    private DataFileServiceBean datafileService;
    private DataverseSession dataverseSession;
    private NavigationWrapper navigationWrapper;
    private UserNotificationRepository userNotificationRepository;
    private ConfirmEmailServiceBean confirmEmailService;
    private WidgetWrapper widgetWrapper;
    private SamlSessionRegistry samlSessionRegistry;

    private List<Breadcrumb> breadcrumbs = new ArrayList<>();

    private Long unreadNotificationCount;
    private DvObject currentDvObject;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DataverseHeaderFragment() { }

    @Inject
    public DataverseHeaderFragment(DataverseDao dataverseDao, 
                                   SettingsServiceBean settingsService,
                                   SystemConfig systemConfig, 
                                   DataFileServiceBean datafileService,
                                   DataverseSession dataverseSession, 
                                   NavigationWrapper navigationWrapper,
                                   UserNotificationRepository userNotificationRepository, 
                                   ConfirmEmailServiceBean confirmEmailService,
                                   WidgetWrapper widgetWrapper, 
                                   SamlSessionRegistry samlSessionRegistry) {
        this.dataverseDao = dataverseDao;
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
        this.datafileService = datafileService;
        this.dataverseSession = dataverseSession;
        this.navigationWrapper = navigationWrapper;
        this.userNotificationRepository = userNotificationRepository;
        this.confirmEmailService = confirmEmailService;
        this.widgetWrapper = widgetWrapper;
        this.samlSessionRegistry = samlSessionRegistry;
    }

    // -------------------- GETTERS --------------------

    public List<Breadcrumb> getBreadcrumbs() {
        return this.breadcrumbs;
    }

    // -------------------- LOGIC --------------------

    public void initBreadcrumbs(final DvObject dvObject) {
        this.currentDvObject = dvObject;
        if (dvObject == null) {
            return;
        }
        if (dvObject.getId() != null) {
            initBreadcrumbs(dvObject, null);
        } else {
            initBreadcrumbs(dvObject.getOwner(), getSubPage());
        }
    }
    
    private String getSubPage() {
        if(this.currentDvObject instanceof Dataverse) {
            return getStringFromBundleWithLocale("newDataverse", dataverseSession.getLocale());
        } else if (this.currentDvObject instanceof Dataset) {
            return getStringFromBundleWithLocale("newDataset", dataverseSession.getLocale());
        } else {
            return null;
        }
    }

    public void initBreadcrumbsForFileMetadata(FileMetadata fmd) {

        initBreadcrumbsForFileMetadata(fmd, null);
    }
    
    public boolean displayBreadcrumbs() {
        return getBreadcrumbs().size() > 1 
                && ! this.dataverseSession.isViewedFromAnonymizedPrivateUrl(this.currentDvObject);
    }

    public void initBreadcrumbsForDataFile(DataFile datafile, String subPage) {
        Dataset dataset = datafile.getOwner();
        Long getDatasetVersionID = dataset.getLatestVersion().getId();
        FileMetadata fmd = datafileService.
                findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, datafile.getId());

        initBreadcrumbsForFileMetadata(fmd, subPage);
    }

    public boolean shouldShowUnconfirmedMailInfoBanner() {
        return confirmEmailService.hasEffectivelyUnconfirmedMail(dataverseSession.getUser());
    }

    public boolean shouldShowAddDatasetButton() {
        return !systemConfig.isReadonlyMode() &&
                (!dataverseSession.isUserLoggedIn() 
                        || !confirmEmailService.hasEffectivelyUnconfirmedMail(dataverseSession.getUser()));
    }

    public boolean shouldShowLoginRedirect() {
        return !dataverseSession.isUserLoggedIn();
    }

    public void initBreadcrumbsForFileMetadata(FileMetadata fmd, String subPage) {
        if (fmd == null) {
            return;
        }

        breadcrumbs.clear();

        String optionalUrlExtension = "&version=".
                concat(fmd.getDatasetVersion().getSemanticVersion());
        //First Add regular breadcrumb for the data file
        DataFile datafile = fmd.getDataFile();
        breadcrumbs.add(0, buildBreadcrumbForDatafile(datafile, optionalUrlExtension));

        //Get the Dataset Owning the Datafile and add version to the breadcrumb
        Dataset dataset = datafile.getOwner();

        breadcrumbs.add(0, buildBreadcrumbForDataset(dataset, optionalUrlExtension));

        // now get Dataverse Owner of the dataset and proceed as usual
        Dataverse dataverse = dataset.getOwner();
        while (dataverse != null) {
            breadcrumbs.add(0, buildBreadcrumbForDataverse(dataverse));
            dataverse = dataverse.getOwner();
        }

        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
        }

    }

    public Long getUnreadNotificationCount() {

        if (unreadNotificationCount != null) {
            return unreadNotificationCount;
        }

        User user = dataverseSession.getUser();
        if (user.isAuthenticated()) {
            AuthenticatedUser aUser = (AuthenticatedUser) user;
            unreadNotificationCount = userNotificationRepository.
                    getUnreadNotificationCountByUser(aUser.getId());
        } else {
            unreadNotificationCount = 0L;
        }
        return this.unreadNotificationCount;
    }

    public void initBreadcrumbs(DvObject dvObject, String subPage) {
        breadcrumbs.clear();
        while (dvObject != null) {
            breadcrumbs.add(0, buildBreadcrumbForDvObject(dvObject));
            dvObject = dvObject.getOwner();
        }

        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
        }
    }

    public String logout() {
        samlSessionRegistry.unregister(dataverseSession);
        dataverseSession.logOut();
        dataverseSession.setStatusDismissed(false);

        String redirectPage = navigationWrapper.getPageFromContext();
        try {
            redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            redirectPage = redirectToRoot();
        }

        if (isEmpty(redirectPage)) {
            redirectPage = redirectToRoot();
        }

        return redirectPage + (!redirectPage.contains("?") ? '?' : '&') + "faces-redirect=true";
    }

    public boolean isSignupAllowed() {
        return systemConfig.isSignupAllowed();
    }

    public boolean isRootDataverseThemeDisabled(Dataverse dataverse) {
        if (dataverse == null) {
            return false;
        }
        if (dataverse.isRoot()) {
            // We're operating on the root dataverse.
            return settingsService.isTrueForKey(Key.DisableRootDataverseTheme);
        } else {
            return false;
        }
    }

    public String getSignupUrl(String loginRedirect) {
        String signUpUrl = settingsService.getValueForKey(Key.SignUpUrl);
        return signUpUrl + (!signUpUrl.contains("?") ? loginRedirect : loginRedirect.replace("?", "&"));
    }

    public void addBreadcrumb(String url, String linkString) {
        breadcrumbs.add(new Breadcrumb(url, linkString));
    }

    public void addBreadcrumb(String text) {
        breadcrumbs.add(new Breadcrumb(text));
    }

    // -------------------- PRIVATE --------------------

    private Breadcrumb buildBreadcrumbForDvObject(DvObject dvObject) {
        if (dvObject.isInstanceofDataverse()) {
            return buildBreadcrumbForDataverse((Dataverse) dvObject);
        } else if (dvObject.isInstanceofDataset()) {
            return buildBreadcrumbForDataset((Dataset) dvObject, null);
        } else if (dvObject.isInstanceofDataFile()) {
            return buildBreadcrumbForDatafile((DataFile) dvObject, null);
        }
        throw new IllegalArgumentException("Unknown dvObject type: " 
                + dvObject.getClass().getName());
    }

    private <T extends DvObject> Breadcrumb buildBreadcrumb(T dvObject, String optionalUrlExtension,
                                                            Function<T, String> urlCreator) {
        String url = urlCreator.apply(dvObject);
        if(optionalUrlExtension != null) {
            url = url.concat(optionalUrlExtension);
        }
        if (widgetWrapper.isWidgetTarget(dvObject)) {
            url = widgetWrapper.wrapURL(url);
        }
        boolean openInNewTab = widgetWrapper.isWidgetView() && !widgetWrapper.isWidgetTarget(dvObject);
        return new Breadcrumb(url, dvObject.getDisplayName(), openInNewTab);
    }

    private Breadcrumb buildBreadcrumbForDataverse(Dataverse dataverse) {
        return buildBreadcrumb(dataverse, null, d -> "/dataverse/" + d.getAlias());
    }

    private Breadcrumb buildBreadcrumbForDataset(Dataset dataset, String optionalUrlExtension) {
        return buildBreadcrumb(dataset, optionalUrlExtension, d -> "/dataset.xhtml?persistentId=" + d.getGlobalIdString());
    }

    private Breadcrumb buildBreadcrumbForDatafile(DataFile datafile, String optionalUrlExtension) {
        return buildBreadcrumb(datafile, optionalUrlExtension, d -> "/file.xhtml?fileId=" + d.getId());
    }

    private String redirectToRoot() {
        return "dataverse.xhtml?alias=".
                concat(this.dataverseDao.findRootDataverse().getAlias());
    }


    // -------------------- INNER CLASSES --------------------

    public static class Breadcrumb implements Serializable {

        private final String breadcrumbText;
        private final String url;
        private final boolean openUrlInNewTab;

        public Breadcrumb(final String url, final String breadcrumbText, 
                final boolean openUrlInNewTab) {
            this.url = url;
            this.breadcrumbText = breadcrumbText;
            this.openUrlInNewTab = openUrlInNewTab;
        }

        public Breadcrumb(String url, String breadcrumbText) {
            this(url, breadcrumbText, false);
        }

        public Breadcrumb(String breadcrumbText) {
            this(null, breadcrumbText, false);
        }

        public String getBreadcrumbText() {
            return this.breadcrumbText;
        }

        public String getUrl() {
            return this.url;
        }

        public boolean isOpenUrlInNewTab() {
            return this.openUrlInNewTab;
        }
    }
}