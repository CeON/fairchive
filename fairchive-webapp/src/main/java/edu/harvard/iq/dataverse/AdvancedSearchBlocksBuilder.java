package edu.harvard.iq.dataverse;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.faces.model.SelectItem;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldFactory;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.LicenseCheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import edu.harvard.iq.dataverse.validation.ValidationEnhancer;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;
import edu.harvard.iq.dataverse.validation.field.validators.DateRangeValidator;
import io.vavr.Tuple;

/**
 * Builder of {@link SearchBlock}s for advanced search page
 */
@Stateless
public class AdvancedSearchBlocksBuilder {

    private SearchFieldFactory searchFieldFactory;
    private DatasetFieldServiceBean datasetFieldService;
    private LicenseRepository licenseRepository;
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    private DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService;

    AdvancedSearchBlocksBuilder() { }

    @Inject
    public AdvancedSearchBlocksBuilder(SearchFieldFactory searchFieldFactory,
            DatasetFieldServiceBean datasetFieldService, LicenseRepository licenseRepository,
            TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory,
            DataverseFieldTypeInputLevelServiceBean dataverseFieldTypeInputLevelService) {
        this.searchFieldFactory = searchFieldFactory;
        this.datasetFieldService = datasetFieldService;
        this.licenseRepository = licenseRepository;
        this.termsOfUseSelectItemsFactory = termsOfUseSelectItemsFactory;
        this.dataverseFieldTypeInputLevelService = dataverseFieldTypeInputLevelService;
    }


    /**
     * Returns list of {@link SearchBlock}s. Each of returned {@link SearchBlock}
     * contains possible {@link SearchField}s for single metadata block that is
     * turned on in the given dataverse.
     */
    public List<SearchBlock> createDatasetMetadataBlocks(Dataverse dataverse) {
        List<SearchBlock> metadataSearchBlocks = new ArrayList<>();

        List<MetadataBlock> metadataBlocks = dataverse.getRootMetadataBlocks();
        List<Long> metadataBlockIds = metadataBlocks.stream()
                .map(MetadataBlock::getId)
                .collect(toList());
        Map<Long, List<DatasetFieldType>> metadataFieldListByBlock
                = datasetFieldService.findAllAdvancedSearchFieldTypesByMetadataBlockIds(metadataBlockIds).stream()
                .collect(Collectors.groupingBy(f -> f.getMetadataBlock().getId()));

        Set<String> hiddenFieldTypeIds = dataverseFieldTypeInputLevelService
                .findByDataverseId(dataverse.getMetadataRootId()).stream()
                .filter(l -> !l.isInclude())
                .map(l -> l.getDatasetFieldType().getName())
                .collect(Collectors.toSet());

        for (MetadataBlock block : metadataBlocks) {
            List<SearchField> searchFields
                    = metadataFieldListByBlock.getOrDefault(block.getId(), Collections.emptyList()).stream()
                    .filter(dft -> !hiddenFieldTypeIds.contains(dft.getName()))
                    .filter(dft -> dft.getParentDatasetFieldType() == null
                                    || !hiddenFieldTypeIds.contains(dft.getParentDatasetFieldType().getName()))
                    .map(searchFieldFactory::create)
                    .filter(f -> !SearchField.EMPTY.equals(f))
                    .collect(toList());

            metadataSearchBlocks.add(new SearchBlock(block.getName(), block.getLocaleDisplayName(), searchFields));
        }
        addExtraFieldsToCitationMetadataBlock(metadataSearchBlocks);

        metadataSearchBlocks.removeIf(sBlock -> sBlock.getSearchFields().isEmpty());
        return metadataSearchBlocks;
    }

    /**
     * Returns {@link SearchBlock} with {@link SearchField}s related to
     * searching for dataverses.
     */
    public SearchBlock createDataversesBlock() {
        return new SearchBlock("dataverses",
                BundleUtil.getStringFromBundle("advanced.search.header.dataverses"), constructDataversesSearchFields());
    }

