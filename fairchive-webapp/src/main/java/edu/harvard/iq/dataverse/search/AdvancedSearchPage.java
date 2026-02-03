package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.AdvancedSearchBlocksBuilder;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.advanced.QueryWrapperCreator;
import edu.harvard.iq.dataverse.search.advanced.SearchBlock;
import edu.harvard.iq.dataverse.search.advanced.field.GroupingSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.validation.SearchFormValidationService;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.omnifaces.cdi.ViewScoped;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;
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
    private WidgetWrapper widgetWrapper;
    private QueryWrapperCreator queryWrapperCreator;
    private SearchFormValidationService validationService;
    private AdvancedSearchBlocksBuilder advancedSearchBlocksBuilder;

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
    public AdvancedSearchPage(final DataverseDao dataverseDao,
                              final WidgetWrapper widgetWrapper, 
                              final QueryWrapperCreator queryWrapperCreator,
                              final SearchFormValidationService validationService,
                              final AdvancedSearchBlocksBuilder advancedSearchBlocksBuilder) {
        this.dataverseDao = dataverseDao;
        this.widgetWrapper = widgetWrapper;
        this.queryWrapperCreator = queryWrapperCreator;
        this.validationService = validationService;
        this.advancedSearchBlocksBuilder = advancedSearchBlocksBuilder;
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
        dataversesSearchBlock = advancedSearchBlocksBuilder.createDataversesBlock();
        filesSearchBlock = advancedSearchBlocksBuilder.createFilesBlock();

        metadataSearchBlocks = advancedSearchBlocksBuilder.createDatasetMetadataBlocks(dataverse);
        searchFieldIndex = buildSearchFieldIndex(metadataSearchBlocks);
        nonSearchFieldIndex = createParentFieldsForSearchFields(searchFieldIndex);
    }

    /** Composes query and redirects to the page with results. */
    public String find() throws IOException {
        final List<FieldValidationResult> fieldValidationResults
                = validationService.validateSearchForm(searchFieldIndex, nonSearchFieldIndex);
        if (!fieldValidationResults.isEmpty()) {
            JsfHelper.addErrorMessage(getStringFromBundle("advanced.search.validation"), EMPTY);
            return EMPTY;
        } else {
	        final List<SearchBlock> allSearchBlocks = new ArrayList<>(metadataSearchBlocks);
	        allSearchBlocks.add(filesSearchBlock);
	        allSearchBlocks.add(dataversesSearchBlock);
	
	        return buildSearchUrl(queryWrapperCreator.constructQueryWrapper(allSearchBlocks));
        }
    }

    // -------------------- PRIVATE --------------------

    private Map<String, SearchField> buildSearchFieldIndex(
    		final List<SearchBlock> searchBlocks) {
        return searchBlocks.stream()
            .flatMap(block -> block.getSearchFields().stream())
            .collect(toMap(SearchField::getName, f -> f, (prev, next) -> next));
    }

    /**
     * As some validators are navigating through parent to access its other
     * subfields, we have to reproduce this structure. We're doing it by
     * accessing {@link DatasetFieldType} and creating new or retrieving
     * existing parent fields in order to connect them with search fields.
     */
    private Map<String, SearchField> createParentFieldsForSearchFields(
    		final Map<String, SearchField> searchFieldIndex) {
        final Map<String, SearchField> result = new HashMap<>();
        int i = 0;
        for (final SearchField field : searchFieldIndex.values()) {
            final DatasetFieldType fieldType = field.getDatasetFieldType();
            if (fieldType == null || fieldType.getParentDatasetFieldType() == null) {
                continue;
            }
            final DatasetFieldType parentType = fieldType.getParentDatasetFieldType();
            final String parentKey = parentType.getName();
            SearchField parentField = searchFieldIndex.get(parentKey);
            parentField = parentField == null ? result.get(parentKey) : parentField;
            if (parentField == null) {
                parentField = new GroupingSearchField(parentKey, 
                		parentType.getDisplayName(), parentType.getDescription(),
                        null, parentType);
                parentField.setDisplayId(parentKey + '_' + (i++));
                result.put(parentKey, parentField);
            }
            parentField.getChildren().add(field);
            field.setParent(parentField);
        }
        return result;
    }

    private String buildSearchUrl(final QueryWrapper queryWrapper) {
        final List<String> filters = queryWrapper.getFilters();
        final String filtersPart = IntStream.range(0, filters.size())
                .mapToObj(i -> "&fq" + i + '=' + safeEncode(filters.get(i)))
                .collect(joining());

        return widgetWrapper.wrapURL(format("/dataverse.xhtml?q=%s&alias=%s",
                safeEncode(queryWrapper.getQuery()), dataverse.getAlias())
                + (isNotBlank(filtersPart) ? filtersPart : EMPTY)
                + "&faces-redirect=true");
    }

    private String safeEncode(final String toEncode) {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (final UnsupportedEncodingException uee) {
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
