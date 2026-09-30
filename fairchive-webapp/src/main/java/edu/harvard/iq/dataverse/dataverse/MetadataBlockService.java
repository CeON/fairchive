package edu.harvard.iq.dataverse.dataverse;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.Stateless;
import javax.faces.model.SelectItem;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevelServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import io.vavr.Tuple;
import io.vavr.Tuple2;

@Stateless
public class MetadataBlockService {

    private MetadataBlockRepository metadataBlockRepo;
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;

    public MetadataBlockService() {}
    
    @Inject
    public MetadataBlockService(final MetadataBlockRepository metadataBlockRepo,
			final DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService) {

		this.metadataBlockRepo = metadataBlockRepo;
		this.dataverseFieldTypeInputLevelService = dataverseFieldTypeInputLevelService;
	}

	/**
     * Extracts dataverse field type input levels to be saved, if field is 
     * optional it is not designed to be saved.
     * Only if it is required or hidden.
     */
    public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevelsToBeSaved(
    		final Collection<MetadataBlock> blocks,
    		final DataverseMetaBlockOptions options,
    		final Dataverse dataverse) {
    	
            dataverse.getRootMetadataBlocks().clear();
            final List<MetadataBlock> selectedBlocks = 
            		getSelectedMetadataBlocks(blocks, options);
            dataverse.setMetadataBlocks(selectedBlocks);
            return getSelectedMetadataFields(selectedBlocks, dataverse, options);
    }

    /**
     * Prepares metadata blocks and dataset fields for edit/creation of 
     * dataverse in order to view all metadata blocks and their fields.
     */
    public Set<MetadataBlock> prepareMetaBlocksAndDatasetfields(
    		final Dataverse dataverse, 
    		final DataverseMetaBlockOptions options) {
    	
        if (dataverse.isMetadataBlockRoot()) {
            options.setInheritMetaBlocksFromParent(false);
        }

        final Set<MetadataBlock> availableBlocks = prepareMetadataBlocks(dataverse);
        buildInitialMetadataBlockOptions(dataverse, options, availableBlocks);
        prepareDatasetFields(options, dataverse, retriveAllDatasetFieldsForMdb(availableBlocks));
        return availableBlocks;
    }

    /**
     * Changes metadata block view options in order to show editable dataset
     *  fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeEditable(
    		final DataverseMetaBlockOptions dataverseMetaBlockOptions, 
    		final Long metadataBlockId) {
    	
    	final MetadataBlockViewOptions options = MetadataBlockViewOptions.newBuilder()
    			 .showDatasetFieldTypes(true)
                 .editableDatasetFieldTypes(true)
                 .selected(true)
                 .build();
        return dataverseMetaBlockOptions.getMdbViewOptions().
        		put(metadataBlockId, options);                            
    }


    /**
     * Refresh metadata block view options in order to change editable 
     * given metadata block.
     */
    public MetadataBlockViewOptions refreshDatasetFieldTypes(
    		final DataverseMetaBlockOptions dataverseMetaBlockOptions, 
    		final Long metadataBlockId,
    		final boolean editable, 
    		final boolean fieldsVisible) {
    	
    	final MetadataBlockViewOptions options = MetadataBlockViewOptions.newBuilder()
                .showDatasetFieldTypes(fieldsVisible)
                .editableDatasetFieldTypes(editable)
                .selected(dataverseMetaBlockOptions.isMetaBlockSelected(metadataBlockId))
                .build();
        return dataverseMetaBlockOptions.getMdbViewOptions().
        		put(metadataBlockId, options);
    }

