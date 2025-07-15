package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;

import java.io.Serializable;

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
        return isMaximumEmbargoSet;
    }

    public int getMaximumEmbargoLength() {
        return maximumEmbargoLength;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        if (!session.getUser().isSuperuser() || config.isReadonlyMode()) {
            return permissionsWrapper.notAuthorized();
        }

        isMaximumEmbargoSet = settings.getValueForKeyAsInt(Key.MaximumEmbargoLength) > 0;
        maximumEmbargoLength = settings.getValueForKeyAsInt(Key.MaximumEmbargoLength);

        return StringUtils.EMPTY;
    }

    public String save() {
        if(isMaximumEmbargoSet) {
            setMaxEmbargoSetting(maximumEmbargoLength);
        } else {
            setMaxEmbargoSetting(0);
        }

        return StringUtils.EMPTY;
    }


    public String cancel() {
        return "/dashboard.xhtml?dataverseId=" 
                + dataverseDao.findRootDataverse().getId() + "&faces-redirect=true";
    }

    // -------------------- PRIVATE ---------------------
    private void setMaxEmbargoSetting(int maxLength) {
        Try.of(() -> settings.setValueForKey(Key.MaximumEmbargoLength, Integer.toString(maxLength)))
                .onSuccess(setting -> JsfHelper.addSuccessMessage(getStringFromBundle("dashboard.card.maximumembargo.save.success")))
                .onFailure(setting -> JsfHelper.addErrorMessage(getStringFromBundle("dashboard.card.maximumembargo.save.failure")));
    }

    // -------------------- SETTERS --------------------

    public void setMaximumEmbargoSet(boolean maximumEmbargoSet) {
        isMaximumEmbargoSet = maximumEmbargoSet;
    }

    public void setMaximumEmbargoLength(int maximumEmbargoLength) {
        this.maximumEmbargoLength = maximumEmbargoLength;
    }
}
