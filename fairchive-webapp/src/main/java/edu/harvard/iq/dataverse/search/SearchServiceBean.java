package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.FriendlyFileTypeUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.query.SearchPublicationStatus;
import edu.harvard.iq.dataverse.search.query.SolrQuerySanitizer;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FacetLocaleNameResolver;
import edu.harvard.iq.dataverse.search.response.FacetLabel;
import edu.harvard.iq.dataverse.search.response.FilterQuery;
import edu.harvard.iq.dataverse.search.response.Highlight;
import edu.harvard.iq.dataverse.search.response.PublicationStatusCounts;
import edu.harvard.iq.dataverse.search.response.SearchParentInfo;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.search.response.SolrSearchLocationResult;
import edu.harvard.iq.dataverse.search.response.GeoPoint;
import edu.harvard.iq.dataverse.search.response.GeoShape;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.joining;

import static edu.harvard.iq.dataverse.search.SearchFields.RELEASE_OR_CREATE_DATE;
import static edu.harvard.iq.dataverse.search.SearchFields.RELEVANCE;

@Stateless
public class SearchServiceBean {

    private static final Logger logger = Logger.getLogger(SearchServiceBean.class.getCanonicalName());

    public enum SortOrder {
        
        asc,
        desc;

        public static Optional<SortOrder> fromString(final String sortOrderString) {
            
            return Try.of(() -> SortOrder.valueOf(sortOrderString)).toJavaOptional();
        }

        public static List<String> allowedOrderStrings() {
            
            return stream(SortOrder.values())
                    .map(Enum::name)
                    .collect(toList());
        }
        
        public static SortOrder defaultFor(final String sortField) {
            
            return (RELEVANCE.equals(sortField)
                    || RELEASE_OR_CREATE_DATE.equals(sortField)) ? desc : asc;
        }
    }

    private DatasetFieldServiceBean datasetFieldService;
    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;
    private PermissionFilterQueryBuilder permissionQueryBuilder;
    private SolrClient solrServer;
    private SolrQuerySanitizer querySanitizer;
    private LicenseRepository licenseRepository;
    private DataverseDao dataverseDao;

    private Long rootDataverseId;

    // -------------------- CONSTRUCTORS --------------------

    public SearchServiceBean() { }

    @Inject
    public SearchServiceBean(DatasetFieldServiceBean datasetFieldService,
                             SettingsServiceBean settingsService, SystemConfig systemConfig,
                             PermissionFilterQueryBuilder permissionQueryBuilder, SolrClient solrServer,
                             SolrQuerySanitizer querySanitizer, LicenseRepository licenseRepository,
                             DataverseDao dataverseDao) {
        this.datasetFieldService = datasetFieldService;
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
        this.permissionQueryBuilder = permissionQueryBuilder;
        this.solrServer = solrServer;
        this.querySanitizer = querySanitizer;
        this.licenseRepository = licenseRepository;
        this.dataverseDao = dataverseDao;
    }


    // -------------------- LOGIC --------------------

