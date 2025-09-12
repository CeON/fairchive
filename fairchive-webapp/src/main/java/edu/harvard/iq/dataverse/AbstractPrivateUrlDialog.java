package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import javax.inject.Inject;
import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@SuppressWarnings("serial")
abstract class AbstractPrivateUrlDialog implements Serializable {

    @Inject
    private DatasetPage datasetPage;

    private PrivateUrl url;

    private boolean displaySuccess = false;

    public void init() {
        this.url = this.datasetPage.getPrivateUrl(isAnonymized());
    }

    public String getHelpUrl() {
        return this.datasetPage.getPrivateUrlHelpUrl();
    }

    public String getUrl() {
        return isUrlGenerated() ? this.url.getLink() : EMPTY;
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
        return isAnonymized() && this.datasetPage.isExistReleasedVersion();
    }

    public void generateUrl() {
        this.url = this.datasetPage.createPrivateUrl(isAnonymized());
        this.displaySuccess = isUrlGenerated();
    }

    public void disableUrl() {
        this.datasetPage.disablePrivateUrl(isAnonymized());
        this.url = null;
        this.displaySuccess = false;
    }

    abstract public boolean isAnonymized();
}
