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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;
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
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlockRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
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
    @Mock
    private MetadataBlockRepository metadataBlockRepo;
    @Mock 
    private SettingsServiceBean settings;

    private Dataverse dataverse = new Dataverse();

    private DatasetFieldType type1 = new DatasetFieldType();
    private DatasetFieldType type2 = new DatasetFieldType();
    private DatasetFieldType type3 = new DatasetFieldType();
    private DatasetFieldType compoundType = new DatasetFieldType();
    private MetadataBlock metaBlock = new MetadataBlock();

    private Dataset dataset = new Dataset();

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
        this.fragment.metadataBlockRepo = this.metadataBlockRepo;
        this.fragment.settings = this.settings;
        this.fragment.postConstruct();
        
        when(this.settings.getValueForKeyAsLong(any(SettingsServiceBean.Key.class), any()))
            .thenReturn(1000L);

        this.dataverse.setId(1L);
        this.fragment.setDataverse(this.dataverse);

        this.type1.setId(1L);
        this.type1.setTitle("def");
        this.type1.setExportToFile(true);
        this.type2.setId(2L);
        this.type2.setTitle("abc");
        this.type3.setId(3L);
        this.type3.setTitle("ghi");
        this.type3.setExportToFile(true);
        this.type3.setParentDatasetFieldType(this.compoundType);
        this.compoundType.setId(4L);
        this.compoundType.setTitle("compound");
        this.compoundType.getChildDatasetFieldTypes().add(this.type3);
        
        
        this.metaBlock.setName("Block1");
        this.metaBlock.setDatasetFieldTypes(asList(this.type1, this.type2, this.compoundType));
        this.type1.setMetadataBlock(this.metaBlock);
        this.type2.setMetadataBlock(this.metaBlock);
        this.type3.setMetadataBlock(this.metaBlock);
        this.compoundType.setMetadataBlock(this.metaBlock);

        DatasetVersion dsv = new DatasetVersion();
        DatasetField df1 = new DatasetField();
        df1.setDatasetFieldType(this.type1);
        df1.setValue("one"); 
        DatasetField df2 = new DatasetField();
        df2.setDatasetFieldType(this.type3);
        df2.setValue("two");
        DatasetField compoundField = new DatasetField();
        compoundField.setDatasetFieldType(this.compoundType);
        compoundField.getChildren().add(df2);
        
        dsv.setDatasetFields(asList(df1, compoundField));
        this.dataset.setVersions(asList(dsv));
    }

    private SolrQueryResponse responseOf(final SolrSearchResult... results) {

        SolrQueryResponse response = new SolrQueryResponse(new SolrQuery());
        response.setFacetCategoryList(new ArrayList<>());
        response.setSolrSearchResults(asList(results));
        response.setNumResultsFound((long) asList(results).size());

        return response;
    }

    @Test
    public void searchresultsSavedToCSV_containIntendedData() throws Exception {

        SolrSearchResult intendedResult = new SolrSearchResult();
        intendedResult.setType(DATASETS);
        intendedResult.setId("id1");
        intendedResult.setEntityId(1L);
        intendedResult.setName("name1");
        intendedResult.setTitle("title1");
        
        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean())).thenReturn(responseOf());
        when(this.searchService.search(any(SolrQuery.class))).thenReturn(asList(intendedResult)); 
        when(this.datasetRepo.findById(eq(intendedResult.getEntityId())))
                .thenReturn(Optional.of(this.dataset));
        when(this.datasetFieldTypeRepo.findAll())
                .thenReturn(asList(this.type1, this.type2));
        when(this.metadataBlockRepo.findSystemMetadataBlocks())
            .thenReturn(asList(this.metaBlock));
        
        this.fragment.search();
        StreamedContent file = this.fragment.getSearchResultsFile();

        assertThat(file.getContentEncoding()).isEqualTo("utf-8");
        assertThat(file.getContentType()).isEqualTo("text/csv");
        assertThat(file.getName()).isEqualTo("searchResults.csv");
        assertThat(file.getContentLength()).isGreaterThan(0);

        List<String> lines = readLines(file.getStream(), "utf-8");
        assertThat(lines.size()).isEqualTo(2);
        assertThat(lines.get(0)).isEqualTo("Id,Name,Title,Block1->def,Block1->compound->ghi");
        assertThat(lines.get(1)).isEqualTo("id1,name1,title1,one,two");
    }

    @Test
    public void emptyResultsSavedToCSV_generateFileWithColumnNamesOnly()
            throws Exception {

        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean())).thenReturn(responseOf());
        when(this.searchService.search(any(SolrQuery.class))).thenReturn(asList());
        when(this.datasetFieldTypeRepo.findAll())
                .thenReturn(asList(this.type1, this.type2, this.type3));
        when(this.metadataBlockRepo.findSystemMetadataBlocks())
                .thenReturn(asList(this.metaBlock));

        this.fragment.search();
        StreamedContent file = this.fragment.getSearchResultsFile();

        assertThat(file.getContentEncoding()).isEqualTo("utf-8");
        assertThat(file.getContentType()).isEqualTo("text/csv");
        assertThat(file.getName()).isEqualTo("searchResults.csv");
        assertThat(file.getContentLength()).isEqualTo(49);

        List<String> lines = readLines(file.getStream(), "utf-8");
        assertThat(lines.size()).isEqualTo(1);
        assertThat(lines.get(0)).isEqualTo("Id,Name,Title,Block1->def,Block1->compound->ghi");
    }

    @Test
    public void onTabChange__list_result() throws SearchException {

        // given
        TabChangeEvent tabChangeEvent = mock(TabChangeEvent.class);
        Tab tab = new Tab();
        tab.setId("test");
        when(tabChangeEvent.getTab()).thenReturn(tab);
        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean())).thenReturn(responseOf());

        // when
        this.fragment.onTabChange(tabChangeEvent);

        // then
        verify(searchService, times(0)).searchDatasetLocation(any(), anyString(), any());
    }

    @Test
    public void onTabChange__dataset_location_result() throws SearchException {

        // given
        TabChangeEvent tabChangeEvent = mock(TabChangeEvent.class);
        Tab tab = new Tab();
        tab.setId("mapSearchResult");
        when(tabChangeEvent.getTab()).thenReturn(tab);
        when(this.searchService.search(any(), any(), anyString(), any(), any(), any(),
                any(), anyInt(), anyInt(), anyBoolean())).thenReturn(responseOf());

        // when
        this.fragment.onTabChange(tabChangeEvent);

        // then
        verify(searchService, times(1)).searchDatasetLocation(any(), anyString(), any());
    }

    @Test
    public void searchDatasetLocation__search_Exception() throws SearchException {

        //given
        when(this.searchService.searchDatasetLocation(any(), anyString(), any())).thenThrow(SearchException.class);

        // when
        this.fragment.searchDatasetLocation();

        //
        assertThat(fragment.isSolrIsDown()).isTrue();
        assertThat(fragment.wasSolrErrorEncountered()).isTrue();
    }
}
