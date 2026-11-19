package edu.harvard.iq.dataverse.globalid;

import com.github.tomakehurst.wiremock.WireMockServer;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataCiteRestApiClientTest {

    @InjectMocks
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Mock
    private SettingsServiceBean settingsService;
    
    private static WireMockServer wireMockServer;
    
    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }
    
    @BeforeEach
    public void beforeEach() throws IOException {
        wireMockServer.resetAll();
        dataCiteRestApiClient.postConstruct();
    }
    
    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    void findDoi() throws IOException {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_sample_response.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "0012");

        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount).isEqualTo(5);

        DataCiteFindDoiResponse.Attributes attributes = doiResponse.getAttributes();

        assertThat(attributes.getPrefix()).isEqualTo("10.5438");
        assertThat(attributes.getSuffix()).isEqualTo("0012");
        assertThat(attributes.getPublisher()).isEqualTo("DataCite");
        assertThat(attributes.getPublicationYear()).isEqualTo(2020);

        assertThat(attributes.getTitles()).hasSize(1);
        assertThat(attributes.getTitles().get(0).getTitle()).isEqualTo("Example");
        assertThat(attributes.getTitles().get(0).getTitleType()).isNull();
        assertThat(attributes.getTitles().get(0).getLang()).isNull();

        assertThat(attributes.getDates()).hasSize(0);
        assertThat(attributes.getTypes().getResourceTypeGeneral()).isEqualTo("Dataset");

        assertThat(attributes.getCreators()).hasSize(1);
        DataCiteFindDoiResponse.Creator creator = attributes.getCreators().get(0);
        assertThat(creator.getName()).isEqualTo("Example, Example");

        assertThat(attributes.getContributors()).hasSize(0);
        assertThat(attributes.getDescriptions()).hasSize(0);
        assertThat(attributes.getFundingReferences()).hasSize(0);
        assertThat(attributes.getRelatedIdentifiers()).hasSize(0);
    }

    @Test
    void findDoi__complete() throws IOException {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_complete.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "0012");

        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount).isEqualTo(1);

        DataCiteFindDoiResponse.Attributes attributes = doiResponse.getAttributes();

        assertThat(attributes.getPrefix()).isEqualTo("10.18150");
        assertThat(attributes.getSuffix()).isEqualTo("0013");
        assertThat(attributes.getPublisher()).isEqualTo("RepOD");
        assertThat(attributes.getPublicationYear()).isEqualTo(2023);

        assertThat(attributes.getTitles()).hasSize(1);
        assertThat(attributes.getTitles().get(0).getTitle())
                .isEqualTo("The influence of parental influences during breeding periods on the success of fishes, an example of the white shark. Season 2017_2");
        assertThat(attributes.getTitles().get(0).getTitleType()).isNull();
        assertThat(attributes.getTitles().get(0).getLang()).isNull();

        assertThat(attributes.getDates()).hasSize(1);
        assertThat(attributes.getDates().get(0).getDate()).isEqualTo("2023");
        assertThat(attributes.getDates().get(0).getDateType()).isEqualTo("Issued");

        assertThat(attributes.getTypes().getResourceTypeGeneral()).isEqualTo("Dataset");

        assertThat(attributes.getCreators()).hasSize(1);
        DataCiteFindDoiResponse.Creator creator = attributes.getCreators().get(0);
        assertThat(creator.getName()).isEqualTo("Tobka, Marcell");

        assertThat(attributes.getContributors()).hasSize(1);
        DataCiteFindDoiResponse.Contributor contributor = attributes.getContributors().get(0);
        assertThat(contributor.getName()).isEqualTo("Tobka, Macell");
        assertThat(contributor.getGivenName()).isEqualTo("Macell");
        assertThat(contributor.getFamilyName()).isEqualTo("Tobka");
        assertThat(contributor.getContributorType()).isEqualTo(DataCiteFindDoiResponse.ContributorType.ContactPerson);
        assertThat(contributor.getAffiliation()).hasSize(1);
        assertThat(contributor.getAffiliation().get(0))
                .isEqualTo("Department of Zoology, Faculty of Veterinary Medicine and Animal Science, Pozna\u0144 University of Life Sciences");
        assertThat(contributor.getNameIdentifiers()).hasSize(1);
        assertThat(contributor.getNameIdentifiers().get(0).getNameIdentifier())
                .isEqualTo("https://orcid.org/0000-0003-1511-074X");
        assertThat(contributor.getNameIdentifiers().get(0).getNameIdentifierScheme())
                .isEqualTo("ORCID");
        assertThat(contributor.getNameIdentifiers().get(0).getSchemeUri())
                .isEqualTo("https://orcid.org");

        assertThat(attributes.getDescriptions()).hasSize(1);
        assertThat(attributes.getDescriptions().get(0).getDescription())
                .isEqualTo("Data on weather conditions (temp., precipitation, wind speed), graphical data (photographs from UFV), " +
                        "movement data obtained by GPS-GSM tracking, graphical data (JPEG, MP4) from trail cameras from nets of the white shark, " +
                        "biometric data of white shark nestlings (xlsx, csv.)");
        assertThat(attributes.getDescriptions().get(0).getDescriptionType()).isEqualTo("Abstract");

        assertThat(attributes.getFundingReferences()).hasSize(1);
        assertThat(attributes.getFundingReferences().get(0).getAwardNumber()).isEqualTo("2016/23/D/NZ1/12810");
        assertThat(attributes.getFundingReferences().get(0).getAwardTitle()).isNull();
        assertThat(attributes.getFundingReferences().get(0).getFunderIdentifier()).isEqualTo("https://ror.org/000aaabbb");
        assertThat(attributes.getFundingReferences().get(0).getFunderIdentifierType()).isEqualTo("ROR");
        assertThat(attributes.getFundingReferences().get(0).getFunderName()).isEqualTo("Narodowe Centrum Nauki");

        assertThat(attributes.getRelatedIdentifiers()).hasSize(2);
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelationType()).isEqualTo("References");
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelatedIdentifier()).isEqualTo("10.18150/0014");
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelatedIdentifierType()).isEqualTo("DOI");
    }

    @Test
    void findDoi_doi_identifier_with_shoulder() throws IOException {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/FK2/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_sample_response.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "FK2/0012");
        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount)
            .isEqualTo(5);
    }
    
    @Test
    void findDoi_not_found() throws IOException {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse().withStatus(404)));

        // when & then
        assertThatThrownBy(() -> dataCiteRestApiClient.findDoi("10.5438", "0012"))
            .isInstanceOf(HttpResponseException.class)
            .hasMessageContaining("status code: 404");
    }

    @Test
    void searchDois() throws IOException {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlPathEqualTo("/dois"))
                .withQueryParam("affiliation-country", equalTo("PL"))
                .withQueryParam("query", equalTo("creators.affiliation.affiliationIdentifier:\"https://ror.org/00bas1c41\""))
                .withQueryParam("page%5Bsize%5D", equalTo("2"))
                .withQueryParam("page%5Bcursor%5D", equalTo("1"))
                .withQueryParam("disable-facets", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/search_dois_first_page.json"))));
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("affiliation-country", "PL");
        queryParams.put("query", "creators.affiliation.affiliationIdentifier:\"https://ror.org/00bas1c41\"");

        // when
        DataCiteDoisPage page = dataCiteRestApiClient.searchDois(queryParams, DataCiteRestApiClient.FIRST_CURSOR, 2);

        // then
        assertThat(page.getMeta().getTotal()).isEqualTo(3);
        assertThat(page.getData())
                .extracting(record -> record.getAttributes().getDoi())
                .containsExactly("10.5061/dryad.bf20k4q4", "10.5072/fk2/abc");
        assertThat(page.getData().get(0).getAttributes().getCreators().get(0).getAffiliation())
                .containsExactly("AGH University of Krakow");
    }

    @Test
    void searchDois__lastPage() throws IOException {
        // given
        mockRestApiUrlSetting();
        String cursor = "MTMxNjE4ODM0NDAwMCwxMC41MDYxL2RyeWFkLmJmMjBrNHE0";
        wireMockServer.stubFor(get(urlPathEqualTo("/dois"))
                .withQueryParam("affiliation-country", equalTo("PL"))
                .withQueryParam("page%5Bcursor%5D", equalTo(cursor))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/search_dois_last_page.json"))));

        // when
        DataCiteDoisPage page = dataCiteRestApiClient.searchDois(singletonMap("affiliation-country", "PL"), cursor, 2);

        // then
        assertThat(page.getData())
                .extracting(record -> record.getAttributes().getDoi())
                .containsExactly("10.18150/0013");
        assertThat(DataCiteRestApiClient.nextCursor(page)).isEmpty();
    }

    @Test
    void searchDois__badRequest() {
        // given
        mockRestApiUrlSetting();
        wireMockServer.stubFor(get(urlPathEqualTo("/dois"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withStatusMessage("Bad Request")
                        .withBody("{\"errors\":[{\"status\":\"400\",\"title\":\"token_mgr_error: Lexical error\"}]}")));
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("query", "creators.affiliation.name:(\"AGH");

        // when & then
        assertThatThrownBy(() -> dataCiteRestApiClient.searchDois(queryParams, DataCiteRestApiClient.FIRST_CURSOR, 2))
                .isInstanceOf(HttpResponseException.class)
                .hasMessageContaining("status code: 400")
                .hasMessageContaining("Lexical error");
    }

    @Test
    void nextCursor() {
        // given
        DataCiteDoisPage page = new DataCiteDoisPage();
        page.getLinks().setNext("https://api.datacite.org/dois?affiliation-country=PL"
                + "&page%5Bcursor%5D=MTMxNjE4ODM0NDAwMCwxMC41MDYx&page%5Bsize%5D=2");

        // when
        String cursor = DataCiteRestApiClient.nextCursor(page).get();

        // then
        assertThat(cursor).isEqualTo("MTMxNjE4ODM0NDAwMCwxMC41MDYx");
    }

    @Test
    void nextCursor__lastPage() {
        // given
        DataCiteDoisPage page = new DataCiteDoisPage();

        // when & then
        assertThat(DataCiteRestApiClient.nextCursor(page)).isEmpty();
    }

    // -------------------- PRIVATE --------------------

    private void mockRestApiUrlSetting() {
        when(settingsService.getValueForKey(Key.DoiDataCiteRestApiUrl)).thenReturn("http://localhost:" + wireMockServer.port());
    }
}
