package edu.harvard.iq.dataverse.search;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.search.SearchFields.DATASET_CITATION;
import static edu.harvard.iq.dataverse.search.SearchFields.DATASET_PERSISTENT_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.DATASET_PUBLICATION_DATE;
import static edu.harvard.iq.dataverse.search.SearchFields.DATAVERSE_AFFILIATION;
import static edu.harvard.iq.dataverse.search.SearchFields.DATAVERSE_ALIAS;
import static edu.harvard.iq.dataverse.search.SearchFields.DATAVERSE_DESCRIPTION;
import static edu.harvard.iq.dataverse.search.SearchFields.DATAVERSE_NAME;
import static edu.harvard.iq.dataverse.search.SearchFields.DATAVERSE_SUBJECT;
import static edu.harvard.iq.dataverse.search.SearchFields.FILE_DESCRIPTION;
import static edu.harvard.iq.dataverse.search.SearchFields.FILE_EXTENSION;
import static edu.harvard.iq.dataverse.search.SearchFields.FILE_NAME;
import static edu.harvard.iq.dataverse.search.SearchFields.FILE_PERSISTENT_ID;
import static edu.harvard.iq.dataverse.search.SearchFields.LICENSE;
import static edu.harvard.iq.dataverse.search.SearchFields.VARIABLE_LABEL;
import static edu.harvard.iq.dataverse.search.SearchFields.VARIABLE_NAME;
import static edu.harvard.iq.dataverse.validation.field.ValidationDescriptor.CONTEXT_PARAM;
import static edu.harvard.iq.dataverse.validation.field.ValidationDescriptor.SEARCH_CONTEXT;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.license.TermsOfUseSelectItemsFactory;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.advanced.QueryWrapperCreator;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldFactory;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GroupingSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.LicenseCheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.validation.SearchFormValidationService;
import edu.harvard.iq.dataverse.validation.ValidationEnhancer;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.validators.DateRangeValidator;
import io.vavr.Tuple;

/**
 * Page class responsible for showing search fields for Metadata blocks, files/dataverses blocks
 * and redirecting to search results.
 */
@SuppressWarnings("serial")
@ViewScoped
@Named("AdvancedSearchPage")
public class AdvancedSearchPage implements Serializable {

    private static final Logger logger = Logger.getLogger(AdvancedSearchPage.class.getCanonicalName());

    private DataverseDao dataverseDao;
    private DatasetFieldServiceBean datasetFieldService;
    private WidgetWrapper widgetWrapper;
    private QueryWrapperCreator queryWrapperCreator;
    private TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory;
    private SearchFormValidationService validationService;
    private SearchFieldFactory searchFieldFactory;
    private LicenseRepository licenseRepository;

    private Dataverse dataverse;
    private String dataverseIdentifier;

    private SearchBlock dataversesSearchBlock;
    private SearchBlock filesSearchBlock;
    private List<SearchBlock> metadataSearchBlocks = new ArrayList<>();

    private Map<String, SearchField> searchFieldIndex = new HashMap<>();
    private Map<String, SearchField> nonSearchFieldIndex = new HashMap<>();

    // -------------------- CONSTRUCTORS --------------------

    public AdvancedSearchPage() { }

