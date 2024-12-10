package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;

@Named
@RequestScoped
public class PrivateUrlDialog {

    private final SettingsWrapper settingsWrapper;
    private final DatasetPage datasetPage;

    private PrivateUrl url;
    private boolean displaySuccess = false;

    @Inject
    public PrivateUrlDialog(final SettingsWrapper settingsWrapper,
            final DatasetPage datasetPage) {
        this.settingsWrapper = settingsWrapper;
        this.datasetPage = datasetPage;
    }
    
    @PostConstruct
    public void init() {
        this.url = datasetPage.getPrivateUrl(false);
    }

    public String getHelpUrl() {
        return this.settingsWrapper.getGuidesBaseUrl() + "/"
                + this.settingsWrapper.getGuidesVersion() +
                "/user/dataset-management.html#private-url-for-reviewing-an-unpublished-dataset";
    }
    
    public String getName() {
        return "prvUrlDlg";
    }
    
    public String getPanelName() {
        return "prvUrlDlgPanel";
    }
    public String getConfirmationName() {
        return "prvUrlConfirmDlg";
    }
    
    public String getHeaderText() {
        return getStringFromBundle("dataset.privateurl.header");
    }
    
    public String getTipText() {
        return getStringFromBundle("dataset.privateurl.tip");
    }

    public String getUrl() {
        return this.url.getLink();
    }

    public boolean isUrlGenerated() {
        return this.url != null;
    }

    public boolean displaySuccessMessage() {
        return this.displaySuccess;
    }

    public boolean displayUrlAbsentMessage() {
        return ! isUrlGenerated();
    }

    public boolean displayUrl() {
        return isUrlGenerated();
    }

    public boolean displayGenerateButton() {
        return ! isUrlGenerated();
    }

    public boolean displayDisableButton() {
        return isUrlGenerated();
    }
    
    public boolean displayPublishedWarning() {
        return false;
    }

    public void generateUrl() {
        this.url = this.datasetPage.createPrivateUrl(false);
        this.displaySuccess = isUrlGenerated();
    }

    public void disableUrl() {
        this.datasetPage.disablePrivateUrl(false);
        this.url = null;
        this.displaySuccess = false;
    }
}
