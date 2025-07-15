package edu.harvard.iq.dataverse.dashboard;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.NavigationWrapper;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("MaximumEmbargoPage")
public class DashboardMaximumEmbargoPage implements Serializable {

    private SettingsServiceBean settings;
    private DataverseSession session;
    private NavigationWrapper navigation;
    private DataverseDao dataverseDao;
    private UIMessages ui;

    private boolean isMaximumEmbargoSet;
    private int maximumEmbargoLength;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardMaximumEmbargoPage() {
    }

    @Inject
    public DashboardMaximumEmbargoPage(final SettingsServiceBean settings, 
                                    final DataverseSession session,
                                    final NavigationWrapper navigation,
                                    final DataverseDao dataverseDao,
                                    final UIMessages ui) {
        this.settings = settings;
        this.session = session;
        this.navigation = navigation;
        this.dataverseDao = dataverseDao;
        this.ui = ui;
    }

    // -------------------- GETTERS --------------------

    public boolean isMaximumEmbargoSet() {
        return this.isMaximumEmbargoSet;
    }

    public int getMaximumEmbargoLength() {
        return this.maximumEmbargoLength;
    }

    // -------------------- LOGIC --------------------
    @PostConstruct
    public void init() {
        this.maximumEmbargoLength = this.settings
                .getValueForKeyAsInt(Key.MaximumEmbargoLength);
        this.isMaximumEmbargoSet = this.maximumEmbargoLength > 0;
    }
    
    public String verifyAccess() {
        return this.session.canEditDashboard() ? EMPTY : this.navigation.notAuthorized();
    }

    public String save() {
        try {
            final String maxEngth = this.isMaximumEmbargoSet
                    ? Integer.toString(this.maximumEmbargoLength)
                    : "0";
            this.settings.setValueForKey(Key.MaximumEmbargoLength, maxEngth);
            this.ui.addSuccessMessage(
                    getStringFromBundle("dashboard.card.maximumembargo.save.success"));
        } catch (final Exception e) {
            this.ui.addErrorMessage(
                    getStringFromBundle("dashboard.card.maximumembargo.save.failure"));
        }
        return EMPTY;
    }

    public String cancel() {
        return "/dashboard.xhtml?faces-redirect=true&dataverseId=" 
                + this.dataverseDao.findRootDataverse().getId();
    }

    // -------------------- SETTERS --------------------

    public void setMaximumEmbargoSet(final boolean maximumEmbargoSet) {
        this.isMaximumEmbargoSet = maximumEmbargoSet;
    }

    public void setMaximumEmbargoLength(final int maximumEmbargoLength) {
        this.maximumEmbargoLength = maximumEmbargoLength;
    }
}
