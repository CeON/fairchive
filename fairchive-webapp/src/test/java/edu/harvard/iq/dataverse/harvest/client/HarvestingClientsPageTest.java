package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientsPage.PageMode;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestStyle;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.SelectItem;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HarvestingClientsPageTest {

    private static final String SERVER_URL = "https://oai.example.org/oai";

    @Mock
    private OaiHandler oaiHandler;

    private HarvestingClientsPage page;

    @BeforeEach
    public void setUp() throws Exception {
        page = new HarvestingClientsPage() {
            @Override
            OaiHandler createOaiHandler(String harvestingUrl) {
                return oaiHandler;
            }
        };
        page.setNewHarvestingUrl(SERVER_URL);
    }

    @AfterEach
    public void tearDown() {
        if (FacesContext.getCurrentInstance() != null) {
            FacesContext.getCurrentInstance().release();
        }
    }

    // -------------------- TESTS --------------------

    @Test
    public void validateServerUrlOAI__newClient() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        when(oaiHandler.listSets()).thenReturn(asList("setA", "setB"));

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).extracting(SelectItem::getValue)
                .containsExactly("setA", "setB");
    }

    @Test
    public void validateServerUrlOAI__editedClientWithSet() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient("setB"));
        when(oaiHandler.listSets()).thenReturn(asList("setA", "setB"));

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).extracting(SelectItem::getValue)
                .containsExactly("setA", "setB");
        assertThat(page.getNewOaiSet()).isEqualTo("setB");
    }

    @Test
    public void validateServerUrlOAI__editedClientWithoutSet() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient(null));
        when(oaiHandler.listSets()).thenReturn(asList("setA", "setB"));

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).extracting(SelectItem::getValue)
                .containsExactly("setA", "setB");
        assertThat(page.getNewOaiSet()).isEqualTo("none");
    }

    @Test
    public void validateServerUrlOAI__skipOaiSets() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        page.setSkipOaiSets(true);

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).isEmpty();
        verify(oaiHandler, never()).listSets();
    }

    @Test
    public void validateServerUrlOAI__skipOaiSets_editedClientWithSet() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient("storedSet"));
        page.setSkipOaiSets(true);

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).extracting(SelectItem::getValue)
                .containsExactly("storedSet");
        verify(oaiHandler, never()).listSets();
    }

    @Test
    public void validateServerUrlOAI__skipOaiSets_editedClientWithoutSet() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient(null));
        page.setSkipOaiSets(true);

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isTrue();
        assertThat(page.getOaiSetsSelectItems()).isEmpty();
        verify(oaiHandler, never()).listSets();
    }

    @Test
    public void validateServerUrlOAI__skipOaiSets_serverWithoutMetadataFormats() throws Exception {
        // given
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        page.setNewClientUrlInputField(mock(UIInput.class));
        page.setPageMode(PageMode.CREATE);
        page.setSkipOaiSets(true);
        when(oaiHandler.listMetadataFormats()).thenReturn(emptyList());

        // when
        boolean valid = page.validateServerUrlOAI();

        // then
        assertThat(valid).isFalse();
        assertThat(page.getOaiSetsSelectItems()).isNull();
        verify(oaiHandler, never()).listSets();
    }

    @Test
    public void displayOaiSetsHelpText__newClientOfServerWithSets() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        when(oaiHandler.listSets()).thenReturn(asList("setA", "setB"));
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayOaiSetsHelpText();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayOaiSetsHelpText__newClient_skipOaiSets() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        page.setSkipOaiSets(true);
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayOaiSetsHelpText();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayOaiSetsHelpText__editedClient() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient("setB"));
        when(oaiHandler.listSets()).thenReturn(asList("setA", "setB"));
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayOaiSetsHelpText();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayNoOaiSetsHelpText__newClientOfServerFailingToListSets() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        when(oaiHandler.listSets()).thenThrow(new OaiHandlerException("ListSets failed"));
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayNoOaiSetsHelpText();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayNoOaiSetsHelpText__newClient_skipOaiSets() throws Exception {
        // given
        mockMetadataFormats();
        page.setPageMode(PageMode.CREATE);
        page.setSkipOaiSets(true);
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayNoOaiSetsHelpText();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayNoOaiSetsHelpText__editedClient() throws Exception {
        // given
        mockMetadataFormats();
        page.editClient(harvestingClient(null));
        when(oaiHandler.listSets()).thenReturn(emptyList());
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayNoOaiSetsHelpText();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void getHarvestTypeLabel__datacite() {
        // given
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);

        // when
        String label = page.getHarvestTypeLabel(givenClientOfType(HarvestType.DATACITE_DOI));

        // then
        assertThat(label).isEqualTo("DataCite REST API");
    }

    @Test
    public void getHarvestTypeLabel__oai() {
        // given
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);

        // when
        String label = page.getHarvestTypeLabel(givenClientOfType(HarvestType.OAI));

        // then
        assertThat(label).isEqualTo("OAI-PMH");
    }

    @Test
    public void displayHarvestingUrl__oaiClient() {
        // when
        boolean displayed = page.displayHarvestingUrl(givenClientOfType(HarvestType.OAI));

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayHarvestingUrl__dataciteClient() {
        // when
        boolean displayed = page.displayHarvestingUrl(givenClientOfType(HarvestType.DATACITE_DOI));

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void disableEditButton__idleOaiClient() {
        // when
        boolean disabled = page.disableEditButton(givenClientOfType(HarvestType.OAI));

        // then
        assertThat(disabled).isFalse();
    }

    @Test
    public void disableEditButton__oaiClientBeingHarvested() {
        // given
        HarvestingClient client = givenClientOfType(HarvestType.OAI);
        client.setHarvestingNow(true);

        // when
        boolean disabled = page.disableEditButton(client);

        // then
        assertThat(disabled).isTrue();
    }

    @Test
    public void disableEditButton__oaiClientBeingDeleted() {
        // given
        HarvestingClient client = givenClientOfType(HarvestType.OAI);
        client.setDeleteInProgress(true);

        // when
        boolean disabled = page.disableEditButton(client);

        // then
        assertThat(disabled).isTrue();
    }

    @Test
    public void disableEditButton__idleDataciteClient() {
        // when
        boolean disabled = page.disableEditButton(givenClientOfType(HarvestType.DATACITE_DOI));

        // then
        assertThat(disabled).isFalse();
    }

    @Test
    public void initNewDataciteClient__afterEditingClient() {
        // given
        page.editClient(givenDataciteClient());

        // when
        page.initNewDataciteClient(null);

        // then
        assertThat(page.isCreateMode()).isTrue();
        assertThat(page.isHarvestTypeDatacite()).isTrue();
        assertThat(page.getDataciteConfigForm().getQuery()).isNull();
    }

    @Test
    public void initNewClient__afterDataciteClient() {
        // given
        page.initNewDataciteClient(null);

        // when
        page.initNewClient(null);

        // then
        assertThat(page.isHarvestTypeOAI()).isTrue();
    }

    @Test
    public void editClient__dataciteClient() {
        // when
        page.editClient(givenDataciteClient());

        // then
        assertThat(page.isEditMode()).isTrue();
        assertThat(page.isHarvestTypeDatacite()).isTrue();
        assertThat(page.getDataciteConfigForm().getQuery()).isEqualTo("publicationYear:2023");
    }

    @Test
    public void validateInitialSettings__newDataciteClient() throws Exception {
        // given
        page.initNewDataciteClient(null);
        page.setNewNickname("datacite");
        page.setSelectedDestinationDataverse(new Dataverse());
        page.harvestingClientService = mock(HarvestingClientDao.class);

        // when
        page.validateInitialSettings();

        // then
        assertThat(page.isCreateStepTwo()).isTrue();
        assertThat(page.isInitialSettingsValidated()).isTrue();
        verifyNoInteractions(oaiHandler);
    }

    @Test
    public void validateInitialSettings__editedDataciteClient() throws Exception {
        // given
        page.editClient(givenDataciteClient());

        // when
        page.validateInitialSettings();

        // then
        assertThat(page.isCreateStepTwo()).isTrue();
        verifyNoInteractions(oaiHandler);
    }

    @Test
    public void validateDataciteConfig__valid() {
        // given
        page.initNewDataciteClient(null);
        page.getDataciteConfigForm().setQuery("publicationYear:2023");

        // when
        page.validateDataciteConfig();

        // then
        assertThat(page.isCreateStepThree()).isTrue();
    }

    @Test
    public void validateDataciteConfig__invalid() {
        // given
        FacesContext facesContext = mockEnglishFacesContext();
        page.initNewDataciteClient(null);
        page.backToStepTwo();
        page.getDataciteConfigForm().setAdditionalParams("affiliation-country PL");

        // when
        page.validateDataciteConfig();

        // then
        assertThat(page.isCreateStepTwo()).isTrue();
        ArgumentCaptor<FacesMessage> message = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), message.capture());
        assertThat(message.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(message.getValue().getSummary()).isEqualTo("Invalid DataCite configuration:");
        assertThat(message.getValue().getDetail())
                .isEqualTo("Additional parameter must be given as name=value: affiliation-country PL");
    }

    @Test
    public void createClient__dataciteClient() throws Exception {
        // given
        mockEnglishFacesContext();
        HarvestingClientsService harvestingClientsService = mockCreatedHarvestingClient();
        page.initNewDataciteClient(null);
        page.setNewNickname("datacite");
        page.setSelectedDestinationDataverse(new Dataverse());
        page.getDataciteConfigForm().setQuery("publicationYear:2023");
        page.getDataciteConfigForm().setDois("10.5072/ABC");

        // when
        page.createClient(null);

        // then
        HarvestingClient created = captureCreatedClient(harvestingClientsService);
        assertThat(created.getName()).isEqualTo("datacite");
        assertThat(created.getHarvestType()).isEqualTo(HarvestType.DATACITE_DOI);
        assertThat(created.getHarvestingUrl()).isNull();
        assertThat(created.getArchiveUrl()).isEqualTo("https://doi.org");
        assertThat(created.getMetadataPrefix()).isNull();
        assertThat(created.getHarvestStyle()).isEqualTo(HarvestStyle.DOI);
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(created.getHarvesterConfig());
        assertThat(config.getQueryParams()).containsExactly(entry("query", "publicationYear:2023"));
        assertThat(config.getDois()).containsExactly("10.5072/ABC");
    }

    @Test
    public void createClient__oaiClient() throws Exception {
        // given
        mockEnglishFacesContext();
        HarvestingClientsService harvestingClientsService = mockCreatedHarvestingClient();
        page.initNewClient(null);
        page.setNewNickname("oai");
        page.setNewHarvestingUrl(SERVER_URL);
        page.setSelectedDestinationDataverse(new Dataverse());
        page.setNewOaiSet("setA");
        page.setNewMetadataFormat("oai_dc");

        // when
        page.createClient(null);

        // then
        HarvestingClient created = captureCreatedClient(harvestingClientsService);
        assertThat(created.getHarvestType()).isEqualTo(HarvestType.OAI);
        assertThat(created.getHarvestingUrl()).isEqualTo(SERVER_URL);
        assertThat(created.getArchiveUrl()).isEqualTo("https://oai.example.org");
        assertThat(created.getHarvestingSet()).isEqualTo("setA");
        assertThat(created.getMetadataPrefix()).isEqualTo("oai_dc");
        assertThat(created.getHarvestStyle()).isEqualTo(HarvestStyle.DATAVERSE);
        assertThat(created.getHarvesterConfig()).isNull();
    }

    @Test
    public void createClient__weeklySchedule() throws Exception {
        // given
        mockEnglishFacesContext();
        HarvestingClientsService harvestingClientsService = mockCreatedHarvestingClient();
        page.initNewDataciteClient(null);
        page.setNewNickname("datacite");
        page.setSelectedDestinationDataverse(new Dataverse());
        page.getDataciteConfigForm().setQuery("publicationYear:2023");
        page.setHarvestingScheduleRadio(2);
        page.setNewHarvestingScheduleDayOfWeek("Monday");
        page.setNewHarvestingScheduleTimeOfDay("3");
        page.setHarvestingScheduleRadioAMPM(1);

        // when
        page.createClient(null);

        // then
        HarvestingClient created = captureCreatedClient(harvestingClientsService);
        assertThat(created.isScheduled()).isTrue();
        assertThat(created.getSchedulePeriod()).isEqualTo(HarvestingClient.SCHEDULE_PERIOD_WEEKLY);
        assertThat(created.getScheduleDayOfWeek()).isEqualTo(1);
        assertThat(created.getScheduleHourOfDay()).isEqualTo(15);
    }

    @Test
    public void saveClient__dataciteClient() throws Exception {
        // given
        mockEnglishFacesContextWithFlash();
        HarvestingClientsService harvestingClientsService = mockUpdatedHarvestingClient();
        HarvestingClient client = givenDataciteClient();
        page.editClient(client);
        page.getDataciteConfigForm().setQuery("publicationYear:2024");

        // when
        page.saveClient(null);

        // then
        verify(harvestingClientsService).updateHarvestingClient(client);
        DataciteHarvesterConfig config = DataciteHarvesterConfig.fromJson(client.getHarvesterConfig());
        assertThat(config.getQueryParams()).containsExactly(
                entry("query", "publicationYear:2024"), entry("affiliation-country", "PL"));
        assertThat(client.getHarvestingUrl()).isEqualTo("https://api.datacite.org");
        assertThat(client.getHarvestingSet()).isNull();
        assertThat(client.getHarvestStyle()).isEqualTo(HarvestStyle.DOI);
    }

    @Test
    public void saveClient__unscheduled() throws Exception {
        // given
        mockEnglishFacesContextWithFlash();
        mockUpdatedHarvestingClient();
        HarvestingClient client = givenDataciteClient();
        client.setScheduled(true);
        client.setSchedulePeriod(HarvestingClient.SCHEDULE_PERIOD_DAILY);
        client.setScheduleHourOfDay(1);
        page.editClient(client);
        page.setHarvestingScheduleRadio(0);

        // when
        page.saveClient(null);

        // then
        assertThat(client.isScheduled()).isFalse();
        verify(page.dataverseTimerService).removeHarvestTimer(client);
    }

    @Test
    public void getNewHarvestTypeLabel__datacite() {
        // given
        mockEnglishFacesContext();
        page.initNewDataciteClient(null);

        // when
        String label = page.getNewHarvestTypeLabel();

        // then
        assertThat(label).isEqualTo("DataCite REST API");
    }

    @Test
    public void getClientDialogTitle__newDataciteClient() {
        // given
        mockEnglishFacesContext();
        page.initNewDataciteClient(null);

        // when
        String title = page.getClientDialogTitle();

        // then
        assertThat(title).isEqualTo("Create DataCite REST API Harvesting Client");
    }

    @Test
    public void getClientDialogTitle__editedDataciteClient() {
        // given
        mockEnglishFacesContext();
        page.editClient(givenDataciteClient());

        // when
        String title = page.getClientDialogTitle();

        // then
        assertThat(title).isEqualTo("Edit DataCite REST API Harvesting Client");
    }

    @Test
    public void getClientDialogTitle__newOaiClient() {
        // given
        mockEnglishFacesContext();
        page.initNewClient(null);

        // when
        String title = page.getClientDialogTitle();

        // then
        assertThat(title).isEqualTo("Create Harvesting Client");
    }

    @Test
    public void getClientDialogTitle__editedOaiClient() {
        // given
        mockEnglishFacesContext();
        page.editClient(harvestingClient("setA"));

        // when
        String title = page.getClientDialogTitle();

        // then
        assertThat(title).isEqualTo("Edit Harvesting Client");
    }

    @Test
    public void getStepLegend__dataciteClient() {
        // given
        mockEnglishFacesContext();
        page.initNewDataciteClient(null);

        // when
        String legend = page.getStepLegend(2);

        // then
        assertThat(legend).isEqualTo("Step 2 of 3 - Query Configuration");
    }

    @Test
    public void getStepLegend__oaiClient() {
        // given
        mockEnglishFacesContext();
        page.initNewClient(null);

        // when
        String legend = page.getStepLegend(2);

        // then
        assertThat(legend).isEqualTo("Step 2 of 4 - Format");
    }

    @Test
    public void displayDataciteConfigStep__dataciteClientInStepTwo() {
        // given
        page.initNewDataciteClient(null);
        page.backToStepTwo();

        // when
        boolean displayed = page.displayDataciteConfigStep();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayDataciteConfigStep__oaiClientInStepTwo() {
        // given
        page.initNewClient(null);
        page.backToStepTwo();

        // when
        boolean displayed = page.displayDataciteConfigStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayDataciteConfigStep__dataciteClientInStepOne() {
        // given
        page.initNewDataciteClient(null);

        // when
        boolean displayed = page.displayDataciteConfigStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayScheduleStep__validatedClientInStepThree() {
        // given
        page.initNewDataciteClient(null);
        page.setInitialSettingsValidated(true);
        page.goToStepThree();

        // when
        boolean displayed = page.displayScheduleStep();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayScheduleStep__notValidatedClient() {
        // given
        page.initNewDataciteClient(null);
        page.goToStepThree();

        // when
        boolean displayed = page.displayScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayScheduleStep__validatedClientInStepTwo() {
        // given
        page.initNewDataciteClient(null);
        page.setInitialSettingsValidated(true);
        page.backToStepTwo();

        // when
        boolean displayed = page.displayScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayCreateButtonOnScheduleStep__newDataciteClient() {
        // given
        page.initNewDataciteClient(null);

        // when
        boolean displayed = page.displayCreateButtonOnScheduleStep();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displayCreateButtonOnScheduleStep__newOaiClient() {
        // given
        page.initNewClient(null);

        // when
        boolean displayed = page.displayCreateButtonOnScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displayCreateButtonOnScheduleStep__editedDataciteClient() {
        // given
        page.editClient(givenDataciteClient());

        // when
        boolean displayed = page.displayCreateButtonOnScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displaySaveButtonOnScheduleStep__editedDataciteClient() {
        // given
        page.editClient(givenDataciteClient());

        // when
        boolean displayed = page.displaySaveButtonOnScheduleStep();

        // then
        assertThat(displayed).isTrue();
    }

    @Test
    public void displaySaveButtonOnScheduleStep__editedOaiClient() {
        // given
        page.editClient(harvestingClient("setA"));

        // when
        boolean displayed = page.displaySaveButtonOnScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    @Test
    public void displaySaveButtonOnScheduleStep__newDataciteClient() {
        // given
        page.initNewDataciteClient(null);

        // when
        boolean displayed = page.displaySaveButtonOnScheduleStep();

        // then
        assertThat(displayed).isFalse();
    }

    // -------------------- PRIVATE --------------------

    private void mockMetadataFormats() throws Exception {
        when(oaiHandler.listMetadataFormats())
                .thenReturn(singletonList(new MetadataFormat().withMetadataPrefix("oai_dc")));
    }

    private FacesContext mockEnglishFacesContext() {
        FacesContext facesContext = FacesContextMocker.mockContext();
        when(facesContext.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        return facesContext;
    }

    private void mockEnglishFacesContextWithFlash() {
        FacesContext facesContext = mockEnglishFacesContext();
        when(facesContext.getExternalContext().getFlash()).thenReturn(mock(Flash.class));
    }

    private HarvestingClientsService mockCreatedHarvestingClient() throws Exception {
        HarvestingClientsService harvestingClientsService = givenPageServices();
        when(harvestingClientsService.createHarvestingClient(any())).thenAnswer(returnsFirstArg());
        return harvestingClientsService;
    }

    private HarvestingClientsService mockUpdatedHarvestingClient() throws Exception {
        HarvestingClientsService harvestingClientsService = givenPageServices();
        when(harvestingClientsService.updateHarvestingClient(any())).thenAnswer(returnsFirstArg());
        return harvestingClientsService;
    }

    private HarvestingClientsService givenPageServices() throws Exception {
        HarvestingClientsService harvestingClientsService = mock(HarvestingClientsService.class);
        FieldUtils.writeField(page, "harvestingClientsService", harvestingClientsService, true);
        page.harvestingClientService = mock(HarvestingClientDao.class);
        page.dataverseTimerService = mock(DataverseTimerServiceBean.class);
        return harvestingClientsService;
    }

    private HarvestingClient captureCreatedClient(HarvestingClientsService harvestingClientsService) {
        ArgumentCaptor<HarvestingClient> created = ArgumentCaptor.forClass(HarvestingClient.class);
        verify(harvestingClientsService).createHarvestingClient(created.capture());
        return created.getValue();
    }

    private HarvestingClient givenDataciteClient() {
        HarvestingClient harvestingClient = givenClientOfType(HarvestType.DATACITE_DOI);
        harvestingClient.setName("datacite");
        harvestingClient.setHarvestingUrl("https://api.datacite.org");
        harvestingClient.setArchiveUrl("https://doi.org");
        harvestingClient.setHarvestStyle(HarvestStyle.DOI);
        harvestingClient.setHarvesterConfig("{\"dois\":[\"10.5072/ABC\"],"
                + "\"queryParams\":{\"query\":\"publicationYear:2023\",\"affiliation-country\":\"PL\"}}");
        return harvestingClient;
    }

    private HarvestingClient givenClientOfType(HarvestType harvestType) {
        HarvestingClient harvestingClient = new HarvestingClient();
        harvestingClient.setHarvestType(harvestType);
        return harvestingClient;
    }

    private HarvestingClient harvestingClient(String harvestingSet) {
        HarvestingClient harvestingClient = new HarvestingClient();
        harvestingClient.setName("client");
        harvestingClient.setHarvestingUrl(SERVER_URL);
        harvestingClient.setHarvestingSet(harvestingSet);
        harvestingClient.setMetadataPrefix("oai_dc");
        return harvestingClient;
    }
}
