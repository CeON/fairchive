package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CollectionFormPrefillEnabled;
import static java.util.Collections.emptyList;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Either;

@SuppressWarnings("serial")
@ViewScoped
@Named("CreateEditDataversePage")
public class CreateEditDataversePage implements Serializable {

    private DataverseService dataverseService;
    private PermissionsWrapper permissionsWrapper;
    private DataverseSession session;
    private SystemConfig config;
    private UIMessages ui;

    private Long dataverseId;
    private Long ownerId;
    private String dataverseOwnerAlias;

    private Dataverse dataverse;
    private DataverseVersion version;
    
    public CreateEditDataversePage() {}
    @Inject
    public CreateEditDataversePage(final DataverseService dataverseService,
    							   final PermissionsWrapper permissionsWrapper,
    							   final DataverseSession session,
    							   final SystemConfig config,
    							   final UIMessages ui) {
    	this.dataverseService = dataverseService;
    	this.permissionsWrapper = permissionsWrapper;
    	this.session = session;
    	this.config = config;
    	this.ui = ui;
    }
    
    // -------------------- GETTERS --------------------
    
    public String getTitle() {
    	return this.dataverse != null && this.dataverse.isNotNew()
    			? getStringFromBundle("dataverse.edit.header") + " - " + this.dataverse.getName()
    			: getStringFromBundle("newDataverse");
    }
    
    public boolean isDataverseNotNew() {
    	return this.dataverse.isNotNew();
    }
    
    public boolean isDataverseNotRoot() {
    	return this.dataverse.isNotRoot();
    }
    
    public String getName() {
    	return this.version.getName();
    }
    
    public void setName(final String name) {
    	this.version.setName(name);
    }
    
    public String getAffiliation() {
    	return this.version.getAffiliation();
    }
    
    public void setAffiliation(final String affiliation) {
    	this.version.setAffiliation(affiliation);
    }
    
    public String getAlias() {
    	return this.version.getAlias();
    }
    
    public void setAlias(final String alias) {
    	this.version.setAlias(alias);
    }
    
    public DataverseType getType() {
        return this.version.getDataverseType();
    }
    
    public void setType(final DataverseType type) {
    	this.version.setDataverseType(type);
    }
    
    public String getDescription() {
    	return this.version.getDescription();
    }
    
    public void setDescription(final String description) {
    	this.version.setDescription(description);
    }
    
    public List<DataverseContact> getContacts() {
    	return this.version.getContacts();
    }
    
    public void addContact(final int index) {
    	this.version.addContact(index);
    }

    public void removeContact(final int index) {
    	this.version.removeContact(index);
    }
    
    public String getAdditionalDescription() {
    	return this.version.getAdditionalDescription();
    }
    
    public void setAdditionalDescription(final String description) {
    	this.version.setAdditionalDescription(description);
    }
    
    public String getMetadataParentRootDataverseName() {
        return this.dataverse.getMetadataParentRootDataverseName();
    }
    
