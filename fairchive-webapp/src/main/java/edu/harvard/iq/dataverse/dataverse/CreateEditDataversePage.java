package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion.State.RELEASED;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.CollectionFormPrefillEnabled;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Either;

@SuppressWarnings("serial")
@ViewScoped
@Named("CreateEditDataversePage")
public class CreateEditDataversePage implements Serializable {

    private DatasetFieldServiceBean datasetFieldService;
    private MetadataBlockService metadataBlockService;
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
    private Long facetMetadataBlockId;
    private Set<MetadataBlock> allMetadataBlocks;
    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());

    private DataverseMetaBlockOptions mdbOptions = new DataverseMetaBlockOptions();
    private DataverseMetaBlockOptions parentRootMdbOptions = new DataverseMetaBlockOptions();

    
    public CreateEditDataversePage() {}
    @Inject
    public CreateEditDataversePage(final DataverseService dataverseService,
    							   final DatasetFieldServiceBean datasetFieldService,
    							   final MetadataBlockService metadataBlockService,
    							   final PermissionsWrapper permissionsWrapper,
    							   final DataverseSession session,
    							   final SystemConfig config,
    							   final UIMessages ui) {
    	this.dataverseService = dataverseService;
    	this.datasetFieldService = datasetFieldService;
    	this.metadataBlockService = metadataBlockService;
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

    public Long getFacetMetadataBlockId() {
        return this.facetMetadataBlockId;
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return this.facets;
    }

    public List<MetadataBlock> getAllMetadataBlocks() {
        return new ArrayList<>(this.allMetadataBlocks);
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
    
    public boolean showFieldTypes(final MetadataBlock block) {
    	return this.mdbOptions.isShowDatasetFieldTypes(block.getId());
    }
    
    public boolean isSelectionDisabledFor(final MetadataBlock block, 
    		final DatasetFieldType type) {
    	return (!type.isHasParent() && (type.isHasRequiredChildren() || type.isRequired())) 
                || !this.mdbOptions.getMdbViewOptions().get(block.getId()).isEditableDatasetFieldTypes()
                || this.mdbOptions.isInheritMetaBlocksFromParent();
    }
    
    public List<SelectItem> getSelectedDatasetFieldsOf(final DatasetFieldType type) {
    	return this.mdbOptions.getDatasetFieldViewOptions().get(type.getId()).
    			getSelectedDatasetFields();
    }
    
    public boolean radioButtonsDisabledFor(final MetadataBlock block) {
    	return !this.mdbOptions.isEditableDatasetFieldTypes(block.getId())
                || this.mdbOptions.isInheritMetaBlocksFromParent();
    }
    
    public boolean isEditableDatasetFieldTypes(final MetadataBlock block) {
        return this.mdbOptions.isEditableDatasetFieldTypes(block.getId());
    }
    
    /**
     * Indicates if metadata blocks are inherited from parent dataverse or if they are being edited by user.
     */
    public boolean isInheritMetaBlocksFromParent() {
        return this.mdbOptions.isInheritMetaBlocksFromParent();
    }
    
    public void setInheritMetaBlocksFromParent(final boolean inherit) {
        this.mdbOptions.setInheritMetaBlocksFromParent(inherit);
    }
    
	public MetadataBlockViewOptions getBlockViewOptionsFor(final MetadataBlock block) {
		return this.mdbOptions.getMdbViewOptions().get(block.getId());
	}
	
	public DatasetFieldViewOptions getFieldTypeViewOptionsFor(final DatasetFieldType type) {
		return this.mdbOptions.getDatasetFieldViewOptions().get(type.getId());
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

        initFacets();
        initMetadataBlocks();
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
    	initFacets();
    	initMetadataBlocks();
    	return EMPTY;
    }
    
    private void initMetadataBlocks() {
    	
        this.allMetadataBlocks = this.metadataBlockService.
        		prepareMetaBlocksAndDatasetfields(this.dataverse, this.mdbOptions);

        final Dataverse dataverse = this.dataverse.isRoot()
        		? this.dataverse
        		: this.dataverse.getOwner();
        
        this.metadataBlockService.prepareMetaBlocksAndDatasetfields(dataverse,
        		this.parentRootMdbOptions);
    }

    private void initFacets() {
        final List<DatasetFieldType> facetsTarget = new ArrayList<>();
        final List<DatasetFieldType> facetsSource = 
        		this.datasetFieldService.findAllFacetableFieldTypes();
        for (final DataverseFacet facet : this.dataverse.getFacets()) {
            final DatasetFieldType type = facet.getDatasetFieldType();
            facetsTarget.add(type);
            facetsSource.remove(type);
        }
        this.facets = new DualListModel<>(facetsSource, facetsTarget);
        this.facetMetadataBlockId = null;
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

    public void refreshDatasetFieldTypes(final Long mdbId, final boolean editable, 
    		final boolean fieldsVisible) {
        this.metadataBlockService.refreshDatasetFieldTypes(this.mdbOptions, 
        		mdbId, editable, fieldsVisible);
    }
    
    public boolean displayEditableDatasetFieldTypesLinkFor(final MetadataBlock block) {
    	return !showFieldTypes(block)
    			&& (this.mdbOptions.getMdbViewOptions().get(block.getId()).isSelected()
    					|| block.isCitationMetaBlock())
    			&& !isInheritMetaBlocksFromParent();
    }
    
    /**
     * Changes metadata block view options in order to show editable dataset fields for given metadata block.
     */
    public void showEditableDatasetFieldTypes(final MetadataBlock block) {
        this.metadataBlockService.prepareDatasetFieldsToBeEditable(this.mdbOptions, 
        		block.getId());
    }
    
    public boolean displayUnEditableDatasetFieldTypesLinkFor(final MetadataBlock block) {
    	return ! showFieldTypes(block)
                && (!this.mdbOptions.getMdbViewOptions().get(block.getId()).isSelected() ||
                		isInheritMetaBlocksFromParent());
    }

    /**
     * Changes metadata block view options in order to show uneditable dataset fields for given metadata block.
     */
    public void showUnEditableDatasetFieldTypes(final MetadataBlock block) {
        this.metadataBlockService.prepareDatasetFieldsToBeUnEditable(this.mdbOptions,
        		block.getId());
    }

    /**
     * Changes metadata block view options in order to hide all dataset fields for given metadata block.
     */
    public void hideDatasetFieldTypes(final Long mdbId) {
        this.metadataBlockService.prepareDatasetFieldsToBeHidden(this.mdbOptions, mdbId);
    }

    /**
     * Resets all view options for metadata blocks, dataset fields and sets metadata blocks to be inherited from parent.
     */
    public String resetToInherit() {
        this.mdbOptions = this.parentRootMdbOptions.deepCopy();
        this.mdbOptions.setInheritMetaBlocksFromParent(true);
        this.dataverse.setMetadataBlockRoot(false);
        return EMPTY;
    }

    public boolean isInheritFacetFromParent() {
        return !this.dataverse.isFacetRoot();
    }

    public void toggleFacetRoot() {
        if (!this.dataverse.isFacetRoot()) {
            initFacets();
        }
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
	        final List<DataverseFieldTypeInputLevel> dftilForSave =
	                this.metadataBlockService.getDataverseFieldTypeInputLevelsToBeSaved(
	                		this.allMetadataBlocks, this.mdbOptions, this.dataverse);
	
	        if (this.dataverse.isNew()) {
	            return saveNewDataverse(dftilForSave);
	        } else {
	        	return saveEditedDataverse(dftilForSave);
	        }
    	} else {
    		this.ui.addErrorMessage(getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
    		return EMPTY;
    	}
    }   
    
    private String saveEditedDataverse(final List<DataverseFieldTypeInputLevel> dftilForSave) {
    	
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> editResult = 
        		this.dataverseService.saveEditedDataverse(dftilForSave, 
        				this.dataverse, this.facets);

        if (editResult.isLeft()) {
            this.ui.addErrorMessage(editResult.getLeft().getErrorMsg(), EMPTY);
            return EMPTY;
        } else {
        	this.ui.addFlashSuccessMessage(getStringFromBundle("dataverse.update.success"));
        	return returnRedirect();
        }
    }

    private String saveNewDataverse(final List<DataverseFieldTypeInputLevel> dftilForSave) {
    	
    	this.dataverse.addVersion(this.version);
        final Either<DataverseError, Dataverse> saveResult = 
        		this.dataverseService.saveNewDataverse(dftilForSave, 
        				this.dataverse, this.facets);

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

    public void changeFacetsMetadataBlock() {
        if (this.facetMetadataBlockId == null) {
            this.facets.setSource(this.datasetFieldService.findAllFacetableFieldTypes());
        } else {
            this.facets.setSource(this.datasetFieldService.
            		findFacetableFieldTypesByMetadataBlock(this.facetMetadataBlockId));
        }

        this.facets.getSource().removeAll(this.facets.getTarget());
    }

    /**
     * Refreshes dataset fields view options for given dataset field.
     */
    public void refreshDatasetFieldsViewOptions(final MetadataBlock block, 
    		final DatasetFieldType type) {
        this.metadataBlockService.refreshDatasetFieldsViewOptions(block.getId(), 
        		type.getId(), this.allMetadataBlocks, this.mdbOptions);
    }

    /**
     * Makes metadata block selectable/editable since it is no longer inherited from parent dataverse.
     */
    public void makeMetadataBlocksSelectable() {
        this.mdbOptions.setInheritMetaBlocksFromParent(false);
        this.dataverse.setMetadataBlockRoot(true);
        this.allMetadataBlocks = this.metadataBlockService.
        		prepareMetaBlocksAndDatasetfields(this.dataverse, this.mdbOptions);

    }

    /**
     * Returns to dataverse view.
     */
    public String cancel() {
        return returnRedirect();
    }

    public void onFacetTransfer(final TransferEvent event) {
    	if(this.facetMetadataBlockId != null) {
	        for (final Object item : event.getItems()) {
	            final DatasetFieldType facet = (DatasetFieldType) item;
	            if (!this.facetMetadataBlockId.equals(facet.getMetadataBlock().getId())) {
	                this.facets.getSource().remove(facet);
	            }
	        }
    	}
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

    public void setFacetMetadataBlockId(final Long id) {
        this.facetMetadataBlockId = id;
    }

    public void setAllMetadataBlocks(final Set<MetadataBlock> blocks) {
        this.allMetadataBlocks = blocks;
    }

    public void setFacets(final DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
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
