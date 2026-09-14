package edu.harvard.iq.dataverse.featured;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataverseDatasetCountServiceTest {

    @Mock
    private SolrClient solrClient;
    @Mock
    private IndexServiceBean indexService;

    @InjectMocks
    private DataverseDatasetCountService service;

    @Mock
    private QueryResponse response;

    private final Dataverse parent = new Dataverse();

    // -------------------- TESTS --------------------

    @Test
    void countDatasetsInChildrenOf__requestsAllFacetTerms() throws Exception {
        // given
        when(indexService.findPathSegments(parent)).thenReturn(singletonList("1"));
        when(solrClient.query(any())).thenReturn(response);
        when(response.getFacetField(SearchFields.SUBTREE)).thenReturn(new FacetField(SearchFields.SUBTREE));
        ArgumentCaptor<SolrParams> query = ArgumentCaptor.forClass(SolrParams.class);

        // when
        service.countDatasetsInChildrenOf(parent);

        // then
        verify(solrClient).query(query.capture());
        assertThat(query.getValue().get(FacetParams.FACET_LIMIT)).isEqualTo("-1");
    }

    @Test
    void countDatasetsInChildrenOf__directChildrenOnly() throws Exception {
        // given
        FacetField subtreeFacet = new FacetField(SearchFields.SUBTREE);
        subtreeFacet.add("/1", 9);
        subtreeFacet.add("/1/5", 3);
        subtreeFacet.add("/1/5/7", 2);
        subtreeFacet.add("/1/6", 1);
        when(indexService.findPathSegments(parent)).thenReturn(singletonList("1"));
        when(solrClient.query(any())).thenReturn(response);
        when(response.getFacetField(SearchFields.SUBTREE)).thenReturn(subtreeFacet);

        // when
        List<DataverseDatasetCount> counts = service.countDatasetsInChildrenOf(parent);

        // then
        assertThat(counts)
                .extracting(DataverseDatasetCount::getDataverseId, DataverseDatasetCount::getDatasetCount)
                .containsExactly(tuple(5L, 3L), tuple(6L, 1L));
    }

    @Test
    void countDatasetsInChildrenOf__solrFailure() throws Exception {
        // given
        when(indexService.findPathSegments(parent)).thenReturn(singletonList("1"));
        when(solrClient.query(any())).thenThrow(new SolrServerException("Solr is down"));

        // when
        Throwable thrown = catchThrowable(() -> service.countDatasetsInChildrenOf(parent));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Couldn't retrieve dataset counts.");
    }
}