    /**
     * @param countsOnly after executing solr query only found object counts
     *                   (ie. datasets, dataverses & files) would be filled in
     *                   returned object, so it is unsuitable for other uses.
     */
    public SolrQueryResponse search(DataverseRequest dataverseRequest, List<Dataverse> dataverses, String query, SearchForTypes typesToSearch,
                                    List<String> filterQueries, String sortField, SortOrder sortOrder, int paginationStart,
                                    int numResultsPerPage, boolean countsOnly)
            throws SearchException {
        if (paginationStart < 0) {
            throw new IllegalArgumentException("paginationStart must be 0 or greater");
        }
        if (numResultsPerPage < 1) {
            throw new IllegalArgumentException("numResultsPerPage must be 1 or greater");
        }

        SolrQuery solrQuery = new SolrQuery();

        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        Map<String, DatasetFieldType> fieldIndex = datasetFields.stream()
                .collect(toMap(DatasetFieldType::getName, Function.identity()));

        query = querySanitizer.sanitizeQuery(query, datasetFields);
        solrQuery.setQuery(query);

        solrQuery.setSort(new SortClause(sortField, sortOrder == SortOrder.asc ? ORDER.asc : ORDER.desc));
        solrQuery.setHighlight(true).setHighlightSnippets(1);
        Integer fragSize = settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.SearchHighlightFragmentSize);
        if (fragSize != null) {
            solrQuery.setHighlightFragsize(fragSize);
        }
        solrQuery.setHighlightSimplePre("<span class=\"search-term-match\">");
        solrQuery.setHighlightSimplePost("</span>");
        Map<String, String> solrFieldsToHightlightOnMap = new HashMap<>();
        solrFieldsToHightlightOnMap.put(SearchFields.NAME, BundleUtil.getStringFromBundle("name"));
        solrFieldsToHightlightOnMap.put(SearchFields.AFFILIATION, BundleUtil.getStringFromBundle("affiliation"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_FRIENDLY, BundleUtil.getStringFromBundle("advanced.search.files.fileType"));
        solrFieldsToHightlightOnMap.put(SearchFields.DESCRIPTION, BundleUtil.getStringFromBundle("description"));
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_NAME, BundleUtil.getStringFromBundle("advanced.search.files.variableName"));
        solrFieldsToHightlightOnMap.put(SearchFields.VARIABLE_LABEL, BundleUtil.getStringFromBundle("advanced.search.files.variableLabel"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TYPE_SEARCHABLE, BundleUtil.getStringFromBundle("advanced.search.files.fileType"));
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PUBLICATION_DATE, BundleUtil.getStringFromBundle("dataset.metadata.publicationYear"));
        solrFieldsToHightlightOnMap.put(SearchFields.DATASET_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.datasets.persistentId"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_PERSISTENT_ID, BundleUtil.getStringFromBundle("advanced.search.files.persistentId"));
        /*
          @todo Dataverse subject and affiliation should be highlighted but
         * this is commented out right now because the "friendly" names are not
         * being shown on the dataverse cards. See also
         * https://github.com/IQSS/dataverse/issues/1431
         */
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_SUBJECT, "Subject");
//        solrFieldsToHightlightOnMap.put(SearchFields.DATAVERSE_AFFILIATION, "Affiliation");
        /*
          @todo: show highlight on file card?
         * https://redmine.hmdc.harvard.edu/issues/3848
         */
        solrFieldsToHightlightOnMap.put(SearchFields.FILENAME_WITHOUT_EXTENSION, BundleUtil.getStringFromBundle("facets.search.fieldtype.fileNameWithoutExtension.label"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_EXTENSION, BundleUtil.getStringFromBundle("advanced.search.files.fileExtension"));
        solrFieldsToHightlightOnMap.put(SearchFields.FILE_TAG_SEARCHABLE, BundleUtil.getStringFromBundle("facets.search.fieldtype.fileTag.label"));
        for (DatasetFieldType datasetFieldType : datasetFields) {

            SolrField dsfSolrField = SolrField.of(datasetFieldType);

            String solrField = dsfSolrField.getNameSearchable();
            String displayName = datasetFieldType.getDisplayName();
            solrFieldsToHightlightOnMap.put(solrField, displayName);
        }

        solrQuery = addHighlightFields(solrQuery, solrFieldsToHightlightOnMap);

        solrQuery.setHighlightRequireFieldMatch(true);
        solrQuery.setParam("fl", "*,score");
        solrQuery.setParam("qt", "/select");
        solrQuery.setParam("facet", "true");
        solrQuery.setParam("facet.mincount", "1");
        //  @todo: do we need facet.query?
        solrQuery.setParam("facet.query", "*");


        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }
        // Remove root dataverse from search results
        solrQuery.addFilterQuery(String.format("-%s:%d", SearchFields.ENTITY_ID, getRootDataverseId()));

        addDvObjectTypeFilterQuery(solrQuery, typesToSearch);

        String permissionFilterQuery = permissionQueryBuilder.buildPermissionFilterQuery(dataverseRequest);
        if (!permissionFilterQuery.isEmpty()) {
            solrQuery.addFilterQuery(permissionFilterQuery);
        }

        // -----------------------------------
        // Facets to Retrieve
        // -----------------------------------
        solrQuery.addFacetField(SearchFields.DATAVERSE_CATEGORY);
        solrQuery.addFacetField(SearchFields.METADATA_SOURCE);
        solrQuery.addFacetField(SearchFields.PUBLICATION_YEAR);
        /*
          @todo when a new method on datasetFieldService is available
         * (retrieveFacetsByDataverse?) only show the facets that the dataverse
         * in question wants to show (and in the right order):
         * https://redmine.hmdc.harvard.edu/issues/3490
         *
         * also, findAll only returns advancedSearchField = true... we should
         * probably introduce the "isFacetable" boolean rather than caring about
         * if advancedSearchField is true or false

         */

