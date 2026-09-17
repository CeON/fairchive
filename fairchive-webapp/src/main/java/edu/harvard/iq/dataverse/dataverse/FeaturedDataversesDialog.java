package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.util.JsfRedirectHelper.redirectToDataverse;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;

@SuppressWarnings("serial")
@ViewScoped
@Named("FeaturedDataversesDialog")
public class FeaturedDataversesDialog implements java.io.Serializable {

    private static final Logger logger = getLogger(FeaturedDataversesDialog.class.getCanonicalName());

    @Inject
    private DataverseService dataverseService;
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private PermissionsWrapper permissionWrapper;
    
    private boolean canEditFeaturedDataverses;
    private DualListModel<Dataverse> featuredDataverses = 
    		new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private Dataverse dataverse;
    private Dataverse.FeaturedDataversesSorting featuredDataversesSorting;

    // -------------------- GETTERS --------------------

    public boolean isCanEditFeaturedDataverses() {
        return this.canEditFeaturedDataverses;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return this.featuredDataverses;
    }

    public Dataverse.FeaturedDataversesSorting getFeaturedDataversesSorting() {
        return this.featuredDataversesSorting;
    }
    
    public boolean displaySelectors() {
    	return !this.featuredDataverses.getSource().isEmpty() 
    			|| !this.featuredDataverses.getTarget().isEmpty();
    }
    
    public boolean displayTip() {
    	return this.featuredDataverses.getSource().isEmpty() 
    			&& this.featuredDataverses.getTarget().isEmpty();
    }

    // -------------------- LOGIC --------------------

    public void init(final Dataverse dataverse) {
        this.canEditFeaturedDataverses = this.permissionWrapper.
        		canIssueUpdateDataverseCommand(dataverse);
        
        if (this.canEditFeaturedDataverses) {
            this.dataverse = dataverse;
        }
    }

    /**
     * Rebuilds the whole dialog state from what is currently persisted, so that reopening it discards
     * any sorting picked and then cancelled, and the shown order always matches the selected sorting.
     */
    public void setupDialog() {
        final List<Dataverse> featuredSource = this.featuredDataverseService.
        		findFeaturableDataverses(this.dataverse.getId());
        final List<Dataverse> featuredTarget = this.featuredDataverseService.
        		findByDataverseId(this.dataverse.getId());

        featuredTarget.forEach(featuredDataverse -> featuredSource.remove(featuredDataverse));

        this.featuredDataversesSorting = this.dataverse.getFeaturedDataversesSorting();
        this.featuredDataverses = new DualListModel<>(featuredSource,
                this.featuredDataverseService.sortFeaturedDataverses(featuredTarget, 
                		this.featuredDataversesSorting));
    }

	public String saveFeaturedDataverse() {
		try {
			this.dataverse.setFeaturedDataversesSorting(this.featuredDataversesSorting);
			this.dataverseService.saveFeaturedDataverse(this.dataverse, this.featuredDataverses.getTarget());
			JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataverse.feature.update"));
		} catch (final Exception e) {
			logger.log(SEVERE, "Unexpected Exception calling dataverse command", e);
			JsfHelper.addFlashErrorMessage(getStringFromBundle("dataverse.update.failure"), "");
		}
		return redirectToDataverse(this.dataverse.getAlias());
	}

    public void updateSort() {
        List<Dataverse> target = this.featuredDataverses.getTarget();
        this.featuredDataverses.setTarget(this.featuredDataverseService.sortFeaturedDataverses(target, 
        		this.featuredDataversesSorting));
    }

    public void manualReorder() {
        this.featuredDataversesSorting = Dataverse.FeaturedDataversesSorting.BY_HAND;
    }

    // -------------------- SETTERS --------------------

    public void setFeaturedDataverses(final DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public void setFeaturedDataversesSorting(final Dataverse.FeaturedDataversesSorting sorting) {
        this.featuredDataversesSorting = sorting;
    }
}
