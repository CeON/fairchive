package edu.harvard.iq.dataverse.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.util.UIMessages;
import io.vavr.control.Either;

@SuppressWarnings("serial")
@ViewScoped
@Named
public class MetadataPage implements Serializable {

    private DatasetFieldServiceBean datasetFieldService;
    private MetadataBlockService metadataBlockService;
    private DataverseService dataverseService;
    private PermissionsWrapper permissionsWrapper;
    private DataverseSession session;
    private UIMessages ui;

    private Long dataverseId;

    private Dataverse dataverse;
    private Long facetMetadataBlockId;
    private Set<MetadataBlock> allMetadataBlocks;
    private DualListModel<DatasetFieldType> facets = new DualListModel<>(new ArrayList<>(), new ArrayList<>());

    private DataverseMetaBlockOptions mdbOptions = new DataverseMetaBlockOptions();
    private DataverseMetaBlockOptions parentRootMdbOptions = new DataverseMetaBlockOptions();

    
    public MetadataPage() {}
    @Inject
    public MetadataPage(final DataverseService dataverseService,
    							   final DatasetFieldServiceBean datasetFieldService,
    							   final MetadataBlockService metadataBlockService,
    							   final PermissionsWrapper permissionsWrapper,
    							   final DataverseSession session,
    							   final UIMessages ui) {
    	this.dataverseService = dataverseService;
    	this.datasetFieldService = datasetFieldService;
    	this.metadataBlockService = metadataBlockService;
    	this.permissionsWrapper = permissionsWrapper;
    	this.session = session;
    	this.ui = ui;
    }
    
    public String getTitle() {
    	return this.dataverse != null && this.dataverse.isNotNew()
    			? getStringFromBundle("dataverse.edit.header") + " - " + this.dataverse.getName()
    			: getStringFromBundle("newDataverse");
    }
    
    public boolean isDataverseNotRoot() {
    	return this.dataverse.isNotRoot();
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
    
    public void setDataverseId(final Long id) {
        this.dataverseId = id;
    }

    public Long getFacetMetadataBlockId() {
        return this.facetMetadataBlockId;
    }
    
    public boolean isInheritFacetFromParent() {
        return !this.dataverse.isFacetRoot();
    }
    
    public void setInheritFacetFromParent(final boolean inherit) {
        this.dataverse.setFacetRoot(!inherit);
    }
    
    public void setFacetMetadataBlockId(final Long id) {
        this.facetMetadataBlockId = id;
    }

    public DualListModel<DatasetFieldType> getFacets() {
        return this.facets;
    }
    
    public void setFacets(final DualListModel<DatasetFieldType> facets) {
        this.facets = facets;
    }
    
    public List<MetadataBlock> getAllMetadataBlocks() {
        return new ArrayList<>(this.allMetadataBlocks);
    }
    
    public void setAllMetadataBlocks(final Set<MetadataBlock> blocks) {
        this.allMetadataBlocks = blocks;
    }
    
    public boolean isFacetPickListDisabled() {
    	return !this.dataverse.isFacetRoot() && this.dataverse.isNotRoot();
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

    public String init() {
    	final Optional<Dataverse> dataverse = this.dataverseService.findById(this.dataverseId);
    	
        if (! dataverse.isPresent()) {
            return this.permissionsWrapper.notFound();
        }
      
        if (!this.permissionsWrapper.canIssueUpdateDataverseCommand(dataverse.get())) {
            return permissionsWrapper.notAuthorized();
        } 
        
        this.dataverse = dataverse.get();
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

    public void toggleFacetRoot() {
        if (!this.dataverse.isFacetRoot()) {
            initFacets();
        }
    }

    public String save() {
    	if(session.isUserLoggedIn()) {
	        final List<DataverseFieldTypeInputLevel> dftilForSave =
	                this.metadataBlockService.getDataverseFieldTypeInputLevelsToBeSaved(
	                		this.allMetadataBlocks, this.mdbOptions, this.dataverse);
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
    	} else {
    		this.ui.addErrorMessage(getStringFromBundle("dataverse.create.authenticatedUsersOnly"));
    		return EMPTY;
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
    	return "/dataverse.xhtml?faces-redirect=true&alias=".
    			concat(this.dataverse.getAlias());
    }
}