    public String getFacetRootDataverseName() {
        return this.dataverse.getFacetRootDataverseName();
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public String getDataverseOwnerAlias() {
        return this.dataverseOwnerAlias;
    }
    
    public boolean isFacetPickListDisabled() {
    	return !this.dataverse.isFacetRoot() && this.dataverse.isNotRoot();
    }
    
    public boolean isMessagesBannersAllowed() {
        return this.version.isMessagesBannersAllowed();
    }

    public void setMessagesBannersAllowed(final boolean allowed) {
        this.version.setMessagesBannersAllowed(allowed);
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (this.dataverseId == null) {
            return initForCreation();
        } else {
        	return initForEdition();
        }
    }
    
    private String initForCreation() {
    	final Optional<Dataverse> owner = this.dataverseService.findById(this.ownerId);

    	if (!owner.isPresent()) {
    	    return this.permissionsWrapper.notFound();
    	}
    	
        if (!this.permissionsWrapper.canIssueCreateDataverseCommand(owner.get())) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        this.dataverse = new Dataverse();
        this.dataverse.setOwner(owner.get());
        this.version = this.dataverse.getEditableVersion();

        if (isPrefillEnabled()) {
        	final User user = this.session.getUser();
            this.version.addContact(user.getDisplayInfo().getEmailAddress());
            this.version.setAffiliation(user.getDisplayInfo().getAffiliation());
            this.version.setName(Dataverse.getSuggestedNameOnCreate(user));
        } 

        return EMPTY;
    }
    
    private String initForEdition() {
    	final Optional<Dataverse> dataverse = this.dataverseService.findById(this.dataverseId);
    	
        if (! dataverse.isPresent()) {
            return this.permissionsWrapper.notFound();
        }
      
        if (!this.permissionsWrapper.canIssueUpdateDataverseCommand(dataverse.get())) {
            return permissionsWrapper.notAuthorized();
        } 
        
        this.dataverse = dataverse.get();
        this.version = this.dataverse.getEditableVersion();
    	this.ownerId = this.dataverse.getOwnerId();
    	return EMPTY;
    }

    public void validateAlias(final FacesContext context, final 
    		UIComponent toValidate, final Object value) {
    	final String alias = (String) value;
        if (isNotEmpty(alias)) {
            this.dataverseService.findByAlias(alias)
            	.filter(this::isNotCurrentDataverse)
            	.ifPresent(d -> {
            		 ((UIInput) toValidate).setValid(false);
                     final FacesMessage message = new FacesMessage(SEVERITY_ERROR, 
                     		getStringFromBundle("dataverse.alias"), 
                     		getStringFromBundle("dataverse.alias.taken"));
                     context.addMessage(toValidate.getClientId(context), message);
            	});
        }
    }
    
    private boolean isNotCurrentDataverse(final Dataverse found) {
    	return this.dataverse.isNew()
    		? true
    		: !this.dataverse.getId().equals(found.getId());
    }

    public boolean isInheritFacetFromParent() {
        return !this.dataverse.isFacetRoot();
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return this.session.isSuperUserLoggedIn();
    }
    
    
    public String saveAndRelease() {
    	if(canUserPublishCollection()) {
    		this.version.setState(RELEASED);
    		return save();
    	} else {
    		return this.permissionsWrapper.notAuthorized();
    	}
    }

    public String save() {
    	if(session.isUserLoggedIn()) {
	        if (this.dataverse.isNew()) {
	            return saveNewDataverse();
	        } else {
	        	return saveEditedDataverse();
	        }
    	} else {
    		this.ui.addErrorMessage(getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
    		return EMPTY;
    	}
    }   
    
    private String saveEditedDataverse() {
    	
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> editResult = 
        		this.dataverseService.saveEditedDataverse(emptyList(), 
        				this.dataverse, new DualListModel<DatasetFieldType>());

        if (editResult.isLeft()) {
            this.ui.addErrorMessage(editResult.getLeft().getErrorMsg(), EMPTY);
            return EMPTY;
        } else {
        	this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.update.success"));
        	return returnRedirect();
        }
    }

    private String saveNewDataverse() {
    	
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> saveResult = 
        		this.dataverseService.saveNewDataverse(emptyList(), 
        				this.dataverse, new DualListModel<DatasetFieldType>());

        if (saveResult.isLeft()) {
            this.ui.addErrorMessage(saveResult.getLeft().getErrorMsg(), EMPTY);
            return EMPTY;
        } else {
            this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.create.success",
                    this.config.getGuidesBaseUrl(this.session.getLocale()),
                    this.config.getGuidesVersion()));
        	return "/dataverse.xhtml?faces-redirect=true&alias=".
        			concat(saveResult.get().getAlias());
        }
    }

    public String cancel() {
        return returnRedirect();
    }

    private String returnRedirect() {
    	return "/dataverse.xhtml?faces-redirect=true&alias=".concat(
    			this.dataverse.isNew() ? this.dataverseOwnerAlias : this.dataverse.getAlias());
    }

    // -------------------- SETTERS --------------------

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseId(final Long id) {
        this.dataverseId = id;
    }

    public void setOwnerId(final Long id) {
        this.ownerId = id;
    }

    public void setInheritFacetFromParent(final boolean inherit) {
        this.dataverse.setFacetRoot(!inherit);
    }

    public void setDataverseOwnerAlias(final String alias) {
        this.dataverseOwnerAlias = alias;
    }

    // -------------------- PRIVATE --------------------

    private boolean isPrefillEnabled() {
        return this.config.isTrueForKey(CollectionFormPrefillEnabled);
    }
    
    public boolean canUserPublishCollection() {
    	return this.permissionsWrapper.canIssuePublishDataverseCommand(this.dataverse.getOwner());
    }
}