        if (dataverseRequest.getUser().isAuthenticated()) {
            solrQuery.addFacetField(SearchFields.PUBLICATION_STATUS);
        }

        if (dataverses != null) {
            for (Dataverse dataverse : dataverses) {
                for (DataverseFacet dataverseFacet : dataverse.getDataverseFacets()) {
                    SolrField dsfSolrField = SolrField.of(dataverseFacet.getDatasetFieldType());
                    solrQuery.addFacetField(dsfSolrField.getNameFacetable());
                }
            };
        }

        solrQuery.addFacetField(SearchFields.LICENSE);

        solrQuery.addFacetField(SearchFields.FILE_TYPE);
        // @todo: hide the extra line this shows in the GUI... at least it's
        solrQuery.addFacetField(SearchFields.TYPE);
        solrQuery.addFacetField(SearchFields.FILE_TAG);
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall)) {
            solrQuery.addFacetField(SearchFields.ACCESS);
        }
        // @todo: do sanity checking... throw error if negative
        solrQuery.setStart(paginationStart);
        solrQuery.setRows(numResultsPerPage);
        logger.fine("Solr query:" + solrQuery);

        // -----------------------------------
        // Make the solr query
        // -----------------------------------
        QueryResponse queryResponse = null;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (RemoteSolrException | SolrServerException | IOException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }
        SolrDocumentList docs = queryResponse.getResults();
        List<SolrSearchResult> solrSearchResults = new ArrayList<>();

        SolrQueryResponse solrQueryResponse = new SolrQueryResponse(solrQuery);
        solrQueryResponse.setDvObjectCounts(convertFacetToDvObjectCounts(queryResponse.getFacetField(SearchFields.TYPE)));
        if (countsOnly) {
            return solrQueryResponse;
        }

        String titleSolrField = null;
        if (fieldIndex.containsKey(DatasetFieldConstant.title)) {
            titleSolrField = SolrField.of(fieldIndex.get(DatasetFieldConstant.title)).getNameSearchable();
        } else {
            logger.info("Couldn't find " + DatasetFieldConstant.title);
        }
        String baseUrl = systemConfig.getDataverseSiteUrl();

        //Going through the results
        for (SolrDocument solrDocument : docs) {
            SolrSearchResult solrSearchResult = searchResultFrom(
                    solrFieldsToHightlightOnMap, queryResponse, titleSolrField,
                    baseUrl, solrDocument);
            solrSearchResults.add(solrSearchResult);
        }

        Map<String, List<String>> spellingSuggestionsByToken = new HashMap<>();
        SpellCheckResponse spellCheckResponse = queryResponse.getSpellCheckResponse();
        if (spellCheckResponse != null) {
            List<SpellCheckResponse.Suggestion> suggestions = spellCheckResponse.getSuggestions();
            for (SpellCheckResponse.Suggestion suggestion : suggestions) {
                spellingSuggestionsByToken.put(suggestion.getToken(), suggestion.getAlternatives());
            }
        }

        List<FacetCategory> facetCategoryList = new ArrayList<>();

        FacetLocaleNameResolver facetCategoryNameResolver = new FacetLocaleNameResolver(licenseRepository, fieldIndex);

        for (FacetField facetField : queryResponse.getFacetFields()) {
            if (!shouldIncludeFacetInResults(facetField)) {
                continue;
            }

            FacetCategory facetCategory = new FacetCategory();
            facetCategory.setName(facetField.getName());
            facetCategory.setFriendlyName(facetCategoryNameResolver.getLocaleFacetCategoryName(facetField.getName()));

            List<FacetLabel> facetLabelList = new ArrayList<>();

            for (FacetField.Count facetFieldCount : facetField.getValues()) {
                // @todo we do want to show the count for each facet
                FacetLabel facetLabel = new FacetLabel(facetFieldCount.getName(),
                        facetCategoryNameResolver.getLocaleFacetLabelName(facetFieldCount.getName(), facetField.getName()),
                        facetFieldCount.getCount());
                // quote field facets
                facetLabel.setFilterQuery(facetField.getName() + ":\"" + facetFieldCount.getName() + '"');
                facetLabelList.add(facetLabel);
            }

            facetCategory.setFacetLabels(facetLabelList);

            if (!facetLabelList.isEmpty()) {
                facetCategoryList.add(facetCategory);
            }
        }

        solrQueryResponse.setSolrSearchResults(solrSearchResults);
        solrQueryResponse.setSpellingSuggestionsByToken(spellingSuggestionsByToken);
        solrQueryResponse.setFacetCategoryList(facetCategoryList);
        solrQueryResponse.setNumResultsFound(queryResponse.getResults().getNumFound());
        solrQueryResponse.setResultsStart(queryResponse.getResults().getStart());
        String[] filterQueriesArray = solrQuery.getFilterQueries();
        if (filterQueriesArray != null) {
            // null check added because these tests were failing: mvn test -Dtest=SearchIT
            List<String> actualFilterQueries = asList(filterQueriesArray);
            logger.fine("actual filter queries: " + actualFilterQueries);
            solrQueryResponse.setFilterQueriesActual(actualFilterQueries);
        } else {
            // how often is this null?
            logger.info("solrQuery.getFilterQueries() was null");
        }

        for (String filterQuery: filterQueries) {

            String[] parts = filterQuery.split(":");
            if (parts.length != 2) {
                solrQueryResponse.addFilterQuery(new FilterQuery(filterQuery));
            } else {
                String key = parts[0];
                String value = parts[1].replaceAll("^\"", "").replaceAll("\"$", "");

                solrQueryResponse.addFilterQuery(new FilterQuery(
                        filterQuery,
                        facetCategoryNameResolver.getLocaleFacetCategoryName(key),
                        facetCategoryNameResolver.getLocaleFacetLabelName(value, key)));
            }

        }

        solrQueryResponse.setPublicationStatusCounts(convertFacetToPublicationStatusCounts(queryResponse.getFacetField(SearchFields.PUBLICATION_STATUS)));

        return solrQueryResponse;
    }

    private static SolrSearchResult searchResultFrom (
            Map<String, String> solrFieldsToHightlightOnMap,
            QueryResponse queryResponse, String titleSolrField, String baseUrl,
            SolrDocument solrDocument) {
        String id = (String) solrDocument.getFieldValue(SearchFields.ID);
        Long entityId = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
        String solrType = (String) solrDocument.getFieldValue(SearchFields.TYPE);
        SearchObjectType type = SearchObjectType.fromSolrValue(solrType);

        float score = (Float) solrDocument.getFieldValue(SearchFields.RELEVANCE);
        logger.fine("score for " + id + ": " + score);
        String identifier = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER);
        String citation = getLocalizedValueWithFallback(solrDocument, SearchFields.DATASET_CITATION);
        String citationPlainHtml = getLocalizedValueWithFallback(solrDocument, SearchFields.DATASET_CITATION_HTML);
        String persistentUrl = (String) solrDocument.getFieldValue(SearchFields.PERSISTENT_URL);
        String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
        String nameSort = (String) solrDocument.getFieldValue(SearchFields.NAME_SORT);
        String title = (String) solrDocument.getFirstValue(titleSolrField);
        Long datasetVersionId = (Long) solrDocument.getFieldValue(SearchFields.DATASET_VERSION_ID);
        String deaccessionReason = (String) solrDocument.getFieldValue(SearchFields.DATASET_DEACCESSION_REASON);
        String fileContentType = (String) solrDocument.getFieldValue(SearchFields.FILE_CONTENT_TYPE);
        Date release_or_create_date = (Date) solrDocument.getFieldValue(SearchFields.RELEASE_OR_CREATE_DATE);
        String identifierOfDataverse = (String) solrDocument.getFieldValue(SearchFields.IDENTIFIER_OF_DATAVERSE);
        String nameOfDataverse = (String) solrDocument.getFieldValue(SearchFields.DATAVERSE_NAME);
        Date embargoUntil = (Date) solrDocument.getFieldValue(SearchFields.EMBARGO_UNTIL);

        List<String> matchedFields = new ArrayList<>();
        List<Highlight> highlights = new ArrayList<>();
        Map<SolrField, Highlight> highlightsMap = new HashMap<>();
        Map<SolrField, List<String>> highlightsMap2 = new HashMap<>();
        Map<String, Highlight> highlightsMap3 = new HashMap<>();
        if (queryResponse.getHighlighting().get(id) != null) {
            for (Map.Entry<String, String> entry : solrFieldsToHightlightOnMap.entrySet()) {
                String field = entry.getKey();
                String displayName = entry.getValue();

                List<String> highlightSnippets = queryResponse.getHighlighting().get(id).get(field);
                if (highlightSnippets != null) {
                    matchedFields.add(field);
                    /*
                      @todo only SolrField.SolrType.STRING? that's not
                     * right... knit the SolrField object more into the
                     * highlighting stuff
                     */
                    SolrField solrField = new SolrField(field, SolrField.SolrType.STRING, true, true, false);
                    Highlight highlight = new Highlight(solrField, highlightSnippets, displayName);
                    highlights.add(highlight);
                    highlightsMap.put(solrField, highlight);
                    highlightsMap2.put(solrField, highlightSnippets);
                    highlightsMap3.put(field, highlight);
                }
            }

        }
        SolrSearchResult solrSearchResult = new SolrSearchResult();
        // @todo put all this in the constructor?
        @SuppressWarnings("unchecked")
        List<String> states = (List<String>) solrDocument.getFieldValue(SearchFields.PUBLICATION_STATUS);
        if (states != null) {
            // set list of all statuses
            // this method also sets booleans for individual statuses
            List<SearchPublicationStatus> publicationStates = states.stream()
                    .map(solrStatus -> SearchPublicationStatus.fromSolrValue(solrStatus))
                    .collect(toList());
            solrSearchResult.setPublicationStatuses(publicationStates);
        }
        solrSearchResult.setId(id);
        solrSearchResult.setEntityId(entityId);
        solrSearchResult.setIdentifier(identifier);
        solrSearchResult.setPersistentUrl(persistentUrl);
        solrSearchResult.setType(type);
        solrSearchResult.setScore(score);
        solrSearchResult.setNameSort(nameSort);
        solrSearchResult.setReleaseOrCreateDate(release_or_create_date);
        solrSearchResult.setMatchedFields(matchedFields);
        solrSearchResult.setHighlightsAsList(highlights);
        solrSearchResult.setHighlightsMap(highlightsMap);
        solrSearchResult.setHighlightsAsMap(highlightsMap3);
        SearchParentInfo parent = new SearchParentInfo();
        String description = (String) solrDocument.getFieldValue(SearchFields.DESCRIPTION);
        solrSearchResult.setDescriptionNoSnippet(description);
        solrSearchResult.setDeaccessionReason(deaccessionReason);
        solrSearchResult.setEmbargoUntil(embargoUntil);

        String originSource = (String) solrDocument.getFieldValue(SearchFields.METADATA_SOURCE);
        if (IndexServiceBean.HARVESTED.equals(originSource)) {
            solrSearchResult.setHarvested(true);
        }

        if (type == SearchObjectType.DATAVERSES) {
            solrSearchResult.setName(name);
            solrSearchResult.setHtmlUrl(baseUrl + SystemConfig.DATAVERSE_PATH + identifier);
            // Do not set the ImageUrl, let the search include fragment fill in
            // the thumbnail, similarly to how the dataset and datafile cards
            // are handled.
            //solrSearchResult.setImageUrl(baseUrl + "/api/access/dvCardImage/" + entityid);
            /*
              @todo Expose this API URL after "dvs" is changed to
             * "dataverses". Also, is an API token required for published
             * dataverses? Michael: url changed.
             */
//                solrSearchResult.setApiUrl(baseUrl + "/api/dataverses/" + entityid);
        } else if (type == SearchObjectType.DATASETS) {
            solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?globalId=" + identifier);
            solrSearchResult.setApiUrl(baseUrl + "/api/datasets/" + entityId);
            //Image url now set via thumbnail api
            //solrSearchResult.setImageUrl(baseUrl + "/api/access/dsCardImage/" + datasetVersionId);
            // No, we don't want to set the base64 thumbnails here.
            // We want to do it inside SearchIncludeFragment, AND ONLY once the rest of the
            // page has already loaded.
            //DatasetVersion datasetVersion = datasetVersionService.find(datasetVersionId);
            //if (datasetVersion != null){
            //    solrSearchResult.setDatasetThumbnail(datasetVersion.getDataset().getDatasetThumbnail(datasetVersion));
            //}

            // @todo Could use getFieldValues (plural) here.
            String firstDatasetDescription = (String) solrDocument.getFirstValue(SearchFields.DATASET_DESCRIPTION);
            solrSearchResult.setDescriptionNoSnippet(firstDatasetDescription);

            solrSearchResult.setDatasetVersionId(datasetVersionId);

            solrSearchResult.setCitation(citation);
            solrSearchResult.setCitationHtml(citationPlainHtml);

            solrSearchResult.setIdentifierOfDataverse(identifierOfDataverse);
            solrSearchResult.setNameOfDataverse(nameOfDataverse);

            if (title != null) {
                solrSearchResult.setTitle(title);
            } else {
                logger.fine("No title indexed. Setting to empty string to prevent NPE. Dataset id " + entityId + " and version id " + datasetVersionId);
                solrSearchResult.setTitle("");
            }
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<String> authors = (List) solrDocument.getFieldValues("dsf_txt_".concat(DatasetFieldConstant.authorName));
            if (authors != null) {
                solrSearchResult.setDatasetAuthors(authors);
            }
        } else if (type == SearchObjectType.FILES) {
            String parentGlobalId = null;
            Object parentGlobalIdObject = solrDocument.getFieldValue(SearchFields.PARENT_IDENTIFIER);
            if (parentGlobalIdObject != null) {
                parentGlobalId = (String) parentGlobalIdObject;
                parent.setParentIdentifier(parentGlobalId);
            }
            solrSearchResult.setHtmlUrl(baseUrl + "/dataset.xhtml?persistentId=" + parentGlobalId);
            solrSearchResult.setDownloadUrl(baseUrl + "/api/access/datafile/" + entityId);
            /*
              @todo We are not yet setting the API URL for files because
             * not all files have metadata. Only subsettable files (those
             * with a datatable) seem to have metadata. Furthermore, the
             * response is in XML whereas the rest of the Search API returns
             * JSON.
             */
            solrSearchResult.setName(name);
            solrSearchResult.setFiletype(FriendlyFileTypeUtil.getUserFriendlyFileTypeForDisplay(fileContentType));
            solrSearchResult.setFileContentType(fileContentType);
            Object fileSizeInBytesObject = solrDocument.getFieldValue(SearchFields.FILE_SIZE_IN_BYTES);
            if (fileSizeInBytesObject != null) {
                try {
                    long fileSizeInBytesLong = (long) fileSizeInBytesObject;
                    solrSearchResult.setFileSizeInBytes(fileSizeInBytesLong);
                } catch (ClassCastException ex) {
                    logger.info("Could not cast file " + entityId + " to long for " + SearchFields.FILE_SIZE_IN_BYTES + ": " + ex.getLocalizedMessage());
                }
            }
            solrSearchResult.setFileMd5((String) solrDocument.getFieldValue(SearchFields.FILE_MD5));
            try {
                solrSearchResult.setFileChecksumType(DataFile.ChecksumType.fromString((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_TYPE)));
            } catch (IllegalArgumentException ex) {
                logger.info("Exception setting setFileChecksumType: " + ex);
            }
            solrSearchResult.setFileChecksumValue((String) solrDocument.getFieldValue(SearchFields.FILE_CHECKSUM_VALUE));
            solrSearchResult.setUnf((String) solrDocument.getFieldValue(SearchFields.UNF));
            solrSearchResult.setDatasetVersionId(datasetVersionId);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<String> fileCategories = (List) solrDocument.getFieldValues(SearchFields.FILE_TAG);
            if (fileCategories != null) {
                solrSearchResult.setFileCategories(fileCategories);
            }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            List<String> tabularDataTags = (List) solrDocument.getFieldValues(SearchFields.TABDATA_TAG);
            if (tabularDataTags != null) {
                Collections.sort(tabularDataTags);
                solrSearchResult.setTabularDataTags(tabularDataTags);
            }
            String filePID = (String) solrDocument.getFieldValue(SearchFields.FILE_PERSISTENT_ID);
            if (null != filePID && !filePID.isEmpty()) {
                solrSearchResult.setFilePersistentId(filePID);
            }

            String fileAccess = (String) solrDocument.getFirstValue(SearchFields.ACCESS);
            solrSearchResult.setFileAccess(fileAccess);
        }
        // @todo store PARENT_ID as a long instead and cast as such
        parent.setId((String) solrDocument.getFieldValue(SearchFields.PARENT_ID))
              .setName((String) solrDocument.getFieldValue(SearchFields.PARENT_NAME))
              .setCitation(getLocalizedValueWithFallback(solrDocument, SearchFields.PARENT_CITATION));
        solrSearchResult.setParent(parent);
        return solrSearchResult;
    }
    
    public List<SolrSearchResult> search(final SolrQuery solrQuery)
            throws SearchException {
        try {
            final List<SolrSearchResult> result = new ArrayList<>();
            final QueryResponse queryResponse = this.solrServer.query(solrQuery);

            final String baseUrl = this.systemConfig.getDataverseSiteUrl();
            List<DatasetFieldType> datasetFields = this.datasetFieldService
                    .findAllOrderedById();
            Map<String, DatasetFieldType> fieldIndex = datasetFields.stream()
                    .collect(toMap(DatasetFieldType::getName, Function.identity()));
            final String titleSolrField = SolrField
                    .of(fieldIndex.get(DatasetFieldConstant.title))
                    .getNameSearchable();
            // Going through the results
            for (SolrDocument solrDocument : queryResponse.getResults()) {
                SolrSearchResult solrSearchResult = searchResultFrom(
                        emptyMap(), queryResponse, titleSolrField,
                        baseUrl, solrDocument);
                result.add(solrSearchResult);
            }
            return result;
        } catch (RemoteSolrException | SolrServerException | IOException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }
    }

    public List<SolrSearchLocationResult> searchDatasetLocation(DataverseRequest dataverseRequest,
                                                                String query,
                                                                List<String> filterQueries) throws SearchException {
        SolrQuery solrQuery = new SolrQuery();
        List<DatasetFieldType> datasetFields = datasetFieldService.findAllOrderedById();
        query = querySanitizer.sanitizeQuery(query, datasetFields);
        solrQuery.setQuery(query);
        solrQuery.setRows(2000);

        setSolrParametersForDatasetLocations(solrQuery);
        setSolrFiltersForDatasetLocations(solrQuery, dataverseRequest, filterQueries);

        logger.fine("Solr query:" + solrQuery);
        // -----------------------------------
        // Make the solr query
        // -----------------------------------
        QueryResponse queryResponse;
        try {
            queryResponse = solrServer.query(solrQuery);
        } catch (RemoteSolrException | SolrServerException | IOException ex) {
            throw new SearchException("Internal Dataverse Search Engine Error", ex);
        }

        SolrDocumentList docs = queryResponse.getResults();
        return parseSolrDatasetLocationResults(docs);
    }

    // -------------------- PRIVATE --------------------

    private Long getRootDataverseId() {
        if (rootDataverseId == null) {
            rootDataverseId = dataverseDao.findRootDataverse().getId();
        }
        return rootDataverseId;
    }

    private SolrQuery addHighlightFields(SolrQuery solrQuery, Map<String, String> solrFieldsToHightlightOnMap) {
        Set<String> dynamicDatasetFieldsPrefixes = new HashSet<>();

        for(String field : solrFieldsToHightlightOnMap.keySet()) {
            if(isFieldDynamic(field)) {
                dynamicDatasetFieldsPrefixes.add(field.substring(0, 8));
            } else {
                solrQuery.addHighlightField(field);
            }
        }

        for (String dynamicFieldPrefix : dynamicDatasetFieldsPrefixes) {
            solrQuery.addHighlightField(dynamicFieldPrefix.concat("*"));
        }

        return solrQuery;
    }

    private boolean isFieldDynamic(String field) {
        return field.length() > 8 && SearchDynamicFieldPrefix.contains(field.substring(0, 8));
    }

    private DvObjectCounts convertFacetToDvObjectCounts(FacetField dvObjectFacetField) {

        DvObjectCounts dvObjectCounts = DvObjectCounts.emptyDvObjectCounts();
        if (dvObjectFacetField == null) {
            return dvObjectCounts;
        }

        for (Count count: dvObjectFacetField.getValues()) {
            SearchObjectType dvType = SearchObjectType.fromSolrValue(count.getName());
            dvObjectCounts.setCountByObjectType(dvType, count.getCount());
        }
        return dvObjectCounts;
    }

    private PublicationStatusCounts convertFacetToPublicationStatusCounts(FacetField publicationStatusFacetField) {

        PublicationStatusCounts publicationStatusCounts = PublicationStatusCounts.emptyPublicationStatusCounts();
        if (publicationStatusFacetField == null) {
            return publicationStatusCounts;
        }

        for (Count count: publicationStatusFacetField.getValues()) {
            SearchPublicationStatus status = SearchPublicationStatus.fromSolrValue(count.getName());
            publicationStatusCounts.setCountByPublicationStatus(status, count.getCount());
        }
        return publicationStatusCounts;
    }

    private void addDvObjectTypeFilterQuery(SolrQuery query, SearchForTypes typesToSearch) {
        String filterValue = typesToSearch.getTypes().stream()
                .sorted()
                .map(SearchObjectType::getSolrValue)
                .collect(joining(" OR "));

        query.addFilterQuery(SearchFields.TYPE + ":(" + filterValue + ')');
    }

    private static String getLocalizedValueWithFallback(SolrDocument document, String fieldName) {
        String suffix = "_".concat(BundleUtil.getCurrentLocale().getLanguage());
        return (String) (document.containsKey(fieldName + suffix)
                ? document.getFieldValue(fieldName + suffix)
                : document.getFieldValue(fieldName + "_en"));
    }

    private boolean shouldIncludeFacetInResults(FacetField facetField) {
        if (facetField.getName().equals(SearchFields.TYPE)) {
            // the "type" facet is special
            return false;
        }
        if (facetField.getName().equals(SearchFields.METADATA_SOURCE) && facetField.getValueCount() < 2) {
            return false;
        }
        return true;
    }

    private List<SolrSearchLocationResult> parseSolrDatasetLocationResults(SolrDocumentList docs) {
        List<SolrSearchLocationResult> results = new ArrayList<>();
        for (SolrDocument solrDocument : docs) {
            String datasetName = (String) solrDocument.getFieldValue(SearchFields.NAME_SORT);
            String doi = (String) solrDocument.getFieldValue(SearchFields.DATASET_PERSISTENT_ID);
            List<String> coordinates = parseCoordinates(solrDocument, SearchFields.GEOGRAPHIC_COORDINATES);

            boolean isDraft = isDraftDataset(solrDocument);
            for (String coords : coordinates) {
                List<GeoPoint> points = GeoPoint.fromCoordinateString(coords);
                Map<String, String> customData = parseCustomData(solrDocument);
                SolrSearchLocationResult result = new SolrSearchLocationResult(
                        datasetName,
                        doi,
                        isDraft,
                        GeoShape.of(points),
                        customData
                );
                results.add(result);
            }
        }

        return results;
    }

    private List<String> parseCoordinates(SolrDocument solrDocument, String fieldName) {
        return solrDocument.getFieldValues(fieldName)
                .stream()
                .map(String.class::cast)
                .collect(toList());
    }

    private Map<String, String> parseCustomData(SolrDocument solrDocument) {
        Map<String, String> customData = new HashMap<>();
        List<String> customFields = settingsService.getValueForKeyAsList(SettingsServiceBean.Key.CustomSearchLocationsSolrFields);
        for (String customField : customFields) {
            customData.put(
                    customField,
                    solrDocument
                            .getFieldValues(customField)
                            .stream()
                            .map(String.class::cast)
                            .collect(Collectors.joining(", "))
            );
        }

        return customData;
    }

    private boolean isDraftDataset(SolrDocument solrDocument) {
        return Optional.of(solrDocument.getFieldValues(SearchFields.PUBLICATION_STATUS))
                .orElse(Collections.emptyList())
                .stream()
                .map(String.class::cast)
                .map(String::toUpperCase).anyMatch(DatasetVersion.VersionState.DRAFT.name()::equals);
    }

    private void setSolrParametersForDatasetLocations(SolrQuery solrQuery) {
        List<String> defaultFields = asList(SearchFields.NAME_SORT,
                SearchFields.DATASET_PERSISTENT_ID,
                SearchFields.PUBLICATION_STATUS,
                SearchFields.GEOGRAPHIC_COORDINATES
        );
        List<String> customFields = settingsService.getValueForKeyAsList(SettingsServiceBean.Key.CustomSearchLocationsSolrFields);
        List<String> allFields = new ArrayList<>();
        allFields.addAll(defaultFields);
        allFields.addAll(customFields);

        solrQuery.setParam("fl", String.join(",", allFields));
        solrQuery.setParam("qt", "/select");
    }

    private void setSolrFiltersForDatasetLocations(SolrQuery solrQuery,
                                                   DataverseRequest dataverseRequest,
                                                   List<String> filterQueries) {
        for (String filterQuery : filterQueries) {
            solrQuery.addFilterQuery(filterQuery);
        }
        // Documents only with geographic location
        solrQuery.addFilterQuery(String.format("%s:%s", SearchFields.GEOGRAPHIC_COORDINATES, "[* TO *]"));

        addDvObjectTypeFilterQuery(solrQuery, SearchForTypes.byTypes(SearchObjectType.DATASETS));

        String permissionFilterQuery = permissionQueryBuilder.buildPermissionFilterQuery(dataverseRequest);
        if (!permissionFilterQuery.isEmpty()) {
            solrQuery.addFilterQuery(permissionFilterQuery);
        }
    }
}
