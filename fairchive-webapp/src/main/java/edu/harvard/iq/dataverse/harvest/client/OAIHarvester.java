package edu.harvard.iq.dataverse.harvest.client;

import static java.util.logging.Level.SEVERE;

import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.dspace.xoai.serviceprovider.exceptions.HarvestException;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandler;
import edu.harvard.iq.dataverse.harvest.client.oai.OaiHandlerException;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

/**
 * Harvester for OAI clients.
 */
@Stateless
@LocalBean
public class OAIHarvester implements Harvester<HarvesterParams.EmptyHarvesterParams> {

    private ImportServiceBean importService;
    
    public OAIHarvester() {}
    
    @Inject
    public OAIHarvester(final ImportServiceBean importService) {
		this.importService = importService;
	}

    @Override
    public HarvestType harvestType() {
        return HarvestType.OAI;
    }

    @Override
    public Class<HarvesterParams.EmptyHarvesterParams> getParamsClass() {
        return HarvesterParams.EmptyHarvesterParams.class;
    }

    /**
     * @param client     the harvesting client object
     * @param hdLogger             custom logger (specific to this harvesting run)
     */
    @Override
    public HarvesterResult harvest(final DataverseRequest request, 
    		final HarvestingClient client, final Logger logger, 
    		final HarvesterParams.EmptyHarvesterParams params) throws ImportException {
 
        try {
            logBeginOaiHarvest(logger, client);
            final HarvesterResult result = new HarvesterResult();
            final OaiHandler handler = new OaiHandler(client)
                        .withFetchedMetadataFormat();
            handler.listIdentifiers().forEachRemaining( header -> {
                final String identifier = header.getIdentifier();
                logger.info("Processing ".concat(identifier));
                processRecord(result, request, logger, handler, identifier);
                logger.info("Total content processed so far: " + result.getNumHarvested());
            });
            logCompletedOaiHarvest(logger, client);
            return result;
        } catch (final OaiHandlerException e) {
        	logger.log(SEVERE, "Harvest '" + client.getName() + "' failed.", e);
            throw new ImportException(e);
        }
    }

    // -------------------- PRIVATE --------------------

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	private void processRecord(final HarvesterResult result, final DataverseRequest request,
			final Logger logger, final OaiHandler handler, final String identifier) {

		try (final FastGetRecord record = handler.getRecord(identifier)) {
			if (record.getErrorMessage() != null) {
				throw new HarvestException(record.getErrorMessage());
			}
			if (record.isDeleted()) {
				logger.info("Deleting harvesting dataset for ".concat(identifier));
				this.importService.doDeleteHarvestedDataset(request, 
						handler.getHarvestingClient(), identifier);
				result.incrementDeleted();
			} else {
				this.importService.doImportHarvestedDataset(request, 
						handler.getHarvestingClient(), identifier,
						handler.resolveImportType(), record.getContent());
				result.incrementHarvested();
			}
		} catch (final Throwable e) {
			result.incrementFailed();
	        logger.log(SEVERE, "Failed to process ".concat(identifier), e);
		}
	}

    private static void logBeginOaiHarvest(final Logger hdLogger, final HarvestingClient client) {
        hdLogger.info("Started harvest: oaiUrl=" + client.getHarvestingUrl()
                + ",set=" + client.getHarvestingSet()
                + ",metadataPrefix=" + client.getMetadataPrefix()
                + ",from=" + client.getLastNonEmptyHarvestTime());
    }

    private static void logCompletedOaiHarvest(final Logger logger, final HarvestingClient client) {
        logger.info("Completed harvest: oaiUrl=" + client.getHarvestingUrl()
                + ",set=" + client.getHarvestingSet()
                + ",metadataPrefix=" + client.getMetadataPrefix()
                + ",from=" + client.getLastNonEmptyHarvestTime());
    }
}
