package edu.harvard.iq.dataverse.search;

import static edu.harvard.iq.dataverse.search.query.SearchObjectType.DATASETS;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.readLines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.SolrSearchResultsService;
import edu.harvard.iq.dataverse.ThumbnailServiceWrapper;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

@ExtendWith(MockitoExtension.class)
public class SearchIncludeFragmentTest {

    private SearchIncludeFragment fragment = new SearchIncludeFragment();
    @Mock
    private HttpServletRequest request;
    @Mock
    private SearchServiceBean searchService;
    @Mock
    private DataverseRequestServiceBean dataverseRequestService;
    @Mock
    private DatasetFieldTypeRepository datasetFieldTypeRepo;
    @Mock
    private DatasetRepository datasetRepo;

    private Dataverse dataverse = new Dataverse();

    private DatasetFieldType type1 = new DatasetFieldType();
    private DatasetFieldType type2 = new DatasetFieldType();
    
    private  Dataset dataset = new Dataset();

    // -------------------------------------------------------------------------
    @BeforeEach
    public void setUp() {

        this.fragment.request = this.request;
        this.fragment.searchService = this.searchService;
        this.fragment.dataverseRequestService = this.dataverseRequestService;
        this.fragment.solrSearchResultsService = new SolrSearchResultsService();
        this.fragment.thumbnailServiceWrapper = new ThumbnailServiceWrapper();
        this.fragment.lastSearchValue = new LastSearchValue();
        this.fragment.datasetFieldTypeRepo = this.datasetFieldTypeRepo;
        this.fragment.datasetRepo = this.datasetRepo;
        this.fragment.postConstruct();

        this.dataverse.setId(1L);

        this.type1.setId(1L);
        this.type1.setTitle("def");
        this.type1.setExportToFile(true);
        this.type2.setId(2L);
        this.type2.setTitle("abc");
        
        DatasetVersion dsv = new DatasetVersion();
        DatasetField df = new DatasetField();
        df.setDatasetFieldType(this.type1);
        df.setValue("one");
        dsv.setDatasetFields(asList(df));
        this.dataset.setVersions(asList(dsv));

        when(this.datasetFieldTypeRepo.findAll())
                .thenReturn(asList(this.type1, this.type2));
    }

    // -------------------------------------------------------------------------
    private SolrQueryResponse responseOf(final SolrSearchResult... results) {

        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery());
        response.setFacetCategoryList(new ArrayList<>());
        response.setSolrSearchResults(asList(results));
        response.setNumResultsFound((long) asList(results).size());

        return response;
    }

    // -------------------------------------------------------------------------
    @Test
    public void searchresultsSavedToCSV_containIntendedData() throws Exception {

        SolrSearchResult intendedResult = new SolrSearchResult();
        intendedResult.setType(DATASETS);
        intendedResult.setId("id1");
        intendedResult.setEntityId(1L);
        intendedResult.setName("name1");
        intendedResult.setTitle("title1");
        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(responseOf(intendedResult));   
        when(this.datasetRepo.getById(eq(intendedResult.getEntityId())))
                .thenReturn(this.dataset);

        this.fragment.search(this.dataverse);
        StreamedContent file = this.fragment.getSearchResultsFile();

        assertThat(file.getContentEncoding()).isEqualTo("utf-8");
        assertThat(file.getContentType()).isEqualTo("text/csv");
        assertThat(file.getName()).isEqualTo("searchResults.csv");
        assertThat(file.getContentLength()).isGreaterThan(0);

        List<String> lines = readLines(file.getStream(), "utf-8");
        assertThat(lines.size()).isEqualTo(2);
        assertThat(lines.get(0)).isEqualTo("Id,Name,Title,def");
        assertThat(lines.get(1)).isEqualTo("id1,name1,title1,one");
    }

    // -------------------------------------------------------------------------
    @Test
    public void emptyResultsSavedToCSV_generateFileWithColumnNamesOnly()
            throws Exception {

        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean())).thenReturn(responseOf());

        this.fragment.search(this.dataverse);
        StreamedContent file = this.fragment.getSearchResultsFile();

        assertThat(file.getContentEncoding()).isEqualTo("utf-8");
        assertThat(file.getContentType()).isEqualTo("text/csv");
        assertThat(file.getName()).isEqualTo("searchResults.csv");
        assertThat(file.getContentLength()).isEqualTo(19);

        List<String> lines = readLines(file.getStream(), "utf-8");
        assertThat(lines.size()).isEqualTo(1);
        assertThat(lines.get(0)).isEqualTo("Id,Name,Title,def");
    }
}
