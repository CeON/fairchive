package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.util.JsfRedirectHelper.redirectToDataverse;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.validator.routines.EmailValidator;
import org.omnifaces.cdi.Param;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.featured.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;


@SuppressWarnings("serial")
@ViewScoped
@Named("DataversePage")
public class DataversePage implements Serializable  {

    private static final Logger logger = getLogger(DataversePage.class);

    private DataverseRepository dataverses;
    private DataverseService dataverseService;
    private FeaturedDataverseServiceBean featuredDataverseService;
    private PermissionServiceBean permissionService;
    private PermissionsWrapper permissionsWrapper;
    private DataverseSession session;
    private SearchIncludeFragment searchIncludeFragment;
    private LinkToDataverseDialog linkToDataverseDialog;
    private FeaturedDataversesDialog featuredDataversesDialog;
    private SystemConfig sysConfig;
    private UIMessages uiMessages;

    @Inject @Param(name = "alias")
    private String dataverseAlias;
    @Inject @Param(name = "id")
    private Long dataverseId;

    private Dataverse dataverse;

    private boolean showDescriptionAndCarousel;
    private List<Dataverse> carouselFeaturedDataverses = new ArrayList<>();
    private final SubmitForReviewDialog submitForReviewDialog = new SubmitForReviewDialog();
    private final SendBackToContributorDialog sendBackToContributorDialog =
    		new SendBackToContributorDialog();
    
    public DataversePage() {}
    
    @Inject
    public DataversePage(final DataverseRepository dataverses,
    		final DataverseService dataverseService,
    		final FeaturedDataverseServiceBean featuredDataverseService,
    		final PermissionServiceBean permissionService,
    		final PermissionsWrapper permissionsWrapper,
    		final DataverseSession session,
    		final SearchIncludeFragment searchIncludeFragment,
    		final LinkToDataverseDialog linkToDataverseDialog,
    		final FeaturedDataversesDialog featuredDataversesDialog,
    		final SystemConfig sysConfig,
    		final UIMessages uiMessages) {
    	
    	this.dataverses = dataverses;
    	this.dataverseService = dataverseService;
    	this.featuredDataverseService = featuredDataverseService;
    	this.permissionService = permissionService;
    	this.permissionsWrapper = permissionsWrapper;
    	this.session = session;
    	this.searchIncludeFragment = searchIncludeFragment;
    	this.linkToDataverseDialog = linkToDataverseDialog;
    	this.featuredDataversesDialog = featuredDataversesDialog;
    	this.sysConfig = sysConfig;
    	this.uiMessages = uiMessages;
    }
    

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

    public void setDataverseAlias(String dataverseAlias) {
		this.dataverseAlias = dataverseAlias;
	}

