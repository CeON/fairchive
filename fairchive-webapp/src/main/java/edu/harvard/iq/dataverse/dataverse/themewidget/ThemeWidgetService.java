package edu.harvard.iq.dataverse.dataverse.themewidget;

import java.nio.file.Path;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.TempLogoStorageManager;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

@Stateless
public class ThemeWidgetService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ThemeWidgetService() {
    }

    @Inject
    public ThemeWidgetService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------

    public Dataverse saveOrUpdateUploadedTheme(Dataverse dataverse, TempLogoStorageManager.Storage storage) {
        return commandEngine.submit(new UpdateDataverseThemeCommand(dataverse, storage, dvRequestService.getDataverseRequest()));
    }

    public Dataverse inheritThemeFromRoot(Dataverse dataverse) {
    	//TODO: verify whether this is correct behavior
        dataverse.getLatestVersion().setTheme(null);

        return commandEngine.submit(new UpdateDataverseThemeCommand(dataverse, null, dvRequestService.getDataverseRequest()));
    }
}