    /**
     * Returns {@link SearchBlock} with {@link SearchField}s related to
     * searching for files.
     */
    public SearchBlock createFilesBlock() {
        return new SearchBlock("files",
                BundleUtil.getStringFromBundle("advanced.search.header.files"), constructFilesSearchFields());
    }

    private void addExtraFieldsToCitationMetadataBlock(List<SearchBlock> metadataSearchBlocks) {
        for (SearchBlock b : metadataSearchBlocks) {
            if (SearchFields.DATASET_CITATION.equals(b.getBlockName())) {
                ValidationEnhancer enhancer = new ValidationEnhancer();
                TextSearchField persistentIdField = textFieldFromBundle(SearchFields.DATASET_PERSISTENT_ID, "dataset.metadata.persistentId", "dataset.metadata.persistentId.tip");
                DatasetFieldType publicationDateType = enhancer.createDatasetFieldType(SearchFields.DATASET_PUBLICATION_DATE,
                        BundleUtil.getStringFromBundle("dataset.metadata.publicationYear"),
                        BundleUtil.getStringFromBundle("dataset.metadata.publicationYear.tip"),
                        enhancer.createValidation(new DateRangeValidator(),
                                Collections.singletonMap(ValidationDescriptor.CONTEXT_PARAM, Collections.singletonList(ValidationDescriptor.SEARCH_CONTEXT))));
                DateSearchField publicationDateField = new DateSearchField(publicationDateType);
                b.addSearchField(persistentIdField);
                b.addSearchField(publicationDateField);
                break;
            }
        }
    }

    private List<SearchField> constructDataversesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_NAME, "name", "advanced.search.dataverses.name.tip"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_ALIAS, "identifier","dataverse.identifier.title"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_AFFILIATION, "affiliation", "advanced.search.dataverses.affiliation.tip"));
        fields.add(textFieldFromBundle(SearchFields.DATAVERSE_DESCRIPTION, "description", "advanced.search.dataverses.description.tip"));

        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(SearchFields.DATAVERSE_SUBJECT,
                BundleUtil.getStringFromBundle("subject"),
                BundleUtil.getStringFromBundle("advanced.search.dataverses.subject.tip"));
        datasetFieldService.findByName(DatasetFieldConstant.subject)
                .getControlledVocabularyValues()
                .forEach(v -> checkboxSearchField.getCheckboxLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        fields.add(checkboxSearchField);

        return fields;
    }

    private List<SearchField> constructFilesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(SearchFields.FILE_NAME, "name", "advanced.search.files.name.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_DESCRIPTION, "description", "advanced.search.files.description.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_EXTENSION, "advanced.search.files.fileExtension", "advanced.search.files.fileExtension.tip"));
        fields.add(textFieldFromBundle(SearchFields.FILE_PERSISTENT_ID, "advanced.search.files.persistentId", "advanced.search.files.persistentId.tip"));
        fields.add(textFieldFromBundle(SearchFields.VARIABLE_NAME, "advanced.search.files.variableName", "advanced.search.files.variableName.tip"));
        fields.add(textFieldFromBundle(SearchFields.VARIABLE_LABEL, "advanced.search.files.variableLabel", "advanced.search.files.variableLabel.tip"));

        Map<Long, String> licenseNames = licenseRepository.findAll().stream()
                .collect(Collectors.toMap(License::getId, License::getName, (prev, next) -> next));
        CheckboxSearchField licenseSearchField = new LicenseCheckboxSearchField(SearchFields.LICENSE,
                BundleUtil.getStringFromBundle("advanced.search.files.license"),
                BundleUtil.getStringFromBundle("advanced.search.files.license.tip"), licenseNames);

        for (SelectItem selectItem : termsOfUseSelectItemsFactory.buildLicenseSelectItems()) {
            licenseSearchField.getCheckboxLabelAndValue().add(Tuple.of(selectItem.getLabel(), selectItem.getValue().toString()));
        }
        fields.add(licenseSearchField);
        return fields;
    }

    private TextSearchField textFieldFromBundle(String name, String displayNameKey, String descriptionKey) {
        return new TextSearchField(name, BundleUtil.getStringFromBundle(displayNameKey), BundleUtil.getStringFromBundle(descriptionKey));
    }
}
