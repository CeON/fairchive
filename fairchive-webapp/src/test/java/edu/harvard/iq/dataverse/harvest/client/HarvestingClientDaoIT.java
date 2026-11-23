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
