package edu.harvard.iq.dataverse.dashboard;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;

@SuppressWarnings("serial")
@ViewScoped
@Named("MaximumEmbargoPage")
public class DashboardMaximumEmbargoPage implements Serializable {

    private SettingsServiceBean settings;
    private DataverseSession session;
    private PermissionsWrapper permissionsWrapper;
    private DataverseDao dataverseDao;
    private SystemConfig config;

    private boolean isMaximumEmbargoSet;
    private int maximumEmbargoLength;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardMaximumEmbargoPage() {
    }

    @Inject
    public DashboardMaximumEmbargoPage(final SettingsServiceBean settings, 
                                    final DataverseSession session,
                                    final PermissionsWrapper permissionsWrapper,
                                    final DataverseDao dataverseDao,
                                    final SystemConfig config) {
        this.settings = settings;
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.dataverseDao = dataverseDao;
        this.config = config;
    }

    // -------------------- GETTERS --------------------

    public boolean isMaximumEmbargoSet() {
        return this.isMaximumEmbargoSet;
    }

    public int getMaximumEmbargoLength() {
        return this.maximumEmbargoLength;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        if (!this.session.getUser().isSuperuser() || this.config.isReadonlyMode()) {
            return this.permissionsWrapper.notAuthorized();
        }

        this.isMaximumEmbargoSet = this.settings.getValueForKeyAsInt(Key.MaximumEmbargoLength) > 0;
        this.maximumEmbargoLength = this.settings.getValueForKeyAsInt(Key.MaximumEmbargoLength);

        return EMPTY;
    }

    public String save() {
        if(this.isMaximumEmbargoSet) {
            setMaxEmbargoSetting(this.maximumEmbargoLength);
        } else {
            setMaxEmbargoSetting(0);
        }

        return EMPTY;
    }


    public String cancel() {
        return "/dashboard.xhtml?dataverseId=" 
                + this.dataverseDao.findRootDataverse().getId() + "&faces-redirect=true";
    }

    // -------------------- PRIVATE ---------------------
    private void setMaxEmbargoSetting(final int maxLength) {
        Try.of(() -> this.settings.setValueForKey(Key.MaximumEmbargoLength, Integer.toString(maxLength)))
                .onSuccess(setting -> JsfHelper.addSuccessMessage(getStringFromBundle("dashboard.card.maximumembargo.save.success")))
                .onFailure(setting -> JsfHelper.addErrorMessage(getStringFromBundle("dashboard.card.maximumembargo.save.failure")));
    }

    // -------------------- SETTERS --------------------

    public void setMaximumEmbargoSet(final boolean maximumEmbargoSet) {
        this.isMaximumEmbargoSet = maximumEmbargoSet;
    }

    public void setMaximumEmbargoLength(final int maximumEmbargoLength) {
        this.maximumEmbargoLength = maximumEmbargoLength;
    }
}
