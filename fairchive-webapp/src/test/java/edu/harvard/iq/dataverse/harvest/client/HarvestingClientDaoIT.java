package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClientRepository;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Transactional(TransactionMode.ROLLBACK)
public class HarvestingClientDaoIT extends WebappArquillianDeployment {

    @Inject
    private HarvestingClientDao harvestingClientDao;

    @Inject
    private DataverseDao dataverseDao;

    @Inject
    private HarvestingClientRepository harvestingClientRepository;

    // -------------------- TESTS --------------------

    @Test
    public void getAllHarvestingClients__everyHarvestType() {
        // given
        harvestingClientRepository.save(givenDataciteClient());

        // when
        List<HarvestingClient> clients = harvestingClientDao.getAllHarvestingClients();

        // then
        assertThat(clients).extracting(HarvestingClient::getName, HarvestingClient::getHarvestType)
                .containsExactly(
                        tuple("datacite_client", HarvestType.DATACITE_DOI),
                        tuple("harvest_client_name", HarvestType.OAI));
    }

    @Test
    public void getNumberOfHarvestedDatasetByClients__harvestedDatasetsOnly() {
        // given
        List<HarvestingClient> clients = singletonList(harvestingClientRepository.getById(12L));

        // when
        Long count = harvestingClientDao.getNumberOfHarvestedDatasetByClients(clients);

        // then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    public void getNumberOfHarvestedDatasetByClients__clientWithoutCollection() {
        // given
        HarvestingClient clientWithoutCollection = givenDataciteClient();
        clientWithoutCollection.setDataverse(null);
        harvestingClientRepository.save(clientWithoutCollection);
        List<HarvestingClient> clients = harvestingClientDao.getAllHarvestingClients();

        // when
        Long count = harvestingClientDao.getNumberOfHarvestedDatasetByClients(clients);

        // then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    public void getNumberOfHarvestedDatasetByClients__noClients() {
        // when
        Long count = harvestingClientDao.getNumberOfHarvestedDatasetByClients(emptyList());

        // then
        assertThat(count).isEqualTo(0L);
    }

    // -------------------- PRIVATE --------------------

    private HarvestingClient givenDataciteClient() {
        HarvestingClient client = new HarvestingClient();
        client.setName("datacite_client");
        client.setHarvestType(HarvestType.DATACITE_DOI);
        client.setDataverse(dataverseDao.findByAlias("ownmetadatablocks"));
        client.setHarvesterConfig("{\"dois\":[\"10.5072/a\"]}");
        return client;
    }
}