	public void setDataverseId(Long dataverseId) {
		this.dataverseId = dataverseId;
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
		return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse) &&
				(!this.dataverse.isReleased() || this.dataverse.hasDraft());
	}
	
	public boolean displayEditButton() {
		return this.permissionsWrapper.canIssueUpdateDataverseCommand(this.dataverse);
	}
	
	public boolean displayDeleteMenuItem() {
		return isEmptyDataverse() 
				&& this.dataverse.getOwner() != null
				&& this.permissionsWrapper.canIssueDeleteDataverseCommand(this.dataverse);
	}
	
    public boolean displayReturnToAuthorButton() {
    	return this.dataverse.hasDraft()
    			&& this.dataverse.isInReview()
    			&& canUserPublishCollection();
    }
    
    public boolean displaySubmitForReviewButton() {
    	return this.dataverse.isDraft() && !canUserPublishCollection();
    }
    
    public boolean disabledSubmitForReviewButton() {
    	return this.dataverse.isInReview();
    }

    public boolean isDataverseInReview() {
    	return this.dataverse.isInReview();
    }
	
	public String getReleaseDialogName() {
		return this.dataverse.canBeReleased() ? "confirmation" : "mayNotRelease";
	}
	
	public SubmitForReviewDialog getSubmitForReviewDialog() {
		return this.submitForReviewDialog;
	}
	
	public SendBackToContributorDialog getSendBackToContributorDialog() {
		return this.sendBackToContributorDialog;
	}

    // -------------------- LOGIC --------------------
    public String init() {
    	
        if (isNotEmpty(this.dataverseAlias)) {
        	this.dataverse = this.dataverses.findByAlias(this.dataverseAlias).orElse(null);
        } else if(this.dataverseId != null) {
        	this.dataverse = this.dataverses.getById(this.dataverseId);
        } else {
        	this.dataverse = this.dataverses.findRoot();
        }
        if (this.dataverse == null) {
        	this.permissionsWrapper.notFound();
        }
        if (!isUserAuthorizedToView()) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        this.linkToDataverseDialog.init(this.dataverse, 
        		this.searchIncludeFragment.getQuery(),
        		this.searchIncludeFragment.getFilterQueriesDebug());
        this.featuredDataversesDialog.init(this.dataverse);
        
        this.searchIncludeFragment.setDataverse(this.dataverse);
        this.searchIncludeFragment.search();
        this.showDescriptionAndCarousel = this.searchIncludeFragment.getFilterQueries().isEmpty() 
        		&& isEmpty(this.searchIncludeFragment.getQuery());
        if (this.showDescriptionAndCarousel) {
        	this.carouselFeaturedDataverses = this.featuredDataverseService.
        			findByDataverseIdQuick(this.dataverse.getId());
        }

        return null;
    }

    public String releaseDataverse() {
        if (this.session.isUserLoggedIn()) {
            try {
            	this.dataverse = this.dataverseService.publish(this.dataverse);
            	addSuccessMessage("dataverse.publish.success");
            } catch (final Exception e) {
                logger.error("Unexpected Exception calling  publish dataverse command", e);
                addErrorMessage("dataverse.publish.failure");
            }
        } else {
            addErrorMessage("dataverse.publish.not.authorized");
        }
        return redirectToDataverse(this.dataverse.getAlias());
    }

	public String deleteDataverse() {
		if (this.session.isUserLoggedIn()) {
			try {
				this.dataverseService.deleteDataverse(this.dataverse);
				addSuccessMessage("dataverse.delete.success");
			} catch (final Exception e) {
				logger.error("Unexpected Exception calling  delete dataverse command", e);
				addErrorMessage("dataverse.delete.failure");
			}
		} else {
			addErrorMessage("dataverse.publish.not.authorized");
		}
		return redirectToDataverse(this.dataverse.getOwner().getAlias());
	}

    public Boolean isEmptyDataverse() {
        return this.dataverses.isEmpty(this.dataverse);
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return this.dataverse.isAllowMessagesBanners() 
        		&& (this.session.isSuperUserLoggedIn()
        		|| this.permissionService.isUserAbleToEditDataverse(this.session.getUser(), this.dataverse));
    }

    public String redirectToMetrics() {
        return "/metrics.xhtml?faces-redirect=true";
    }
    
    private void addSuccessMessage(final String key) {
    	this.uiMessages.addFlashSuccessMessage(getStringFromBundle(key));
    }
    
    private void addErrorMessage(final String key) {
    	this.uiMessages.addFlashErrorMessage(getStringFromBundle(key));
    }
    
    public boolean canUserPublishCollection() {
    	return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse);
    }
    
    private boolean isUserAuthorizedToView() {
		return this.dataverse.isReleased()
				? true
				: this.permissionsWrapper.canViewUnpublishedDataverse(this.dataverse);
    }
    
    //--------------------------------------------------------------------------
    public class SubmitForReviewDialog {
    	
    	private String message = EMPTY;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(final String message) {
			this.message = message;
		}
		
		public void clear() {
			this.message = EMPTY;
		}
    	
		public void send() {
			try {
				dataverseService.submitForReview(dataverse, this.message);
				uiMessages.addFlashSuccessMessage(
						getStringFromBundle("dataset.submit.success"));
			} catch (final Exception e) {
				uiMessages.addFlashErrorMessage(
						getStringFromBundle("dataset.submit.failure", e.getMessage()));
			}
		}
    }
    
    //--------------------------------------------------------------------------
    public class SendBackToContributorDialog {
    	
    	private String message = EMPTY;
    	private String replyTo = EMPTY;
    	private boolean sendCopy = false;

		public String getMessage() {
			return this.message;
		}

		public void setMessage(final String message) {
			this.message = message;
		}
		
	    public String getReplyTo() {
	        return this.replyTo;
	    }

	    public void setReplyTo(final String replyTo) {
	        this.replyTo = replyTo;
	    }
		
	    public boolean isSendCopy() {
	        return this.sendCopy;
	    }

	    public void setSendCopy(final boolean sendCopy) {
	        this.sendCopy = sendCopy;
	    }
		
		public void clear() {
			this.message = EMPTY;
			this.replyTo = EMPTY;
			this.sendCopy = false;
		}
		
	    public void validateReplyToEmail(final FacesContext context, 
	    		final UIComponent component, final Object value) 
	    				throws ValidatorException {
	        final String email = (String) value;
	        if(isNoneBlank(email) && !EmailValidator.getInstance().isValid(email)) {
	            throw new ValidatorException(new FacesMessage(SEVERITY_ERROR,
	                    getStringFromBundle("email.invalid", email), EMPTY));
	        }
	    }
    	
		public void send() {
			try {
		        dataverseService.sendBackToContributor(dataverse, this.message, 
		        		this.replyTo, this.sendCopy);
				uiMessages.addFlashSuccessMessage(
						getStringFromBundle("dataverse.reject.success"));
			} catch (final Exception e) {
				uiMessages.addFlashErrorMessage(
						getStringFromBundle("dataverse.reject.failure", e.getMessage()));
			}
		}
    }
}
