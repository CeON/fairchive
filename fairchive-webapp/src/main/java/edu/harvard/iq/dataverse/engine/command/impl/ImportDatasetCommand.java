package edu.harvard.iq.dataverse.engine.command.impl;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

/**
 * Imports a dataset from a different system. This command validates that the PID
 * of the new dataset exists, and then inserts the new dataset into the database.
 *
 * <b>NOTE:</b> At the moment, this command only supports a single version in the
 * dataset, and was tested with package files only.
 *
 * @author michael
 */
@SuppressWarnings("serial")
public class ImportDatasetCommand extends AbstractCreateDatasetCommand {

    private static final Logger logger = Logger.getLogger(ImportDatasetCommand.class.getName());

    /**
     * Creates a new instance of the command.
     *
     * @param theDataset The dataset we want to import.
     * @param aRequest   Request context for the command.
     */
    public ImportDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(theDataset, aRequest);
    }

    /**
     * Validate that the PID of the dataset, if any, exists.
     *
     * @param ctxt
     * @throws CommandException
     */
    @Override
    protected void additionalParameterTests(CommandContext ctxt)  {

        if (!getUser().isSuperuser()) {
            throw new PermissionException("ImportDatasetCommand can only be issued by a super-user.", this, emptySet(), getDataset());
        }

        Dataset ds = getDataset();

        if (isEmpty(ds.getIdentifier())) {
            throw new IllegalCommandException("Imported datasets must have a persistent global identifier.", this);
        }

        if (!ctxt.datasetService().isIdentifierLocallyUnique(ds)) {
            throw new IllegalCommandException("Persistent identifier " + 
                    ds.getGlobalId() + " already exists in this Dataverse installation.", this);
        }

        String url = ds.getPersistentURL();

        try (CloseableHttpClient client = HttpClients.createDefault()){
        	try(CloseableHttpResponse response = client.execute(new HttpGet(url))) {
        		int responseStatus = response.getStatusLine().getStatusCode();

	            if (responseStatus == 404) {
	                throw new CommandExecutionException("Provided PID does not exist. Status code for GET '" + url + "' is 404.", this);
	            }
        	}
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error while validating PID at '" + url + "' for an imported dataset: " + ex.getMessage(), ex);
            throw new CommandExecutionException("Cannot validate PID due to a connection error: " + ex.getMessage(), this);
        }

    }

    @Override
    protected void handlePid(Dataset theDataset, CommandContext ctxt) {
        theDataset.setGlobalIdCreateTime(getTimestamp());
    }

    @Override
    protected void postPersist(Dataset theDataset, CommandContext ctxt)  {
    }


}
