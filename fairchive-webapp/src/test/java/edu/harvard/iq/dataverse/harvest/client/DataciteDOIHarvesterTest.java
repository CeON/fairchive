package edu.harvard.iq.dataverse.harvest.client;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.globalid.DataCiteDoisPage;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.globalid.DataCiteRestApiClient;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import wiremock.com.google.common.collect.Lists;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataciteDOIHarvesterTest {

    @Mock
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Mock
    private ImportServiceBean importService;

    @Spy
    private DataciteDatasetMapper dataciteDatasetMapper = new DataciteDatasetMapper();

    @Mock
    private HarvestingClient harvestingClient;

    @InjectMocks
    private DataciteDOIHarvester dataciteDOIHarvester = new DataciteDOIHarvester();

    private final List<DataciteHarvesterParams.DOIValue> DOI_REPO = ImmutableList.of(
            newDOI("10.5438/0012"),
            newDOI("10.18150/0013"),
            newDOI("10.18150/0014"),
            newDOI("10.18150/0015"),
            newDOI("10.18150/0016"));

    @Test
    public void harvest__import() throws ImportException, IOException {
        // given
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            when(dataCiteRestApiClient.findDoi(doi.getAuthority(), doi.getId())).thenReturn(newResponse(doi));
        }
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiImport(DOI_REPO);
        DataverseRequest dataverseRequest = dataverseRequest();

       // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(doi.getFull()),
                    argThat(dto -> dto.getAuthority().equals(doi.getAuthority()) && dto.getIdentifier().equals(doi.getId())));
        }
        assertThat(result.getNumHarvested()).isEqualTo(5);
        assertThat(result.getNumDeleted()).isEqualTo(0);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__delete() throws ImportException {
        // given
        when(importService.doDeleteHarvestedDataset(any(), any(), any())).thenReturn(true);
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiRemove(DOI_REPO);
        DataverseRequest dataverseRequest = dataverseRequest();

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO) {
            verify(importService, times(1)).doDeleteHarvestedDataset(any(), any(),
                    eq(doi.getFull()));
        }
        assertThat(result.getNumHarvested()).isEqualTo(0);
        assertThat(result.getNumDeleted()).isEqualTo(5);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__deleteOfNotHarvestedDataset() throws ImportException {
        // given
        when(importService.doDeleteHarvestedDataset(any(), any(), any())).thenReturn(false);
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiRemove(DOI_REPO);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        assertThat(result.getNumDeleted()).isEqualTo(0);
        assertThat(result.getNumFailed()).isEqualTo(5);
    }

    @Test
    public void harvest__failed() throws ImportException, IOException {
        // given
        List<DataciteHarvesterParams.DOIValue> impSuccess = Lists.newArrayList(DOI_REPO.get(0));
        List<DataciteHarvesterParams.DOIValue> impFail = Lists.newArrayList(DOI_REPO.get(1));
        List<DataciteHarvesterParams.DOIValue> delSuccess = Lists.newArrayList(DOI_REPO.get(2), DOI_REPO.get(4));
        List<DataciteHarvesterParams.DOIValue> delFail = Lists.newArrayList(DOI_REPO.get(3));

        for (DataciteHarvesterParams.DOIValue s : impSuccess) {
            when(dataCiteRestApiClient.findDoi(s.getAuthority(), s.getId())).thenReturn(newResponse(s));
        }

        for (DataciteHarvesterParams.DOIValue f : impFail) {
            when(dataCiteRestApiClient.findDoi(f.getAuthority(), f.getId())).thenThrow(new IOException("Boom!"));
        }

        for (DataciteHarvesterParams.DOIValue f : delSuccess) {
            when(importService.doDeleteHarvestedDataset(any(), any(), eq(f.getFull()))).thenReturn(true);
        }

        for (DataciteHarvesterParams.DOIValue f : delFail) {
            doThrow(new ImportException("Boom!")).when(importService).doDeleteHarvestedDataset(any(), any(), eq(f.getFull()));
        }

        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setDoiImport(ListUtils.union(impSuccess, impFail));
        params.setDoiRemove(ListUtils.union(delSuccess, delFail));
        DataverseRequest dataverseRequest = dataverseRequest();

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue rs : impSuccess) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(rs.getFull()),
                    argThat(dto -> dto.getAuthority().equals(rs.getAuthority()) && dto.getIdentifier().equals(rs.getId())));
        }
        for (DataciteHarvesterParams.DOIValue rs : delSuccess) {
            verify(importService, times(1)).doDeleteHarvestedDataset(any(), any(),
                    eq(rs.getFull()));
        }
        assertThat(result.getNumHarvested()).isEqualTo(1);
        assertThat(result.getNumDeleted()).isEqualTo(2);
        assertThat(result.getNumFailed()).isEqualTo(2);
    }

    @Test
    public void harvest__query() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenReturn(page(3, "c2", DOI_REPO.get(0), DOI_REPO.get(1)));
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "c2", 1000))
                .thenReturn(page(3, null, DOI_REPO.get(2)));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        for (DataciteHarvesterParams.DOIValue doi : DOI_REPO.subList(0, 3)) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(doi.getFull()),
                    argThat(dto -> dto.getAuthority().equals(doi.getAuthority()) && dto.getIdentifier().equals(doi.getId())));
        }
        assertThat(result.getNumHarvested()).isEqualTo(3);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__query_maxRecords() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 2))
                .thenReturn(page(5, "c2", DOI_REPO.get(0), DOI_REPO.get(1)));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);
        params.setMaxRecords(2);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c2"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(2);
    }

    @Test
    public void harvest__query_defaultMaxRecords() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        DataciteHarvesterParams.DOIValue[] fullPage = new DataciteHarvesterParams.DOIValue[1000];
        for (int i = 0; i < fullPage.length; i++) {
            fullPage[i] = newDOI("10.5072/record-" + i);
        }
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000)).thenReturn(page(80000000, "c2", fullPage));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c2"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(1000);
    }

    @Test
    public void harvest__query_noMatches() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "XX");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000)).thenReturn(page(0, null));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        verify(importService, never()).doImportHarvestedDataset(any(), any(), anyString(), any(DatasetDTO.class));
        assertThat(result.getNumHarvested()).isEqualTo(0);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__query_failedRecord() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenReturn(page(2, null, DOI_REPO.get(0), DOI_REPO.get(1)));
        doThrow(new ImportException("Boom!")).when(importService)
                .doImportHarvestedDataset(any(), any(), eq(DOI_REPO.get(0).getFull()), any(DatasetDTO.class));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        assertThat(result.getNumHarvested()).isEqualTo(1);
        assertThat(result.getNumFailed()).isEqualTo(1);
    }

    @Test
    public void harvest__query_searchFails() throws IOException {
        // given
        Map<String, String> query = singletonMap("query", "creators.affiliation.name:(\"AGH");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenThrow(new IOException("status code: 400, reason phrase: Bad Request Lexical error"));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when & then
        assertThatThrownBy(() ->
                dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("Lexical error");
    }

    @Test
    public void harvest__query_ownResourceType() throws ImportException, IOException {
        // given
        Map<String, String> query = new LinkedHashMap<>();
        query.put("affiliation-country", "PL");
        query.put("resource-type-id", "software");
        when(dataCiteRestApiClient.searchDois(query, "1", 1000))
                .thenReturn(page(1, null, DOI_REPO.get(0)));
        DataciteHarvesterParams params = new DataciteHarvesterParams();
        params.setQueryParams(query);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"), params);

        // then
        assertThat(result.getNumHarvested()).isEqualTo(1);
    }

    private static DataCiteDoisPage page(long total, String nextCursor, DataciteHarvesterParams.DOIValue... dois) {
        DataCiteDoisPage page = new DataCiteDoisPage();
        page.getMeta().setTotal(total);
        for (DataciteHarvesterParams.DOIValue doi : dois) {
            page.getData().add(newResponse(doi));
        }
        if (nextCursor != null) {
            page.getLinks().setNext("https://api.datacite.org/dois?page%5Bcursor%5D=" + nextCursor + "&page%5Bsize%5D=2");
        }
        return page;
    }

    private static Map<String, String> datasetsOnly(Map<String, String> query) {
        Map<String, String> search = new LinkedHashMap<>(query);
        search.put("resource-type-id", "dataset");
        return search;
    }

    private DataverseRequest dataverseRequest() {
        return new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.2"));
    }

    private DataciteHarvesterParams.DOIValue newDOI(String doi) {
        return new DataciteHarvesterParams.DOIValue(doi);
    }

    private static DataCiteFindDoiResponse newResponse(DataciteHarvesterParams.DOIValue doi) {
        DataCiteFindDoiResponse response = new DataCiteFindDoiResponse();
        response.setId(RandomStringUtils.random(5));
        response.getAttributes().setCitationCount(3);
        response.getAttributes().setDoi(doi.getFull());
        response.getAttributes().setContributors(emptyList());
        return response;
    }
}