    /**
     * Changes metadata block view options in order to show uneditable dataset fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeUnEditable(
    		final DataverseMetaBlockOptions dataverseMetaBlockOptions, 
    		final Long metadataBlockId) {
    	
    	final MetadataBlockViewOptions options = MetadataBlockViewOptions.newBuilder()
                .showDatasetFieldTypes(true)
                .editableDatasetFieldTypes(false)
                .selected(dataverseMetaBlockOptions.isMetaBlockSelected(metadataBlockId))
                .build();
        return dataverseMetaBlockOptions.getMdbViewOptions().
        		put(metadataBlockId, options);
    }

    /**
     * Changes metadata block view options in order to hide all dataset fields for given metadata block.
     */
    public MetadataBlockViewOptions prepareDatasetFieldsToBeHidden(
    		final DataverseMetaBlockOptions dataverseMetaBlockOptions, 
    		final Long metadataBlockId) {
    	
    	final MetadataBlockViewOptions options = MetadataBlockViewOptions.newBuilder()
                .showDatasetFieldTypes(false)
                .selected(dataverseMetaBlockOptions.isMetaBlockSelected(metadataBlockId))
                .build();
    	
        return dataverseMetaBlockOptions.getMdbViewOptions().
        		put(metadataBlockId, options);
    }

    /**
     * Refreshes dataset fields view options for given dataset field.
     */
    public void refreshDatasetFieldsViewOptions(final Long mdbId, 
    		final long dsftId, final Collection<MetadataBlock> allMetadataBlocks, 
    		final DataverseMetaBlockOptions mdbOptions) {
    	
        final List<DatasetFieldType> childDSFT = new ArrayList<>();
        setViewOptionsForDatasetFields(mdbId, dsftId, allMetadataBlocks,
        		mdbOptions, childDSFT);

        if (!childDSFT.isEmpty()) {
            setViewOptionsForDatasetFieldsChilds(mdbId, allMetadataBlocks,
            		mdbOptions, childDSFT);
        }
    }

    // -------------------- PRIVATE --------------------

    private Set<MetadataBlock> retriveAllDataverseParentsMetaBlocks(
    		final Dataverse dataverse) {
    	
        final Set<MetadataBlock> blocks = new HashSet<>();
        
        for (final Dataverse owner : dataverse.getOwners()) {
            blocks.addAll(owner.getMetadataBlocks());
        }

        return blocks;
    }

    private void setViewOptionsForDatasetFieldsChilds(final Long mdbId, 
    		final Collection<MetadataBlock> allMetadataBlocks, 
    		final DataverseMetaBlockOptions mdbOptions, 
    		final List<DatasetFieldType> childDSFT) {
    	
        for (final DatasetFieldType dsftUpdate : childDSFT) {
            for (final MetadataBlock mdb : allMetadataBlocks) {
                if (mdb.getId().equals(mdbId)) {
                    for (final DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                        if (dsftTest.getId().equals(dsftUpdate.getId())) {
                            final DatasetFieldViewOptions dsftViewOptions = 
                            		mdbOptions.getDatasetFieldViewOptions().
                            			get(dsftTest.getId());
                            dsftViewOptions.setSelectedDatasetFields(
                            		setViewOptionForDatasetField(mdbOptions, dsftTest));
                        }
                    }
                    break;
                }
            }
        }
    }

