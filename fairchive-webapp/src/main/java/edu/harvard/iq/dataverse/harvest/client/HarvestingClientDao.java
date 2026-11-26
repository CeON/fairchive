package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.persistence.harvest.ClientHarvestRun;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 * <p>
 * Dedicated service for managing Harvesting Client Configurations
 */
@SuppressWarnings("serial")
@Stateless
public class HarvestingClientDao implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvestingClinetServiceBean");

    public HarvestingClient find(Object pk) {
        return em.find(HarvestingClient.class, pk);
    }

    public HarvestingClient findByNickname(String nickName) {
        try {
            return em.createQuery("SELECT hc FROM HarvestingClient hc WHERE LOWER(hc.name)=:nickName", HarvestingClient.class)
                    .setParameter("nickName", nickName.toLowerCase())
                    .getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.fine("Unable to find a single harvesting client by nickname \"" + nickName + "\": " + ex);
            return null;
        }
    }

    /**
     * All harvesting clients, of every harvest type, ordered by nickname.
     */
    public List<HarvestingClient> getAllHarvestingClients() {
        try {
            return em.createQuery("SELECT object(c) FROM HarvestingClient AS c ORDER BY c.name", HarvestingClient.class)
                    .getResultList();
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while looking up configured Harvesting Clients: " + ex.getMessage());
        }
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void resetHarvestInProgress(Long hcId) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);
        harvestingClient.setHarvestingNow(false);

        // And if there is an unfinished RunResult object, we'll
        // just mark it as a failure:
        if (harvestingClient.getLastRun() != null
                && harvestingClient.getLastRun().isInProgress()) {
            harvestingClient.getLastRun().setFailed();
        }

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestInProgress(Long hcId, Date startTime) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);
        harvestingClient.setHarvestingNow(true);
        if (harvestingClient.getRunHistory() == null) {
            harvestingClient.setRunHistory(new ArrayList<>());
        }
        ClientHarvestRun currentRun = new ClientHarvestRun();
        currentRun.setHarvestingClient(harvestingClient);
        currentRun.setStartTime(startTime);
        currentRun.setInProgress();
        harvestingClient.getRunHistory().add(currentRun);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setDeleteInProgress(Long hcId) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient); // why are we doing this?
        harvestingClient.setDeleteInProgress(true);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestSuccess(Long hcId, Date currentTime, int harvestedCount, int failedCount, int deletedCount) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);

        ClientHarvestRun currentRun = harvestingClient.getLastRun();

        if (currentRun != null && currentRun.isInProgress()) {
            // TODO: what if there's no current run in progress? should we just
            // give up quietly, or should we make a noise of some kind? -- L.A. 4.4      

            currentRun.setSuccess();
            currentRun.setFinishTime(currentTime);
            currentRun.setHarvestedDatasetCount(new Long(harvestedCount));
            currentRun.setFailedDatasetCount(new Long(failedCount));
            currentRun.setDeletedDatasetCount(new Long(deletedCount));
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setHarvestFailure(Long hcId, Date currentTime) {
        HarvestingClient harvestingClient = em.find(HarvestingClient.class, hcId);
        if (harvestingClient == null) {
            return;
        }
        em.refresh(harvestingClient);

        ClientHarvestRun currentRun = harvestingClient.getLastRun();

        if (currentRun != null && currentRun.isInProgress()) {
            // TODO: what if there's no current run in progress? should we just
            // give up quietly, or should we make a noise of some kind? -- L.A. 4.4      

            currentRun.setFailed();
            currentRun.setFinishTime(currentTime);
        }
    }

    /**
     * Number of datasets harvested by any of the given clients. Counted
     * through the client a dataset was harvested from, so a client with no
     * destination collection is no obstacle, and a dataset created directly
     * in a collection a client harvests into is not counted.
     */
    public Long getNumberOfHarvestedDatasetByClients(List<HarvestingClient> clients) {
        if (clients.isEmpty()) {
            return 0L;
        }
        try {
            return em.createQuery("SELECT COUNT(d) FROM Dataset d WHERE d.harvestedFrom IN :clients", Long.class)
                    .setParameter("clients", clients)
                    .getSingleResult();
        } catch (Exception ex) {
            logger.info("Warning: exception trying to count harvested datasets by clients: " + ex.getMessage());
            return 0L;
        }
    }
}
