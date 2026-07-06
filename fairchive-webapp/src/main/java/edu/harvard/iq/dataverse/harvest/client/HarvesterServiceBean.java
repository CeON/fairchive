package edu.harvard.iq.dataverse.harvest.client;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.google.gson.Gson;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

/**
 * @author Leonid Andreev
 */
@Stateless(name = "harvesterService")
@ManagedBean
public class HarvesterServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    private HarvestingClientDao harvestingClientService;
    @EJB
    private OAIHarvester oaiHarvester;
    @EJB
    private DataciteDOIHarvester dataciteDOIHarvester;

    private final Map<HarvestType, Harvester<?>> harvesterMap = new HashMap<>();

    private static final Logger logger = getLogger(HarvesterServiceBean.class.getName());
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    public HarvesterServiceBean() {

    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void postConstruct() {
        this.harvesterMap.put(this.oaiHarvester.harvestType(), this.oaiHarvester);
        this.harvesterMap.put(this.dataciteDOIHarvester.harvestType(), this.dataciteDOIHarvester);
    }

    /**
     * Called to run an "On Demand" harvest.
     */
    @Asynchronous
    public void doAsyncHarvest(final DataverseRequest request, 
    		final HarvestingClient client, final HarvesterParams params) {

        try {
            doHarvest(request, client.getId(), params);
        } catch (final Exception e) {
            logger.info("Caught exception running an asynchronous harvest (dataverse \"" + 
            			client.getName() + "\")");
        }
    }

    /**
     * Run a harvest for an individual harvesting Dataverse
     *
     * @param request
     * @param harvestingClientId
     * @throws IOException
     */
    public <T extends HarvesterParams> void doHarvest(final DataverseRequest request, 
    		final Long harvestingClientId, final HarvesterParams params) 
    				throws IOException {
    	
        final HarvestingClient client = harvestingClientService.find(harvestingClientId);
        if (client == null) {
            throw new IOException("No such harvesting client: id=" + harvestingClientId);
        }

        final Dataverse harvestingDataverse = client.getDataverse();

        final String logTimestamp = logFormatter.format(new Date());
        Logger hdLogger = getLogger(HarvesterServiceBean.class.getName() + '.' + 
        		harvestingDataverse.getAlias() + logTimestamp);
        final String logFileName = "../logs" + File.separator + "harvest_" + 
        		client.getName() + '_' + logTimestamp + ".log";
        final FileHandler fileHandler = new FileHandler(logFileName);
        fileHandler.setFormatter(new SimpleFormatter()); 
        hdLogger.setUseParentHandlers(false);
        hdLogger.addHandler(fileHandler);

        try {
            if (client.isHarvestingNow()) {
                hdLogger.log(SEVERE, "Cannot begin harvesting, Dataverse " + 
                		harvestingDataverse.getName() + " is currently being harvested.");

            } else {
                this.harvestingClientService.resetHarvestInProgress(harvestingClientId);
                this.harvestingClientService.setHarvestInProgress(harvestingClientId, new Date());

                final Harvester<T> harvester = resolveHarvester(client);
                final HarvesterResult result = harvester.harvest(request, 
                		client, hdLogger, params.getParams(harvester.getParamsClass()));

                this.harvestingClientService.setHarvestSuccess(harvestingClientId,
                		new Date(), result.getNumHarvested(), result.getNumFailed(), 
                		result.getNumDeleted());
                hdLogger.log(INFO, "COMPLETED HARVEST, server=" + client.getArchiveUrl() + 
                		", metadataPrefix=" + client.getMetadataPrefix());
                hdLogger.log(INFO, "Datasets created/updated: " + result.getNumHarvested() + 
                		", datasets deleted: " + result.getNumDeleted() + 
                		", datasets failed: " + result.getNumFailed());
            }
        } catch (Throwable e) {
            final String message = "Exception processing harvest, server= " + 
                    client.getHarvestingUrl() + ",format=" + 
                    client.getMetadataPrefix() + ' ' + e.getClass().getName();
            hdLogger.log(SEVERE, message, e);
            //logException(e, hdLogger);
            hdLogger.log(INFO, "HARVEST NOT COMPLETED DUE TO UNEXPECTED ERROR.");
            // TODO:
            // even though this harvesting run failed, we may have had successfully
            // processed some number of datasets, by the time the exception was thrown.
            // We should record that number too. And the number of the datasets that
            // had failed, that we may have counted.  -- L.A. 4.4
            this.harvestingClientService.setHarvestFailure(harvestingClientId, new Date());

        } finally {
            this.harvestingClientService.resetHarvestInProgress(harvestingClientId);
            fileHandler.close();
            hdLogger.removeHandler(fileHandler);
        }
    }

    public HarvesterParams parseParams(final HarvestingClient client, final String paramsJson) {
        return isBlank(paramsJson)
            ? HarvesterParams.empty()
            : new Gson().fromJson(paramsJson, resolveHarvester(client).getParamsClass());
    }

    // -------------------- PRIVATE --------------------

    @SuppressWarnings("unchecked")
    private <T extends HarvesterParams> Harvester<T> resolveHarvester(final HarvestingClient client) {
        final Harvester<?> harvester = this.harvesterMap.get(client.getHarvestType());
        if (harvester == null) {
            throw new IllegalStateException("Unsupported harvest type");
        }

        return (Harvester<T>) harvester;
    }
}