    private void setViewOptionsForDatasetFields(final Long mdbId, final long dsftId,
    		final Collection<MetadataBlock> allMetadataBlocks, 
    		final DataverseMetaBlockOptions mdbOptions, 
    		final List<DatasetFieldType> childDSFT) {
    	
        for (final MetadataBlock mdb : allMetadataBlocks) {
            if (mdb.getId().equals(mdbId)) {
                for (final DatasetFieldType dsftTest : mdb.getDatasetFieldTypes()) {
                    if (dsftTest.getId().equals(dsftId)) {
                        final DatasetFieldViewOptions dsftViewOptions = 
                        		mdbOptions.getDatasetFieldViewOptions().
                        			get(dsftTest.getId());

                        dsftViewOptions.setSelectedDatasetFields(
                        		setViewOptionForDatasetField(mdbOptions, dsftTest));

                        if ((dsftTest.isHasParent() && 
                        		!mdbOptions.isDsftIncludedField(dsftTest.getParentDatasetFieldType().getId()))
                                || (!dsftTest.isHasParent() && !dsftViewOptions.isIncluded())) {
                            dsftViewOptions.setRequiredField(false);
                        }
                        if (dsftTest.isHasChildren()) {
                            childDSFT.addAll(dsftTest.getChildDatasetFieldTypes());
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private void prepareDatasetFields(final DataverseMetaBlockOptions mdbOptions, 
    		final Dataverse freshDataverse, final Set<DatasetFieldType> datasetFieldTypes) {
    	
        for (final DatasetFieldType rootDatasetFieldType : datasetFieldTypes) {
            setViewOptionsForDatasetFieldTypes(freshDataverse.getMetadataRootId(), 
            		rootDatasetFieldType, mdbOptions);

            if (rootDatasetFieldType.isHasChildren()) {
                for (final DatasetFieldType childDatasetFieldType : 
                	rootDatasetFieldType.getChildDatasetFieldTypes()) {
                    setViewOptionsForDatasetFieldTypes(
                    		freshDataverse.getMetadataRootId(), childDatasetFieldType, 
                    		mdbOptions);
                }

            }
        }
    }

    private Set<MetadataBlock> prepareMetadataBlocks(final Dataverse dataverse) {
    	
        final Set<MetadataBlock> blocks = new TreeSet<>(
        		comparingInt(MetadataBlock::getDisplayOrder).thenComparingLong(MetadataBlock::getId));
        
        blocks.addAll(this.metadataBlockRepo.findSystemMetadataBlocks());
        blocks.addAll(retriveAllDataverseParentsMetaBlocks(dataverse));

        return blocks;
    }

    private Map<Long, MetadataBlockViewOptions> buildInitialMetadataBlockOptions(
    		final Dataverse dataverse, 
    		final DataverseMetaBlockOptions mdbOptions, 
    		final Set<MetadataBlock> availableBlocks) {
    	
        for (final MetadataBlock mdb : availableBlocks) {

            MetadataBlockViewOptions.Builder builder = MetadataBlockViewOptions.newBuilder()
                    .selected(false)
                    .showDatasetFieldTypes(false);

            if (dataverse.isNotRoot()) {
                if (dataverse.getOwner().getRootMetadataBlocks().contains(mdb)) {
                    builder.selected(true);
                }

                if (dataverse.getOwner().getMetadataBlocks().contains(mdb)) {
                    builder.selected(true);
                }
            }

            if (dataverse.getId() != null && dataverse.getMetadataBlocks().contains(mdb)) {
                builder.selected(true);
            }

            mdbOptions.getMdbViewOptions().put(mdb.getId(), builder.build());
        }

        return mdbOptions.getMdbViewOptions();
    }

    private void setViewOptionsForDatasetFieldTypes(final Long dataverseId, 
    		final DatasetFieldType rootDatasetFieldType, 
    		final DataverseMetaBlockOptions mdbOptions) {

        final DataverseFieldTypeInputLevel dftil = this.dataverseFieldTypeInputLevelService.
        		findByDataverseIdDatasetFieldTypeId(dataverseId, rootDatasetFieldType.getId());

        if (dftil != null) {
            setDsftilViewOptions(mdbOptions, rootDatasetFieldType.getId(), 
            		dftil.isRequired(), dftil.isInclude());
        } else {
            setDsftilViewOptions(mdbOptions, rootDatasetFieldType.getId(), 
            		rootDatasetFieldType.isRequired(), true);
        }

        mdbOptions.getDatasetFieldViewOptions()
                .get(rootDatasetFieldType.getId())
                .setSelectedDatasetFields(setViewOptionForDatasetField(mdbOptions, rootDatasetFieldType));
    }

    private List<SelectItem> setViewOptionForDatasetField(
    		final DataverseMetaBlockOptions mdbOptions, final DatasetFieldType typeIn) {
    	
        final List<SelectItem> result = new ArrayList<>();

        if ((typeIn.isHasParent() && mdbOptions.isDsftIncludedField(typeIn.getParentDatasetFieldType().getId())) ||
                (!typeIn.isHasParent() && mdbOptions.isDsftIncludedField(typeIn.getId()))) {
            result.add(generateSelectedItem("dataverse.item.required", true, false));
            result.add(generateSelectedItem("dataverse.item.optional", false, false));
        } else {
            result.add(generateSelectedItem("dataverse.item.hidden", false, true));
        }
        return result;
    }

    private Tuple2<Boolean, Boolean> setDsftilViewOptions(
    		final DataverseMetaBlockOptions mdbOptions, final long datasetFieldTypeId, 
    		final boolean requiredField, final boolean included) {
    	
        mdbOptions.getDatasetFieldViewOptions().put(datasetFieldTypeId, 
        		new DatasetFieldViewOptions(requiredField, included));
        return Tuple.of(requiredField, included);
    }

    private SelectItem generateSelectedItem(final String label, 
    		final boolean selected, final boolean disabled) {
    	
        final SelectItem item = new SelectItem();
        item.setLabel(BundleUtil.getStringFromBundle(label));
        item.setValue(selected);
        item.setDisabled(disabled);
        return item;
    }

    private Set<DatasetFieldType> retriveAllDatasetFieldsForMdb(
    		final Collection<MetadataBlock> mdb) {
    	
        final Set<DatasetFieldType> result = new HashSet<>();

        for (final MetadataBlock metadataBlock : mdb) {
            result.addAll(metadataBlock.getDatasetFieldTypes());
        }

        return result;
    }

    private List<DataverseFieldTypeInputLevel> getSelectedMetadataFields(
    		final List<MetadataBlock> selectedBlocks, 
    		final Dataverse dataverse, final DataverseMetaBlockOptions options) {
    	
        final List<DataverseFieldTypeInputLevel> result = new ArrayList<>();

        for (final MetadataBlock selectedBlock : selectedBlocks) {
            for (final DatasetFieldType type : selectedBlock.getDatasetFieldTypes()) {

                if (isDatasetFieldChildOrParentRequired(type, options)) {
                    result.add(createDataverseFieldTypeInputLevel(type, 
                    		dataverse, true, true));
                }

                if (isDatasetFieldChildOrParentNotIncluded(type, options)) {
                    result.add(createDataverseFieldTypeInputLevel(type, 
                    		dataverse, false, false));
                }
            }

        }

        return result;
    }

    private boolean isDatasetFieldChildOrParentNotIncluded(
    		final DatasetFieldType type, final DataverseMetaBlockOptions options) {
    	
        return (!type.isHasParent() && !options.isDsftIncludedField(type.getId()))
                || (type.isHasParent() && !options.isDsftIncludedField(type.getParentDatasetFieldType().getId()));
    }

    private boolean isDatasetFieldChildOrParentRequired(
    		final DatasetFieldType type, final DataverseMetaBlockOptions blockOptions) {
    	
        final DatasetFieldViewOptions fieldOptions = blockOptions.
        		getDatasetFieldViewOptions().get(type.getId());

        DatasetFieldViewOptions parentFieldViewOptions = null;

        if (type.isHasParent()) {
            parentFieldViewOptions = blockOptions.getDatasetFieldViewOptions().
            		get(type.getParentDatasetFieldType().getId());
        }

        final boolean isParentIncluded = parentFieldViewOptions != null && 
        		parentFieldViewOptions.isIncluded();

        return fieldOptions.isRequiredField() && !type.isRequired()
                && ((!type.isHasParent() && fieldOptions.isIncluded())
                || (type.isHasParent() && isParentIncluded));
    }

    private DataverseFieldTypeInputLevel createDataverseFieldTypeInputLevel(
    		final DatasetFieldType type, final Dataverse dataverse,
            final boolean isRequired, final boolean isIncluded) {
    	
        final DataverseFieldTypeInputLevel result = new DataverseFieldTypeInputLevel();
        result.setDatasetFieldType(type);
        result.setVersion(dataverse.getLatestVersion());
        result.setRequired(isRequired);
        result.setInclude(isIncluded);
        return result;
    }

    private List<MetadataBlock> getSelectedMetadataBlocks(
    		final Collection<MetadataBlock> blocks, 
    		final DataverseMetaBlockOptions options) {
    	
    	return blocks.
    			stream().
    			filter(block -> !options.isInheritMetaBlocksFromParent() && 
    					(options.isMetaBlockSelected(block.getId()) || block.isCitationMetaBlock())).
    			collect(toList());
    	
    }
}
