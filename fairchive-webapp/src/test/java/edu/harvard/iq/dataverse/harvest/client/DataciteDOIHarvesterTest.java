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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
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

    private final List<DataciteHarvesterConfig.DOIValue> DOI_REPO = ImmutableList.of(
            newDOI("10.5438/0012"),
            newDOI("10.18150/0013"),
            newDOI("10.18150/0014"),
            newDOI("10.18150/0015"),
            newDOI("10.18150/0016"));

    @Test
    public void harvest__import() throws ImportException, IOException {
        // given
        for (DataciteHarvesterConfig.DOIValue doi : DOI_REPO) {
            when(dataCiteRestApiClient.findDoi(doi.getAuthority(), doi.getId())).thenReturn(newResponse(doi));
        }
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setDois(fullDois(DOI_REPO));
        mockHarvesterConfig(config);
        DataverseRequest dataverseRequest = dataverseRequest();

       // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest, harvestingClient, Logger.getLogger("test"));

        // then
        for (DataciteHarvesterConfig.DOIValue doi : DOI_REPO) {
            verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                    eq(doi.getFull()),
                    argThat(dto -> dto.getAuthority().equals(doi.getAuthority()) && dto.getIdentifier().equals(doi.getId())));
        }
        assertThat(result.getNumHarvested()).isEqualTo(5);
        assertThat(result.getNumDeleted()).isEqualTo(0);
        assertThat(result.getNumFailed()).isEqualTo(0);
    }

    @Test
    public void harvest__failed() throws ImportException, IOException {
        // given
        DataciteHarvesterConfig.DOIValue imported = DOI_REPO.get(0);
        DataciteHarvesterConfig.DOIValue failed = DOI_REPO.get(1);
        when(dataCiteRestApiClient.findDoi(imported.getAuthority(), imported.getId())).thenReturn(newResponse(imported));
        when(dataCiteRestApiClient.findDoi(failed.getAuthority(), failed.getId())).thenThrow(new IOException("Boom!"));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setDois(asList(imported.getFull(), failed.getFull()));
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        verify(importService, times(1)).doImportHarvestedDataset(any(), any(),
                eq(imported.getFull()),
                argThat(dto -> dto.getAuthority().equals(imported.getAuthority()) && dto.getIdentifier().equals(imported.getId())));
        assertThat(result.getNumHarvested()).isEqualTo(1);
        assertThat(result.getNumFailed()).isEqualTo(1);
    }

    @Test
    public void harvest__noConfig() {
        // given
        when(harvestingClient.getName()).thenReturn("datacite");

        // when & then
        assertThatThrownBy(() -> dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test")))
                .isInstanceOf(ImportException.class)
                .hasMessage("Invalid DataCite configuration of client datacite: Missing DataCite harvester configuration");
    }

    @Test
    public void harvest__invalidConfig() {
        // given
        when(harvestingClient.getName()).thenReturn("datacite");
        mockHarvesterConfig(new DataciteHarvesterConfig());

        // when & then
        assertThatThrownBy(() -> dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test")))
                .isInstanceOf(ImportException.class)
                .hasMessage("Invalid DataCite configuration of client datacite: Missing DOI's or query parameters");
    }

    @Test
    public void harvest__query() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenReturn(page(3, "c2", DOI_REPO.get(0), DOI_REPO.get(1)));
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "c2", 1000))
                .thenReturn(page(3, null, DOI_REPO.get(2)));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        for (DataciteHarvesterConfig.DOIValue doi : DOI_REPO.subList(0, 3)) {
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
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        config.setMaxRecords(2);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c2"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(2);
    }

    @Test
    public void harvest__query_defaultMaxRecords() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000)).thenReturn(page(80000000, "c2", givenDois(0, 1000)));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c2"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(1000);
    }

    @Test
    public void harvest__query_maxRecordsWithinLaterPage() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenReturn(page(5000, "c2", givenDois(0, 1000)));
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "c2", 1000))
                .thenReturn(page(5000, "c3", givenDois(1000, 1000)));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        config.setMaxRecords(1500);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        verify(importService, never()).doImportHarvestedDataset(any(), any(), eq("10.5072/record-1500"), any(DatasetDTO.class));
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c3"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(1500);
    }

    @Test
    public void harvest__query_emptyPageWithNextCursor() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "PL");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000))
                .thenReturn(page(1, "c2", DOI_REPO.get(0)));
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "c2", 1000))
                .thenReturn(page(1, "c3"));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        verify(dataCiteRestApiClient, never()).searchDois(any(), eq("c3"), anyInt());
        assertThat(result.getNumHarvested()).isEqualTo(1);
    }

    @Test
    public void harvest__query_noMatches() throws ImportException, IOException {
        // given
        Map<String, String> query = singletonMap("affiliation-country", "XX");
        when(dataCiteRestApiClient.searchDois(datasetsOnly(query), "1", 1000)).thenReturn(page(0, null));
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

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
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

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
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when & then
        assertThatThrownBy(() ->
                dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test")))
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
        DataciteHarvesterConfig config = new DataciteHarvesterConfig();
        config.setQueryParams(query);
        mockHarvesterConfig(config);

        // when
        HarvesterResult result = dataciteDOIHarvester.harvest(dataverseRequest(), harvestingClient, Logger.getLogger("test"));

        // then
        assertThat(result.getNumHarvested()).isEqualTo(1);
    }

    private static DataCiteDoisPage page(long total, String nextCursor, DataciteHarvesterConfig.DOIValue... dois) {
        DataCiteDoisPage page = new DataCiteDoisPage();
        page.getMeta().setTotal(total);
        for (DataciteHarvesterConfig.DOIValue doi : dois) {
            page.getData().add(newResponse(doi));
        }
        if (nextCursor != null) {
            page.getLinks().setNext("https://api.datacite.org/dois?page%5Bcursor%5D=" + nextCursor + "&page%5Bsize%5D=2");
        }
        return page;
    }

    private DataciteHarvesterConfig.DOIValue[] givenDois(int first, int count) {
        DataciteHarvesterConfig.DOIValue[] dois = new DataciteHarvesterConfig.DOIValue[count];
        for (int i = 0; i < count; i++) {
            dois[i] = newDOI("10.5072/record-" + (first + i));
        }
        return dois;
    }

    private static Map<String, String> datasetsOnly(Map<String, String> query) {
        Map<String, String> search = new LinkedHashMap<>(query);
        search.put("resource-type-id", "dataset");
        return search;
    }

    private void mockHarvesterConfig(DataciteHarvesterConfig config) {
        when(harvestingClient.getHarvesterConfig()).thenReturn(config.toJson());
    }

    private static List<String> fullDois(List<DataciteHarvesterConfig.DOIValue> dois) {
        return dois.stream()
                .map(DataciteHarvesterConfig.DOIValue::getFull)
                .collect(toList());
    }

    private DataverseRequest dataverseRequest() {
        return new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.2"));
    }

    private DataciteHarvesterConfig.DOIValue newDOI(String doi) {
        return new DataciteHarvesterConfig.DOIValue(doi);
    }

    private static DataCiteFindDoiResponse newResponse(DataciteHarvesterConfig.DOIValue doi) {
        DataCiteFindDoiResponse response = new DataCiteFindDoiResponse();
        response.setId(RandomStringUtils.random(5));
        response.getAttributes().setCitationCount(3);
        response.getAttributes().setDoi(doi.getFull());
        response.getAttributes().setContributors(emptyList());
        return response;
    }
}
