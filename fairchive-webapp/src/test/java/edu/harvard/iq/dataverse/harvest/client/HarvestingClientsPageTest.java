package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.arquillian.facesmock.FacesContextMocker;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientsPage.PageMode;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(oaiHandler.listMetadataFormats())
                .thenReturn(singletonList(new MetadataFormat().withMetadataPrefix("oai_dc")));
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
        page.editClient(harvestingClient(null));
        when(oaiHandler.listSets()).thenReturn(emptyList());
        page.validateServerUrlOAI();

        // when
        boolean displayed = page.displayNoOaiSetsHelpText();

        // then
        assertThat(displayed).isFalse();
    }

    // -------------------- PRIVATE --------------------

    private HarvestingClient harvestingClient(String harvestingSet) {
        HarvestingClient harvestingClient = new HarvestingClient();
        harvestingClient.setName("client");
        harvestingClient.setHarvestingUrl(SERVER_URL);
        harvestingClient.setHarvestingSet(harvestingSet);
        harvestingClient.setMetadataPrefix("oai_dc");
        return harvestingClient;
    }
}