    @Inject
    public AdvancedSearchPage(DataverseDao dataverseDao, DatasetFieldServiceBean datasetFieldService,
                              WidgetWrapper widgetWrapper, QueryWrapperCreator queryWrapperCreator,
                              TermsOfUseSelectItemsFactory termsOfUseSelectItemsFactory, 
                              SearchFormValidationService validationService,
                              SearchFieldFactory searchFieldFactory, 
                              LicenseRepository licenseRepository) {
        this.dataverseDao = dataverseDao;
        this.datasetFieldService = datasetFieldService;
        this.widgetWrapper = widgetWrapper;
        this.queryWrapperCreator = queryWrapperCreator;
        this.termsOfUseSelectItemsFactory = termsOfUseSelectItemsFactory;
        this.validationService = validationService;
        this.searchFieldFactory = searchFieldFactory;
        this.licenseRepository = licenseRepository;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void init() {
        if (dataverseIdentifier != null) {
            dataverse = dataverseDao.findByAlias(dataverseIdentifier);
        }
        if (dataverse == null) {
            dataverse = dataverseDao.findRootDataverse();
        }
        buildFieldStructure();
    }

    /** Composes query and redirects to the page with results. */
    public String find() throws IOException {
        List<FieldValidationResult> fieldValidationResults
                = validationService.validateSearchForm(searchFieldIndex, nonSearchFieldIndex);
        if (!fieldValidationResults.isEmpty()) {
            JsfHelper.addErrorMessage(getStringFromBundle("advanced.search.validation"), EMPTY);
            return StringUtils.EMPTY;
        }

        List<SearchBlock> allSearchBlocks = new ArrayList<>(metadataSearchBlocks);
        allSearchBlocks.add(filesSearchBlock);
        allSearchBlocks.add(dataversesSearchBlock);

        String returnString = buildSearchUrl(queryWrapperCreator.constructQueryWrapper(allSearchBlocks));
        logger.fine(returnString);
        return returnString;
    }

    // -------------------- PRIVATE --------------------

    private void buildFieldStructure() {
        extractSearchableFieldsForMetadataBlocks();
        createParentFieldsForSearchFields();
        createDataversesAndFilesBlocks();
    }

    private void extractSearchableFieldsForMetadataBlocks() {
        List<MetadataBlock> metadataBlocks = dataverse.getRootMetadataBlocks();
        List<Long> metadataBlockIds = metadataBlocks.stream()
                .map(MetadataBlock::getId)
                .collect(toList());
        Map<Long, List<DatasetFieldType>> metadataFieldListByBlock
                = datasetFieldService.findAllAdvancedSearchFieldTypesByMetadataBlockIds(metadataBlockIds).stream()
                .collect(groupingBy(f -> f.getMetadataBlock().getId()));
        for (MetadataBlock block : metadataBlocks) {
            List<SearchField> searchFields
                    = metadataFieldListByBlock.getOrDefault(block.getId(), emptyList())
                    .stream()
                    .map(searchFieldFactory::create)
                    .filter(f -> !SearchField.EMPTY.equals(f))
                    .collect(toList());
            searchFieldIndex.putAll(searchFields.stream()
                    .collect(toMap(SearchField::getName, f -> f, (prev, next) -> next)));
            metadataSearchBlocks.add(new SearchBlock(block.getName(), 
                    block.getLocaleDisplayName(), searchFields));
        }
        addExtraFieldsToCitationMetadataBlock();
    }

    /**
     * As some validators are navigating through parent to access its other
     * subfields, we have to reproduce this structure. We're doing it by
     * accessing {@link DatasetFieldType} and creating new or retrieving
     * existing parent fields in order to connect them with search fields.
     */
    private void createParentFieldsForSearchFields() {
        int i = 0;
        for (SearchField field : searchFieldIndex.values()) {
            DatasetFieldType fieldType = field.getDatasetFieldType();
            if (fieldType == null || fieldType.getParentDatasetFieldType() == null) {
                continue;
            }
            DatasetFieldType parentType = fieldType.getParentDatasetFieldType();
            String parentKey = parentType.getName();
            SearchField parentField = searchFieldIndex.get(parentKey);
            parentField = parentField == null ? nonSearchFieldIndex.get(parentKey) : parentField;
            if (parentField == null) {
                parentField = new GroupingSearchField(parentKey, 
                        parentType.getDisplayName(), parentType.getDescription(),
                        null, parentType);
                parentField.setDisplayId(parentKey + "_" + (i++));
                nonSearchFieldIndex.put(parentKey, parentField);
            }
            parentField.getChildren().add(field);
            field.setParent(parentField);
        }
    }

    private void createDataversesAndFilesBlocks() {
        dataversesSearchBlock = new SearchBlock("dataverses",
                getStringFromBundle("advanced.search.header.dataverses"), 
                constructDataversesSearchFields());
        filesSearchBlock = new SearchBlock("files",
                getStringFromBundle("advanced.search.header.files"), 
                constructFilesSearchFields());
    }

    private void addExtraFieldsToCitationMetadataBlock() {
        for (SearchBlock b : metadataSearchBlocks) {
            if (DATASET_CITATION.equals(b.getBlockName())) {
                ValidationEnhancer enhancer = new ValidationEnhancer();
                TextSearchField persistentIdField = textFieldFromBundle(DATASET_PERSISTENT_ID, 
                        "dataset.metadata.persistentId", "dataset.metadata.persistentId.tip");
                DatasetFieldType publicationDateType = enhancer.createDatasetFieldType(DATASET_PUBLICATION_DATE,
                        getStringFromBundle("dataset.metadata.publicationYear"),
                        getStringFromBundle("dataset.metadata.publicationYear.tip"),
                        enhancer.createValidation(new DateRangeValidator(),
                                singletonMap(CONTEXT_PARAM, singletonList(SEARCH_CONTEXT))));
                DateSearchField publicationDateField = new DateSearchField(publicationDateType);
                b.addSearchField(persistentIdField);
                b.addSearchField(publicationDateField);
                searchFieldIndex.put(persistentIdField.getName(), persistentIdField);
                searchFieldIndex.put(publicationDateField.getName(), publicationDateField);
                break;
            }
        }
    }

    private List<SearchField> constructFilesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(FILE_NAME, "name", 
                "advanced.search.files.name.tip"));
        fields.add(textFieldFromBundle(FILE_DESCRIPTION, 
                "description", "advanced.search.files.description.tip"));
        fields.add(textFieldFromBundle(FILE_EXTENSION, 
                "advanced.search.files.fileExtension", "advanced.search.files.fileExtension.tip"));
        fields.add(textFieldFromBundle(FILE_PERSISTENT_ID, 
                "advanced.search.files.persistentId", "advanced.search.files.persistentId.tip"));
        fields.add(textFieldFromBundle(VARIABLE_NAME, 
                "advanced.search.files.variableName", "advanced.search.files.variableName.tip"));
        fields.add(textFieldFromBundle(VARIABLE_LABEL, 
                "advanced.search.files.variableLabel", "advanced.search.files.variableLabel.tip"));

