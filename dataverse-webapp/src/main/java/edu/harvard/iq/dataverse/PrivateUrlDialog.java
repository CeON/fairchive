package edu.harvard.iq.dataverse;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.privateurl.PrivateUrl;

@SuppressWarnings("serial")
public class PrivateUrlDialog implements Serializable {

    private final DatasetPage datasetPage;
    private final boolean anonymized;
    
    private PrivateUrl url;
    private boolean displaySuccess = false;

    public PrivateUrlDialog(final DatasetPage datasetPage, final boolean anonymized) {
        this.datasetPage = datasetPage;
        this.anonymized = anonymized;
    }
    
    @PostConstruct
    public void init() {
        this.url = this.datasetPage.getPrivateUrl(this.anonymized);
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
        return this.anonymized && this.datasetPage.isExistReleasedVersion();
    }

    public void generateUrl() {
        this.url = this.datasetPage.createPrivateUrl(this.anonymized);
        this.displaySuccess = isUrlGenerated();
    }

    public void disableUrl() {
        this.datasetPage.disablePrivateUrl(this.anonymized);
        this.url = null;
        this.displaySuccess = false;
    }

    //--------------------------------------------------------------------------
    @ApplicationScoped
    public static class Factory implements Serializable {
        
        private final Instance<DatasetPage> datasetPage;
        
        @Inject
        public Factory(final Instance<DatasetPage> datasetPage) {
            this.datasetPage = datasetPage;
        }
        @Produces
        @ViewScoped
        @Named("privateUrlDialog")
        public PrivateUrlDialog createPrivateUrlDialog() {
            return new PrivateUrlDialog(this.datasetPage.get(), false);
        }
        @Produces
        @ViewScoped
        @Named("anonymizedPrivateUrlDialog")
        public PrivateUrlDialog createAnonymizedPrivateUrlDialog() {
            return new PrivateUrlDialog(this.datasetPage.get(), true);
        }
    }
}
