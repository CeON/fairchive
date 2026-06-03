package edu.harvard.iq.dataverse.harvest.client;

import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.dspace.xoai.serviceprovider.exceptions.HarvestException;

import edu.harvard.iq.dataverse.api.imports.HarvestImporterType;
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
    public HarvesterResult harvest(final DataverseRequest dataverseRequest, 
    		final HarvestingClient client, final Logger logger, 
    		final HarvesterParams.EmptyHarvesterParams params) throws ImportException {
 
        try {
            logBeginOaiHarvest(logger, client);
            final HarvesterResult result = new HarvesterResult();
            final OaiHandler handler = new OaiHandler(client)
                        .withFetchedMetadataFormat();
            handler.listIdentifiers().forEachRemaining( header -> {
                final String identifier = header.getIdentifier();
                logger.info("Processing identifier: ".concat(identifier));
                processRecord(result, dataverseRequest, logger, handler, identifier);
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
    private void processRecord(HarvesterResult result, DataverseRequest dataverseRequest, 
    		Logger hdLogger, OaiHandler oaiHandler, String identifier) {
        logGetRecord(hdLogger, oaiHandler, identifier);
        File tempFile = null;

        try {
            FastGetRecord record = oaiHandler.getRecord(identifier);
            String errMessage = record.getErrorMessage();
            if (errMessage != null) {
                throw new HarvestException("Error calling GetRecord - " + errMessage);
            }

            if (record.isDeleted()) {
                hdLogger.info("Deleting harvesting dataset for " + identifier + ", per the OAI server's instructions.");

                importService.doDeleteHarvestedDataset(dataverseRequest, oaiHandler.getHarvestingClient(), identifier);
                result.incrementDeleted();

            } else {
                hdLogger.info("Successfully retrieved GetRecord response.");
                HarvestImporterType importType = HarvestImporterType.resolve(oaiHandler.getMetadataFormat())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unsupported import metadata format: " + oaiHandler.getMetadataFormat().getMetadataPrefix()));

                tempFile = record.getMetadataFile();
                String metadataFileContents = new String(Files.readAllBytes(tempFile.toPath()));

                hdLogger.info("importing " + importType + ": " + tempFile.getAbsolutePath());
                importService.doImportHarvestedDataset(dataverseRequest,
                        oaiHandler.getHarvestingClient(),
                        identifier,
                        importType,
                        metadataFileContents);

                result.incrementHarvested();

                hdLogger.fine("Harvest Successful for identifier " + identifier);
                hdLogger.fine("Size of this record: " + record.getMetadataFile().length());
            }
        } catch (Throwable e) {
            result.incrementFailed();
            logGetRecordException(hdLogger, oaiHandler, identifier, e);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    private void logBeginOaiHarvest(final Logger hdLogger, final HarvestingClient client) {
        hdLogger.info("Started harvest: oaiUrl=" + client.getHarvestingUrl()
                + ",set=" + client.getHarvestingSet()
                + ",metadataPrefix=" + client.getMetadataPrefix()
                + ",from=" + client.getLastNonEmptyHarvestTime());
    }

    private void logCompletedOaiHarvest(final Logger logger, final HarvestingClient client) {
        logger.info("Completed harvest: oaiUrl=" + client.getHarvestingUrl()
                + ",set=" + client.getHarvestingSet()
                + ",metadataPrefix=" + client.getMetadataPrefix()
                + ",from=" + client.getLastNonEmptyHarvestTime());
    }

    private void logGetRecord(final Logger logger, final OaiHandler handler, 
    		final String identifier) {
        logger.fine("Calling GetRecord: oaiUrl ="  + handler.getBaseOaiUrl()
                + "?verb=GetRecord&identifier=" + identifier
                + "&metadataPrefix=" + handler.getMetadataPrefix());
    }

    private void logGetRecordException(final Logger logger,  
    		final OaiHandler handler, final String identifier, final Throwable e) {
        final String message = "Exception processing getRecord: oaiUrl=" + handler.getBaseOaiUrl()
                + ",identifier=" + identifier;
        logger.log(SEVERE, message, e);
    }
}
