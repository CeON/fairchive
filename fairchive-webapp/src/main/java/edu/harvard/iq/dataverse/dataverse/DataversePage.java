package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.util.JsfRedirectHelper.redirectToDataverse;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;


/**
 * @author gdurand
 */
@RequestScoped
@Named("DataversePage")
public class DataversePage {

    private static final Logger logger = LoggerFactory.getLogger(DataversePage.class);

    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private DataverseService dataverseService;
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private PermissionServiceBean permissionService;
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseSession session;
    @Inject
    private SearchIncludeFragment searchIncludeFragment;
    @Inject
    private LinkToDataverseDialog linkToDataverseDialog;
    @Inject
    private FeaturedDataversesDialog featuredDataversesDialog;
    @Inject 
    private SystemConfig sysConfig;
    @Inject
    private UIMessages uiMessages;

    @Inject @Param(name = "alias")
    private String dataverseAlias;
    @Inject @Param(name = "id")
    private Long dataverseId;

    private Dataverse dataverse;

    private boolean showDescriptionAndCarousel;
    private List<Dataverse> carouselFeaturedDataverses = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public String getDataverseAlias() {
        return this.dataverseAlias;
    }

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public boolean isShowDescriptionAndCarousel() {
        return this.showDescriptionAndCarousel;
    }

    public List<Dataverse> getCarouselFeaturedDataverses() {
        return this.carouselFeaturedDataverses;
    }
    
    public boolean displayMetrics() {
    	return this.dataverse.isRoot() && !this.sysConfig.isRsyncOnly();
    }
    
	public boolean displayEditPublishButtons() {
		return this.permissionsWrapper.canIssueUpdateDataverseCommand(this.dataverse)
				|| this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse);
	}
	
	public boolean displayPublishButton() {
		return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse);
	}
	
	public boolean displayEditButton() {
		return this.permissionsWrapper.canIssueUpdateDataverseCommand(this.dataverse);
	}
	
	public boolean displayDeleteMenuItem() {
		return isEmptyDataverse() 
				&& this.dataverse.getOwner() != null
				&& this.permissionsWrapper.canIssueDeleteDataverseCommand(this.dataverse);
	}

    // -------------------- LOGIC --------------------
    @PostConstruct
    public void postConstruct() {

        if (isNotEmpty(this.dataverseAlias)) {
        	this.dataverse = dataverseDao.findByAlias(this.dataverseAlias);
        } else if(this.dataverseId != null) {
        	this.dataverse = dataverseDao.find(this.dataverseId);
        } else {
        	this.dataverse = this.dataverseDao.findRootDataverse();
        }
        if (this.dataverse == null) {
        	this.permissionsWrapper.notFound();
        }

        this.searchIncludeFragment.setDataverse(this.dataverse);
    }

    public String init() {

        if (!this.dataverse.isReleased() 
        		&& !this.permissionsWrapper.canViewUnpublishedDataverse(this.dataverse)) {
            return this.permissionsWrapper.notAuthorized();
        }

        this.searchIncludeFragment.search();
        this.linkToDataverseDialog.init(this.dataverse, 
        		this.searchIncludeFragment.getQuery(),
        		this.searchIncludeFragment.getFilterQueriesDebug());

        this.featuredDataversesDialog.init(this.dataverse);

        this.showDescriptionAndCarousel = this.searchIncludeFragment.getFilterQueries().isEmpty() 
        		&& isEmpty(this.searchIncludeFragment.getQuery());
        if (this.showDescriptionAndCarousel) {
        	this.carouselFeaturedDataverses = this.featuredDataverseService.findByDataverseIdQuick(this.dataverse.getId());
        }

        return null;
    }

    public String releaseDataverse() {
        if (!this.session.isUserLoggedIn()) {
            this.uiMessages.addFlashErrorMessage(getStringFromBundle("dataverse.publish.not.authorized"));
        }
        
        try {
        	this.dataverseService.publishDataverse(this.dataverse);
        	this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataverse.publish.success"));
        } catch (final Exception e) {
            logger.error("Unexpected Exception calling  publish dataverse command", e);
            this.uiMessages.addFlashErrorMessage(getStringFromBundle("dataverse.publish.failure"));
        }
        return redirectToDataverse(this.dataverse.getAlias());
    }

    public String deleteDataverse() {

    	try {
    		this.dataverseService.deleteDataverse(this.dataverse);
    		this.uiMessages.addFlashSuccessMessage(getStringFromBundle("dataverse.delete.success"));
    	} catch (final Exception e) {
            logger.error("Unexpected Exception calling  delete dataverse command", e);
            this.uiMessages.addFlashErrorMessage(getStringFromBundle("dataverse.delete.failure"));
    	}
        return redirectToDataverse(this.dataverse.getOwner().getAlias());
    }

    public Boolean isEmptyDataverse() {
        return !this.dataverseDao.hasData(dataverse);
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return this.dataverse.isAllowMessagesBanners() 
        		&& (this.session.isSuperUserLoggedIn()
        		|| this.permissionService.isUserAbleToEditDataverse(this.session.getUser(), this.dataverse));
    }

    public String redirectToMetrics() {
        return "/metrics.xhtml?faces-redirect=true";
    }

}
