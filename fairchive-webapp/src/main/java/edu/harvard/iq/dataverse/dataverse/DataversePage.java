package edu.harvard.iq.dataverse.dataverse;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;

import edu.harvard.iq.dataverse.util.JsfRedirectHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Try;


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
        return dataverse;
    }

    public String getDataverseAlias() {
        return dataverseAlias;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public boolean isShowDescriptionAndCarousel() {
        return showDescriptionAndCarousel;
    }

    public List<Dataverse> getCarouselFeaturedDataverses() {
        return carouselFeaturedDataverses;
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

        if (StringUtils.isNotEmpty(this.dataverseAlias)) {
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

        if (!this.dataverse.isReleased() && !this.permissionsWrapper.canViewUnpublishedDataverse(this.dataverse)) {
            return this.permissionsWrapper.notAuthorized();
        }

        this.searchIncludeFragment.search();
        this.linkToDataverseDialog.init(this.dataverse, this.searchIncludeFragment.getQuery(), this.searchIncludeFragment.getFilterQueriesDebug());

        this.featuredDataversesDialog.init(this.dataverse);

        this.showDescriptionAndCarousel = this.searchIncludeFragment.getFilterQueries().isEmpty() && StringUtils.isEmpty(this.searchIncludeFragment.getQuery());
        if (this.showDescriptionAndCarousel) {
        	this.carouselFeaturedDataverses = this.featuredDataverseService.findByDataverseIdQuick(this.dataverse.getId());
        }

        return null;
    }

    public String releaseDataverse() {
        if (!this.session.isUserLoggedIn()) {
            this.uiMessages.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.not.authorized"));
        }

        Try.of(() -> this.dataverseService.publishDataverse(this.dataverse))
                .onFailure(ex -> {
                    logger.error("Unexpected Exception calling  publish dataverse command", ex);
                    this.uiMessages.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));
                })
                .onSuccess(dv -> this.uiMessages.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success")));

        return JsfRedirectHelper.redirectToDataverse(this.dataverse.getAlias());
    }

    public String deleteDataverse() {

        Try.run(() -> dataverseService.deleteDataverse(this.dataverse))
                .onFailure(ex -> {
                    logger.error("Unexpected Exception calling  delete dataverse command", ex);
                    this.uiMessages.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
                })
                .onSuccess(dv -> this.uiMessages.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success")));

        return JsfRedirectHelper.redirectToDataverse(this.dataverse.getOwner().getAlias());
    }

    public Boolean isEmptyDataverse() {
        return !this.dataverseDao.hasData(dataverse);
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return this.dataverse.isAllowMessagesBanners() && (this.session.getUser().isSuperuser() || this.permissionService.isUserAbleToEditDataverse(this.session.getUser(), this.dataverse));
    }

    public String redirectToMetrics() {
        return "/metrics.xhtml?faces-redirect=true";
    }

}
