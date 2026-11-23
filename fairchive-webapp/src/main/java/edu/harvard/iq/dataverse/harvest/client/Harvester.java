package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import java.util.logging.Logger;

/**
 * Harvester implementations of a harvest type.
 */
public interface Harvester {

    /**
     * What type of harvest the concrete harvester is handling.
     */
    HarvestType harvestType();

    /**
     * Perform the harvest using the configuration stored with the client.
     */
    HarvesterResult harvest(DataverseRequest dataverseRequest,
            HarvestingClient harvestingClient, Logger hdLogger)
            throws ImportException;
}