        Map<Long, String> licenseNames = licenseRepository.findAll().stream()
                .collect(toMap(License::getId, License::getName, (prev, next) -> next));
        CheckboxSearchField licenseSearchField = new LicenseCheckboxSearchField(LICENSE,
                getStringFromBundle("advanced.search.files.license"),
                getStringFromBundle("advanced.search.files.license.tip"), licenseNames);

        for (SelectItem selectItem : termsOfUseSelectItemsFactory.buildLicenseSelectItems()) {
            licenseSearchField.getCheckboxLabelAndValue()
                .add(Tuple.of(selectItem.getLabel(), selectItem.getValue().toString()));
        }
        fields.add(licenseSearchField);
        return fields;
    }

    private List<SearchField> constructDataversesSearchFields() {
        List<SearchField> fields = new ArrayList<>();

        fields.add(textFieldFromBundle(DATAVERSE_NAME, 
                "name", "advanced.search.dataverses.name.tip"));
        fields.add(textFieldFromBundle(DATAVERSE_ALIAS, 
                "identifier","dataverse.identifier.title"));
        fields.add(textFieldFromBundle(DATAVERSE_AFFILIATION, 
                "affiliation", "advanced.search.dataverses.affiliation.tip"));
        fields.add(textFieldFromBundle(DATAVERSE_DESCRIPTION, 
                "description", "advanced.search.dataverses.description.tip"));

        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(DATAVERSE_SUBJECT,
                getStringFromBundle("subject"),
                getStringFromBundle("advanced.search.dataverses.subject.tip"));
        datasetFieldService.findByName(DatasetFieldConstant.subject)
                .getControlledVocabularyValues()
                .forEach(v -> checkboxSearchField.getCheckboxLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        fields.add(checkboxSearchField);

        return fields;
    }

    private TextSearchField textFieldFromBundle(String name, 
            String displayNameKey, String descriptionKey) {
        return new TextSearchField(name, getStringFromBundle(displayNameKey), 
                getStringFromBundle(descriptionKey));
    }

    private String buildSearchUrl(QueryWrapper queryWrapper) {
        List<String> filters = queryWrapper.getFilters();
        String filtersPart = IntStream.range(0, filters.size())
                .mapToObj(i -> "&fq" + i + "=" + safeEncode(filters.get(i)))
                .collect(joining());

        return widgetWrapper.wrapURL(format("/dataverse.xhtml?q=%s&alias=%s",
                safeEncode(queryWrapper.getQuery()), dataverse.getAlias())
                + (isNotBlank(filtersPart) ? filtersPart : EMPTY)
                + "&faces-redirect=true");
    }

    private String safeEncode(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            logger.log(WARNING, "Encoding problem: ", uee);
            throw new RuntimeException(uee);
        }
    }

    // -------------------- GETTERS --------------------

    public Dataverse getDataverse() {
        return dataverse;
    }

    public String getDataverseIdentifier() {
        return dataverseIdentifier;
    }

    public List<SearchBlock> getMetadataSearchBlocks() {
        return metadataSearchBlocks;
    }

    public SearchBlock getDataversesSearchBlock() {
        return dataversesSearchBlock;
    }

    public SearchBlock getFilesSearchBlock() {
        return filesSearchBlock;
    }

    // -------------------- SETTERS --------------------

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseIdentifier(String dataverseIdentifier) {
        this.dataverseIdentifier = dataverseIdentifier;
    }
}
