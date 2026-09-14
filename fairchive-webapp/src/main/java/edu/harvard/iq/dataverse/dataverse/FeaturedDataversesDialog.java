package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.JsfRedirectHelper;
import io.vavr.control.Try;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
@ViewScoped
@Named("FeaturedDataversesDialog")
public class FeaturedDataversesDialog implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(FeaturedDataversesDialog.class.getCanonicalName());

    @Inject
    private DataverseService dataverseService;
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private PermissionsWrapper permissionWrapper;
    
    private boolean canEditFeaturedDataverses;
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private Dataverse dataverse;
    private Dataverse.FeaturedDataversesSorting featuredDataversesSorting;

    // -------------------- GETTERS --------------------

    public boolean isCanEditFeaturedDataverses() {
        return canEditFeaturedDataverses;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    public Dataverse.FeaturedDataversesSorting getFeaturedDataversesSorting() {
        return featuredDataversesSorting;
    }

    // -------------------- LOGIC --------------------

    public void init(Dataverse dataverse) {
        canEditFeaturedDataverses = permissionWrapper.canIssueUpdateDataverseCommand(dataverse);
        
        if (canEditFeaturedDataverses) {
            this.dataverse = dataverse;
        }
    }

    /**
     * Rebuilds the whole dialog state from what is currently persisted, so that reopening it discards
     * any sorting picked and then cancelled, and the shown order always matches the selected sorting.
     */
    public void setupDialog() {
        List<Dataverse> featuredSource = featuredDataverseService.findFeaturableDataverses(dataverse.getId());
        List<Dataverse> featuredTarget = featuredDataverseService.findByDataverseId(dataverse.getId());

        featuredTarget.forEach(featuredDataverse -> featuredSource.remove(featuredDataverse));

        featuredDataversesSorting = dataverse.getFeaturedDataversesSorting();
        featuredDataverses = new DualListModel<>(featuredSource,
                featuredDataverseService.sortFeaturedDataverses(featuredTarget, featuredDataversesSorting));
    }

    public String saveFeaturedDataverse() {

        dataverse.setFeaturedDataversesSorting(featuredDataversesSorting);

        Try.of(() -> dataverseService.saveFeaturedDataverse(dataverse, featuredDataverses.getTarget()))
                .onSuccess(savedDataverse -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.feature.update")))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.update.failure"), "");
                });

        return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
    }

    public void updateSort() {
        List<Dataverse> target = featuredDataverses.getTarget();
        featuredDataverses.setTarget(featuredDataverseService.sortFeaturedDataverses(target, featuredDataversesSorting));
    }

    public void manualReorder() {
        featuredDataversesSorting = Dataverse.FeaturedDataversesSorting.BY_HAND;
    }

    // -------------------- SETTERS --------------------

    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public void setFeaturedDataversesSorting(Dataverse.FeaturedDataversesSorting featuredDataversesSorting) {
        this.featuredDataversesSorting = featuredDataversesSorting;
    }
}
