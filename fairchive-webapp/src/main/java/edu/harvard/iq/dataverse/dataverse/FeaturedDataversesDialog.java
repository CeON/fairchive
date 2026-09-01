package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.FeaturedDataversesSorting.BY_HAND;
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
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("FeaturedDataversesDialog")
public class FeaturedDataversesDialog implements java.io.Serializable {

    private static final Logger logger = getLogger(FeaturedDataversesDialog.class.getCanonicalName());

    private DataverseService dataverseService;
    private FeaturedDataverseServiceBean featuredDataverseService;
    private PermissionsWrapper permissions;
    private UIMessages ui;
    
    private boolean canEditFeaturedDataverses;
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private Dataverse dataverse;
    
    
    public FeaturedDataversesDialog() {}
    
    @Inject
    public FeaturedDataversesDialog(final DataverseService dataverseService,
			final FeaturedDataverseServiceBean featuredDataverseService, 
			final PermissionsWrapper permissions,
			final UIMessages ui) {
		this.dataverseService = dataverseService;
		this.featuredDataverseService = featuredDataverseService;
		this.permissions = permissions;
		this.ui = ui;
	}
    
    public boolean displaySelectors() {
    	return !this.featuredDataverses.getSource().isEmpty() 
    			|| !this.featuredDataverses.getTarget().isEmpty();
    }
    
    public boolean displayTip() {
    	return this.featuredDataverses.getSource().isEmpty() 
    			&& this.featuredDataverses.getTarget().isEmpty();
    }
    
    // -------------------- GETTERS --------------------

	public boolean isCanEditFeaturedDataverses() {
        return this.canEditFeaturedDataverses;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return this.featuredDataverses;
    }

    public Dataverse.FeaturedDataversesSorting getFeaturedDataversesSorting() {
        return this.dataverse.getFeaturedDataversesSorting();
    }

    // -------------------- LOGIC --------------------

    public void init(final Dataverse dataverse) {
        this.canEditFeaturedDataverses = this.permissions.
        		canIssueUpdateDataverseCommand(dataverse);
        
        if (this.canEditFeaturedDataverses) {
            this.dataverse = dataverse;
        }
    }

    public void setupDialog() {
        final List<Dataverse> featuredSource = this.featuredDataverseService.
        		findFeaturableDataverses(this.dataverse.getId());
        final List<Dataverse> featuredTarget = this.featuredDataverseService.
        		findByDataverseId(this.dataverse.getId());

        featuredTarget.forEach(featuredDataverse -> featuredSource.remove(featuredDataverse));

        this.featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);
    }

    public String saveFeaturedDataverse() {
    	try {
    		this.dataverseService.saveFeaturedDataverse(this.dataverse, 
    				this.featuredDataverses.getTarget());
        this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.feature.update"));
    	} catch(final Exception e) {
            logger.log(SEVERE, "Unexpected Exception calling dataverse command", e);
            this.ui.addFlashErrorMessage(getStringFromBundle("dataverse.update.failure"));
    	}
    	
        return redirectToDataverse(this.dataverse.getAlias());
    }

    public void updateSort() {
        final List<Dataverse> target = this.featuredDataverses.getTarget();
        this.featuredDataverses.setTarget(this.featuredDataverseService.
        		sortFeaturedDataverses(target, this.dataverse.getFeaturedDataversesSorting()));
    }

    public void manualReorder() {
        this.dataverse.setFeaturedDataversesSorting(BY_HAND);
    }

    // -------------------- SETTERS --------------------

    public void setFeaturedDataverses(final DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }

    public void setFeaturedDataversesSorting(final Dataverse.FeaturedDataversesSorting featuredDataversesSorting) {
        this.dataverse.setFeaturedDataversesSorting(featuredDataversesSorting);
    }
}
